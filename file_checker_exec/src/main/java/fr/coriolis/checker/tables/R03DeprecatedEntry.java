package fr.coriolis.checker.tables;

public class R03DeprecatedEntry {
	private String paramName;
	private String attributeKey;
	private String oldValue;
	private String status;
	private String message;

	public R03DeprecatedEntry(String paramName, String attributeKey, String oldValue, String status, String message) {
		this.paramName = paramName;
		this.attributeKey = attributeKey;
		this.oldValue = oldValue;
		this.status = status;
		this.message = message;
	}

	@Override
	public String toString() {
	    return String.format("DeprecatedEntry{param='%s', attribute='%s', oldValue='%s', status='%s', message='%s'}",
	        paramName, attributeKey, oldValue, status, message);
	}
	// ===================
	// GETTERS AND SETTERS
	// ===================

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getParamName() {
		return paramName;
	}

	public void setParamName(String paramName) {
		this.paramName = paramName;
	}

	public String getAttributeKey() {
		return attributeKey;
	}

	public void setAttributeKey(String attributeKey) {
		this.attributeKey = attributeKey;
	}

	public String getOldValue() {
		return oldValue;
	}

	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
