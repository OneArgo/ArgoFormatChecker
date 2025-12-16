package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Check PROGRAM_NAME against reference table")
class validateProgramNameCheckInMetaFileIT {

	private final String TEST_DIR_NAME = "TEST_META_0004";

	@BeforeAll
	public static void init() {
		TestsUtils.init(validateProgramNameCheckInMetaFileIT.class);

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "1902735_meta_bad_programName.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION",
			"1902735_meta_empty_programName.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION" })
	void fileChecker_shouldAcceptMetaFile_WhenBadProgramNameValue(String fileName, String dac, String result,
			String phase) throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have warning {2}")
	@CsvSource(delimiter = '|', value = {
			"1902735_meta_bad_programName.nc|coriolis|PROGRAM_NAME: 'test de program name' Status: Invalid (not in reference table)",
			"1902735_meta_empty_programName.nc|coriolis|PROGRAM_NAME: '' Status: Invalid (not in reference table)" })
	void fileChecker_ShouldRaiseWarning_WhenBadProgramNameValue(String fileName, String dac, String warningMessage)
			throws IOException, InterruptedException {
		TestsUtils.e2eTestWarningPresence(fileName, dac, warningMessage, TEST_DIR_NAME);
	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should not have warning")
	@CsvSource({ "5907141_meta_good_programName.nc,coriolis" })
	void fileChecker_ShouldNotRaiseWarning_WhenGoodProgramNameValue(String fileName, String dac)
			throws IOException, InterruptedException {
		TestsUtils.e2eTestWarningAbsence(fileName, dac, TEST_DIR_NAME);
	}

}
