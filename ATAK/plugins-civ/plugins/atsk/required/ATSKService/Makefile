####################################################################################################
# Command line functionality.


APK=build/ATSKService-release.apk

all:	$(APK)

$(APK):
	gradle assembleRelease; \
	cp build/outputs/apk/ATSKService-release.apk $(APK); \
        cp build/intermediates/transforms/dex/release/folders/1000/1f/main/classes.dex build/tmp.dex; \

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

clean:
	rm -fr $(APK)

cleanall:  
	-(gradle clean)

release: 
release: $(APK)
