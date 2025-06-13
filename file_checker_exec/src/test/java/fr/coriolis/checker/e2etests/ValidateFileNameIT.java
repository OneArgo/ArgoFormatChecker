package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Test the validation of the file name which must be conform to GDAC standards")
public class ValidateFileNameIT {

	private final String TEST_DIR_NAME = "TEST_ALL_0001";

	@BeforeAll
	public static void init() {
		TestsUtils.init(ValidateFileNameIT.class);
	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "6903283_tech.nc,coriolis,FILE-ACCEPTED,FILE-NAME-CHECK",
			"6903283_badName.nc,coriolis,FILE-REJECTED,FILE-NAME-CHECK" })
	public void fileChecker_shouldAcceptFile_whenValidFileName(String fileName, String dac, String result, String phase)
			throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME, "-format-only");
	}

}
