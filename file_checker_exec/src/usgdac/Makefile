.SUFFIXES: .java .class

DOC_DIR = doc

USGDAC_CLASSES := \
	ArgoAttribute.class \
	ArgoConfigTechParam.class \
	ArgoDataFile.class \
	ArgoDate.class \
	ArgoDimension.class \
	ArgoFileSpecification.class \
	ArgoMetadataFile.class \
	ArgoProfileFile.class \
	ArgoReferenceTable.class \
	ArgoTechnicalFile.class \
	ArgoTrajectoryFile.class \
	ArgoVariable.class


all: $(USGDAC_CLASSES)



$(USGDAC_CLASSES): %.class: %.java

ArgoMetadataFile.class : ArgoReferenceTable.class


doc: FORCE
	if [[ ! -d $(DOC_DIR) ]]; then mkdir $(DOC_DIR); fi
	javadoc -d $(DOC_DIR) $(USGDAC_CLASSES:%.class=%.java)


clean: FORCE
	-rm *.class

FORCE:

.java.class:
	@echo "==> $<"
	javac -J-Xms2g -J-Xmx2g -Xlint  $<
