# $Id: Makefile 1334 2022-04-23 17:00:47Z ignaszewski $


.SUFFIXES: .java .class

DOCDIR = doc

PROGRAMS := \
	DumpProfile.class \
	DumpTraj.class \
	Dump4Compare.class \
	IndexMetaData.class \
	ReadArgoVariable.class \
	ReadVariablesFromFiles.class \
	ResultsFile.class \
	ValidateSubmit.class \
	WriteFloatMultiProfile.class \
	WriteMultiProfile.class 

#	WriteMergeProfile.class \


all: $(PROGRAMS) usgdac_classes Application.properties


$(PROGRAMS):  %.class: %.java  

jar: ValidateSubmit.class
	mkdir -p ../../jar
	find . -name '*.jar' -exec cp {} ../../jar \;
	cp log4j2.xml.production ../../jar/log4j2.xml
	cp Application.properties ../../jar
	./make_exe_jar ../../jar

usgdac_classes: FORCE
	cd usgdac; make


Application.properties: FORCE
	set -x ;\
	if [[ ! -f $@ ]] ;\
	then echo "Version = unknown" > $@ ;\
	fi ;\
	if svn info ;\
	then \
	   v=$$(svnversion -n -c . | sed 's/^.*://') ;\
	   sed "/Version/c \Version = -r$$v" $@ >| $@.new ;\
	   mv $@.new $@ ;\
	else \
	   echo -e "\nNO SVNVERSION INFO for code\n" ;\
	fi


.PHONY: doc
doc: FORCE
	if [[ ! -d $(DOCDIR) ]]; then mkdir $(DOCDIR); fi
	javadoc -d $(DOCDIR) $(PROGRAMS:%.class=%.java)
	cd usgdac; make doc DOCDIR=../$(DOCDIR)


clean: FORCE
	-rm *.class
	-cd usgdac ; make clean

FORCE:

.java.class:
	@echo "==> $<"
	javac -J-Xms2g -J-Xmx2g  -Xlint  $<

