package fr.coriolis.checker.e2etests;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class validateVerticalSampingSchemeIT {

	private final String TEST_DIR_NAME = "TEST_PROFILE_0006";

	@BeforeAll
	public static void init() {
		TestsUtils.init(validateVerticalSampingSchemeIT.class);
	}

	@Tag(TEST_DIR_NAME)
	@ParameterizedTest(name = "{0} from dac {1} should have status {2} at phase {3}")
	@CsvSource({ "BR6990526_136_imbricatedBrackets.nc,coriolis" })
	void fileChecker_shouldNotRaiseWarning_WhenImbricatedBracketsInOptText(String fileName, String dac)
			throws IOException, InterruptedException {

		TestsUtils.e2eTestWarningAbsence(fileName, dac, TEST_DIR_NAME);

	}

}
