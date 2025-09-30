"""Fixtures for tests suite."""
import os
import shutil
import subprocess
from pathlib import Path

import pytest
import requests


@pytest.fixture(scope="session")
def specs_directory(tmp_path_factory) -> Path:
    """Fixture which download the up-to-date specs folder from the file checker git.
    
    Add it to the session context for tests 'data validation' suite.
    Need to provide Argo file checker github repo informations.
    """
    specs_repo_url = os.getenv("SPECS_REPO_URL", "https://github.com/OneArgo/ArgoFormatChecker.git")
    ref = os.getenv("SPECS_REPO_REF", "main")         # branch
    specs_subdir = os.getenv("TEST_REPO_SUBDIR", "file_checker_spec/") #sub folder to extract

    dest = tmp_path_factory.mktemp("specs") # session wide folder to save specs files

    _clone_subdir (specs_repo_url,ref,specs_subdir,dest) # clone repo subdir in the dest directory
   
    return dest


@pytest.fixture(scope="session")
def file_checker_jar_file(tmp_path_factory) -> Path:
    """Fixture to provide the file checker executable into the session context for tests 'data validation' suite."""
    jar_download_url = os.getenv("JAR_URL", "https://github.com/OneArgo/ArgoFormatChecker/releases/download/v2.9.3/file_checker_exec-2.9.3.jar")

    dest = tmp_path_factory.mktemp("file_checker_exec")
    # download file checker JAR :
    _download (jar_download_url, dest, 'app.jar')
   
    file_path = dest / 'app.jar'
           
    return file_path


def _download (url: str, dest: Path, out_file_name : str, timeout: int=120):
    """Download file from provided url.

    Arg : 
        url (str) : url of the file
        dest (Path) : destination folder
        out_file_name (str) : name of the downloaded file in the dest folder
    """
    file_path = dest / "app.jar"
    # dowload the jar from the provide url
    with open (dest / out_file_name, 'wb') as out_file :
        content = requests.get(url, stream=True).content
        out_file.write(content)

    if not file_path.exists():
        raise FileNotFoundError(f"File checker jar not found at {file_path}") # noqa: TRY003

    

def _clone_subdir (repo_url: str,branch:str ,subdir: str,dest:Path) :
    dest.parent.mkdir(parents=True, exist_ok=True)
    clone_dir = dest.parent / ".clone"
    if clone_dir.exists():
        shutil.rmtree(clone_dir)
    
    #clone git repo :
    cmd_clone = ["git", "clone", "--depth=1", "--filter=blob:none", "--sparse", repo_url, str(clone_dir)]    
    #change branch
    cmd_checkout = ["git", "-C",str(clone_dir), "checkout", branch]
    #Only subdir needed :
    cmd_sparse_checkout = ["git", "-C", str(clone_dir), "sparse-checkout", "set", subdir]
    #launch subprocesses :
    subprocess.run(cmd_clone, cwd=None, check=True)
    subprocess.run(cmd_checkout, cwd=None, check=True)
    subprocess.run(cmd_sparse_checkout, cwd=None, check=True)

    # move sub folder to dest :
    src = clone_dir / subdir    
    if not src.exists():
        raise FileNotFoundError(f"Directory '{subdir}' not found in {repo_url}@{branch}") # noqa: TRY003
    if dest.exists():
        shutil.rmtree(dest)
    shutil.move(str(src), str(dest))

    # Nettoyage du clone temporaire
    shutil.rmtree(clone_dir, ignore_errors=True)

