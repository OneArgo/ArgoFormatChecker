package fr.coriolis.checker.specs;

public class AuxilliarySettings {

	private String errComment;
	private String errLongName;
	private String presAxis;
	private String presAdjAxis;

	public AuxilliarySettings() {
		this.errComment = null;
		this.errLongName = null;
		this.presAdjAxis = "Z";
		this.presAxis = "Z";
	}

	public String getErrComment() {
		return this.errComment;
	}

	public String getErrLongName() {
		return errLongName;
	}

	public void setErrLongName(String errLongName) {
		this.errLongName = errLongName;
	}

	public String getPresAxis() {
		return presAxis;
	}

	public void setPresAxis(String presAxis) {
		this.presAxis = presAxis;
	}

	public String getPresAdjAxis() {
		return presAdjAxis;
	}

	public void setPresAdjAxis(String pres_adjAxis) {
		this.presAdjAxis = pres_adjAxis;
	}

	public void setErrComment(String errComment) {
		this.errComment = errComment;
	}

}
