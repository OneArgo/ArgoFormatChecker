package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ValidateTechTimeseriesIT {
	private final String TEST_DIR_NAME = "TEST_TECH_0001";

	@BeforeAll
	public static void init() {
		TestsUtils.init(ValidateTechTimeseriesIT.class);

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "3901682_tech.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION",
			"3901682_tech_No-JULD.nc,coriolis,FILE-REJECTED,FORMAT-VERIFICATION",
			"6903283_tech_No-Timeseries.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION",
			"3901682_tech_bad_long_name.nc,coriolis,FILE-REJECTED,FORMAT-VERIFICATION" })
	void fileChecker_shouldAcceptTechTimeseries_WhenConformToSpec(String fileName, String dac, String result,
			String phase) throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);

	}

}
