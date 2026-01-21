package fr.coriolis.checker.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * This class is useful to extract special attributes integrated into NVS table entry's definition field.
 * Indeed for physical PARAM (table R03) , some attributes like units, valid_min, valid_max, category, etc present in the legacy file checker  table 'argo-physical_params-spec' cannot , yet, be described in the R03 table wich is the table for parameters name.
 * The choice has been made to include theses attributes in the defintion field of each R03 parameter entry. 
 * Ex : CHLA -> Definition = "Bisulfide concentration (umol/kg). Local_Attributes:{long_name:Bisulfide; standard_name:-; units:micromole/kg; valid_min:-; valid_max:-; category:b; fill_value:99999.f; data_type:float}."
 * Same issue for R18 table and the <short_sensor_name> definition by attributes.
 * 
 * It is needed to parse this definition string and extract these attributes and provide a Map with key : attribute name, and value : attribute's value.
 * 
 * 
 * 
 */
public final class NvsDefinitionParser {

	private NvsDefinitionParser() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Build a Map attributeName/AttributeValue from a "Definition" field containing
	 * a pattern "objectName:{attr1:value1;attr2:value2...}"
	 * 
	 * @param objectName    (String)
	 * @param stringToParse (String)
	 * @return
	 */
	public static Map<String, String> parseAttributes(String objectName, String stringToParse) {
		Map<String, String> attributesMap = new HashMap<>();

		String strPattern = objectName + ":\\{(.*?)\\}";
		Pattern pattern = Pattern.compile(strPattern);
		Matcher matcher = pattern.matcher(stringToParse);

		// pattern has been found, extract attributes from it
		if (matcher.find()) {
			attributesMap = buildAttributesMap(matcher.group(1));
		}

		return attributesMap;

	}

	/**
	 * Extract attributes from a pattern and return a map
	 * attributeName/AttributeValue. Ex : "long_name:Bisulfide; standard_name:-;
	 * units:micromole/kg; valid_min:-; valid_max:-; category:b; fill_value:99999.f;
	 * data_type:float"
	 * 
	 * @param extractedPattern (String) : ex : "long_name:Bisulfide;
	 *                         standard_name:-;units:micromole/kg; valid_min:-;
	 *                         valid_max:-; category:b;
	 *                         fill_value:99999.f;data_type:float"
	 * @return attributesMap (Map<String, String>) : ex : keys : long_name,
	 *         standard_name, valid_min, valid_max, category,etc. Values :
	 *         Bisulfide, - , micromole/kg , etc.
	 */
	private static Map<String, String> buildAttributesMap(String extractedPattern) {
		Map<String, String> attributesMap = new HashMap<>();
		String[] nameValueAttributes = extractedPattern.split(";");
		// for each attribute:value with split using ":" sperator
		for (String nameValueString : nameValueAttributes) {
			String[] nameValueArray = nameValueString.split(":");
			// we should have only 2 elements:
			if (nameValueArray.length == 2) {
				attributesMap.put(nameValueArray[0].trim(), nameValueArray[1].trim());
			}
		}
		return attributesMap;
	}

}
