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
		DM_QC_FLAG("DM_QC_FLAG"), PLATFORM_TYPE("PLATFORM_TYPE"), PLATFORM_MAKER("PLATFORM_MAKER");

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

	public ArgoNVSReferenceTable(String nvsFolderPath) throws IOException {
		NVS_REFERENCE_TABLES = new HashMap<>();
		// get list of nvs tables files :
		Set<File> tablesFiles = listNvsTablesFiles(nvsFolderPath);

		// Index of all skos concept :
		// Map<String, SkosConcept> conceptsIndex = new HashMap<>();

		// table parser :
		ArgoNVSReferenceTableParser nvsTablesParser = new ArgoNVSReferenceTableParser();

		// loop over tables files
		for (File tableFile : tablesFiles) {
			SkosCollection table = nvsTablesParser.getCollection(tableFile);
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
