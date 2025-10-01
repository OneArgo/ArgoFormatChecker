"""Demo script to use argofilechecker_python_wrapper in docker container."""

import os
from typing import List

from argofilechecker_python_wrapper import FileChecker, ValidationResult
from argofilechecker_python_wrapper.models import ResultType


# 1) Instantiate the wrapper (explicit paths).
# file checker jar is already contained in the docker image
file_checker = FileChecker(specs_path=specs_path)


# 2) Validate a list of files from the same DAC
list_files = [f"/data/{file}" for file in os.listdir("/data")]
validation_results: List[ValidationResult] = file_checker.check_files(list_files, "coriolis")

# 3) Iterate over the results
for file_result in validation_results:
    print(file_result.to_string())

    if file_result.result in {ResultType.FAILURE, ResultType.ERROR}:
        print(*file_result.errors_messages)
        print(*file_result.warnings_messages)
