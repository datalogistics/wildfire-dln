####################################################################################################
# Command line functionality.
#SVNVERSION=$(shell basename $(subst Revision: ,,$(shell svn info | grep "^Revision:" || echo "1")))
#SVNBRANCH=$(shell basename $(subst URL: ,,$(shell svn info | grep "^URL:" || echo "none")))
#SVNID=$(SVNBRANCH)-$(SVNVERSION)
SVNVERSION=0
SVNBRANCH=0
SVNID=0

APK=build/$(APKNAME)-$(SVNVERSION).apk

.PHONY : help format lint clean cleanall push install javadoc doxygen pathcheck checkcomponents



all:	devrelease

help:
	@echo "$(APKNAME) Management Makefile"
	@echo "-----------------------------------------------------------------------"
	@echo "all -"
	@echo "    produces the APK "
	@echo "clean -"
	@echo "    deletes the $(APKNAME) APK forcing recompilation of the $(APKNAME) modified classes only"
	@echo "cleanall -"
	@echo "    deletes all classes across all of the $(APKNAME) main projects"
	@echo "updatever -"
	@echo "    modifies the version number of atak based on the svn revision of the repository"
	@echo "devrelease -"
	@echo "    produces a valid release version of the software without obfuscation"
	@echo "release -"
	@echo "    enables obfuscation and produces a release apk"
	@echo "release-noclean -"
	@echo "    enables obfuscation running release, but does not clean prior"
	@echo "install -"
	@echo "    installs using adb install"
	@echo "push -"
	@echo "    pushes the apk to the phone's sdcard"
	@echo "lint -"
	@echo "    produces a lint report for the entire svn project"
	@echo "strip -"
	@echo "    runs dos2unix on the codebase"
	@echo "format -"
	@echo "    runs the eclipse formatter on the codebase"
	@echo "doxygen -"
	@echo "    produces the preferred documentation for the entire svn project"
	@echo "javadoc -"
	@echo "    produces the javadoc documentation for the MapCoreInterface project"
	@echo "printversion"
	@echo "    prints the version that will be used for the numeric version number and the textual version string"
	@echo "branchhistory"
	@echo "    prints the branch history across all branches for informational purposes"


doxygen:
	doxygen docs/Doxyfile

printversion:
	@echo "$(SVNID) $(SVNNUM)"

strip:
	find . -name \*.java -exec dos2unix {} \;
	find . -name \*.xml -exec dos2unix {} \;
	find . -name \*.aidl -exec dos2unix {} \;
	find . -name \*.java -exec sed -i 's/\t/    /g' {} \;
	find . -name \*.xml -exec sed -i 's/\t/    /g' {} \;
	find . -name \*.aidl -exec sed -i 's/\t/    /g' {} \;

$(APK):
	@if [ ! -z "$(OBFUSCATE)" ]; then \
		./gradlew assembleRelease || exit ; \
		cp $(PREFIX)build/outputs/apk/*-release.apk $(APK); \
                cp $(PREFIX)build/intermediates/transforms/dex/release/folders/1000/1f/main/classes.dex build/tmp.dex; \
	fi

	@if [ -z "$(OBFUSCATE)" ]; then \
		./gradlew assembleDebug || exit ; \
		cp $(PREFIX)build/outputs/apk/*-debug.apk $(APK); \
                cp $(PREFIX)build/intermediates/transforms/dex/debug/folders/1000/1f/main/classes.dex build/tmp.dex; \
	fi

	@echo "=================================="
	@echo "APK generated: $@ "
	-@echo "dex limit reminder: `cat build/tmp.dex | head -c 92 | tail -c 4 | hexdump -e '1/4 \"%d\n\"'`"
	@echo "=================================="


install: $(APK)
	for SN in `adb devices | awk '{ print $$1 }' | grep -v L | grep -v \*`; do \
                echo "installing to: $$SN" ;\
		export ANDROID_SERIAL="$$SN" ;\
		adb install -r $(APK) ;\
	done

push: $(APK)
	for SN in `adb devices | awk '{ print $$1 }' | grep -v L | grep -v \*`; do \
		echo "installing to: $$SN" ;\
		export ANDROID_SERIAL="$$SN" ;\
		adb push $< /sdcard/  ;\
	done

format:
	eclipse -nosplash -application org.eclipse.jdt.core.JavaCodeFormatter -config ../../android-formatting.prefs src

clean:
	rm -fr $(APK)

cleanall:  
	-(./gradlew clean)
	-(rm -fr gen build)
	-(rm -fr .gradle)

cleanall_fast:  
	-(rm -fr build)

devrelease: $(APK)


release: 
release: OBFUSCATE=true
release: $(APK)
