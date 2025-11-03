import os
import shutil
from fastapi import FastAPI, UploadFile
from pathlib import Path
from tempfile import TemporaryDirectory

from argofilechecker_python_wrapper import FileChecker, ValidationResult

ROOT_PATH = os.getenv("API_ROOT_PATH", "")
UPLOAD_FILES_DIR = Path(os.getenv("UPLOAD_FILES_DIR", Path.cwd()))

app = FastAPI(root_path=ROOT_PATH)


@app.get("/")
def app_status() -> dict[str, str]:
    """
    Health check endpoint to confirm that the app is running.
    :return: status message
    """
    return {"status": "OK"}


@app.post("/check-files")
def check_file_list(files: list[UploadFile], dac: str) -> list[ValidationResult]:
    """
    Main endpoint to upload files to be checked.
    :param files:
        List of files uploaded as multipart/form-data
    :param dac:
        Relevant DAC for the files, e.g. coriolis, bodc, aoml, etc. Must be the same DAC for all files

    :return:
        list[ValidationResult]
    """
    if not files:
        raise ValueError("No files to check.")
    if not UPLOAD_FILES_DIR.exists():
        raise FileNotFoundError(f"Upload directory does not exist: {UPLOAD_FILES_DIR}")
    if not UPLOAD_FILES_DIR.is_dir():
        raise NotADirectoryError(f"Upload directory path is not a directory: {UPLOAD_FILES_DIR}")

    with TemporaryDirectory(dir=UPLOAD_FILES_DIR) as request_tmp_dir:
        request_file_dir = Path(request_tmp_dir)
        for upload_file in files:
            try:
                with request_file_dir.joinpath(upload_file.filename).open("wb") as buffer:
                    shutil.copyfileobj(upload_file.file, buffer)
            finally:
                upload_file.file.close()
        file_checker = FileChecker()
        results = file_checker.check_files(request_file_dir.glob("*"), dac)
    if not results:
        raise RuntimeError("An error occurred while handling uploaded files.")
    return results
