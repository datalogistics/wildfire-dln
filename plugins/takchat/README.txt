TAK Chat Plugin
_________________________________________________________________
PURPOSE AND CAPABILITIES

The TAK Chat plugin is used to chat securely with other systems via XMPP.
Since XMPP is a standard, this plugin can chat with other apps and
platforms, including non TAK products.

Primary features currently include:
Contact List
Point to Point Chat
Conference (group) Chat
Delivery Receipts
Synchronizing chats from server
	Chats from same user, different device
	Conference chats missed while offline
Chat state notifications (e.g. "user is typing")


_________________________________________________________________
STATUS

Released, minor maintenance ongoing

_________________________________________________________________
ATAK VERSIONS

3.5+

_________________________________________________________________
POINT OF CONTACTS

Primary Developer Contact:     Brian Young.  byoung@ara.com / 1-919-582-3300 / ARA
Program Office Contact:  Josh Sterling.  joshua.d.sterling.civ@mail.mil / USASOC 

_________________________________________________________________
USER GROUPS

USASOC

_________________________________________________________________
EQUIPMENT REQUIRED

None

_________________________________________________________________
EQUIPMENT SUPPORTED

N/A

_________________________________________________________________
COMPILATION

Gradle build or Android Studio file

_________________________________________________________________
DEVELOPER NOTES

This plugin currently leverage Smack Java library, which transparently provides support for some XEPs not listed below.
https://www.igniterealtime.org/projects/smack/ 

Notable supported XMPP protocols:
https://xmpp.org/extensions

XMPP Core (Messaging and Presence):	https://xmpp.org/rfcs/rfc3920.html
			https://xmpp.org/rfcs/rfc6121.html
XEP-0045	Multi-User Chat	https://xmpp.org/extensions/xep-0045.pdf
XEP-0085	Chat State Notifications	https://xmpp.org/extensions/xep-0085.html
XEP-0128	Service Discovery Extensions	https://xmpp.org/extensions/xep-0128.html
XEP-0198	Stream Management		http://xmpp.org/extensions/xep-0198.html
XEP-0144	Roster Item Exchange	https://xmpp.org/extensions/xep-0144.html
XEP-0054	vcard-temp	https://xmpp.org/extensions/xep-0054.html
XEP-0153	vCard-Based Avatars	http://xmpp.org/extensions/xep-0153.html
XEP-0184	Message Delivery Receipts	https://xmpp.org/extensions/xep-0184.html
XEP-0203	Delayed Delivery	https://xmpp.org/extensions/xep-0203.html
XEP-0224	Attention	https://xmpp.org/extensions/xep-0224.html
XEP-0313	Message Archive Management	https://xmpp.org/extensions/xep-0313.html
Custom Extensions:
	TAK UID

Future Considerations
XEP-0080	User Location	https://xmpp.org/extensions/xep-0080.html
XEP-0138	Stream Compression	http://xmpp.org/extensions/xep-0138.html
XEP-0234	Jingle File Transfer	http://xmpp.org/extensions/xep-0234.html
XEP-0363	HTTP File Upload	http://xmpp.org/extensions/xep-0363.html
XEP-0258	Security Labels	https://xmpp.org/extensions/xep-0258.html
XEP-0286	Mobile Considerations	https://xmpp.org/extensions/xep-0286.pdf


Reference 3rd party XMPP server:
	OpenFire
	ejabberd
	
Reference 3rd party XMPP client: 
	Xabber, Conversations for Android
	Spark, Pidgin for Windows

The TAK Chat Plugin supports a basic Android Intent API which which may be used by
other tools/plugins for XMPP integration. For more information refer to:
	com.atakmap.android.takchat.api.TAKChatApi.java
