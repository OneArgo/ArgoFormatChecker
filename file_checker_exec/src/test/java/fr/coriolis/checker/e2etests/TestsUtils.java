package fr.coriolis.checker.e2etests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public final class TestsUtils {

	public static String version = "";
	public static String jarPath;
	public static File jarFile;
	public static final String OUTPUT_DIR_PATH = "target/test-results";
	public static String SPEC_DIR_PATH = "../file_checker_spec";
	public static String TEST_FILES_DIR = "src/test/netcdf-test-files";
	public static File specDirDir = new File(SPEC_DIR_PATH);
	public static File outputDir;

	public static void init(Class<?> clazz) {
		Properties properties = new Properties();
		try {
			properties.load(clazz.getClassLoader().getResourceAsStream("Application.properties"));
			version = properties.getProperty("Version", "");
			jarPath = String.format("target/file_checker_exec-%s.jar", version);
			jarFile = new File(jarPath);
			Files.createDirectories(Paths.get(OUTPUT_DIR_PATH));
			outputDir = new File(OUTPUT_DIR_PATH);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void genericFileCheckerE2ETest(String fileName, String dac, String result, String phase,
			String testDirName, String options) throws IOException, InterruptedException {

		// ARANGE
		String inPutDirPath = TestsUtils.TEST_FILES_DIR + "/" + testDirName;
		File intputDir = new File(inPutDirPath);
		File testFile = new File(inPutDirPath + "/" + fileName);
		// before executing jar, verify file and dir exists :
		assertThat(TestsUtils.jarFile).exists().isFile().as("jar should be created in target folder");
		;
		assertThat(testFile).exists().isFile().as("netcdf test file should be in test/netcdf/TEST* resources folder");
		assertThat(TestsUtils.specDirDir).exists().isDirectory().as("specifications directory should exist");
		assertThat(intputDir).exists().isDirectory().as("input directory should exist");
		assertThat(TestsUtils.outputDir).exists().isDirectory().as("output directory should exist");

		// ACT
		ProcessBuilder builder = new ProcessBuilder("java", "-jar", TestsUtils.jarPath, options, dac,
				TestsUtils.SPEC_DIR_PATH, TestsUtils.OUTPUT_DIR_PATH, inPutDirPath, fileName);
		builder.redirectErrorStream(true);
		Process process = builder.start();
		process.waitFor();

		// ASSESS
		// No error
		int exitCode = process.waitFor();
		assertThat(exitCode).isZero().as("execution should complete without errors");
		// result file created
		File xmlResultFile = new File(TestsUtils.OUTPUT_DIR_PATH + "\\" + fileName + ".filecheck");
		assertThat(xmlResultFile).exists().isFile().as("Result file should be created in %s",
				TestsUtils.OUTPUT_DIR_PATH);
		// expected status
		String content = String.join("\n", Files.readAllLines(xmlResultFile.toPath()));
		assertThat(content).isNotEmpty().contains("<status>" + result);
		// in the expected
		assertThat(content).isNotEmpty().contains("<phase>" + phase);

	}

	// We use an overloaded method to provide a default value for the last argument
	// "options".
	public static void genericFileCheckerE2ETest(String fileName, String dac, String result, String phase,
			String testDirName) throws IOException, InterruptedException {

		genericFileCheckerE2ETest(fileName, dac, result, phase, testDirName, "-no-name-check");

	}

}
