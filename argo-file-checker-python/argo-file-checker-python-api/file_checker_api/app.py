import os
import shutil
from fastapi import FastAPI, UploadFile
from pathlib import Path
from typing import Annotated
from uuid import uuid4

from argofilechecker_python_wrapper import FileChecker

ROOT_PATH = os.getenv("API_ROOT_PATH", "")

app = FastAPI(root_path=ROOT_PATH)


@app.get("/")
def app_status():
    """
    Health check endpoint to confirm that the app is running.
    :return: status message
    """
    return {"status": "OK"}


@app.post("/check-files")
def check_file_list(files: list[UploadFile], dac: str):
    """
    Main endpoint to upload files to be checked.
    :param files:
        List of files uploaded as multipart/form-data
    :param dac:
        Relevant DAC for the files, e.g. coriolis, bodc, aoml, etc. Must be the same DAC for all files

    :return:
        { "results": ValidationResult object }
    """
    request_id = uuid4()
    request_file_dir = Path(f"/home/app/input/{request_id}")
    os.makedirs(request_file_dir)
    for upload_file in files:
        try:
            with request_file_dir.joinpath(upload_file.filename).open("wb") as buffer:
                shutil.copyfileobj(upload_file.file, buffer)
        finally:
            upload_file.file.close()
    file_checker = FileChecker()
    results = {"results": file_checker.check_files(request_file_dir.glob("*"), dac)}
    shutil.rmtree(request_file_dir)
    return results
