package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Check read of reference table for optional parameters values")
public class ValidateOptionalParametersValueCheckIT {

	private final String TEST_DIR_NAME = "TEST_ALL_0004";

	@BeforeAll
	public static void init() {
		TestsUtils.init(ValidateOptionalParametersValueCheckIT.class);

	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "6903281_meta_PROGRAM_NAME_in_ref-table-41.nc,coriolis,FILE-ACCEPTED,DATA-VALIDATION",
			"6903281_meta_PROGRAM_NAME_Not-in_ref-table-41.nc,coriolis,FILE-REJECTED,DATA-VALIDATION" })
	void fileChecker_shouldAcceptPROGRAMNAME_WhenConformToRefTable41(String fileName, String dac, String result,
			String phase) throws IOException, InterruptedException {

		TestsUtils.genericFileCheckerE2ETest(fileName, dac, result, phase, TEST_DIR_NAME);

	}
}
