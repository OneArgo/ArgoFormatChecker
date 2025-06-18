package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Check <param>_STD and _<param>_MED")
class ValidateStatParamIT {

	private final String TEST_DIR_NAME = "TEST_PROFILE_0002";

	@BeforeAll
	public static void init() {
		TestsUtils.init(ValidateStatParamIT.class);
	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "R6903129_088_QC0_TEMP_STD.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION",
			"R6903129_088_QC0_TEMP.nc,coriolis,FILE-REJECTED,DATA-VALIDATION" })
	void fileChecker_shouldAcceptFile_WhenSTDorMEDHaveAllQC0(String fileName, String dac, String result, String phase)
			throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);

	}

}
