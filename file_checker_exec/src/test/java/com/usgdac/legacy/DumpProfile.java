package com.usgdac.legacy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.filetypes.ArgoDataFile;
import fr.coriolis.checker.filetypes.ArgoProfileFile;
import fr.coriolis.checker.specs.ArgoDate;
import fr.coriolis.checker.specs.ArgoFileSpecification;
import fr.coriolis.checker.specs.ArgoVariable;
import ucar.ma2.DataType;

public class DumpProfile {

	public static void main(String args[]) throws IOException {
		// .....extract the options....
		String listFile = null;
		int next;

		boolean only_first = false;
		boolean no_trailing = false;
		boolean id_inc_prof = false;
		boolean group_prof = false;

		for (next = 0; next < args.length; next++) {
			if (args[next].equals("-help")) {
				Help();
				System.exit(0);
			} else if (args[next].equals("-only-first") || args[next].equals("-1")) {
				only_first = true;
			} else if (args[next].equals("-no-trailing-missing") || args[next].equals("-b")) {
				no_trailing = true;
			} else if (args[next].equals("-id-includes-prof") || args[next].equals("-i")) {
				id_inc_prof = true;
			} else if (args[next].equals("-group-prof") || args[next].equals("-g")) {
				group_prof = true;
			} else if (args[next].equals("-list-file") || args[next].equals("-l")) {
				next++;
				if (next < args.length) {
					listFile = args[next];
				}
				// } else if (args[next].startsWith("-inc-history")) {
				// addHistory = true;
			} else if (args[next].startsWith("-")) {
				stderr.println("Invalid argument: '" + args[next] + "'");
				System.exit(1);
			} else {
				break;
			}
		}

		// .....parse the positional parameters.....

		log.info("DumpProfile:  START");

		if (args.length < (2 + next)) {
			log.error("too few arguments: " + args.length);
			Help();
			stderr.println("Too few arguments: " + args.length);
			System.exit(1);
		}

		String specDirName = args[next++];
		String outFileName = args[next++];

		List<String> inFileList = null; // ..list of input files
		if (next < args.length) {
			inFileList = new ArrayList<String>(args.length - next);
			for (; next < args.length; next++) {
				inFileList.add(args[next]);
			}
		}

		log.debug("outFileName = '" + outFileName + "'");
		log.debug("number of inFileList = " + (inFileList == null ? "null" : inFileList.size()));

		stdout.println("\nDumpProfile inputs:");
		stdout.println("   Output file name:        " + outFileName);

		// .........get list of input files........
		// ..input files are chosen in the following priority order
		// ..1) an input-file-list (overrides all other lists)
		// ..2) file name arguments (already parsed above, if specified)

		if (listFile != null) {
			// ..a list file was specified - open and read it
			// ..this overrides all other "input lists"
			File f = new File(listFile);
			if (!f.isFile()) {
				stderr.println("\nERROR: -list-file does not exist: '" + listFile + "'");
				log.error("-list-file does not exist: '" + listFile + "'");
				System.exit(1);
			} else if (!f.canRead()) {
				log.error("-list-file '" + listFile + "' cannot be read");
				stderr.println("\nERROR: -list-file cannot be read: '" + listFile + "'");
				System.exit(1);
			}

			// ..open and read the file
			BufferedReader file = new BufferedReader(new FileReader(listFile));
			inFileList = new ArrayList<String>(200);
			String line;
			while ((line = file.readLine()) != null) {
				if (line.trim().length() > 0) {
					inFileList.add(line.trim());
				}
			}
			log.info("Read {} entries from -list-file '{}'", inFileList.size(), listFile);

			stdout.println("   Input files read from:   '" + listFile + "'");

		} else if (inFileList == null) {
			stderr.println("\nERROR: No input files specified\n");
			log.error("No input files specified");
			System.exit(1);

		} else {
			stdout.println("   Input files read from command line");
		}

		stdout.println("   Number of input files:   " + inFileList.size());

		/*
		 * //..output file if ((new File(outFileName)).exists()) { stderr.println
		 * ("\nERROR: Output file MUST NOT EXIST :'" + outFileName + "'\n");
		 * log.error("Output file MUST NOT EXIST: '" + outFileName +"'");
		 * System.exit(1); }
		 */

		// ..create the output file
		PrintWriter outFile = new PrintWriter(new BufferedWriter(new java.io.FileWriter(outFileName)));

		// .....write shtuff to the file.....
		log.debug(".....writing data to file.....");

		// .....loop over input files --- read from innie, write to outie

		int nFile = -1;
		int nInProf;
		int nextOutProf = 0;

		stdout.println("\nProcessing File:");
		stdout.println(" FileNum  N_PROF  N_CALIB  N_HISTORY  N_PARAM  N_LEVELS  File Name");

		for (String inFileName : inFileList) {
			nFile++;

			ArgoProfileFile arFile = null;
			boolean openSuccessful = false;
			try {
				arFile = ArgoProfileFile.open(inFileName, specDirName, true);
			} catch (Exception e) {
				log.error("ArgoDataFile.open exception:\n" + e);
				stdout.println("ERROR: Exception opening Argo file:");
				e.printStackTrace(stderr);
				System.exit(1);
			}

			if (arFile == null) {
				stdout.printf(" %6d  Open failed.  Not an Argo file\n");
				continue;
			}

			ArgoFileSpecification spec = arFile.getFileSpec();

			int nProf = arFile.findDimension("N_PROF").getLength();
			int nCalib = arFile.findDimension("N_CALIB").getLength();
			int nHistory = arFile.findDimension("N_HISTORY").getLength();
			int nParam = arFile.findDimension("N_PARAM").getLength();
			int nLevel = arFile.findDimension("N_LEVELS").getLength();

			if (log.isDebugEnabled()) {
				log.debug("N_PROF = {}", nProf);
				log.debug("N_CALIB = {}", nCalib);
				log.debug("N_HISTORY = {}", nHistory);
				log.debug("N_PARAM = {}", nParam);
				log.debug("N_LEVELS = {}", nLevel);
			}

			stdout.printf(" %6d %8d %8d %10d %8d %9d  %s\n", nFile, nProf, nCalib, nHistory, nParam, nLevel,
					inFileName.trim());

			ArrayList<String> varNameList = arFile.getVariableNames();

			// .............group-print requested..............
			// .. need to determine the variables that will be group-printed
			// .. need to determine the "main" variable for the group

			HashSet<String> groupMainVar = new HashSet<String>();
			HashSet<String> groupPrintVar = new HashSet<String>();

			if (group_prof) {
				log.debug(".....group_prof requested.....");

				for (String varName : varNameList) {
					ArgoVariable specVar = spec.getVariable(varName);

					if (specVar.isParamVar()) {
						// ..potential "group-print" variable

						String gName = spec.inGroup(varName);
						String altName = null;

						// ..the possible group names are: null, PARAM, PARAM_QC, PARAM_ADJUSTED
						// ..decode PARAM from that

						String pName = gName;

						if (gName == null) {
							pName = varName;
						} else if (pName.endsWith("_QC")) {
							pName = gName.substring(0, pName.length() - 3);
							altName = pName + "_ADJUSTED";
						} else if (gName.endsWith("_ADJUSTED")) {
							pName = gName.substring(0, pName.length() - 9);
							altName = pName + "_QC";
						}

						// ..check the PARAM to see if it is group-printable (ie, rank 2)

						if (varName.equals(pName)) {

							// ..this is the "main" variable
							// ..need to see if it has extra dimensions -- can't group-print those

							int r = arFile.findVariable(varName).getRank();

							if (r == 2) {
								// ..we can group-print this group
								groupMainVar.add(pName);

								if (gName != null) {
									groupPrintVar.addAll(spec.groupMembers(gName));
								}
								if (altName != null) {
									groupPrintVar.addAll(spec.groupMembers(altName));
								}

								log.debug("group-print variable '{}' (groups '{}', '{}'). rank = {}", pName, gName,
										altName, r);

							} else {
								stdout.println("        " + varName + ": cannot group-print variables > rank 2");
								log.debug("skip group-print for '{}'. rank = {}", pName, r);
							}
						}
					}
				}
			}

			// .............loop over the profiles.....................

			for (int n = 0; n < (only_first ? 1 : nProf); n++) {
				// ..Need plfm_number, cycle, position and time for the header

				log.debug("******* PROFILE #{} ********", n);

				String plfm_num = arFile.readString("PLATFORM_NUMBER", n);
				int cycle = arFile.readInt("CYCLE_NUMBER", n);

				double lat = arFile.readDouble("LATITUDE", n);
				double lon = arFile.readDouble("LONGITUDE", n);

				double juld = arFile.readDouble("JULD", n);
				Date dateJuld = ArgoDate.get(juld);
				String juldDTG = ArgoDate.format(dateJuld);

				if (id_inc_prof) {
					outFile.printf("\n===> %s_%03d_%03d %10.3f %10.3f %s\n", plfm_num.trim(), cycle, n, lat, lon,
							juldDTG);
				} else {
					outFile.printf("\n===> %s_%03d %10.3f %10.3f %s\n", plfm_num.trim(), cycle, lat, lon, juldDTG);
				}

				outFile.printf("   %-25s %6d\n", "Dimensions: N_PROF", nProf);
				outFile.printf("   %-25s %6d\n", "Dimensions: N_PARAM", nParam);
				outFile.printf("   %-25s %6d\n", "Dimensions: N_LEVELS", nLevel);
				outFile.printf("   %-25s %6d\n", "Dimensions: N_CALIB", nCalib);
				outFile.printf("   %-25s %6d\n", "Dimensions: N_HISTORY", nHistory);
				outFile.printf("   %-25s %6d (of %6d)\n", "profile-number", n + 1, nProf);

				// ............loop over variables............

				for (String varName : varNameList) {
					log.debug("VARIABLE: {}", varName);

					if (varName.startsWith("HISTORY")) {
						log.debug(" skip history variable '{}'", varName);
						continue;
					}

					ArgoVariable specVar = spec.getVariable(varName);

					if (specVar == null) {
						stdout.println("   WARNING: Variable not part of spec: '" + varName + "'");
						log.debug(" skip var not in spec '{}'", varName);
						continue;
					}

					DataType varType = specVar.getType();

					// ..handle it based on the type of data
					// ..if-else structure based on knowledge of file structure

					if (group_prof && groupPrintVar.contains(varName)) {
						// ..requested to "group-print" param variables
						// ..and this variable has been determined to be a group-print variable

						if (groupMainVar.contains(varName)) {
							// ..this is the main var for the group --- print it now
							dumpGroup(arFile, outFile, varName, varType, n, no_trailing);
							log.debug("   group-print = '{}'", varName);
						}

					} else if (specVar.isPerCalib()) {
						log.debug(" isPerCalib: '{}'  rank: {}", varName, specVar.getRank());

						// ..*********variables dependent on N_CALIB*************

						// ..only the rank 4 (N_PROF, N_CALIB, N_PARAM, str) variables
						// ..PARAMETER + the SCI*_CALIB_*

						boolean hdr = true;
						for (int m = 0; m < nCalib; m++) {
							String str[] = arFile.readStringArr(varName, n, m);

							if (str != null) {
								if (hdr) {
									outFile.printf("   %-25s\n", varName);
									hdr = false;
								}

								int len = str.length;
								for (int k = 0; k < len; k++) {
									outFile.printf("   %5d,%5d) %s\n", m, k, str[k]);
								}

							} else {
								stdout.println("   " + varName + " not in the file");
								break;
							}
						}

					} else if (specVar.isPerLevel()) {

						// ..***********variables dependent on N_LEVEL**************

						// ..only the phys <PARAM> variables - "(N_PROF, N_LEVEL [, n_values])" data

						log.debug(" variable isPerLevel: '{}'  rank: {}", varName, specVar.getRank());

						int vRank = 2;
						if (specVar.canHaveExtraDimensions()) {
							vRank = arFile.findVariable(varName).getRank();
						}

						if (vRank == 2) {

							if (varType == DataType.CHAR) {
								String str = arFile.readString(varName, n);

								if (str != null) {
									outFile.printf("   %-25s %s\n", varName, str.trim());
								} else {
									stdout.println("   " + varName + " not in the file");
								}

								/*
								 * } else if (varType == DataType.INT) { int v[] = arFile.readIntArr(varName,
								 * n);
								 * 
								 * if (v != null) { outFile.printf("   %-25s", varName); for (int m = 0; m <
								 * v.length; m++) { outFile.printf("   %5d) %15d\n", m, v[m]); } } else {
								 * stdout.println("   "+varName+" not in the file"); }
								 */
							} else if (varType == DataType.DOUBLE) {
								// stdout.println("readDoubleArr "+varName+" "+n);
								double v[] = arFile.readDoubleArr(varName, n);

								if (v != null) {
									int len = v.length;
									if (no_trailing) {
										len = 0;
										for (int m = v.length - 1; m >= 0; m--) {
											if (!ArgoDataFile.is_99_999_FillValue(v[m])) {
												len = m + 1;
												break;
											}
										}
									}

									if (len <= 0) {
										outFile.printf("   %-25s No non-FillValue data for this profile\n", varName);

									} else {
										outFile.printf("   %-25s\n", varName);

										for (int m = 0; m < len; m++) {
											outFile.printf("   %5d) %15.5f\n", m, v[m]);
										}
									}
								} else {
									stdout.println("   " + varName + " not in the file");
								}

							} else if (varType == DataType.FLOAT) {
								// stdout.println("readFloatArr "+varName+" "+n);
								float v[] = arFile.readFloatArr(varName, n);

								if (v != null) {
									int len = v.length - 1;
									if (no_trailing) {
										len = 0;
										for (int m = v.length - 1; m >= 0; m--) {
											if (!ArgoDataFile.is_99_999_FillValue(v[m])) {
												len = m + 1;
												break;
											}
										}
									}

									if (len == 0) {
										outFile.printf("   %-25s No non-FillValue data for this profile\n", varName);

									} else {
										outFile.printf("   %-25s\n", varName);
										for (int m = 0; m < len; m++) {
											outFile.printf("   %5d) %15.5f\n", m, v[m]);
										}
									}
								} else {
									stdout.println("   " + varName + " not read");
									stdout.println("   message: " + ArgoDataFile.getMessage());
								}

							} else {
								stdout.println("   " + varName + ": data type '" + varType + "' not handled");
							}

						} else if (vRank == 3) {

							// ..this is an "extra dimension" variable

							if (varType == DataType.CHAR) {
								stdout.println("   " + varName + ": not currently handling CHAR rank " + vRank);
							} else if (varType == DataType.DOUBLE) {
								stdout.println("   " + varName + ": not currently handling DOUBLE rank " + vRank);
							} else if (varType == DataType.FLOAT) {

								float prm[][] = new float[nLevel][];

								for (int k = 0; k < nLevel; k++) {
									float v[] = arFile.readFloatArr(varName, n, k);
									prm[k] = Arrays.copyOf(v, v.length);
								}

								int len = nLevel;

								if (no_trailing) {
									len = 0;
									k_loop: for (int k = nLevel - 1; k >= 0; k--) {
										for (int m = 0; m < prm[k].length; m++) {
											if (!ArgoDataFile.is_99_999_FillValue(prm[k][m])) {
												len = k + 1;
												break k_loop;
											}
										}
									}
								}

								if (len <= 0) {
									outFile.printf("   %-25s No non-FillValue data for this profile\n", varName);

								} else {
									outFile.printf("   %-25s\n", varName);

									for (int k = 0; k < len; k++) {
										for (int m = 0; m < prm[k].length; m++) {
											outFile.printf("   %5d, %5d) %15.5f\n", k, m, prm[k][m]);
										}
									}
								}

							} else {
								stdout.println("   " + varName + ": data type '" + varType + "' not handled");
							}

						} else {
							stdout.println("   " + varName + ": not currently handling rank " + vRank);
							outFile.printf("   %25s   CANNOT PRINT RANK %d variable\n", varName, vRank);
						}

					} else if (specVar.isPerParam()) {

						// *************variables dependent on N_PARAM****************

						// ..in combination with if-else above... char (N_PROF, N_PARAM, [str])
						// ..- station_param(n_prof, n_param, str)
						// ..- parameter_data_mode(n_prof, n_param) [optional]

						log.debug(" isPerParam: '{}'  rank: {}", varName, specVar.getRank());

						if (varName.equals("PARAMETER_DATA_MODE")) {
							String str = arFile.readString(varName, n);

							if (str != null) {
								outFile.printf("   %-25s %s\n", varName, str.trim());
							} else {
								stdout.println("   " + varName + ": not in the file");
							}

						} else {
							String str[] = arFile.readStringArr(varName, n);

							if (str != null) {
								outFile.printf("   %-25s\n", varName);

								int len = str.length;
								/*
								 * if (no_trailing) { for (int m = v.length-1; m >= 0; m--) { if (!
								 * ArgoDataFile.is_99_999_FillValue(v[m])) { len = m + 1; break; } } }
								 */
								for (int m = 0; m < len; m++) {
									outFile.printf("   %5d) %s\n", m, str[m].trim());
								}
							} else {
								stdout.println("   " + varName + ": not in the file");
							}
						}

					} else if (specVar.isPerProfile()) {

						// *****************variable dependent on N_PROF*****************
						// ..in combination with above: [N_PROF] data

						log.debug(" isPerProfile");

						if (varType == DataType.CHAR) {
							String str;
							if (specVar.isString()) {
								// ..it is a char[n_prof, stringx]
								str = arFile.readString(varName, n);

							} else {
								// ..it is a char[n_prof]
								str = arFile.readString(varName);
								str = str.substring(n, n + 1);
							}

							if (str != null) {
								outFile.printf("   %-25s %s\n", varName, str.trim());
							} else {
								stdout.println("   " + varName + " not in the file");
							}

						} else if (varType == DataType.INT) {
							int i = arFile.readInt(varName, n);

							if (i != INT_MAX) {
								outFile.printf("   %-25s %15d\n", varName, i);
							} else {
								stdout.println("   " + varName + " not in the file");
							}

						} else if (varType == DataType.DOUBLE) {
							double d = arFile.readDouble(varName, n);

							if (d != DBL_NAN) {
								outFile.printf("   %-25s %15.5f\n", varName, d);
							} else {
								stdout.println("   " + varName + " not in the file");
							}

						} else {
							stdout.println("   data type '" + varType + "' not handled for " + varName);
						}

					} // ..end if isPerProfile
					else {
						log.debug(" independent variable: '{}'  rank: {}", varName, specVar.getRank());

						String str = arFile.readString(varName);

						if (str != null) {
							outFile.printf("   %-25s %s\n", varName, str.trim());
						} else {
							stdout.println("   " + varName + ": not in the file");
						}
					}

				} // ..end for varName
			} // ..end for nProf

			arFile.close();
		} // ..end for (inFileName)

		// ..............end of data writes.....................

		outFile.close();

	} // ..end main

	// ...................................
	// .. Help ..
	// ...................................
	public static void Help() {
		stdout.println("\n" + "Purpose: Dumps Argo Profile files\n" + "\n"
				+ "Usage: java DumpProfile [options] spec-dir " + "dump-file input-files ...\n" + "\n" + "Options:\n"
				+ "   -list-file <list-file-path>  File containing list of files to process\n" + "     -l\n"
				+ "   -only-first (-1)            Only dump first profile in data file(s)\n"
				+ "                                  default: Dump all profiles\n"
				+ "   -no-trailing-missing (-b)   Does not print missing levels at the end of profiles\n"
				+ "   -id-includes-prof (-i)      Ob-id (header line) includes the prof # within the file\n"
				+ "   -group-prof (-g)            Group profile variables together\n" + "\n" + "Arguments:\n"
				+ "   spec-dir       Directory to specification files\n" + "   dump-file      Output file\n"
				+ "   input-files    Input Argo NetCDF profile file(s)\n" + "\n" + "Input Files:\n"
				+ "   Names of input files are determined as follows (priority order):\n"
				+ "   1) -list-file              List of names will be read from <list-file-path>\n"
				+ "   2) [file-names] argument   Files listed on command-line will be processed\n" + "\n");
		return;
	}

	public static void dumpGroup(ArgoProfileFile in, PrintWriter out, String groupName, DataType varType, int nProf,
			boolean no_trailing_missing) {
		log.debug("....start dumpGroup '{}'......", groupName);

		// ..read PROFILE_..._QC

		String name = "PROFILE_" + groupName + "_QC";
		String prof_qc = in.readString(name);

		// ..read PARAM and related variables

		float[] prm = null;
		String sPrm = groupName;
		String prm_qc = null;
		String sPQc = groupName + "_QC";
		float[] prm_adj = null;
		String sAdj = groupName + "_ADJUSTED";
		float[] prm_err = null;
		String sErr = groupName + "_ADJUSTED_ERROR";
		String adj_qc = null;
		String sAQc = groupName + "_ADJUSTED_QC";

		if (varType == DataType.DOUBLE) {
			// stdout.println("readDoubleArr "+groupName+" "+n);
			double v[] = in.readDoubleArr(sPrm, nProf);
			if (v != null) {
				prm = new float[v.length];
				for (int m = 0; m < v.length; m++) {
					prm[m] = (float) v[m];
				}
			}

			v = in.readDoubleArr(sAdj, nProf);
			if (v != null) {
				prm_adj = new float[v.length];
				for (int m = 0; m < v.length; m++) {
					prm_adj[m] = (float) v[m];
				}
			}

			v = in.readDoubleArr(sErr, nProf);
			if (v != null) {
				prm_err = new float[v.length];
				for (int m = 0; m < v.length; m++) {
					prm_err[m] = (float) v[m];
				}
			}

		} else if (varType == DataType.FLOAT) {
			// stdout.println("readFloatArr "+groupName+" "+n);
			prm = in.readFloatArr(sPrm, nProf);
			prm_adj = in.readFloatArr(sAdj, nProf);
			prm_err = in.readFloatArr(sErr, nProf);

		}

		prm_qc = in.readString(sPQc, nProf);
		adj_qc = in.readString(sAQc, nProf);

		if (prm == null) {
			stdout.println("WARNING: STATION_PARAMETER 'groupName' is not a PARAM variable in the file ");
			return;
		}

		// .....determine where to end profile list.....

		int LEN = prm.length;

		if (no_trailing_missing) {
			LEN = -1;
			for (int m = prm.length - 1; m > -1; m--) {
				if (prm != null && !ArgoDataFile.is_99_999_FillValue(prm[m])) {
					LEN = m + 1;
					break;
				}
				if (prm_adj != null && !ArgoDataFile.is_99_999_FillValue(prm_adj[m])) {
					LEN = m + 1;
					break;
				}
				if (prm_err != null && !ArgoDataFile.is_99_999_FillValue(prm_err[m])) {
					LEN = m + 1;
					break;
				}
			}
		}

		// ............output..........
		// ..PROFILE_..._QC
		if (prof_qc != null) {
			out.printf("   %-25s %c\n", name, prof_qc.charAt(nProf));
		}

		// ..profile

		if (LEN <= 0) {
			out.printf("   %-25s No non-FillValue data for this profile\n", sPrm);

		} else {

			out.println("   " + sPrm);
			if (LEN > 0) {
				out.printf("   %5s  %15s %4s %15s %15s %4s\n", "N", sPrm, "_QC", "_ADJUSTED", "_ERROR", "_QC");
			}

			for (int m = 0; m < LEN; m++) {
				out.printf("   %5d) %15.5f", m, prm[m]);

				if (prm_qc != null) {
					out.printf("    %c", prm_qc.charAt(m));
				} else {
					out.print("    -");
				}

				if (prm_adj != null) {
					out.print(String.format(" %15.5f", prm_adj[m]));
				} else {
					out.print("               -");
				}

				if (prm_err != null) {
					out.print(String.format(" %15.5f", prm_err[m]));
				} else {
					out.print("               -");
				}

				if (adj_qc != null) {
					out.printf("    %c", adj_qc.charAt(m));
				} else {
					out.print("    -");
				}

				out.print("\n");
			}
		}
	}

	// ......................Variable Declarations................

	// ..standard i/o shortcuts
	static PrintStream stdout = new PrintStream(System.out);
	static PrintStream stderr = new PrintStream(System.err);

	static {
		System.setProperty("logfile.name", "DumpProfile_LOG");
	}

	private static final Logger log = LogManager.getLogger("DumpProfile");

	// ..class variables
	static String outFileName;
	static String specFileName;

	// static boolean addHistory = false;
	static HashMap<String, Integer> n_prof;
	static HashMap<String, Integer> n_param;
	static HashMap<String, Integer> n_levels;
	static HashMap<String, Integer> n_calib;
	// static HashMap<String, Integer> n_history;
	static Set<String> parameters = new HashSet<String>();

	static int INT_MAX = Integer.MAX_VALUE;
	static double DBL_NAN = Double.NaN;

	private final static DecimalFormat fFmt = new DecimalFormat("####0.0000;-####0.0000");
	private final static DecimalFormat nFmt = new DecimalFormat("#####");

} // ..end class
