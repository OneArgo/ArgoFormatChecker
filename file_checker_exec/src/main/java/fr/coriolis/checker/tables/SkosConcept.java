package fr.coriolis.checker.tables;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldId;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldProperty;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

@JsonldType("skos:Concept")
public class SkosConcept {

	public final static String INVALID_ALTLABEL_MESSAGE = "Invalid";
	public final static String DEPRECATED_CONCEPT = "Deprecated";

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

	@JsonldProperty("skos:narrower")
	private Object narrowerConceptIds;

	@JsonldProperty("skos:broader")
	private Object broaderConceptIds;

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

	public boolean isDeprecated() {
		return deprecated;
	}

	public Set<String> getRelatedConceptIds() {
		return getSetOfIds(relatedConceptIds);
	}

	public Set<String> getNarrowerConceptIds() {
		return getSetOfIds(narrowerConceptIds);
	}

	public Set<String> getBroaderConceptIds() {
		return getSetOfIds(broaderConceptIds);
	}

	// ===================
	// CONVENIENCE METHODS
	// ===================

	private Set<String> getSetOfIds(Object objectContainingIds) {
		if (objectContainingIds == null) {
			return Collections.emptySet();
		}

		// Only one id (map)
		if (objectContainingIds instanceof Map) {
			String id = (String) ((Map<?, ?>) objectContainingIds).get("@id");
			if (id == null) {
				return Collections.emptySet();
			}
			return Collections.singleton(id);
		}
		// multiple id (list)
		if (objectContainingIds instanceof Iterable) {
			Set<String> ids = new java.util.HashSet<>();
			for (Object obj : (Iterable<?>) objectContainingIds) {
				if (obj instanceof Map && ((Map<?, ?>) obj).containsKey("@id")) {
					ids.add((String) ((Map<?, ?>) obj).get("@id"));
				}
			}
			return ids;
		}
		// fallback
		return Collections.emptySet();
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

	public boolean checkRelatedReference(String otherConceptId) {
		return this.getRelatedConceptIds().contains(otherConceptId);
	}

	public boolean checkNarowerReference(String otherConceptId) {
		return this.getNarrowerConceptIds().contains(otherConceptId);
	}

	public boolean checkBroaderReference(String otherConceptId) {
		return this.getBroaderConceptIds().contains(otherConceptId);
	}

}
