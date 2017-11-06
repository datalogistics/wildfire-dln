FILES= adsb AID airoverlays airwarden aoupdate art HUD atsk Avalanche batdok BatteryLogPlugin bda BSIPlugin bubblewrap datasync CITrapPlugin cltd CMissionPlanning CombatSwim commout commout-simplesocket DACO deadreckoning DNC DoghouseExtras  Dronekit DSMManager DSPro effects ElevenLine emaps ew-urc ExCheck externalbt flirbt GeoTakCam GoTenna GroundGuidance GvLFStreamer Handrails HelloBuildings helloworld hindsight intercept intervalometer ISP ISRTK Jolly jsmcomm JumpInfo LeanServices lobster LunchTimePlugin milestone mobileData  MoDeep mortar nett NINJA oceusvpn pDDL platformsim PluginTemplate PointMensurationTool ProfilePic qlog QuickChat rn sadl samgear SiteExploitation SlantReport SniperToolkit spotplot sse stackmanager SwimErrorBoxes TacticalGraphics takchat TakSubscription tcpfunnel trackexport twRadio uas VideoManager videooverlay VISCAcam VMFBlueSA  wamivideo Wardriving wave waverelay wxreport GRGBuilder Hello3D OverheadPallet  

.PHONY : $(FILES)


all: $(FILES)

$(FILES):
	make -C $@ $(RELEASE) $(CLEAN)

release: RELEASE=release
release: $(FILES)

clean: CLEAN=clean
clean: $(FILES)

cleanall: CLEAN=cleanall_fast
cleanall: $(FILES)

update:
	for FILE in */AndroidManifest.xml;do sed -i "s/com.atakmap.app@3.6.0/com.atakmap.app@3.6.1/" $$FILE;done
	# note -- when moved for the first time run:
	#  for FILE in */build.gradle;do sed -i "s/versionName = /versionName = \"3.6.0.\" + /" $FILE;done

