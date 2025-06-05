package com.usgdac.legacy;

import java.io.IOException;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.coriolis.checker.filetypes.ArgoDataFile;
import fr.coriolis.checker.filetypes.ArgoProfileFile;

/**
 * Tests the getGdacFileName and validateFileName methods of ArgoDataFile
 * <p>
 * 
 * @author Mark Ignaszewski
 * @version $Id: ValidateSubmit.java 372 2015-12-02 19:02:31Z ignaszewski $
 */
public class TestPsalStats {

	// ......................Variable Declarations................

	private static ArgoDataFile argo;

	// ..standard i/o shortcuts
	static PrintStream stdout = new PrintStream(System.out);
	static PrintStream stderr = new PrintStream(System.err);

	static {
		System.setProperty("logfile.name", "TestPsalStats_LOG");
	}

	static Logger log = LoggerFactory.getLogger("TestPsalStats");

	// .........................main...............................

	public static void main(String args[]) throws IOException {

		// .....extract the options....
		for (String file : args) {
			if (file.equals("-help")) {
				Help();
				System.exit(0);
			}

			ArgoProfileFile argo = null;

			try {
				argo = ArgoProfileFile.open(file);

			} catch (Exception e) {
				stdout.println("ArgoDataFile.open exception:\n" + e);
				e.printStackTrace(stdout);

				continue;
			}

			// ..null file means it did not meet the min criteria to be an argo file
			if (argo == null) {
				stdout.println("ArgoDataFile.open failed: " + ArgoDataFile.getMessage());
				continue;
			}

			// ..compute PSAL statistics

			double[] stats = argo.computePsalAdjStats();

			stdout.printf("FILE: %-30s: %10.6f %10.6f\n", file, stats[0], stats[1]);
		}

	}

	public static void Help() {
		stdout.println("\n" + "Purpose: Tests the GDAC file name methods of ArgoDataFile\n" + "\n"
				+ "Usage: java TestGdacFileName file-names...\n" + "Options:\n"
				+ "   -help | -H | -U   Help -- this message\n" + "\n");
	}

}
