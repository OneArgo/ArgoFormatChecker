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
			"5907141_meta_batteryType_empty.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION" })
	void fileChecker_shouldAcceptMetaFile_WhenBatteryTypeBadOrGood(String fileName, String dac, String result,
			String phase) throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "5907141_meta_BATTERY_PACKS_bad-size.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION",
			"5907141_meta_BATTERY_PACKS_bad-type.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION",
			"5907141_meta_BATTERY_PACKS_empty.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION" })
	void fileChecker_shouldAcceptMetaFile_WhenBatteryPacksBadOrEmpty(String fileName, String dac, String result,
			String phase) throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have warning {2}")
	@CsvSource(delimiter = '|', value = {
			"5907141_meta_bad-type.nc|coriolis|BATTERY_TYPE[1]: Invalid type: '{BAD}'   *** WILL BECOME AN ERROR ***",
			"5907141_meta_bad-Manufacturer.nc|coriolis|BATTERY_TYPE[1]: Invalid manufacturer: '{BAD}'   *** WILL BECOME AN ERROR ***",
			"5907141_meta_incoherent_type.nc|coriolis|Inconsistent battery's type in BATTERY_TYPE[1] and BATTERY_PACKS[1]. BATTERY_TYPE's type ={Lithium}, BATTERY_PACKS's type = {Hyb}",
			"5907141_meta_incoherent_type_second_pack.nc|coriolis|Inconsistent battery's type in BATTERY_TYPE[2] and BATTERY_PACKS[2]. BATTERY_TYPE's type ={Lithium}, BATTERY_PACKS's type = {Alk}" })
	void fileChecker_ShouldRaiseWarning_WhenBadBatteryType(String fileName, String dac, String warningMessage)
			throws IOException, InterruptedException {
		TestsUtils.e2eTestWarningPresence(fileName, dac, warningMessage, TEST_DIR_NAME);
	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should not have warning")
	@CsvSource(delimiter = '|', value = { "5907141_meta_good_batteryType.nc|coriolis", })
	void fileChecker_ShouldNotRaiseWarning_WhenBatteryParamGood(String fileName, String dac)
			throws IOException, InterruptedException {
		TestsUtils.e2eTestWarningAbsence(fileName, dac, TEST_DIR_NAME);
	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have warning {2}")
	@CsvSource(delimiter = '|', value = {
			"5907141_meta_BATTERY_PACKS_bad-size.nc|coriolis|BATTERY_PACKS[1]: Invalid style of battery: '{X}'   *** WILL BECOME AN ERROR ***",
			"5907141_meta_BATTERY_PACKS_bad-type.nc|coriolis|BATTERY_PACKS[1]: Invalid type: '{BAD}'   *** WILL BECOME AN ERROR ***",
			"5907141_meta_Wrong-number-Battery-Packs.nc|coriolis|Number of BATTERY_TYPES {1} != number of BATTERY_PACKS {2}   *** WILL BECOME AN ERROR ***" })
	void fileChecker_ShouldRaiseWarning_WhenBadBatteryPacks(String fileName, String dac, String warningMessage)
			throws IOException, InterruptedException {
		TestsUtils.e2eTestWarningPresence(fileName, dac, warningMessage, TEST_DIR_NAME);
	}

}
