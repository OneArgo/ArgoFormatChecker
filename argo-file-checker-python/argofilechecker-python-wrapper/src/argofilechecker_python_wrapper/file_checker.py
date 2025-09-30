
"""FileChecker python wrapper module."""

import logging
import os
import subprocess  # nosec B404
import tempfile
from pathlib import Path
from typing import Dict, List, Optional
from xml.etree.ElementTree import Element, ElementTree, ParseError  # nosec B405: this import is just for type.

import defusedxml.ElementTree as DefusedET

from .models import PhaseType, ResultType, ValidationResult

logger = logging.getLogger(__name__)

class FileChecker:
    """Python Wrapper for Argo Netcdf File Checker.

    File Checker will is run as subprocess so the location of the JAR file and specs must be provided as ENV variables 
    or explicitly in init.   
    The env vars are used in dockerfile : 
    The jar is included in file checker Docker image.
    The spec dir could be included in the image in a future version of File Checker.

    Example:
        # Explicit paths
        checker = FileChecker("D:/test_checker/file_checker_exec-2.9.3.jar", "D:/test_checker/file_checker_spec")
        
        # Using environment variables (Docker)
        checker = FileChecker()  # Uses env vars        

    """

    def __init__ (self,
                  jar_path : Optional[str] = None,
                  specs_path: Optional[str] = None):
        """Initialize the File Checker.

        Args:
            jar_path : Path to the File Checker .jar file.
                       If None -> use the FILE_CHECKER_JAR env var.
            specs_path : Path to the specification (file_checker_spec) directory.
                         If None -> use the FILE_CHECKER_SPECS env var.

        Raises:
            FileNotFoundError: If JAR or spec path don't exist
            ValueError: If paths not provided and env vars not set

        """
        jar_path = jar_path or os.getenv('FILE_CHECKER_JAR') 
        specs_path = specs_path or os.getenv('FILE_CHECKER_SPECS')

        # checks :
        if jar_path is None :
            logger.error("jar_path not provided and FILE_CHECKER_JAR environment variable not defined")
            raise ValueError
        else :
            self.jar_path = Path(jar_path)
        if specs_path is None :
            logger.error("jar_path not provided and FILE_CHECKER_JAR environment variable not defined")
            raise ValueError
        else :
            self.specs_path = Path(specs_path) 
         
        if not self.jar_path.exists() :
            logger.error(f"File checker .jar file not found: {self.jar_path}")
            raise FileNotFoundError
        if not self.specs_path.exists() :
            logger.error(f"Spec directory not found at : {self.specs_path}")
            raise FileNotFoundError


    def check_files (
            self, 
            files_paths_str: List[str], 
            dac_name: str, 
            options: list[str] = []
        ) -> List[ValidationResult]:
        """Check a list of files from a same dac using the File Checker.

        Args :
            file_paths (list[str]) : list of files to checks.
                                     ex : ['D:/data/file1.nc', 'D:/data/file2.nc', 'D:/other_data/file3.nc']
            dac_name (str) : dac name of the files : coriolis, bodc, aoml, etc. Must be the same DAC for all files.
            options (list[str]) : list of file checker options. ex: ['-no-name-check']

        Return : a list of ValidationResult objects containing the file checker results for each input files

        """
        if not files_paths_str:
            logger.error("file_paths list cannot be empty")
            raise ValueError
        
        #check if all files exist :
        existing_files, missing_files = self._get_valid_files_path(files_paths_str)
        if len(missing_files) > 0:
            warning_msg = f"{len(missing_files)} file(s) was/were not found"
            logger.warning(warning_msg)        
        if len(existing_files) == 0 :
            logger.error("No existing files were found")
            raise FileNotFoundError
        
        # as the file checker can process a list of files from a given directory,
        # we need to extract directories and files's names
        # and launch files checker on each directory :
        files_by_directory = self._get_filesnames_by_directory(existing_files)        

        # for each directory, run the file checker on the associate list of files names :
        validation_result_list = []
        for dir, files_names_list_in_dir in files_by_directory.items() :
            validation_result_list.extend(self._check_files_in_directory(dir,files_names_list_in_dir,dac_name,options))
        
        return validation_result_list

    def _check_files_in_directory (
            self, 
            input_directory: Path, 
            files_names: List[str], 
            dac_name: str, 
            options: list[str] 
        ) -> List[ValidationResult] :
        """Run the file checker on a given directory and given files names.

        Args :
            input_directory (Path) : the input directory Path
            files_names (List[str]): List of files names in the directory
            dac_name (str) : dac name of the files : coriolis, bodc, aoml, etc. Must be the same DAC for all files.
            options (List[str]) :  list of file checker options. ex: ['-no-name-check']
        
        Returns :
            A list of ValidationResult objects.

        """
        #need a temp directory for XML output
        with tempfile.TemporaryDirectory(prefix="filechecker_output_") as temp_output_dir:
            temp_output = Path(temp_output_dir)

            cmd = ["java",
                "-jar",
                str(self.jar_path),
                *options,
                dac_name,
                str(self.specs_path),
                str(temp_output),
                str(input_directory),
                *files_names]

            # launch subprocess :
            try:
                result = subprocess.run(cmd, capture_output=True, text=True, check=False, timeout=300, shell=False) # nosec B603: no shell, fixed executable path
                if (result.stderr) :
                    logger.error(result.stderr.strip())
                if (result.stdout) :
                    logger.info(result.stdout.strip())
                # parse xml results files
                return self._parse_batch_results(temp_output,files_names )
            
            except subprocess.TimeoutExpired :
                logger.exception("Timed out after 5 minutes")
                raise RuntimeError
            except Exception as e :
                logger.exception("Failed to execute batch file checker")
                raise RuntimeError from e

    def _parse_batch_results (self,temp_output_dir: Path, files_names: List[str]) -> List[ValidationResult] :
        """Parse XML results files in the temp_ouput directory.
        
        For each result file build a ValidationResult and add it to the return list.
        For each output file, parse the xml result and create a ValidationResult object and add it to the return list.
        
        Args :
            temp_output (Path) : tempory folder to save XML output
            input_directory (Path) : input directory path
            files_names (List[str]) : list of files name that have been processed

        return :
            A list of ValidationResult coming from the parsing of XML result files.
        
        """
        validation_results = []
        # get list of XML files :
        xml_files = list(temp_output_dir.glob("*.filecheck"))

        if xml_files :
            if len(xml_files) != len(files_names):
                logger.warning("Unexpected number of output files")

            for xml_file in xml_files:
                validation_result = self._parse_xml_output(xml_file) 
                if validation_result is not None :
                    validation_results.append(validation_result)
        else :
            logger.error("No results files were found")

        return validation_results



    def _parse_xml_output(self,xml_file:Path) -> ValidationResult | None :
        """Parse a XML result file and build a ValidationResult object.

        Args :
            xml_file (Path) : the xml file to be parsed

        Return :
            an ValidationResult object
        """
        try :
            # Parsing XML file using defusedxml
            tree:ElementTree[Element[str] | None] = DefusedET.parse(xml_file) # type: ignore[misc]     
            root:Element | None = tree.getroot()
            
            if root is None :
                logger.error("Empty XML document: %s", xml_file)
                return None


            def _extract_text (tag: str, default:str) -> str :
                elem = root.find(tag)
                if elem is not None and elem.text is not None:
                    return elem.text
                else :
                    return default

            # file  checker version
            file_checker_version = root.attrib["filechecker_version"]

            # processed file's name
            file = _extract_text("file","")

            # result
            result = _extract_text("status","")     
            try:
                result_enum = ResultType(result) if result else ResultType.ERROR
            except ValueError:
                result_enum = ResultType.ERROR

            # checking phase
            phase = _extract_text("phase","")
            try:
                phase_enum = PhaseType(phase) if result else PhaseType.FORMAT
            except ValueError:
                phase_enum = PhaseType.FORMAT       

            #errors
            errors_elem = root.find("errors")            
            if errors_elem is not None :
                errors_number = errors_elem.attrib["number"]
                errors_messages= list(errors_elem.itertext())                                
            else :
                errors_number = "0"
                errors_messages = []

            #warnings
            warnings_elem = root.find("warnings")            
            if warnings_elem is not None :
                warnings_number = warnings_elem.attrib["number"]
                warnings_messages= list(warnings_elem.itertext())                         
            else :
                warnings_number = "0"
                warnings_messages = []
            
            validation_result = ValidationResult(
                file_checker_version=file_checker_version, 
                file=str(file),result=result_enum, 
                phase=phase_enum, 
                errors_number=int(errors_number),
                warnings_number=int(warnings_number), 
                errors_messages=errors_messages,
                warnings_messages=warnings_messages)

        except ParseError:  
            logger.exception("Error when parsing file : " + str(xml_file))
            return None
        
        else:
            return validation_result

    def _get_valid_files_path (self, files_paths_str: List[str]) -> tuple[list[Path], list[str]]:
        """From a list of files, determinate for each file if it exist or not.

        Args : 
            files_paths_ str  (list[str]) : list of files

        Return :
            existing_files (List[Path]): list of existing files Path
            missing_files (List[str]) : list of input files that don't exist.
        """
        existing_files = []
        missing_files = []

        for file_path_str in files_paths_str :
            file_path = Path(file_path_str)
            if file_path.exists():
                existing_files.append(file_path)
            else :
                missing_files.append(file_path_str)

        return existing_files, missing_files


    def _get_filesnames_by_directory (self, files_path : List[Path]) -> Dict[Path, List[str]] :
        """Associate a source directory and list of files names from a list of files Path.

        Args :
            files_path (list[Path]) : a list of files'path
        
        Return : 
            Dict[Path, List[str]  : A dict with key : a directory's Path
            and value : a list of files's names present in the directory.
        """
        files_by_directory: Dict[Path, List[str]] = {}
        for file_path in files_path:
            parent_dir = file_path.parent
            if parent_dir not in files_by_directory:
                files_by_directory[parent_dir] = []
            files_by_directory[parent_dir].append(file_path.name)

        return files_by_directory