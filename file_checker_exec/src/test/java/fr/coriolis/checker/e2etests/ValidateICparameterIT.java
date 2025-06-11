package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Check read of physical parameter's core/bio/intermediate specification")
class ValidateICparameterIT {

	private final String TEST_DIR_NAME = "TEST_PROFILE_0005";

	@BeforeAll
	public static void init() {
		TestsUtils.init(ValidateICparameterIT.class);
	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "D4900757_024_CNDC_with_ADJUSTED.nc,aoml,FILE-ACCEPTED,DATA-VALIDATION",
			"D4900757_024_CNDC_without_ADJUSTED.nc,aoml,FILE-ACCEPTED,DATA-VALIDATION" })
	void fileChecker_shouldAcceptIntermediateCoreParameters_WhenNoAdjustedVariables(String fileName, String dac,
			String result, String phase) throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);

	}
}
