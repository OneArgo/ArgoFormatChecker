package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Check PROGRAM_NAME against reference table")
class validatePINameChecksIT {

	private final String TEST_DIR_NAME = "TEST_ALL_0006";

	@BeforeAll
	public static void init() {
		TestsUtils.init(validatePINameChecksIT.class);

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "5907141_meta_apostrophe.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION",
			"5907141_meta_Bad_PIName.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION",
			"5907141_meta_BadSpaceCaractere.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION",
			"5907141_meta_multipleName.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION" })
	void fileChecker_shouldAcceptFile_WhenGoodOrBadPiNames(String fileName, String dac, String result, String phase)
			throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "5907141_meta_multipleName.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION",
			"5907141_meta_apostrophe.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION" })
	void fileChecker_shouldNotRaiseWarnings_WhenGoodPiNames(String fileName, String dac, String result, String phase)
			throws IOException, InterruptedException {

		TestsUtils.e2eTestWarningAbsence(fileName, dac, TEST_DIR_NAME);

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have warning {2}")
	@CsvSource(delimiter = '|', value = {
			"5907141_meta_Bad_PIName.nc|coriolis|PI_NAME : 'BAD' Status: Invalid (not in NVS R40 table)",
			"5907141_meta_BadSpaceCaractere.nc|coriolis|PI_NAME : 'Fabrizio\u00A0D'ORTENZIO' Status: Invalid (not in NVS R40 table)" })

	void fileChecker_ShouldRaiseWarning_WhenBadPiName(String fileName, String dac, String warningMessage)
			throws IOException, InterruptedException {
		TestsUtils.e2eTestWarningPresence(fileName, dac, warningMessage, TEST_DIR_NAME);
	}
//

}
