package fr.coriolis.checker.tables;

import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldId;

public class SkosConceptId {
	@JsonldId
	private String id;

	public String getId() {
		return id;
	}

}
