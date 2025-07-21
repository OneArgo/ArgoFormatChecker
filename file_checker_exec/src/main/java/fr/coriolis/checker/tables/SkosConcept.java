package fr.coriolis.checker.tables;

import java.util.Map;
import java.util.Set;

import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldId;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldProperty;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

@JsonldType("skos:Concept")
public class SkosConcept {
	@JsonldId
	private String id;
	@JsonldProperty("skos:notation")
	private Object notation;
	@JsonldProperty("skos:altLabel")
	private Object altLabel;
	@JsonldProperty("skos:prefLabel")
	private Object prefLabel;
	@JsonldProperty("skos:definition")
	private Object definition;
	@JsonldProperty("owl:deprecated")
	private boolean deprecated;

	@JsonldProperty("skos:related")
	private Object relatedConceptIds;

	// private Set<SkosConcept> relatedConcepts;

	// ===================
	// GETTERS and SETTERS
	// ===================
	public String getId() {
		return id;
	}

	public String getNotation() {
		return getValue(notation);
	}

	public String getAltLabel() {
		return getValue(altLabel);
	}

	public String getPrefLabel() {
		return getValue(prefLabel);
	}

	public String getDefinition() {
		return getValue(definition);
	}

//
//	public Set<SkosConcept> getRelatedConcepts() {
//		return relatedConcepts;
//	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public Set<String> getRelatedConceptIds() {
		if (relatedConceptIds == null) {
			return Set.of();
		}

		// Only one id (map)
		if (relatedConceptIds instanceof Map) {
			String id = (String) ((Map<?, ?>) relatedConceptIds).get("@id");
			return Set.of(id);
		}
		// multiple id (list)
		if (relatedConceptIds instanceof Iterable) {
			Set<String> ids = new java.util.HashSet<>();
			for (Object obj : (Iterable<?>) relatedConceptIds) {
				if (obj instanceof Map && ((Map<?, ?>) obj).containsKey("@id")) {
					ids.add((String) ((Map<?, ?>) obj).get("@id"));
				}
			}
			return ids;
		}
		// fallback
		return Set.of();
	}

	/**
	 * Case where properties is an object. ex: "skos:definition": { "@language":
	 * "en", "@value": "Estimated value (interpolated, extrapolated or other
	 * estimation)." },
	 * 
	 * @param object
	 */
	private String getValue(Object object) {
		if (object instanceof Map) {
			return (String) ((Map<?, ?>) object).get("@value");
		} else if (object instanceof String) {
			return (String) object;
		}
		return null;
	}
}
