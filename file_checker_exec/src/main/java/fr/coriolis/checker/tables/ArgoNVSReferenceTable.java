package fr.coriolis.checker.tables;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ArgoNVSReferenceTable {

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

	public static Map<RELEVANT_TABLES, SkosCollection> NVS_REFERENCE_TABLES;

	public ArgoNVSReferenceTable(String nvsFolderPath) {
		NVS_REFERENCE_TABLES = new HashMap<>();
		// get list of nvs tables files :
		Set<File> tablesFiles = listNvsTablesFiles(nvsFolderPath);

		// Index of all skos concept :
		// Map<String, SkosConcept> conceptsIndex = new HashMap<>();

		// table parser :
		ArgoNVSReferenceTableParser nvsTablesParser = new ArgoNVSReferenceTableParser();

		// loop over tables files
		for (File tableFile : tablesFiles) {
			SkosCollection table;
			try {
				table = nvsTablesParser.getCollection(tableFile);
			} catch (IOException e) {
				continue;
			}
			// is it a relevant table ?
			RELEVANT_TABLES enumKey = RELEVANT_TABLES.fromName(table.getAltLabel());
			if (enumKey != null) {
				NVS_REFERENCE_TABLES.put(enumKey, table);
			}

			// add all its member in conceptsIndex
			// conceptsIndex.putAll(table.getConceptMembers());

		}

//		//resolveAllCollectionsMembersRelatedConcepts(nvsReferenceTables, conceptsIndex);

	}

	public static SkosCollection getNvsTableByName(String tableName) {
		if (RELEVANT_TABLES.fromName(tableName) == null) {
			return null;
		}
		return NVS_REFERENCE_TABLES.get(RELEVANT_TABLES.fromName(tableName));
	}

	private Set<File> listNvsTablesFiles(String nvsFolderPath) {
		File nvsDir = new File(nvsFolderPath);
		return Stream.of(nvsDir.listFiles()).filter(file -> !file.isDirectory()).collect(Collectors.toSet());
	}

//	private void resolveAllCollectionsMembersRelatedConcepts(Map<RELEVANT_TABLES, SkosCollection> nvsReferenceTables2,
//			Map<String, SkosConcept> conceptsIndex) {
//		// Iterate through all NVS tables (collections)
//		for (SkosCollection collection : nvsReferenceTables.values()) {
//			resolveCollectionMembersRelatedConcepts(collection, conceptsIndex);
//		}
//
//	}
//
//	private void resolveCollectionMembersRelatedConcepts(SkosCollection collection,
//			Map<String, SkosConcept> conceptsIndex) {
//		// For each NVS table, iterate throug its members (SkosConcept)
//		for (SkosConcept concept : collection.getConceptMembers().values()) {
//			resolveConceptRelatedConcept(concept, conceptsIndex);
//		}
//
//	}
//
//	private void resolveConceptRelatedConcept(SkosConcept concept, Map<String, SkosConcept> conceptsIndex) {
//		for (String relatedConceptId : concept.getRelatedConceptIds()) {
//			// for each SkosConcept, iterate through its relatedConceptId, find the
//			// associated SkosConcept in conceptsIndex and add it in the SkosConcept
//			// relatedConcept Attributes.
//			SkosConcept relatedContept = conceptsIndex.get(relatedConceptId);
//			if (relatedContept != null) {
//				concept.getRelatedConcepts().add(relatedContept);
//			}
//		}
//
//	}

}
