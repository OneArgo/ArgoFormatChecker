package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Check good naming of parameters and sensor from duplicate sensors")
class ValidateMultipleSensorIT {

	private final String TEST_DIR_NAME = "TEST_ALL_0003";
	private final String TEST_DIR_NAME2 = "TEST_META_0002";

	@BeforeAll
	public static void init() {
		TestsUtils.init(ValidateMultipleSensorIT.class);
	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "BD4900476_032_DOXY2.nc,aoml,FILE-REJECTED,FORMAT-VERIFICATION",
			"BD4900476_032_DOXY_2.nc,aoml,FILE-ACCEPTED,DATA-VALIDATION" })
	void fileChecker_shouldRejectDuplicateParameters_WhenIntegerNotSeparatedByUnderscore(String fileName, String dac,
			String result, String phase) throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);

	}

	@Tag(TEST_DIR_NAME2)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "6990661_meta_DOXY_2.nc,coriolis,FILE-REJECTED,DATA-VALIDATION",
			"6990661_meta_DOXY_2_SENSOR.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION" })
	void fileChecker_shouldRejectDuplicateMetaSENSOR_WhenIntegerNotSeparatedByUnderscore(String fileName, String dac,
			String result, String phase) throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME2);

	}

}
