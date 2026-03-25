package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Check BATTERY_* params against reference table")
class validateBatteryParamsCheckInMetaFileIT {

	private final String TEST_DIR_NAME = "TEST_META_0006";

	@BeforeAll
	public static void init() {
		TestsUtils.init(validateBatteryParamsCheckInMetaFileIT.class);

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "5907141_meta_good_batteryType.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION",
			"5907141_meta_bad-type.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION",
			"5907141_meta_bad-Manufacturer.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION",
			"5907141_meta_batteryType_empty.nc,coriolis,FILE-REJECTED,DATA-VALIDATION" })
	void fileChecker_shouldAcceptMetaFile_WhenBatteryTypeNotEmpty(String fileName, String dac, String result,
			String phase) throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME,
				"-no-name-check -battery-check");

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have warning {2}")
	@CsvSource(delimiter = '|', value = {
			"5907141_meta_bad-type.nc|coriolis|BATTERY_TYPE[1]: Invalid type: '{BAD}'   *** WILL BECOME AN ERROR ***",
			"5907141_meta_bad-Manufacturer.nc|coriolis|BATTERY_TYPE[1]: Invalid manufacturer: '{BAD}'   *** WILL BECOME AN ERROR ***" })
	void fileChecker_ShouldRaiseWarning_WhenBadBatteryType(String fileName, String dac, String warningMessage)
			throws IOException, InterruptedException {
		TestsUtils.e2eTestWarningPresence(fileName, dac, warningMessage, TEST_DIR_NAME,
				"-no-name-check -battery-check");
	}

}
