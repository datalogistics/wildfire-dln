# These are instructions for building the required components for 
# the GoTenna application.  The existing problem is that on some 
# versions of android, devices will get:
#  STACK_TRACE=java.lang.IllegalAccessError: Class ref in pre-verified class resolved to unexpected implementation
#	at com.gotenna.sdk.utils.Utils.registerForLocalBroadcast(SourceFile:141)
#	at com.gotenna.sdk.commands.GTResetHelper.startListening(SourceFile:43)
#	at com.gotenna.sdk.GoTenna.setApplicationToken(SourceFile:128)
#	at com.gotenna.atak.plugin.plugin.GotennaDropDownReceiver.initializeGuiElements(SourceFile:415)
#	at com.gotenna.atak.plugin.plugin.GotennaDropDownReceiver.onReceive(SourceFile:467)
#	at android.app.LoadedApk$ReceiverDispatcher$Args.run(LoadedApk.java:759)
#	at android.os.Handler.handleCallback(Handler.java:733)
#	at android.os.Handler.dispatchMessage(Handler.java:95)
#	at android.os.Looper.loop(Looper.java:136)
#	at android.app.ActivityThread.main(ActivityThread.java:5584)
#	at java.lang.reflect.Method.invokeNative(Native Method)
#	at java.lang.reflect.Method.invoke(Method.java:515)
#	at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:1268)
#	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1084)
#	at dalvik.system.NativeStart.main(Native Method)
#
# This is because ATAK already supplies a version of support v4 version 19.
# These instructions will compile the zxing libraries with the provided 
# keyword so it does not include another version of the libraries.
#

git clone --depth 1 https://github.com/dm77/barcodescanner.git
git clone --depth 1 https://github.com/zxing/zxing.git


# building bar code scanner (need the 3 resulting aar files)
( 
    cd barcodescanner
    patch -p 1 < ../barcodescanner.patch
    ./gradlew assembleDebug
    find . -name *.aar -exec cp {} ../../libs \;
    cd ..
)

# building zxing (need the resulting core jar file)
(
cd zxing
mvn install
find . -name core-3.3.1-SNAPSHOT.jar -exec cp {} ../../libs \;
)



