package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Check read of meta spec CDL file for variables and dimensions")
class validateMetaCDLSpecFileReadIT {

	private final String TEST_DIR_NAME = "TEST_META_0001";

	@BeforeAll
	public static void init() {
		TestsUtils.init(validateMetaCDLSpecFileReadIT.class);

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({
			"6903281_meta_DEPLOYMENT_PLATFORM_STRING128_SENSOR_FIRMWARE_VERSION.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION" })
	void fileChecker_shouldAcceptMetaFile_WhenConformToMetaCDLSpec(String fileName, String dac, String result,
			String phase) throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);

	}

}
