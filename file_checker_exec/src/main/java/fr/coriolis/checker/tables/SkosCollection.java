package fr.coriolis.checker.tables;

import java.util.HashSet;
import java.util.Set;

import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldId;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldProperty;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

@JsonldType("skos:Collection")
public class SkosCollection {

	@JsonldId
	private String id;
	@JsonldProperty("skos:altLabel")
	private String altLabel;
	@JsonldProperty("dc:title")
	private String title;
	@JsonldProperty("skos:member")
	private Set<SkosConceptId> membersIds;

	private Set<SkosConcept> conceptMembers = new HashSet<>();

	// ===================
	// GETTERS and SETTERS
	// ===================
	public String getId() {
		return id;
	}

	public String getAltLabel() {
		return altLabel;
	}

	public String getTitle() {
		return title;
	}

	public Set<SkosConceptId> getMembersIds() {
		return membersIds;
	}

	public Set<SkosConcept> getConceptMembers() {
		return conceptMembers;
	}

}
