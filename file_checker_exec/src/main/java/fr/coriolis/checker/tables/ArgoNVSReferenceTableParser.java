package fr.coriolis.checker.tables;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ioinformarics.oss.jackson.module.jsonld.JsonldModule;

public class ArgoNVSReferenceTableParser {
	private final ObjectMapper objectMapper;

	public ArgoNVSReferenceTableParser() {
		this.objectMapper = initObjectMapper();
	}

	private ObjectMapper initObjectMapper() {
		final ObjectMapper objectMapper = new ObjectMapper();
		// Ignore unknown properties in deserialization input
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// create the JSON-LD serialization/deserialization module
		final JsonldModule module = new JsonldModule();
		// Register module :
		objectMapper.registerModule(module);

		return objectMapper;
	}

	public SkosCollection getCollection(File jsonLdFile) throws IOException {
		// read json :

		JsonNode root = objectMapper.readTree(jsonLdFile);
		JsonNode graphArray = root.get("@graph");
		if (graphArray == null || !graphArray.isArray()) {
			throw new IllegalArgumentException("JSON-LD do not contains @graph array !");
		}
		// find Collection and concepts :
		SkosCollection collection = null;
		Map<String, SkosConcept> conceptsMap = new HashMap<>();

		for (JsonNode node : graphArray) {
			// try to bin the node to a SkosCollection :
			collection = readCollection(node);
			// if node is a concept, index it :
			SkosConcept skosConcept = readConcepts(node);
			if (skosConcept != null) {
				conceptsMap.put(skosConcept.getId(), skosConcept);
			}
		}

		// populate collection members :
		resolveMembers(collection, conceptsMap);

		return collection;
	}

	/**
	 * Deserialize a SkosCollection object (if exists) from a json ld node.
	 * 
	 * @param node - jsonld node
	 * @throws IOException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 */
	private SkosCollection readCollection(JsonNode node) throws IOException, JsonParseException, JsonMappingException {
		if (node.has("@type") && node.get("@type").asText().equals("skos:Collection")) {
			return objectMapper.readValue(node.toString(), SkosCollection.class);
		}
		return null;
	}

	/**
	 * Deserialize a SkosConcept object (if exists) from a json ld node.
	 * 
	 * @param node - jsonld node
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private SkosConcept readConcepts(JsonNode node) throws JsonParseException, JsonMappingException, IOException {
		if (node.has("@type") && node.get("@type").asText().equals("skos:Concept")) {
			return objectMapper.readValue(node.toString(), SkosConcept.class);
		}
		return null;
	}

	/**
	 * add concepts to SkosCollection 's conceptsMembers attribute.
	 * 
	 * @param collection  - The SkosCollection object
	 * @param conceptsMap - a map containing concept's ids and associated concept
	 *                    object
	 */
	private void resolveMembers(SkosCollection collection, Map<String, SkosConcept> conceptsMap) {
		for (SkosConceptId conceptId : collection.getMembersIds()) {
			// retrieve the corresponding concept from concepts map if exists :
			SkosConcept conceptMember = conceptsMap.get(conceptId.getId());
			if (conceptMember != null) {
				// add it to collection's concept members
				// collection.getConceptMembersByIdsMap().put(conceptMember.getId(),
				// conceptMember);
				collection.getConceptMembersByAltLabelMap().put(conceptMember.getAltLabel(), conceptMember);
			}
		}

	}

}
