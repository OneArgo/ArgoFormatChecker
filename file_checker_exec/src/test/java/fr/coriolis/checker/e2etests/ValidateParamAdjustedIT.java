package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Check <param>_ADJUSTED and _ADJUSTED_QC")
class ValidateParamAdjustedIT {

	private final String TEST_DIR_NAME = "TEST_PROFILE_0001";

	@BeforeAll
	public static void init() {
		TestsUtils.init(ValidateParamAdjustedIT.class);
	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "R6903724_001_notFillValue.nc,bodc,FILE-REJECTED,DATA-VALIDATION",
			"R6903724_001_FillValue.nc,bodc,FILE-ACCEPTED,DATA-VALIDATION" })
	void fileChecker_shouldRejectFile_WhenDataModeIsRAndAjustedNotFillValue(String fileName, String dac, String result,
			String phase) throws IOException, InterruptedException {
		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);
	}

}
