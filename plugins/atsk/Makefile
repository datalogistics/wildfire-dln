
APKNAME=atsk
TARGET=android-16

include ../common.mk


help:
	@echo "all -"
	@echo "       constructs the plugin with the previously deployed required apks"
	@echo "release -"
	@echo "       constructs the obfuscated plugin with the previously deployed"
	@echo "       required apks"
	@echo "iall -" 
	@echo "       install atsk and all required apps"
	@echo "help -"
	@echo "       prints this help"

iall:	idepends clean install

idepends:
	cd required/ATSKService && make clean release install

deploy: 
	cd required/SurveyInterface && make 
	( cd required/ATSKService && make clean release)
	cp required/ATSKService/build/ATSKService-release.apk assets/apks
	echo "$(SVNVERSION)" > assets/apks/version.txt
	echo "finished"

coverity:
	cd required/ATSKService && make clean release

javadoc:
	-mkdir build
	-rm build/javadoc-$(SVNID).jar
	rm -fr tmp && mkdir tmp && cp -R src tmp && cp -R required/SurveyInterface/src tmp
	-( cd tmp/src; find . -type f -name "*.java" | xargs javadoc -linkoffline http://d.android.com/reference file://$(ANDROID_SDK)/docs/reference -d ../javadoc -classpath .:$(ANDROID_SDK)/platforms/android-15/android.jar )
	( cd tmp/javadoc && jar cvf ../../build/ATSK-javadoc-$(SVNID).jar . )
	rm -fr tmp && mkdir tmp && cp -R required/ATSKService/src tmp && cp -R required/SurveyInterface/src tmp
	-( cd tmp/src; find . -type f -name "*.java" | xargs javadoc -linkoffline http://d.android.com/reference file://$(ANDROID_SDK)/docs/reference -d ../javadoc -classpath .:$(ANDROID_SDK)/platforms/android-15/android.jar )
	( cd tmp/javadoc && jar cvf ../../build/ATSKService-javadoc-$(SVNID).jar . )
	rm -fr tmp

