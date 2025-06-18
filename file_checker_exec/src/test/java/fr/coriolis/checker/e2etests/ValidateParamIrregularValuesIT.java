package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Check <PARAM> and <PARAM>_ADJUSTED irregular values (NaN, Inf)")
class ValidateParamIrregularValuesIT {

	private final String TEST_PROFILE_0003 = "TEST_PROFILE_0003";
	private final String TEST_PROFILE_0004 = "TEST_PROFILE_0004";

	@BeforeAll
	public static void init() {
		TestsUtils.init(ValidateParamIrregularValuesIT.class);
	}

	@Tag(TEST_PROFILE_0003)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "R6903724_001_bodc_No-NaN.nc,bodc,FILE-ACCEPTED,DATA-VALIDATION",
			"R6903724_001_bodc_NaN.nc,bodc,FILE-REJECTED,DATA-VALIDATION" })
	void fileChecker_shouldRejectFile_WhenNaNValues(String fileName, String dac, String result, String phase)
			throws IOException, InterruptedException {
		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_PROFILE_0003);
	}

	@Tag(TEST_PROFILE_0004)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "R2901301_144_incois-PSAL-No-Inf.nc,incois,FILE-ACCEPTED,DATA-VALIDATION",
			"R2901301_144_incois-PSAL-Inf.nc,incois,FILE-REJECTED,DATA-VALIDATION" })
	void fileChecker_shouldRejectFile_WhenInfValues(String fileName, String dac, String result, String phase)
			throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_PROFILE_0004);

	}

}
