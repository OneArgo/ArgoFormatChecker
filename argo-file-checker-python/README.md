# ArgoFileChecker Python Wrapper

A lightweight Python wrapper around the **Argo NetCDF File Checker**.  
It runs the File Checker JAR as a **subprocess**, parses the generated XML output files (which are generated in a temporary folder), and returns typed `ValidationResult` objects.

## Installation

### With Poetry

```bash
cd ./argofilechecker-python-wrapper
poetry install
```

## Run ArgoFileChecker Python Wrapper (after installing the package and any additional dependencies)
The wrapper can receive paths explicitly or through environment variables:

FILE_CHECKER_JAR: path to file_checker_exec-*.jar

FILE_CHECKER_SPECS: path to the file_checker_spec directory

### Usage example
See /demo_scripts where a demo python script is provided along with a file checker .jar and some netcdf test data.

```bash
cd ./demo_scripts
poetry run python demo_test.py
```

## Run ArgoFileChecker Python Wrapper in a pre-built container image

### build and run the image

```bash
docker build --target file-checker-python -t argofilechecker-python:latest .
```

Argo File checker .jar file will be included in the docker image but you still need to mount your data and your script file (if not used in interactive mode) :

```bash
cd ./argofilechecker-python-wrapper/demo_scripts
docker run --rm -v ${pwd}/demo_docker.py:/scripts/demo_docker.py -v ${pwd}/test_data/2903996:/data argofilechecker-python:latest /scripts/demo_docker.py
```
Ensure that the correct volumes names are used in your script.

You can use interactive mode to execute python code inside the container :

```bash
cd ./argofilechecker-python-wrapper/demo_scripts
docker run --rm -it -v ${pwd}/test_data/2903996:/data argofilechecker-python:latest
>>> from argofilechecker_python_wrapper import FileChecker
>>> filechecker = FileChecker()
>>> results = filechecker.check_files(['/data/2903996_meta.nc'],"coriolis")
>>> results[0].to_string()
'/data/2903996_meta.nc : FILE ACCEPTED with 0 error(s) and 0 warning(s)'
```


### Results Values

- ResultType.SUCCESS → the file is accepted by the File Checker.

- ResultType.FAILURE → the file is rejected (errors found).

- ResultType.ERROR → internal error in the checker or parsing failure.

The phase (PhaseType) indicates the stage where the error occurred: opening, format check, data validation, or file name check.

Errors and warnings's number and messages can be accessed also.

For more information see models.py

# Argo File Checker Python API

A FastAPI app providing an endpoint to access file checker functionality via the /check-files endpoint.

## Build and run using Docker

```bash
docker build --target file-checker-api -t argofilechecker-api:latest .
docker run -p 8000:8000 argofilechecker-api:latest
```

The file checker endpoint will then be available on http://localhost:8000/check-files
API documentation can be found on http://localhost:8000/docs

The DAC for the files needs to be specified as a parameter. The files to check need to be included in the body of a `multipart/form-data` type request, e.g.:
For example, post an example file found in the _argofilechecker-python-wrapper/demo_scripts/test_data/2903996_ folder to `http://localhost:8000/check-files?dac=coriolis`
```http request
curl -X 'POST' \
  'http://localhost:8000/check-files?dac=coriolis' \
  -H 'accept: application/json' \
  -H 'Content-Type: multipart/form-data' \
  -F 'files=@2903996_meta.nc'
```
