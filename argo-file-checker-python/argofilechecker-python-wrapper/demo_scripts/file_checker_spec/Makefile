# $Id $
#
# Simply makes the VersionInfo.properties file with the current revision number in it
#

VersionInfo.properties: force
	set -x ;\
	cd ../spec ;\
	f=VersionInfo.properties ;\
	if [[ ! -f $$f ]] ;\
	then echo "Version = unknown" > $$f ;\
	fi ;\
	if svn info ;\
	then \
	   v=$$(svnversion -n -c . | sed 's/^.*://') ;\
	   sed "/Version/c \Version = -r$$v" $$f >| $$f.new ;\
	   mv $$f.new $$f ;\
	else \
	   echo -e "\nNO SVNVERSION INFO for spec\n" ;\
	fi

force:
