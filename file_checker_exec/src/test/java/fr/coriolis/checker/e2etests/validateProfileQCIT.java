package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class validateProfileQCIT {

	private final String TEST_DIR_NAME = "TEST_PROFILE_0005";

	@BeforeAll
	public static void init() {
		TestsUtils.init(validateProfileQCIT.class);
	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "R6903724_001_PROFILE_QC_CORRECT.nc,bodc,FILE-ACCEPTED,DATA-VALIDATION",
			"R6903724_001_PROFILE_QC_WRONG.nc,bodc,FILE-REJECTED,DATA-VALIDATION" })
	void fileChecker_shouldAcceptFile_WhenProfilParamQCisCorrect(String fileName, String dac, String result,
			String phase) throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);

	}

}
