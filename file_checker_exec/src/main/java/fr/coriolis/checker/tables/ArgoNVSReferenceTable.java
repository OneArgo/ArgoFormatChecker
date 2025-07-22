package fr.coriolis.checker.tables;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ArgoNVSReferenceTable {

	private static PrintStream stderr = new PrintStream(System.err);

	public static enum RELEVANT_TABLES {
		DM_QC_FLAG("DM_QC_FLAG"), PLATFORM_TYPE("PLATFORM_TYPE"), PLATFORM_MAKER("PLATFORM_MAKER"),
		PROF_QC_FLAG("PROF_QC_FLAG"), POSITION_ACCURACY("POSITION_ACCURACY"),
		DATA_STATE_INDICATOR("DATA_STATE_INDICATOR"), HISTORY_ACTION("HISTORY_ACTION"),
		ARGO_WMO_INST_TYPE("ARGO_WMO_INST_TYPE"), POSITIONING_SYSTEM("POSITIONING_SYSTEM"),
		TRANS_SYSTEM("TRANS_SYSTEM"), VERTICAL_SAMPLING_SCHEME("VERTICAL_SAMPLING_SCHEME"), STATUS("STATUS"),
		GROUNDED("GROUNDED"), PLATFORM_FAMILY("PLATFORM_FAMILY"), SENSOR("SENSOR"), SENSOR_MAKER("SENSOR_MAKER"),
		SENSOR_MODEL("SENSOR_MODEL"), MEASUREMENT_CODE_ID("MEASUREMENT_CODE_ID"),
		TECHNICAL_PARAMETER_NAME("TECHNICAL_PARAMETER_NAME"), CONFIG_PARAMETER_NAME("CONFIG_PARAMETER_NAME"),
		PARAMETER("PARAMETER");

		public final String name;

		RELEVANT_TABLES(String d) {
			name = d;
		}

		public static RELEVANT_TABLES fromName(String name) {
			for (RELEVANT_TABLES t : RELEVANT_TABLES.values()) {
				if (t.name.equals(name)) {
					return t;
				}
			}
			return null;
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

	// ====
	// INIT
	// ====
	/**
	 * Initialize NVS references tables (static variables) : loop over all files in
	 * the specified folder and instanciate a SkosCollection if file is a NVS jsonld
	 * table. Then populate all static variable of the Argo netcdf files checkers 's
	 * useful tables.
	 * 
	 * @param nvsFolderPath
	 */
	public static void initialize(String nvsFolderPath) {
		Map<RELEVANT_TABLES, SkosCollection> nvsReferenceTables = new HashMap<>();
		// get list of nvs tables files :
		Set<File> tablesFiles = listFolderFiles(nvsFolderPath);

		// table parser :
		ArgoNVSReferenceTableParser nvsTablesParser = new ArgoNVSReferenceTableParser();

		// loop over tables files
		for (File tableFile : tablesFiles) {
			SkosCollection table;
			try {
				table = nvsTablesParser.getCollection(tableFile);
			} catch (IOException e) {
				stderr.println("Failed to parse table file: " + tableFile + " (" + e.getMessage() + ")");
				continue;
			}
			// is it a relevant table ?
			RELEVANT_TABLES enumKey = RELEVANT_TABLES.fromName(table.getAltLabel());
			if (enumKey != null) {
				nvsReferenceTables.put(enumKey, table);
			}
		}

		populateStaticTables(nvsReferenceTables);
	}

	// ==================
	// CONVENIENT METHODS
	// ==================

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
	}

	private static Set<File> listFolderFiles(String nvsFolderPath) {
		File nvsDir = new File(nvsFolderPath);
		return Stream.of(nvsDir.listFiles()).filter(file -> !file.isDirectory()).collect(Collectors.toSet());
	}

}
