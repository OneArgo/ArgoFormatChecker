package fr.coriolis.checker.tables;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.coriolis.checker.specs.SpecIO;
import fr.coriolis.checker.utils.NetUtils;

public final class ArgoNVSReferenceTable {

	private static PrintStream stderr = new PrintStream(System.err);

	public static enum RELEVANT_TABLES {
		DM_QC_FLAG("DM_QC_FLAG", "RD2"), PLATFORM_TYPE("PLATFORM_TYPE", "R23"), PLATFORM_MAKER("PLATFORM_MAKER", "R24"),
		PROF_QC_FLAG("PROF_QC_FLAG", "RP2"), POSITION_ACCURACY("POSITION_ACCURACY", "R05"),
		DATA_STATE_INDICATOR("DATA_STATE_INDICATOR", "R06"), HISTORY_ACTION("HISTORY_ACTION", "R07"),
		ARGO_WMO_INST_TYPE("ARGO_WMO_INST_TYPE", "R08"), POSITIONING_SYSTEM("POSITIONING_SYSTEM", "R09"),
		TRANS_SYSTEM("TRANS_SYSTEM", "R10"), VERTICAL_SAMPLING_SCHEME("VERTICAL_SAMPLING_SCHEME", "R16"),
		STATUS("STATUS", "R19"), GROUNDED("GROUNDED", "R20"), PLATFORM_FAMILY("PLATFORM_FAMILY", "R22"),
		SENSOR("SENSOR", "R25"), SENSOR_MAKER("SENSOR_MAKER", "R26"), SENSOR_MODEL("SENSOR_MODEL", "R27"),
		MEASUREMENT_CODE_ID("MEASUREMENT_CODE_ID", "R15"), TECHNICAL_PARAMETER_NAME("TECHNICAL_PARAMETER_NAME", "R14"),
		CONFIG_PARAMETER_NAME("CONFIG_PARAMETER_NAME", "R18"), PARAMETER("PARAMETER", "R03"),
		PROGRAM_NAME("PROGRAM_NAME", "R41");

		public final String name;
		public final String code;

		RELEVANT_TABLES(String name, String code) {
			this.name = name;
			this.code = code;
		}

		public static RELEVANT_TABLES fromName(String name) {
			for (RELEVANT_TABLES t : RELEVANT_TABLES.values()) {
				if (t.name.equals(name)) {
					return t;
				}
			}
			return null;
		}

		public String getCode() {
			return code;
		}
	};

	// ==========
	// ALL TABLES
	// ==========
	public static SkosCollection DM_QC_FLAG_TABLE;
	public static SkosCollection PLATFORM_TYPE_TABLE;
	public static SkosCollection PLATFORM_MAKER_TABLE;
	public static SkosCollection PROF_QC_FLAG_TABLE;
	public static SkosCollection POSITION_ACCURACY_TABLE;
	public static SkosCollection DATA_STATE_INDICATOR_TABLE;
	public static SkosCollection HISTORY_ACTION_TABLE;
	public static SkosCollection ARGO_WMO_INST_TYPE_TABLE;
	public static SkosCollection POSITIONING_SYSTEM_TABLE;
	public static SkosCollection TRANS_SYSTEM_TABLE;
	public static SkosCollection VERTICAL_SAMPLING_SCHEME_TABLE;
	public static SkosCollection STATUS_TABLE;
	public static SkosCollection GROUNDED_TABLE;
	public static SkosCollection PLATFORM_FAMILY_TABLE;
	public static SkosCollection SENSOR_TABLE;
	public static SkosCollection SENSOR_MAKER_TABLE;
	public static SkosCollection SENSOR_MODEL_TABLE;
	public static SkosCollection MEASUREMENT_CODE_ID_TABLE;
	public static SkosCollection TECHNICAL_PARAMETER_NAME_TABLE;
	public static SkosCollection CONFIG_PARAMETER_NAME_TABLE;
	public static SkosCollection PARAMETER_TABLE;
	public static SkosCollection PROGRAM_NAME_TABLE;

	// ====
	// INIT
	// ====
	/**
	 * Initialize NVS references tables (static variables) : loop over all files in
	 * the spec folder (from SpecIO) and instanciate a SkosCollection if file is a
	 * NVS jsonld table. Then populate all static variable of the Argo netcdf files
	 * checkers 's useful tables.
	 * 
	 * @param nvsFolderPath
	 */
	public static void initialize() {
		// MAp to store the tables
		Map<RELEVANT_TABLES, SkosCollection> nvsReferenceTables = new HashMap<>();

		// loop over relevant table list
		for (RELEVANT_TABLES t : RELEVANT_TABLES.values()) {
			String fileRableName = "NVS/" + t.getCode() + ".jsonld";
			try (InputStream tableInputStream = SpecIO.getInstance().open(fileRableName)) {
				processNVSTableFile(nvsReferenceTables, tableInputStream);
			} catch (FileNotFoundException e) {
				stderr.println("Table file not found : " + fileRableName + " (" + e.getMessage() + ")");
				break;
			} catch (IOException e) {
				stderr.println("Failed to parse table file: " + fileRableName + " (" + e.getMessage() + ")");
				break;
			}
		}

		populateStaticTables(nvsReferenceTables);
	}

	/**
	 * Initialize the NVS tables from the nerc server on internet.
	 * 
	 */

	public static void initializeFromInternet(String baseUrl) {
		Map<RELEVANT_TABLES, SkosCollection> nvsReferenceTables = new HashMap<>();

		// Loop through relevant tables list :
		for (RELEVANT_TABLES t : RELEVANT_TABLES.values()) {
			String tableUrl = baseUrl + t.getCode() + "/current/?_profile=nvs&_mediatype=application/ld+json";
			try (InputStream tableInputStream = NetUtils.openInputStream(tableUrl)) {
				processNVSTableFile(nvsReferenceTables, tableInputStream);
			} catch (IOException e) {
				stderr.println("Table file not found on NVS : " + tableUrl + " (" + e.getMessage() + ")");
				break;
			} catch (InterruptedException e) {
				stderr.println("Error while retrieving table on NVS : " + tableUrl + " (" + e.getMessage() + ")");
				break;
			}
		}

		populateStaticTables(nvsReferenceTables);

	}

	// ==================
	// CONVENIENT METHODS
	// ==================
	private static void processNVSTableFile(Map<RELEVANT_TABLES, SkosCollection> nvsReferenceTables,
			InputStream tableInput) throws IOException {
		// table parser :
		ArgoNVSReferenceTableParser nvsTablesParser = new ArgoNVSReferenceTableParser();

		SkosCollection table;

		// parse table :
		table = nvsTablesParser.getCollection(tableInput);

		// is it a relevant table ?
		RELEVANT_TABLES enumKey = RELEVANT_TABLES.fromName(table.getAltLabel());
		if (enumKey != null) {
			nvsReferenceTables.put(enumKey, table);
		}
	}

	private static void populateStaticTables(Map<RELEVANT_TABLES, SkosCollection> nvsReferenceTables) {
		DM_QC_FLAG_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.DM_QC_FLAG);
		PLATFORM_TYPE_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.PLATFORM_TYPE);
		PLATFORM_MAKER_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.PLATFORM_MAKER);
		PROF_QC_FLAG_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.PROF_QC_FLAG);
		POSITION_ACCURACY_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.POSITION_ACCURACY);
		DATA_STATE_INDICATOR_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.DATA_STATE_INDICATOR);
		HISTORY_ACTION_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.HISTORY_ACTION);
		ARGO_WMO_INST_TYPE_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.ARGO_WMO_INST_TYPE);
		POSITIONING_SYSTEM_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.POSITIONING_SYSTEM);
		TRANS_SYSTEM_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.TRANS_SYSTEM);
		VERTICAL_SAMPLING_SCHEME_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.VERTICAL_SAMPLING_SCHEME);
		STATUS_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.STATUS);
		GROUNDED_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.GROUNDED);
		PLATFORM_FAMILY_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.PLATFORM_FAMILY);
		SENSOR_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.SENSOR);
		SENSOR_MAKER_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.SENSOR_MAKER);
		SENSOR_MODEL_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.SENSOR_MODEL);
		MEASUREMENT_CODE_ID_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.MEASUREMENT_CODE_ID);
		TECHNICAL_PARAMETER_NAME_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.TECHNICAL_PARAMETER_NAME);
		CONFIG_PARAMETER_NAME_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.CONFIG_PARAMETER_NAME);
		PARAMETER_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.PARAMETER);
		PROGRAM_NAME_TABLE = nvsReferenceTables.get(RELEVANT_TABLES.PROGRAM_NAME);
	}

	private static Set<File> listFolderFiles(String nvsFolderPath) {
		File nvsDir = new File(nvsFolderPath);
		return Stream.of(nvsDir.listFiles()).filter(file -> !file.isDirectory()).collect(Collectors.toSet());
	}

}
