package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Check <PARAM> deprecated attributes and properties table")
class validatePhysParamDeprecatedAttributeOrPropertiesIT {

	private final String TEST_DIR_NAME = "TEST_ALL_0005";

	@BeforeAll
	public static void init() {
		TestsUtils.init(ValidateParamAdjustedIT.class);
	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "R6903724_001_v3.1_TEMP_Bad_validMin.nc,bodc,FILE-REJECTED,FORMAT-VERIFICATION",
			"R6903724_001_v3.0_TEMP_Dep_validMin.nc,bodc,FILE-ACCEPTED,FORMAT-VERIFICATION" })
	void fileChecker_shouldRejectFile_WhenAttrValueNotDefinedInLocalAttributeNorInDepTable(String fileName, String dac,
			String result, String phase) throws IOException, InterruptedException {
		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);
	}

//	@Tag(TEST_DIR_NAME)
//	@ParameterizedTest(name = "{0} from dac {1} should have warning {2}")
//	@CsvSource(delimiter = '|', value = {
//			"R6903724_001_v3.0_TEMP_Dep_validMin.nc|bodc|This valid_min for TEMP is now deprecated in format version [3.0]", })
//	void fileChecker_shouldRaiseWarnings_WhenAttrValueNotDefinedInLocalAttributeButIsInDepTable(String fileName,
//			String dac, String warningMessage) throws IOException, InterruptedException {
//
//		TestsUtils.e2eTestWarningPresence(fileName, dac, warningMessage, TEST_DIR_NAME);
//	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "D4900757_024_CNDC_without_ADJUSTED_version-3.0_DMode-A.nc,aoml,FILE-REJECTED,FORMAT-VERIFICATION",
			"D4900757_024_CNDC_with_ADJUSTED_version-3.0_DMode-A.nc,aoml,FILE-ACCEPTED,FORMAT-VERIFICATION" })
	void fileChecker_shouldTakeIntoaccount_deprecatedProperties(String fileName, String dac, String result,
			String phase) throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);

	}

}
