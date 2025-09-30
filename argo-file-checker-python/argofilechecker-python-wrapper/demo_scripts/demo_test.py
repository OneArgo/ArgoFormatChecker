"""Demo script to use argofilechecker_python_wrapper."""
import os
from typing import List

from argofilechecker_python_wrapper import FileChecker, ValidationResult
from argofilechecker_python_wrapper.models import ResultType

jar_path ="./file_checker_exec-2.9.3-SNAPSHOT.jar"
spec_path = "./file_checker_spec"


# 1) Instantiate the wrapper (explicit paths)
file_checker = FileChecker(jar_path, spec_path)

# 2) Validate a list of files from the same DAC
list_files = [f'./test_data/2903996/{file}' for file in os.listdir('./test_data/2903996')]
validation_results:List[ValidationResult] = file_checker.check_files(list_files,"coriolis" )

# 3) Iterate over the results
for file_result in validation_results :

    print (file_result.to_string())

    if file_result.result in {ResultType.FAILURE, ResultType.ERROR}:
        print(*file_result.errors_messages)
        print(*file_result.warnings_messages)
