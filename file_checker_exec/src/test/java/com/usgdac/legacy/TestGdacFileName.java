package com.usgdac.legacy;

import java.io.IOException;
import java.io.PrintStream;

import fr.coriolis.checker.filetypes.ArgoDataFile;

/**
 * Tests the getGdacFileName and validateFileName methods of ArgoDataFile
 * <p>
 * 
 * @author Mark Ignaszewski
 * @version $Id: ValidateSubmit.java 372 2015-12-02 19:02:31Z ignaszewski $
 */
public class TestGdacFileName {

	public static void main(String args[]) throws IOException {

		// .....extract the options....
		for (String file : args) {
			if (file.equals("-help")) {
				Help();
				System.exit(0);
			}

			stdout.println("\n\n==> FILE: " + file);

			ArgoDataFile argo = null;

			try {
				argo = ArgoDataFile.open(file);

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

			// ..get and validate file name
			String gdac = argo.getGdacFileName();
			stdout.println("GDAC File Name: '" + gdac + "'");

			boolean valid = argo.validateGdacFileName();
			if (valid) {
				stdout.println("File name is VALID");
			} else {
				stdout.println("File name is INvalid");
			}

			if (argo.nFormatErrors() == 0) {
				stdout.println("No errors");

			} else {
				stdout.println("ERRORS:\n");
				for (String err : argo.formatErrors()) {
					stdout.println(err + "\n");
				}
			}
		}

	}

	public static void Help() {
		stdout.println("\n" + "Purpose: Tests the GDAC file name methods of ArgoDataFile\n" + "\n"
				+ "Usage: java TestGdacFileName file-names...\n" + "Options:\n"
				+ "   -help | -H | -U   Help -- this message\n" + "\n");
	}

	// ......................Variable Declarations................

	private static ArgoDataFile argo;

	// ..standard i/o shortcuts
	static PrintStream stdout = new PrintStream(System.out);
	static PrintStream stderr = new PrintStream(System.err);
}
