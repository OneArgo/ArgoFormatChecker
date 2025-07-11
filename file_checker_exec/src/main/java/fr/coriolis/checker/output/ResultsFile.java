package fr.coriolis.checker.output;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.HashSet;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.core.ArgoDataFile;
import fr.coriolis.checker.core.ArgoDataFile.FileType;
import fr.coriolis.checker.core.ValidationResult;
import fr.coriolis.checker.specs.ArgoDate;
import fr.coriolis.checker.validators.ArgoFileValidator;

public class ResultsFile {

	// ..object variables

	private PrintWriter out;
	boolean doXml;

	StringWriter stringWriter;
	XMLStreamWriter xml;

	// ArgoDataFile argo = null;

	// ..class variables
	private final static DecimalFormat cycleFmt = new DecimalFormat("000");
	private final static DecimalFormat dFmt = new DecimalFormat("####0.0000;-####0.0000");

	private static final Logger log = LogManager.getLogger("ResultsFile");

	// .............................................................
	//
	// constructors
	//
	// .............................................................

	public ResultsFile(boolean doXml, String resultsFileName, String fcVersion, String spVersion, String inputFileName)
			throws IOException, XMLStreamException {
		out = new PrintWriter(new BufferedWriter(new java.io.FileWriter(resultsFileName)));

		this.doXml = doXml;

		if (doXml) {
			// ......if XML, open the XMLStreamWriter

			stringWriter = new StringWriter();

			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			xml = xMLOutputFactory.createXMLStreamWriter(stringWriter);

			xml.writeStartDocument();

			xml.writeStartElement("FileCheckResults");
			xml.writeAttribute("filechecker_version", fcVersion);
			xml.writeAttribute("spec_version", spVersion);

			xml.writeStartElement("file");
			xml.writeCharacters(inputFileName);
			xml.writeEndElement();

			log.debug("...ResultsFile: xml file");

		} else {
			out.println("VERSION-INFO: FileChecker = '" + fcVersion + "' Specification = '" + spVersion + "'");
			out.println("FILE-NAME: " + inputFileName);
			log.debug("...ResultsFile: text file");
		}

	} // ..end contstructor

	// .....................................................................
	//
	// methods
	//
	// .....................................................................

	public void close()
			throws IOException, XMLStreamException, TransformerConfigurationException, TransformerException {
		if (out != null) {
			if (doXml) {
				xml.writeEndElement();
				xml.writeEndDocument();

				xml.flush();
				xml.close();

				String xmlString = stringWriter.getBuffer().toString();
				stringWriter.close();
				xmlString = scrubInvalidXmlChar(xmlString);

				Source xmlInput = new StreamSource(new StringReader(xmlString));

				StringWriter stringWriter = new StringWriter();
				Result xmlOutput = new StreamResult(stringWriter);

				// out.println(xmlString);
				// out.println();
				// out.flush();

				log.debug("transform xml");

				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				transformerFactory.setAttribute("indent-number", 2);
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				transformer.setErrorListener(new ErrorListener() {
					@Override
					public void error(TransformerException e) throws TransformerException {
						log.warn(e.getMessage());
					}

					@Override
					public void fatalError(TransformerException e) throws TransformerException {
						log.warn(e.getMessage());
					}

					@Override
					public void warning(TransformerException e) throws TransformerException {
						log.warn(e.getMessage());
					}
				});

				try {
					transformer.transform(xmlInput, xmlOutput);
				} catch (TransformerException e) {
					log.warn("Transformer failed, retrying after re-scrubbing", e);
					// ..tranform exception: probably an invalid character
					// ..scrub and re-transform
					xmlString = scrubInvalidXmlChar(xmlString);

					xmlInput = new StreamSource(new StringReader(xmlString));

					stringWriter = new StringWriter();
					xmlOutput = new StreamResult(stringWriter);

					transformer.transform(xmlInput, xmlOutput);
					log.debug("close: caught TransformerException - cleaned up input");
				}

				log.debug("output xml");
				out.println(stringWriter.toString());
			}
			out.close();
		}
	} // ..end close

	public void openError(Exception e) throws XMLStreamException {
		if (doXml) {
			xml.writeStartElement("status");
			xml.writeCharacters("ERROR");
			xml.writeEndElement();

			xml.writeStartElement("phase");
			xml.writeCharacters("OPEN-FILE");
			xml.writeEndElement();

			xml.writeStartElement("errors");
			xml.writeAttribute("number", "1");
			xml.writeStartElement("error");
			xml.writeCharacters(e.toString());
			xml.writeEndElement();
			xml.writeEndElement();

		} else {
			out.println("ERROR: Open exception:\n" + e);
			out.println("PHASE: OPEN-FILE");
		}
	} // ..end openError

	public void oldDModeFile(String dacName, String version) throws XMLStreamException {
		if (doXml) {
			xml.writeStartElement("status");
			xml.writeCharacters("FILE-REJECTED");
			xml.writeEndElement();

			xml.writeStartElement("phase");
			xml.writeCharacters("DMODE-VERSION-CHECK");
			xml.writeEndElement();

			xml.writeStartElement("metadata");
			xml.writeStartElement("dac");
			xml.writeCharacters(dacName);
			xml.writeEndElement();
			xml.writeStartElement("DATA_TYPE");
			xml.writeCharacters("Argo profile");
			xml.writeEndElement();
			xml.writeStartElement("FORMAT_VERSION");
			xml.writeCharacters(version);
			xml.writeEndElement();
			xml.writeEndElement();

			xml.writeStartElement("errors");
			xml.writeAttribute("number", "1");
			xml.writeStartElement("error");
			xml.writeCharacters("D-mode: Version prior to v3.1 is not allowed");
			xml.writeEndElement();
			xml.writeEndElement();

			xml.writeStartElement("warnings");
			xml.writeAttribute("number", "0");
			xml.writeEndElement();

		} else {
			out.println("STATUS: FILE-REJECTED");
			out.println("PHASE: DMODE-VERSION-CHECK");
			out.println("META-DATA: start");
			out.println("DAC: " + dacName);
			out.println("TYPE: Argo profile");
			out.println("FORMAT_VERSION: " + version);
			out.println("META-DATA: end");
			out.println("FORMAT-ERRORS: start");
			out.println("D-mode: Pre-v3.1 is not allowed");
			out.println("FORMAT-ERRORS: end");
			out.println("FORMAT-WARNINGS: start");
			out.println("FORMAT-WARNINGS: end");
		}
	} // ..end oldDModeFile

	public void notArgoFile(String dacName) throws XMLStreamException {
		if (doXml) {
			xml.writeStartElement("status");
			xml.writeCharacters("FILE-REJECTED");
			xml.writeEndElement();

			xml.writeStartElement("phase");
			xml.writeCharacters("OPEN-FILE");
			xml.writeEndElement();

			xml.writeStartElement("metadata");
			xml.writeStartElement("dac");
			xml.writeCharacters(dacName);
			xml.writeEndElement();
			xml.writeEndElement();

			xml.writeStartElement("errors");
			xml.writeAttribute("number", "1");
			xml.writeStartElement("error");
			xml.writeCharacters(ValidationResult.getMessage());
			xml.writeEndElement();
			xml.writeEndElement();

		} else {
			out.println("STATUS: FILE-REJECTED");
			out.println("PHASE: OPEN-FILE");
			out.println("META-DATA: start");
			out.println("DAC: " + dacName);
			out.println("META-DATA: end");
			out.println("FORMAT-ERRORS: start");
			out.println(ValidationResult.getMessage());
			out.println("FORMAT-ERRORS: end");
			out.println("FORMAT-WARNINGS: start");
			out.println("FORMAT-WARNINGS: end");
		}
	} // ..end notArgoFile

	public void formatErrorMessage(String phase) throws XMLStreamException {
		if (doXml) {
			xml.writeStartElement("status");
			xml.writeCharacters("ERROR");
			xml.writeEndElement();

			xml.writeStartElement("phase");
			xml.writeCharacters(phase);
			xml.writeEndElement();

			xml.writeStartElement("errors");
			xml.writeAttribute("number", "1");
			xml.writeStartElement("error");
			xml.writeCharacters("Format check failed. " + ValidationResult.getMessage());
			xml.writeEndElement();
			xml.writeEndElement();

		} else {
			out.println("ERROR: Format check failed." + ValidationResult.getMessage());
			out.println("PHASE: " + phase);
		}
	}

	public void dataErrorMessage(String type) throws XMLStreamException {
		String phase = "DATA-VALIDATION";

		if (doXml) {
			xml.writeStartElement("status");
			xml.writeCharacters("ERROR");
			xml.writeEndElement();

			xml.writeStartElement("phase");
			xml.writeCharacters(phase);
			xml.writeEndElement();

			xml.writeStartElement("errors");
			xml.writeAttribute("number", "1");
			xml.writeStartElement("error");
			xml.writeCharacters(type + " validation failed. " + ValidationResult.getMessage());
			xml.writeEndElement();
			xml.writeEndElement();

		} else {
			out.println("ERROR: " + type + " validation failed: " + ValidationResult.getMessage());
		}
	}

	public void statusAndPhase(boolean accepted, String phase) throws XMLStreamException {
		final String acc = "FILE-ACCEPTED";
		final String rej = "FILE-REJECTED";

		String status;
		if (accepted) {
			status = acc;
		} else {
			status = rej;
		}

		if (doXml) {
			xml.writeStartElement("status");
			xml.writeCharacters(status);
			xml.writeEndElement();

			xml.writeStartElement("phase");
			xml.writeCharacters(phase);
			xml.writeEndElement();

		} else {
			out.println("STATUS: " + status);
			out.println("PHASE: " + phase);
		}
	}

	public void metaData(String dacName, ArgoDataFile argo, boolean formatPassed, boolean doPsalStats)
			throws XMLStreamException {
		// ...............report meta-data results...............
		// ..status is that open was successful
		// ..- that means identified as Argo netCDF file (DATA_TYPE and FORMAT_VERSION)
		// ..- format may or may not have passed
		// .. - if format did not pass, trying to retrieve the numeric meta-data
		// .. may cause aborts -- i think string types are safe
		// ..try to get as much of the meta-data as exists, but avoid aborts

		String str;
		int i;

		if (doXml) {
			xml.writeStartElement("metadata");

			xml.writeStartElement("dac");
			xml.writeCharacters(dacName);
			xml.writeEndElement();

		} else {
			out.println("META-DATA: start");
			out.println("DAC: " + dacName);
		}
		log.debug("meta-data: dac = '" + dacName + "'");

		// ..it is implied by the code that to get here
		// ..openSuccessful must be true
		switch (argo.fileType()) {
		case METADATA:
			str = "Argo meta-data";
			break;
		case BIO_PROFILE:
			str = "B-Argo profile";
			break;
		case BIO_TRAJECTORY:
			str = "B-Argo trajectory";
			break;
		case PROFILE:
			str = "Argo profile";
			break;
		case TECHNICAL:
			str = "Argo technical data";
			break;
		case TRAJECTORY:
			str = "Argo trajectory";
			break;
		default:
			str = "File type not determined";
			break;
		}

		if (doXml) {
			xml.writeStartElement("DATA_TYPE");
			xml.writeCharacters(str);
			xml.writeEndElement();

		} else {
			out.println("TYPE: " + str);
		}
		log.debug("meta-data: type = '" + str + "'");

		metaStr(argo, "FORMAT_VERSION", (String) null);
		metaStr(argo, "DATE_UPDATE", (String) null);

		if (argo.fileType() == FileType.PROFILE || argo.fileType() == FileType.BIO_PROFILE) {
			metaStrArray(argo, "DATA_CENTRE", (String) null);
			metaStrArray(argo, "PLATFORM_NUMBER", (String) null);
			metaStrArray(argo, "PI_NAME", (String) null);
			metaStrArray(argo, "WMO_INST_TYPE", (String) null);
			metaStr(argo, "DATA_MODE", (String) null);
			metaStr(argo, "DIRECTION", (String) null);

			if (formatPassed) { // ..skip these if format was bad to prevent potential aborts
				i = argo.getDimensionLength("N_PROF");
				if (doXml) {
					xml.writeStartElement("N_PROF");
					xml.writeCharacters(Integer.toString(i));
					xml.writeEndElement();
				} else {
					out.println("N_PROF: " + i);
				}
				log.debug("n_prof: {}", i);

				i = argo.getDimensionLength("N_LEVELS");
				if (doXml) {
					xml.writeStartElement("N_LEVELS");
					xml.writeCharacters(Integer.toString(i));
					xml.writeEndElement();
				} else {
					out.println("N_LEVELS: " + i);
				}
				log.debug("n_levels: {}", i);

				metaIntArray(argo, "CYCLE_NUMBER", cycleFmt);
				metaTimeArray(argo, "JULD", (String) null);
				metaDoubleArray(argo, "LATITUDE", dFmt);
				metaDoubleArray(argo, "LONGITUDE", dFmt);
				metaStr(argo, "JULD_QC", (String) null);
				metaStr(argo, "POSITION_QC", (String) null);
			}

			metaStr(argo, "PROFILE_TEMP_QC", (String) null);
			metaStr(argo, "PROFILE_PSAL_QC", (String) null);
			metaStr(argo, "PROFILE_DOXY_QC", (String) null);
			metaStationParameters(argo);

			if (formatPassed && argo.fileType() == FileType.PROFILE) {
				addMetaPsalStats(argo);
			}

		} else if (argo.fileType() == FileType.TRAJECTORY || argo.fileType() == FileType.BIO_TRAJECTORY) {
			metaStr(argo, "DATA_CENTRE", (String) null);
			metaStr(argo, "PLATFORM_NUMBER", (String) null);
			metaStr(argo, "PI_NAME", (String) null);
			metaStr(argo, "WMO_INST_TYPE", (String) null);
			metaStr(argo, "DATA_MODE", (String) null);

			if (formatPassed) { // ..skip these if format was bad to prevent potential aborts
				double l[], min, max;
				l = argo.readDoubleArr("LATITUDE");
				min = 99999.D;
				max = -99999.D;

				for (double dbl : l) {
					if (dbl < 99990.D) {
						if (dbl > max) {
							max = dbl;
						}
						if (dbl < min) {
							min = dbl;
						}
					}
				}
				if (max < -99990.D) {
					max = 99999.D;
				}

				if (doXml) {
					xml.writeStartElement("min_latitude");
					xml.writeCharacters(dFmt.format(min));
					xml.writeEndElement();
				} else {
					out.println("MIN-LATITUDE:" + dFmt.format(min));
				}
				log.debug("min-latitude: '" + min + "'");

				if (doXml) {
					xml.writeStartElement("max_latitude");
					xml.writeCharacters(dFmt.format(max));
					xml.writeEndElement();
				} else {
					out.println("MAX-LATITUDE:" + dFmt.format(max));
				}
				log.debug("max-latitude: '" + max + "'");

				l = argo.readDoubleArr("LONGITUDE");
				min = 99999.D;
				max = -99999.D;

				for (double dbl : l) {
					if (dbl < 99990.D) {
						if (dbl > max) {
							max = dbl;
						}
						if (dbl < min) {
							min = dbl;
						}
					}
				}
				if (max < -99990.D) {
					max = 99999.D;
				}

				if (doXml) {
					xml.writeStartElement("min_longitude");
					xml.writeCharacters(dFmt.format(min));
					xml.writeEndElement();
				} else {
					out.println("MIN-LONGITUDE:" + dFmt.format(min));
				}
				log.debug("min-longitude: '" + min + "'");

				if (doXml) {
					xml.writeStartElement("max_longitude");
					xml.writeCharacters(dFmt.format(max));
					xml.writeEndElement();
				} else {
					out.println("MAX-LONGITUDE:" + dFmt.format(max));
				}
				log.debug("max-longitude: '" + max + "'");

				if (argo.fileType() == FileType.BIO_TRAJECTORY) {
					metaTrajectoryParameters(argo);
				}
			}

		} else if (argo.fileType() == FileType.METADATA) {
			metaStr(argo, "DATA_CENTRE", (String) null);
			metaStr(argo, "PLATFORM_NUMBER", (String) null);
			metaStr(argo, "PI_NAME", (String) null);
			metaStr(argo, "WMO_INST_TYPE", (String) null);

		} else if (argo.fileType() == FileType.TECHNICAL) {
			metaStr(argo, "DATA_CENTRE", (String) null);
			metaStr(argo, "PLATFORM_NUMBER", (String) null);
		}

		if (doXml) {
			xml.writeEndElement();
		} else {
			out.println("META-DATA: end");
		}
	} // ..end metaData

	public void addMetaPsalStats(ArgoDataFile argo) throws XMLStreamException {
		// ...............report PSAL adjustment statistics...............
		// ..assumes:
		// ..- Argo-open was successful
		// ..- this is a core-profile file (checked)
		// ..- format has been passed

		double[] stats = argo.computePsalAdjStats();

		double[] arr1 = { stats[0] };
		double[] arr2 = { stats[1] };

		metaDoubleValArray(arr1, "psal-adj-mean", dFmt);
		metaDoubleValArray(arr2, "psal-adj-sdev", dFmt);

	}// ..end addMetaPsalStats

	// ************************** errorsAndWarnings ************************

	public void errorsAndWarnings(ArgoFileValidator argoFileValidator) throws XMLStreamException {
		if (doXml) {
			xml.writeStartElement("errors");
			xml.writeAttribute("number", Integer.toString(argoFileValidator.getValidationResult().nFormatErrors()));
		} else {
			out.println("FORMAT-ERRORS: start");
		}
		log.debug("format errors:" + argoFileValidator.getValidationResult().nFormatErrors());

		for (String err : argoFileValidator.getValidationResult().getErrors()) {
			if (doXml) {
				xml.writeStartElement("error");
				xml.writeCharacters(err);
				xml.writeEndElement();
			} else {
				out.println(err + "\n");
			}
			log.debug(err);
		}

		if (doXml) {
			xml.writeEndElement();
		} else {
			out.println("FORMAT-ERRORS: end");
		}
		log.debug("...end errors");

		// ...............report warnings................
		if (doXml) {
			xml.writeStartElement("warnings");
			xml.writeAttribute("number", Integer.toString(argoFileValidator.getValidationResult().nFormatWarnings()));
		} else {
			out.println("FORMAT-WARNINGS: start");
		}
		log.debug("format warnings: " + argoFileValidator.getValidationResult().nFormatWarnings());

		for (String err : argoFileValidator.getValidationResult().getWarnings()) {
			if (doXml) {
				xml.writeStartElement("warning");
				xml.writeCharacters(err);
				xml.writeEndElement();
			} else {
				out.println(err + "\n");
			}
			log.debug(err);
		}

		if (!doXml) {
			out.println("FORMAT-WARNINGS: end");
		}

		log.debug("...end warnings");
	}// ..end errorsAndWarnings

	// ************************** metaStr **************************

	private void metaStr(ArgoDataFile argo, String var, String fmt) throws XMLStreamException {
		String str = argo.readString(var);
		if (str == null) {
			str = "null";
		}

		if (doXml) {
			xml.writeStartElement(var);
		} else {
			out.print(var + ": ");
		}

		if (fmt == null) {
			if (doXml) {
				xml.writeCharacters(str);
				xml.writeEndElement();
			} else {
				out.println(str);
			}

		} else {
			if (doXml) {
				xml.writeCharacters(String.format(fmt, str));
				xml.writeEndElement();
			} else {
				out.println(String.format(fmt, str));
			}
		}

		log.debug("meta-data: '" + var + "' = '" + str + "' (single string)");
	}// ..end metaStr

	// ************************ metaStrArray ****************************

	private void metaStrArray(ArgoDataFile argo, String var, String fmt) throws XMLStreamException {
		String arr[] = argo.readStringArr(var);

		if (doXml) {
			xml.writeStartElement(var);
		} else {
			out.print(var + ":");
		}

		if (fmt == null) {
			char comma = ' ';
			for (String val : arr) {
				if (doXml) {
					xml.writeCharacters(comma + val.trim());
				} else {
					out.print(comma + val.trim());
				}
				comma = ',';
			}

		} else {
			char comma = ' ';
			for (String val : arr) {
				if (doXml) {
					xml.writeCharacters(comma + String.format(fmt, val.trim()));
				} else {
					out.print(comma + String.format(fmt, val.trim()));
				}
				comma = ',';
			}
		}

		if (doXml) {
			xml.writeEndElement();
		} else {
			out.println();
		}
		log.debug("meta-data: '" + var + "' (string array)");
	}

	// ********************* metaIntArray *******************************

	private void metaIntArray(ArgoDataFile argo, String var, DecimalFormat fmt) throws XMLStreamException {
		int[] arr = argo.readIntArr(var);

		if (doXml) {
			xml.writeStartElement(var);
		} else {
			out.print(var + ":");
		}

		if (fmt == null) {
			char comma = ' ';
			for (int val : arr) {
				if (doXml) {
					xml.writeCharacters(comma + Integer.toString(val));
				} else {
					out.print(comma + val);
				}
				comma = ',';
			}

		} else {
			char comma = ' ';
			for (int val : arr) {
				if (doXml) {
					xml.writeCharacters(comma + fmt.format(val));
				} else {
					out.print(comma + fmt.format(val));
				}
				comma = ',';
			}
		}

		if (doXml) {
			xml.writeEndElement();
		} else {
			out.println();
		}
		log.debug("meta-data: '" + var + "'");
	}// ..end metaIntArray

	// ******************** metaDoubleArray ********************************

	private void metaDoubleArray(ArgoDataFile argo, String var, DecimalFormat fmt) throws XMLStreamException {
		double[] arr = argo.readDoubleArr(var);

		if (doXml) {
			xml.writeStartElement(var);
		} else {
			out.print(var + ":");
		}

		if (fmt == null) {
			char comma = ' ';
			for (double val : arr) {
				if (doXml) {
					xml.writeCharacters(comma + Double.toString(val));
				} else {
					out.print(comma + val);
				}
				comma = ',';
			}

		} else {
			char comma = ' ';
			for (double val : arr) {
				if (doXml) {
					xml.writeCharacters(comma + fmt.format(val));
				} else {
					out.print(comma + fmt.format(val));
				}
				comma = ',';
			}
		}

		if (doXml) {
			xml.writeEndElement();
		} else {
			out.println();
		}

		log.debug("meta-data: '" + var + "'");
	}

	// ******************** metaDoubleValArray ********************************

	private void metaDoubleValArray(double[] arr, String var, DecimalFormat fmt) throws XMLStreamException {
		if (doXml) {
			xml.writeStartElement(var);
		} else {
			out.print(var + ":");
		}

		if (fmt == null) {
			char comma = ' ';

			for (double val : arr) {
				if (doXml) {
					xml.writeCharacters(comma + Double.toString(val));
				} else {
					out.print(comma + val);
				}
				comma = ',';
			}

		} else {
			char comma = ' ';
			for (double val : arr) {
				if (doXml) {
					xml.writeCharacters(comma + fmt.format(val));
				} else {
					out.print(comma + fmt.format(val));
				}
				comma = ',';
			}
		}

		if (doXml) {
			xml.writeEndElement();
		} else {
			out.println();
		}

		log.debug("meta-data: '" + var + "'");
	}// ..end metaDoubleVal

	// ************************** metaTimeArray *************************

	private void metaTimeArray(ArgoDataFile argo, String var, String fmt) throws XMLStreamException {
		double[] arr = argo.readDoubleArr(var);

		if (doXml) {
			xml.writeStartElement(var + "-dtg");
		} else {
			out.print(var + "-DTG:");
		}

		if (fmt == null) {
			char comma = ' ';
			for (double val : arr) {
				String str = ArgoDate.format(ArgoDate.get(val));
				if (doXml) {
					xml.writeCharacters(comma + str);
				} else {
					out.print(comma + str);
				}
				comma = ',';
			}

		} else {
			char comma = ' ';
			for (double val : arr) {
				String str = ArgoDate.format(ArgoDate.get(val));
				if (doXml) {
					xml.writeCharacters(comma + String.format(str, val));
				} else {
					out.print(comma + String.format(str, val));
				}
				comma = ',';
			}
		}

		if (doXml) {
			xml.writeEndElement();
		} else {
			out.println();
		}

		log.debug("meta-data: '" + var + "'");
	}// ..end metaTimeArray

	// ************************** metaStationParameters *************************

	private void metaStationParameters(ArgoDataFile argo) throws XMLStreamException {
		int n_prof = argo.getDimensionLength("N_PROF");
		int n_param = argo.getDimensionLength("N_PARAM");
		if (n_prof < 0 || n_param < 0) {
			return;
		}

		HashSet<String> set = new HashSet<String>();
		StringBuilder list = null;
		StringBuilder pMode = null;

		for (int i = 0; i < n_prof; i++) {
			String[] str = argo.readStringArr("STATION_PARAMETERS", i);
			String pdm = null;

			if (argo.fileType() == FileType.BIO_PROFILE) {
				pdm = argo.readString("PARAMETER_DATA_MODE", i);
				// log.debug ("*** prof {}: pdm = '{}'", i, pdm);
			}

			for (int j = 0; j < n_param; j++) {
				String s = str[j].trim();

				char p;
				if (pdm == null) {
					p = '-';
				} else {
					p = pdm.charAt(j);
				}

				if (!s.isEmpty()) {
					if (!set.contains(s)) {
						set.add(s);
						if (list == null) {
							list = new StringBuilder(s);
							pMode = new StringBuilder(String.valueOf(p));
							// log.debug("***** init: list = '{}'; pMode = '{}'", list, pMode);
						} else {
							list.append(" " + s);
							pMode.append(p);
							// log.debug("***** add: list = '{}'; pMode = '{}'", list, pMode);
						}
					}
				}
			}
		}

		if (doXml) {
			xml.writeStartElement("STATION_PARAMETERS");
		} else {
			out.print("STATION_PARAMETERS: ");
		}

		if (list != null) {
			if (doXml) {
				xml.writeCharacters(list.toString());
				xml.writeEndElement();
			} else {
				out.println(list);
			}
		}

		if (pMode != null) {
			if (doXml) {
				xml.writeStartElement("PARAMETER_DATA_MODE");
			} else {
				out.print("PARAMETER_DATA_MODE: ");
			}

			if (doXml) {
				xml.writeCharacters(pMode.toString());
				xml.writeEndElement();
			} else {
				out.println(pMode);
			}
		}

		log.debug("meta-data: 'STATION_PARAMETER' = '" + list + "' (single string)");
	}// ..end metaStationParameters

	// ************************** metaTrajectoryParameters *************************

	private void metaTrajectoryParameters(ArgoDataFile argo) throws XMLStreamException {
		int n_param = argo.getDimensionLength("N_PARAM");
		if (n_param < 0) {
			return;
		}

		HashSet<String> set = new HashSet<String>();
		StringBuilder list = null;
		StringBuilder pMode = null;

		String[] str = argo.readStringArr("TRAJECTORY_PARAMETERS");
		String pdm = null;

		for (int j = 0; j < n_param; j++) {
			String s = str[j].trim();

			char p;
			if (pdm == null) {
				p = '-';
			} else {
				p = pdm.charAt(j);
			}

			if (!s.isEmpty()) {
				if (!set.contains(s)) {
					set.add(s);
					if (list == null) {
						list = new StringBuilder(s);
						pMode = new StringBuilder(String.valueOf(p));
						// log.debug("***** init: list = '{}'; pMode = '{}'", list, pMode);
					} else {
						list.append(" " + s);
						pMode.append(p);
						// log.debug("***** add: list = '{}'; pMode = '{}'", list, pMode);
					}
				}
			}
		}

		if (doXml) {
			xml.writeStartElement("STATION_PARAMETERS");
		} else {
			out.print("STATION_PARAMETERS: ");
		}

		if (list != null) {
			if (doXml) {
				xml.writeCharacters(list.toString());
				xml.writeEndElement();
			} else {
				out.println(list);
			}
		}

		if (pMode != null) {
			if (doXml) {
				xml.writeStartElement("PARAMETER_DATA_MODE");
			} else {
				out.print("PARAMETER_DATA_MODE: ");
			}

			if (doXml) {
				xml.writeCharacters(pMode.toString());
				xml.writeEndElement();
			} else {
				out.println(pMode);
			}
		}

		log.debug("meta-data: 'TRAJECTORY_PARAMETER' = '" + list + "' (single string)");
	}// ..end metaTrajectoryParameters

	// ************************** convenience methods ******************************
	private String scrubInvalidXmlChar(String input) {
		if (input == null) {
			return null;
		}

		StringBuilder out = new StringBuilder();

		input.codePoints().forEach(cp -> {
			if ((cp == 0x9) || (cp == 0xA) || (cp == 0xD) || (cp >= 0x20 && cp <= 0xD7FF)
					|| (cp >= 0xE000 && cp <= 0xFFFD) || (cp >= 0x10000 && cp <= 0x10FFFF)) {
				out.appendCodePoint(cp);
			} else {
				out.append('-');
			}
		});

		return out.toString();

	}

} // ..end class ResultsFile
