package fr.coriolis.checker.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import fr.coriolis.checker.config.Options;
import fr.coriolis.checker.specs.ArgoReferenceTable;

@Tag("ExtractOptionsTests")
@DisplayName("Succeed to parse and validate the differents options from command-line arguments")
class OptionsTest {

	@BeforeEach
	public void undefOptionsInstance() throws Exception {
		Field instanceField = Options.class.getDeclaredField("instance");
		instanceField.setAccessible(true);
		instanceField.set(null, null);

	}

	@Test
	void getInstance_shouldReturnANewInstanceOfOptions_whenValidArguments() {
		// ARRANGE
		String dacName = "dac";
		String specDir = "someDir";
		String outDir = "someDir2";
		String inDir = "someDir3";
		String listOfFiles = "list_of_files.txt";
		String file1 = "file1";
		String file2 = "file2";

		String[] args = { "-help", "-version", "-battery-check", "-no-name-check", "-text-result", "-format-only",
				"-null-warn", "-data-check-all", "-psal-stats", "-list-file", listOfFiles, dacName, specDir, outDir,
				inDir, file1, file2 };

		// ACT
		Options options = Options.getInstance(args);

		// ASSERT
		assertThat(options.isHelp()).isTrue();
		assertThat(options.isVersion()).isTrue();
		assertThat(options.isDoBatteryChecks()).isTrue();
		assertThat(options.isDoNameCheck()).isFalse();
		assertThat(options.isDoXml()).isFalse();
		assertThat(options.isDoFormatOnly()).isTrue();
		assertThat(options.isDoNulls()).isTrue();
		assertThat(options.isDoFormatOnlyPre31()).isFalse();
		assertThat(options.isDoPsalStats()).isTrue();
		assertThat(options.getListFile()).isEqualTo(listOfFiles);
		assertThat(options.getDacName()).isEqualTo(dacName);
		assertThat(options.getSpecDirName()).isEqualTo(specDir);
		assertThat(options.getOutDirName()).isEqualTo(outDir);
		assertThat(options.getInDirName()).isEqualTo(inDir);
		assertThat(options.getInFileList()).containsExactly(file1, file2);
	}

	@Test
	public void getInstance_shouldThrowException_whenMissingArgumentAfterListFile() {
		// ARRANGE
		String[] args = { "-help", "-version", "-battery-check", "-no-name-check", "-text-result", "-format-only",
				"-list-file" };

		// ACT & ASSERT
		assertThatThrownBy(() -> Options.getInstance(args)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Missing argument");

	}

	@Test
	public void getInstance_shouldThrowException_whenInvalidArgument() {
		// ARRANGE
		String[] args = { "-InvalidArgument" };

		// ACT & ASSERT
		assertThatThrownBy(() -> Options.getInstance(args)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid argument");
	}

	@Test
	public void getInstance_shouldThrowException_whenTooFewArguments() {

		// ARRANGE
		String[] args = { "-help", "-version", "-battery-check", "-no-name-check", "-text-result", "-format-only",
				"-list-file", "list_of_files.txt", "dacName", "specDir", "outDir" };

		// ACT & ASSERT
		assertThatThrownBy(() -> Options.getInstance(args)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Too few arguments");
	}

	@Test
	public void checkDacName_shouldNotThrowAnException_whenValidDacName() {
		// ARRANGE
		String validDac = ArgoReferenceTable.DACS.CORIOLIS.name;

		// ACT & ASSERT
		assertThatNoException().isThrownBy(() -> Options.checkDacName(validDac));

	}

	@Test
	public void checkDacName_shouldThrowAnException_whenInvalidValidDacName() {
		// ARRANGE
		String notValidDac = "unexistent DAC";

		// ACT & ASSERT
		assertThatThrownBy(() -> Options.checkDacName(notValidDac)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown DAC name");

	}

	@Test
	public void checkDirectory_shouldThrowAnException_whenDirIsInvalid() {
		// ARRANGE
		String notValidDir = "unexistent dir";

		// ACT & ASSERT
		assertThatThrownBy(() -> Options.checkDirectory(notValidDir)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("is not a directory");

	}

}
