# Argo NetCDF file format checker

The Argo NetCDF file format checker performs format and content checks on Argo NetCDF files.  
The Argo NetCDF format is described in "Argo user's manual" <http://dx.doi.org/10.13155/29825>  
More information on <https://www.argodatamgt.org/Documentation>  
The format checker has two directories:

- file_checker_exec : the java code
- file_checker_spec : the rules applied on the NetCDF file by the java code

The rules files implement the Argo vocabularies managed on [NVS vocabulary server](https://vocab.nerc.ac.uk/search_nvs/).  
Vocabularies and format changes are managed on [Argo Vocabs Task Team - AVTT GitHub](https://github.com/orgs/OneArgo/projects/4/views/1).

**With each release (from 2.9.2) you will find :**

- **file_checker_exec-[version].jar** which consolidates both application's compiled code and all its dependencies into a single executable file.
- source code

## Run Argo NetCDF file format checker

### Using file_checker_exec-{version}.jar :

```bash
java -jar file_checker_exec-{version}.jar $OPTION $DAC_NAME $SPEC $OUTPUT_DIR $INPUT_DIR [$FILES_NAMES]
```

$FILES_NAMES is a list of file's name from the INPUT_DIR. It is optional : without it, all files from INPUT_DIR will be checked.

### Run the application using Docker

```bash
docker run --rm -v [ABSOLUTE_PATH_TO_SPEC]:/app/file_checker_spec -v [ABSOLUTE_PATH_TO_DATA_FOLDER]:/app/data -v [ABSOLUTE_PATH_TO_OUTPUT_DIR]:/app/results ghcr.io/oneargo/argoformatchecker/app:{TAG} [$OPTIONS] $DAC_NAME ./file_checker_spec ./results ./data [$FILES_NAMES]
```

You need to mount external directories to the container :

[ABSOLUTE_PATH_TO_SPEC] : the file_checker_spec directory path.

[ABSOLUTE_PATH_TO_DATA_FOLDER] : Path to directory containing the argo necdf files to be checked. The fileChecker will not seek files in subfolders

[ABSOLUTE_PATH_TO_OUTPUT_DIR] : the directory where xml results files \*.filecheck will be created

Example :

```bash
docker run --rm -v D:\test_file_checker\file_checker_spec:/app/file_checker_spec -v D:\test_file_checker\datatest:/app/data -v D:\test_file_checker\results:/app/results ghcr.io/oneargo/argoformatchecker/app:develop  -no-name-check coriolis ./file_checker_spec ./results ./data
```

### Run the application using Docker Compose

To facilitate the use of Argo file checker a compose.yaml and .env files are provided :

- Prepare your data.
- Copy `.env.docs` as `.env` file, and customize variables to configure the file checker for your environment.
- Download compose.yaml
- Run te service using Docker Compose:

```bash
docker compose -f compose.yaml up
```

or in background :

```bash
docker compose -f compose.yaml up -d
```

Example of an .env file :

```text
# file checker image
FILECHECKER_IMAGE=ghcr.io/oneargo/argoformatchecker/app
FILECHECKER_IMAGE_TAG=develop

# External directories to mount to the container
FILECHECKER_SPEC_VOLUME='D:\test_compose\file_checker_spec'
FILECHECKER_INPUT_VOLUME='D:\test_compose\data'
FILECHECKER_OUTPUT_VOLUME='D:\test_compose\results'

# Variable specific to floats to check
DAC_NAME=bodc
FILECHECKER_OPTIONS=
FILES_NAMES=
```

### Run the application on demonstration files

Demonstration data are availables to run the application locally easily.

- Clone the repository :

```bash
git clone https://github.com/OneArgo/ArgoFormatChecker.git
```

- Run the script dedicated

```bash
./run-file-checker-linux.sh
```

output files will be generated in `./demo/outputs`.

### Test data

To test the Argo File Checker, you will find argo data here : https://www.argodatamgt.org/DataAccess.html

The Argo File Checker is not yet designed to checking *prof.nc and *Sprof.nc. It checks only TRAJ, META, TECH and PROFILES files.

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

### build docker image

- Build the application with Docker :

```bash
docker build -t filechecker_2.8.14 .
```

### Run integration tests

The source code comes with some netcdf test files. You can run the integration tests with this following command :

```bash
./mvnw verify
```
