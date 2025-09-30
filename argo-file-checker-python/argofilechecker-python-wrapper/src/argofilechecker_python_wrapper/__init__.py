"""File Checker Python wrapper package."""

__version__ = "0.0.0" # version will be updated automatically by poetry_dynamic_versioning


from .file_checker import FileChecker
from .models import PhaseType, ResultType, ValidationResult

__all__ = [
    "FileChecker",
    "PhaseType",
    "ResultType",
    "ValidationResult",
    "__version__"
]