# Hub-Based TP-Link Bulb, Plug, and Switch SmartThing Integration

Two versions of TP-Link to Smart Things Integraion now exist:

a. Cloud-Based TP-Link to Smart Things Integraion: This version that relies on the TP-Link Kasa cloud. Attributes:

    Reliant on TP-Link cloud (and the continued availabilty of same).
    Must have TP-Link account.
    Simpler setup. Install Service Manager and applicable device handlers. Runs service Manager.

b. Hub-Based TP-Link to Smart Things Integraion: The Hub-based version that requires an always on Hub device (bridge). Attributes:

    Requires user-configured (PC, Android, Raspberry) Hub with node.js and server script.
    Does not require a token captured from the TP-Link cloud.
    Manual device installation and setting static IP addresses.

# Version 2 Update - 1/30/2018
The Files have been updated to Version 2. Changes from previous version

a. Added transition time of (user selectable) for bulb power-on and off.

b. Made Energy Monitor function compatible with the Australian version of the HS110 (HS110(AU)).

c. Changed file naming to be function-based instead of product number based due to expanded TP-Link Product Base.

d. Changed naming convention to be compatible with SmartThings - GitHub integration.

Upgrade recommendation: Not needed unless you are adding products not supported with the previous version.

2/172018 - Updated Energy Monitor device handlers to not process the entire previous month data. Also, modified calculations to clean up logic and use Groovy date.

# Known Issue:
On March 1 (and 2) of standard years, the month total nergy data do not include the 2 days (1 day) days in January. The month average energy value is calculated using 28 days (29 days).

# Hub-Based Pre-requisites:
-  bridge device running node.js and the TP-LinkServer applet.  This device must be on continuously with th program running.  Examples of devices:

        Window PC, Laptop, Tablet, Stick Computer
        Android devices with 'Server Ultimate'
        Amazon Kindle Fire (tablet) with 'Server Ultimate'
        Raspberry PI
   
-  Static IP address for the bridge and TP-Link devices.  (done through your WiFi router as DHCP address reservations.

# Files in Device Handler Folder:
    TP-Link Model: FileName // Namespace/Name
    HS100, HS105, HS200, HS210, KP100: (Hub) TP-Link Plug-Switch.groovy // davegut - (Hub) TP-Link Plug-Switch
    HS110, HS115: (Hub) TP-Link EnergyMonitor Plug.groovy // davegut - (Hub) TP-Link EnergyMonitor Plug
    LB100, LB110, LB200, KB100: (Hub) TP-Link SoftWhite Bulb.groovy // davegut - (Cloud) TP-Link Softwhite Bulb
    LB120: (Hub) TP-Link TunableWhite Bulb.groovy // davegut - (Cloud) TP-LinkTunableWhite Bulb
    LB130, L230, KB130: (Hub) TP-Link Color Bulb.groovy // davegut - (Cloud) TP-Link Color Bulb
    LB110: (Hub) TP-Link SoftWhite Bulb Emon .groovy // davegut - (Cloud) TP-Link Softwhite Bulb Emon
    LB120: (Hub) TP-Link TunableWhite Bulb Emon .groovy // davegut - (Cloud) TP-LinkTunableWhite Bulb Emon
    LB130, L230, KB130: (Hub) TP-Link Color Bulb Emon .groovy // davegut - (Cloud) TP-Link Color Bulb Emon

Installation instructions can be found in the documentation folder.

# Files:
Top Level.  Contains the 'TP-LinkHub_v2.js' and windows 'TP-LinkHub_v2.bat' files for the Hub installation.

DeviceHandlers.  All SmartThings device handlers.  Names are clear as to device applicability.

Utilities.  Windows batch file 'cmdPrompt.bat'.  he tool file 'GetDeviceIPs.js' and windows companion 'GetDeviceIPs.bat' will pop a window on your device with the TP-Link devices, IP, MAC Address, and Alias.  Useful in installation.

Documentation.  Installation instructions, Design Notes, and Interface description.

