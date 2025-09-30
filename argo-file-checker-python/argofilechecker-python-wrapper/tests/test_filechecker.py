"""Tests for the argofilechecker_python_wrapper module."""
import pytest

from argofilechecker_python_wrapper import FileChecker


@pytest.fixture
def env_paths(tmp_path, monkeypatch):
    """Create a mock JAR file and specs dir and add it to env variables."""
    jar = tmp_path / "app.jar"
    jar.touch()  # simulate jar existence
    specs = tmp_path / "specs"
    specs.mkdir()

    monkeypatch.setenv("FILE_CHECKER_JAR", str(jar))
    monkeypatch.setenv("FILE_CHECKER_SPECS", str(specs))
    return jar, specs

def test_check_files_empty_input_files_list(env_paths) :
    """Should raise an Error when no files list are provided."""
    file_checker = FileChecker()

    with pytest.raises(ValueError):
        file_checker.check_files([], "coriolis")


def test_check_files_all_missing_raise(env_paths):
    """Should raise an error when a file provided doesn't exists."""
    file_checker = FileChecker()

    with pytest.raises(FileNotFoundError):
        file_checker.check_files(["NOEXISTS.nc"], "coriolis")