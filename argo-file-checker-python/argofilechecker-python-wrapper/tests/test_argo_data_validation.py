"""Validation tests on real floats data."""
import os
from ftplib import FTP
from pathlib import Path
from typing import List
from urllib.parse import urlparse

import pytest

from argofilechecker_python_wrapper import FileChecker
from argofilechecker_python_wrapper.models import ResultType, ValidationResult


@pytest.mark.e2e
@pytest.mark.parametrize ("float_number,dac_name", [("2903996","coriolis"), ("5900177", "bodc"), ("1900360", "aoml"), ("6990538", "coriolis"), ("4901791", "meds")]) # noqa: E501
def test_files_from_gdac_should_be_accepted(float_number, dac_name, tmp_path, specs_directory,file_checker_jar_file ):
    """Argo netcdf files from GDAC should be accepted."""
    #first download relevant files in temp directory :
    ftp_url = os.getenv("GDAC_FTP_HOST", "ftp://ftp.ifremer.fr/ifremer/argo/dac")
    dest_folder = tmp_path / float_number
    dest_folder.mkdir()
    _download_relevant_files_from_gdac_ftp(ftp_url, dac_name, float_number, dest_folder)
    
    # run file checker on each file and expect to have only accepted files
    # Files come from GDAC and should be accepted by file checker
    list_files = [f'{dest_folder}/{file}' for file in os.listdir(dest_folder)]
    file_checker= FileChecker(file_checker_jar_file, specs_directory)
   
    results:List[ValidationResult] = file_checker.check_files(list_files, dac_name)
    # check if all files have been processed
    assert len(list_files) == len (results)
    # check if all files are ACCEPTED (which is the expected result for GDAC files)
    failures = [r.file for r in results if r.result != ResultType.SUCCESS]
    assert not failures, f"REJECTED detected: {failures}"


@pytest.mark.e2e
def test_bad_argo_netcdf_should_be_rejected (specs_directory,file_checker_jar_file) :
    """Bad formatted argo netcdf should be rejected."""
    # files in local 'rejected_argo_data' folder :
    test_dir = Path(__file__).parent
    rejected_dir = test_dir / "rejected_argo_data"
    list_files = [str(rejected_dir/file) for file in os.listdir(rejected_dir)]
    # run file checker :
    file_checker = FileChecker(file_checker_jar_file, specs_directory)
    results:List[ValidationResult] = file_checker.check_files(list_files, "coriolis", ["-no-name-check"])
    # check if all files have been processed
    assert len(list_files) == len (results)
    # check if all files are REJECTED 
    success = [r.file for r in results if r.result == ResultType.SUCCESS]
    assert not success, f"ACCEPTED detected: {success}"
     

def _download_relevant_files_from_gdac_ftp (ftp_url : str, dac_name : str, float_number : str, dest : Path) :
    """Download from a ftp server the different files (tech, meta, traj, profiles) for a specified dac and float number.

    Args :
        ftp_url (str) : root directory where dac directories are found
        dac_name (str) 
        float_number (str)
    """
    # parse url to retrieve host and path
    float_ftp_url = f"{ftp_url}/{dac_name}/{float_number}"
    parsed_url = urlparse(float_ftp_url)
    hostname = parsed_url.hostname
    path = parsed_url.path    
   
    # connect to ftp
    if hostname is not None :
        ftp = FTP(hostname)
        ftp.login()
        # go to the right folder : 
        ftp.cwd(path)
        # download meta, traj and tech files : 
        meta_tech_traj_files_list = [f'{float_number}_Rtraj.nc', f'{float_number}_tech.nc', f'{float_number}_meta.nc']
        _download_files_from_ftp(ftp,meta_tech_traj_files_list, dest )
        
        # dowload profiles files :
        ftp.cwd('profiles')
        profiles_files_names = ftp.nlst()
        # exclude Synthetic profiles (first letters is 'S') which are not checked by fileChecker
        profiles_files_names_without_synth = [f for f in profiles_files_names if not f.startswith('S')]
        _download_files_from_ftp(ftp, profiles_files_names_without_synth, dest)

        ftp.quit()


def _download_files_from_ftp (ftp, files_names_list, dest) :
    """Download a list of files to a dest directory from a FTP connexion.

    Args :
        ftp (FTP) : FTP object (ftplib) connected to the right directory
        files_names_list (List[str]) : list of file names present in the ftp directory
        dest (Path) : Path of the directory to download files
    """
    for file_name in files_names_list :
            if file_name  in ftp.nlst() :
                with open (dest/file_name, 'wb') as fp :
                    ftp.retrbinary(f'RETR {file_name}', fp.write)





