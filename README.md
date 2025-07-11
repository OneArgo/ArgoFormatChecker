# Argo NetCDF file format checker
The Argo NetCDF file format checker performs format and content checks on Argo NetCDF files.  
The Argo NetCDF format is described in "Argo user's manual" http://dx.doi.org/10.13155/29825  
More information on https://www.argodatamgt.org/Documentation  
The format checker has two directories:
- file_checker_exec : the java code
- file_checker_spec : the rules applied on the NetCDF file by the java code

The rules files implement the Argo vocabularies managed on [NVS vocabulary server](https://vocab.nerc.ac.uk/search_nvs/).  
Vocabularies and format changes are managed on [Argo Vocabs Task Team - AVTT GitHub](https://github.com/orgs/OneArgo/projects/4/views/1).

**With each release (from 2.9.2) you will find :**
- **file_checker_exec-[version].jar** which consolidates both application's compiled code and all its dependencies into a single executable file.
- source code 

## Run Argo NetCDF file format checker
- Using file_checker_exec-[version].jar :
```bash
java -jar file_checker_exec-[version].jar $OPTION $DAC_NAME $SPEC $OUTPUT_DIR $INPUT_DIR $FILE_NAME
```

Only this JAR file is needed. You can delete legacy log4j2 & netcdf libraries jar files.

## TOOLS

### Maven Wrapper

Thanks to the wrapper, Maven is embedded in the project (in a defined version). No need to install Maven on your development workstation, just use the wrapper script included in the project.

## Getting Started

- Clone the repository :

```bash
git clone https://github.com/OneArgo/ArgoFormatChecker.git
```

### building jar

- Build the application with maven (will requiert Java jdk installed), in file_checker_exec folder :


```bash
cd file_checker_exec
./mvnw clean install
```

In target folder you will find both original-file_checker_exec and file_checker_exec-[version]. It is this last one to use.


### Using docker


- Build the application with Docker :

```
docker build -t filechecker_2.8.14 .
```

- Run the application using Docker

```
docker run --rm -v [ABSOLUTE_PATH_TO_file_checker_spec]:/app/file_checker_spec -v [ABSOLUTE_PATH_TO_DATA_FOLDER]:/app/data -v [ABSOLUTE_PATH_TO_OUTPUT_DIR]:/app/results filechecker_2.8.14:latest $DAC_NAME ./file_checker_spec ./results ./data $FILE_NAME
```

### Run integration tests
The source code comes with some netcdf test files. You can run the integration tests with this following command :

```bash
./mvnw verify
```
