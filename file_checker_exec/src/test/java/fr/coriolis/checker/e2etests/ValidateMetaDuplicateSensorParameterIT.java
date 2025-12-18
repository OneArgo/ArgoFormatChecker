package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Check if SENSOR and PARAMETER don't contain duplicate value")
class ValidateMetaDuplicateSensorParameterIT {

	private final String TEST_DIR_NAME = "TEST_META_0003";

	@BeforeAll
	public static void init() {
		TestsUtils.init(ValidateMetaDuplicateSensorParameterIT.class);

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "2903795_meta_duplicate_PARAMETER.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION" })
	void fileChecker_shouldAcceptMetaFile_WhenDuplicateInSensorOrParameter(String fileName, String dac, String result,
			String phase) throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have warning {2}")
	@CsvSource(delimiter = '|', value = {
			"2903795_meta_duplicate_PARAMETER.nc|coriolis|PARAMETER variable contains duplicate values: [PPOX_DOXY, DOXY]",
			"5902129_meta_duplicate_PARAMETER_SENSOR.nc|coriolis|SENSOR variable contains duplicate values: [CTD_TEMP, CTD_PRES]" })
	void fileChecker_ShouldRaiseWarning_WhenDuplicateInSensorOrParamete(String fileName, String dac,
			String warningMessage) throws IOException, InterruptedException {
		TestsUtils.e2eTestWarningPresence(fileName, dac, warningMessage, TEST_DIR_NAME);
	}

}
