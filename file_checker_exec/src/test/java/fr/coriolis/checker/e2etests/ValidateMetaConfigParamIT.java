package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Check read of meta R18 reference table to check CONFIG PARAM name")
public class ValidateMetaConfigParamIT {

	private final String TEST_DIR_NAME = "TEST_META_0005";

	@BeforeAll
	public static void init() {
		TestsUtils.init(ValidateMetaConfigParamIT.class);
	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should not have warnings")
	@CsvSource({ "1900848_meta_CDLexception_ShortSensorName.nc,coriolis",
			"3901639_meta_multiples_template_ssn_cpn_n.nc,coriolis" })
	void fileChecker_shouldNotRaiseWarnings_WhenParamNameConformToR18TemplatesValues(String fileName, String dac)
			throws IOException, InterruptedException {

		TestsUtils.e2eTestWarningAbsence(fileName, dac, TEST_DIR_NAME);

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have warning {2}")
	@CsvSource(delimiter = '|', value = {
			"3901639_meta_Bad-short-sensor-name.nc|coriolis|LAUNCH_CONFIG_PARAMETER_NAME[49]: Invalid template/value 'shortsensorname'/'Bad' in 'CONFIG_BadAscentPhaseDepthZone3StartPres'   *** WILL BECOME AN ERROR ***",
			"3901639_meta_N-Not-integer.nc|coriolis|LAUNCH_CONFIG_PARAMETER_NAME[49]: Invalid name 'CONFIG_CtdAscentPhaseDepthZoneXStartPres'   *** WILL BECOME AN ERROR ***",
			"3901639_meta_Bad-cycle-phase-name.nc|coriolis|LAUNCH_CONFIG_PARAMETER_NAME[49]: Invalid template/value 'cyclephasename'/'BadPhase' in 'CONFIG_CtdBadPhaseDepthZone3StartPres'   *** WILL BECOME AN ERROR ***",
			"3901639_meta_N-OutOfRange.nc|coriolis|LAUNCH_CONFIG_PARAMETER_NAME[49]: Invalid template/value 'N'/'6' in 'CONFIG_CtdAscentPhaseDepthZone6StartPres'   *** WILL BECOME AN ERROR ***" })
	void fileChecker_shouldRaiseWarnings_WhenParamNameNotConformToR18TemplatesValues(String fileName, String dac,
			String warningMessage) throws IOException, InterruptedException {

		TestsUtils.e2eTestWarningPresence(fileName, dac, warningMessage, TEST_DIR_NAME);
	}

}
