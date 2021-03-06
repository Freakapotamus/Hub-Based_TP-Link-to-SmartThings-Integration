/*
TP-Link Plug and Switch Device Handler, 2018, Version 2
Copyright 2018 Dave Gutheinz

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the
License. You may obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, 
software distributed under the License is distributed on an 
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
either express or implied. See the License for the specific 
language governing permissions and limitations under the 
License.

Discalimer:  This Service Manager and the associated Device 
Handlers are in no way sanctioned or supported by TP-Link.  
All development is based upon open-source data on the 
TP-Link devices; primarily various users on GitHub.com.

	===== History =============================================
2018-01-31	Update to Version 2
	a.	Common file content for all bulb implementations,
		using separate files by model only.
	b.	User file-internal selection of Energy Monitor
		function enabling.
2018-02-17	Updated Energy Monitor Functions
	a.	Allowed for full month collection in previous month
	b.	Cleaned-up algorithm to use Groovy date.
//	===== Hub or Cloud Installation =========================*/
	//def installType = "Cloud"
	def installType = "Hub"
//	===========================================================

metadata {
	definition (name: "(${installType}) TP-Link EnergyMonitor Plug",
				namespace: "davegut",
				author: "Dave Gutheinz",
				deviceType: "EnergyMonitor Plug",
				energyMonitor: "EnergyMonitor",
				installType: "${installType}") {
		capability "Switch"
		capability "refresh"
		capability "polling"
		capability "Sensor"
		capability "Actuator"
		capability "Power Meter"
		command "getPower"
		capability "Energy Meter"
		command "getEnergyStats"
		attribute "monthTotalE", "string"
		attribute "monthAvgE", "string"
		attribute "weekTotalE", "string"
		attribute "weekAvgE", "string"
	}
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc",
				nextState:"waiting"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff",
				nextState:"waiting"
				attributeState "waiting", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#15EE10",
				nextState:"waiting"
				attributeState "commsError", label:'Comms Error', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#e86d13",
				nextState:"waiting"
			}
 			tileAttribute ("deviceError", key: "SECONDARY_CONTROL") {
				attributeState "deviceError", label: '${currentValue}'
			}
		}
		
		standardTile("refresh", "capability.refresh", width: 2, height: 1, decoration: "flat") {
			state "default", label:"Refresh", action:"refresh.refresh"
		}
		
		valueTile("currentPower", "device.power", decoration: "flat", height: 1, width: 2) {
			state "power", label: 'Current Power \n\r ${currentValue} W'
		}

		valueTile("energyToday", "device.energy", decoration: "flat", height: 1, width: 2) {
			state "energy", label: 'Usage Today\n\r${currentValue} WattHr'
		}

		valueTile("monthTotal", "device.monthTotalE", decoration: "flat", height: 1, width: 2) {
			state "monthTotalE", label: '30 Day Total\n\r ${currentValue} KWH'
		}

		valueTile("monthAverage", "device.monthAvgE", decoration: "flat", height: 1, width: 2) {
			state "monthAvgE", label: '30 Day Avg\n\r ${currentValue} KWH'
		}
 
		valueTile("weekTotal", "device.weekTotalE", decoration: "flat", height: 1, width: 2) {
			state "weekTotalE", label: '7 Day Total\n\r ${currentValue} KWH'
		}

		valueTile("weekAverage", "device.weekAvgE", decoration: "flat", height: 1, width: 2) {
			state "weekAvgE", label: '7 Day Avg\n\r ${currentValue} KWH'
		}

		valueTile("4x1Blank", "default", decoration: "flat", height: 1, width: 4) {
			state "default", label: ''
		}

		main("switch")
		details("switch", "refresh" ,"4x1Blank",
				"currentPower", "weekTotal", "monthTotal",
				"energyToday", "weekAverage", "monthAverage")
	}

	def rates = [:]
	rates << ["5" : "Refresh every 5 minutes"]
	rates << ["10" : "Refresh every 10 minutes"]	
	rates << ["15" : "Refresh every 15 minutes"]
	rates << ["30" : "Refresh every 30 minutes"]

	preferences {
		if (installType == "Hub") {
			input("deviceIP", "text", title: "Device IP", required: true, displayDuringSetup: true)
			input("gatewayIP", "text", title: "Gateway IP", required: true, displayDuringSetup: true)
		}
		input name: "refreshRate", type: "enum", title: "Refresh Rate", options: rates, description: "Select Refresh Rate", required: false
	}
}

//	===== Update when installed or setting changed =====
def installed() {
	update()
}

def updated() {
	runIn(2, update)
}

def update() {
	state.deviceType = metadata.definition.deviceType
	state.installType = metadata.definition.installType
	state.emon = metadata.definition.energyMonitor
	state.emeterText = "emeter"
	state.getTimeText = "time"
	unschedule()
	switch(refreshRate) {
		case "5":
			runEvery5Minutes(refresh)
			log.info "Refresh Scheduled for every 5 minutes"
			break
		case "10":
			runEvery10Minutes(refresh)
			log.info "Refresh Scheduled for every 10 minutes"
			break
		case "15":
			runEvery15Minutes(refresh)
			log.info "Refresh Scheduled for every 15 minutes"
			break
		default:
			runEvery30Minutes(refresh)
			log.info "Refresh Scheduled for every 30 minutes"
	}
	schedule("0 05 0 * * ?", setCurrentDate)
	schedule("0 10 0 * * ?", getEnergyStats)
	setCurrentDate()
	runIn(2, refresh)
	runIn(7, getEnergyStats)
}

void uninstalled() {
	if (state.installType == "Cloud") {
		def alias = device.label
		log.debug "Removing device ${alias} with DNI = ${device.deviceNetworkId}"
		parent.removeChildDevice(alias, device.deviceNetworkId)
	}
}

//	===== Basic Plug Control/Status =====
def on() {
	sendCmdtoServer('{"system":{"set_relay_state":{"state": 1}}}', "deviceCommand", "commandResponse")
	runIn(2, refresh)
}

def off() {
	sendCmdtoServer('{"system":{"set_relay_state":{"state": 0}}}', "deviceCommand", "commandResponse")
	runIn(2, refresh)
}

def poll() {
	sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "deviceCommand", "commandResponse")
}

def refresh(){
	sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "deviceCommand", "commandResponse")
	runIn(2, getPower)
}

def commandResponse(cmdResponse){
	if (cmdResponse.system.set_relay_state == null) {
		def status = cmdResponse.system.get_sysinfo.relay_state
		if (status == 1) {
			status = "on"
		} else {
			status = "off"
		}
		log.info "${device.name} ${device.label}: Power: ${status}"
		sendEvent(name: "switch", value: status)
	}	else {
		return
	}
}

//	===== Get Current Energy Data =====
def getPower(){
	sendCmdtoServer("""{"${state.emeterText}":{"get_realtime":{}}}""", "deviceCommand", "energyMeterResponse")
	runIn(5, getConsumption)
}

def energyMeterResponse(cmdResponse) {
	def realtime = cmdResponse["emeter"]["get_realtime"]
	if (realtime.power == null) {
		state.powerScale = "power_mw"
		state.energyScale = "energy_wh"
	} else {
		state.powerScale = "power"
		state.energyScale = "energy"
	}
	def powerConsumption = realtime."${state.powerScale}"
		if (state.powerScale == "power_mw") {
			powerConsumption = Math.round(powerConsumption/10) / 100
		} else {
		powerConsumption = Math.round(100*powerConsumption) / 100
		}
	sendEvent(name: "power", value: powerConsumption)
	log.info "$device.name $device.label: Updated CurrentPower to $powerConsumption"
}

//	===== Get Today's Consumption =====
def getConsumption(){
	sendCmdtoServer("""{"${state.emeterText}":{"get_daystat":{"month": ${state.monthToday}, "year": ${state.yearToday}}}}""", "emeterCmd", "useTodayResponse")
}

def useTodayResponse(cmdResponse) {
	def wattHrToday
	def wattHrData
	def dayList = cmdResponse["emeter"]["get_daystat"].day_list
	for (int i = 0; i < dayList.size(); i++) {
		wattHrData = dayList[i]
		if(wattHrData.day == state.dayToday) {
			wattHrToday = wattHrData."${state.energyScale}"
 		}
	}
	if (state.powerScale == "power") {
		wattHrToday = Math.round(1000*wattHrToday)
	}
	sendEvent(name: "energy", value: wattHrToday)
	log.info "$device.name $device.label: Updated Usage Today to ${wattHrToday}"
}

//	===== Get Weekly and Monthly Stats =====
def getEnergyStats() {
	state.monTotEnergy = 0
	state.monTotDays = 0
	state.wkTotEnergy = 0
	state.wkTotDays = 0
	sendCmdtoServer("""{"${state.emeterText}":{"get_daystat":{"month": ${state.monthToday}, "year": ${state.yearToday}}}}""", "emeterCmd", "engrStatsResponse")
	runIn(4, getPrevMonth)
}

def getPrevMonth() {
//	If all of the data is in this month, do not request previous month.
//	This will occur when the current month is 31 days.
	if (state.monthToday == state.monthStart) {
		return
	} else {
		sendCmdtoServer("""{"${state.emeterText}":{"get_daystat":{"month": ${state.monthStart}, "year": ${state.yearStart}}}}""", "emeterCmd", "engrStatsResponse")
	}
}

def engrStatsResponse(cmdResponse) {
/*	
	This method parses up to two energy status messages from the device,
	adding the energy for the previous 30 days and week, ignoring the
	current day.  It then calculates the 30 and 7 day average formatted
	in kiloWattHours to two decimal places.
*/
	def dayList = cmdResponse[state.emeterText]["get_daystat"].day_list
	if (!dayList[0]) {
		log.info "$device.name $device.label: Month has no energy data."
		return
	}
	def monTotEnergy = state.monTotEnergy
	def wkTotEnergy = state.wkTotEnergy
	def monTotDays = state.monTotDays
	def wkTotDays = state.wkTotDays
	if (dayList[0].month == state.monthToday) {
		for (int i = 0; i < dayList.size(); i++) {
			def energyData = dayList[i]
			monTotEnergy += energyData."${state.energyScale}"
			monTotDays += 1
			if (state.dayToday < 8 || energyData.day >= state.weekStart) {
				wkTotEnergy += energyData."${state.energyScale}"
				wkTotDays += 1
			}
			if(energyData.day == state.dayToday) {
				monTotEnergy -= energyData."${state.energyScale}"
				wkTotEnergy -= energyData."${state.energyScale}"
				monTotDays -= 1
				wkTotDays -= 1
			}
		}
	} else {
		for (int i = 0; i < dayList.size(); i++) {
			def energyData = dayList[i]
			if (energyData.day >= state.dayStart) {
				monTotEnergy += energyData."${state.energyScale}"
				monTotDays += 1
			}
			if (energyData.day >= state.weekStart && state.dayToday < 8) {
				wkTotEnergy += energyData."${state.energyScale}"
				wkTotDays += 1
			}
		}
	}
	state.monTotDays = monTotDays
	state.monTotEnergy = monTotEnergy
	state.wkTotEnergy = wkTotEnergy
	state.wkTotDays = wkTotDays
	log.info "$device.name $device.label: Update 7 and 30 day energy consumption statistics"
	def monAvgEnergy =monTotEnergy/monTotDays
	def wkAvgEnergy = wkTotEnergy/wkTotDays
	if (state.powerScale == "power_mw") {
		monAvgEnergy = Math.round(monAvgEnergy/10)/100
		wkAvgEnergy = Math.round(wkAvgEnergy/10)/100
		monTotEnergy = Math.round(monTotEnergy/10)/100
		wkTotEnergy = Math.round(wkTotEnergy/10)/100
	} else {
		monAvgEnergy = Math.round(100*monAvgEnergy)/100
		wkAvgEnergy = Math.round(100*wkAvgEnergy)/100
		monTotEnergy = Math.round(100*monTotEnergy)/100
		wkTotEnergy = Math.round(100*wkTotEnergy)/100
	}
	sendEvent(name: "monthTotalE", value: monTotEnergy)
	sendEvent(name: "monthAvgE", value: monAvgEnergy)
	sendEvent(name: "weekTotalE", value: wkTotEnergy)
	sendEvent(name: "weekAvgE", value: wkAvgEnergy)
}

//	===== Obtain Week and Month Data =====
def setCurrentDate() {
	def df = new java.text.SimpleDateFormat("ddMMyyyy")
	df.setTimeZone(location.timeZone)
	def curDay = df.format(new Date())
	state.dayToday = curDay.substring(0,2).toInteger()
	state.monthToday = curDay.substring(2,4).toInteger()
	state.yearToday = curDay.substring(4,8).toInteger()
	def dayOne = df.format(new Date()-30)
	state.dayStart = dayOne.substring(0,2).toInteger()
	state.monthStart = dayOne.substring(2,4).toInteger()
	state.yearStart = dayOne.substring(4,8).toInteger()
	def wkStart = df.format(new Date()-7)
	state.weekStart = wkStart.substring(0,2).toInteger()
}

//	----- SEND COMMAND TO CLOUD VIA SM -----
private sendCmdtoServer(command, hubCommand, action) {
	if (state.installType == "Hub") {
		sendCmdtoHub(command, hubCommand, action)
	} else {
		sendCmdtoCloud(command, hubCommand, action)
	}
}

private sendCmdtoCloud(command, hubCommand, action){
	def appServerUrl = getDataValue("appServerUrl")
	def deviceId = getDataValue("deviceId")
	def cmdResponse = parent.sendDeviceCmd(appServerUrl, deviceId, command)
	String cmdResp = cmdResponse.toString()
	if (cmdResp.substring(0,5) == "ERROR"){
		def errMsg = cmdResp.substring(7,cmdResp.length())
		log.error "${device.name} ${device.label}: ${errMsg}"
		sendEvent(name: "switch", value: "commsError", descriptionText: errMsg)
		sendEvent(name: "deviceError", value: errMsg)
		action = ""
	} else {
		sendEvent(name: "deviceError", value: "OK")
	}
		actionDirector(action, cmdResponse)
}

private sendCmdtoHub(command, hubCommand, action){
	def headers = [:] 
	headers.put("HOST", "$gatewayIP:8082")	//	Same as on Hub.
	headers.put("tplink-iot-ip", deviceIP)
	headers.put("tplink-command", command)
	headers.put("action", action)
	headers.put("command", hubCommand)
	sendHubCommand(new physicalgraph.device.HubAction([
		headers: headers],
		device.deviceNetworkId,
		[callback: hubResponseParse]
	))
}

def hubResponseParse(response) {
	def action = response.headers["action"]
	def cmdResponse = parseJson(response.headers["cmd-response"])
	if (cmdResponse == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR at hubResponseParse TCP Timeout")
		sendEvent(name: "deviceError", value: "TCP Timeout in Hub")
	} else {
		actionDirector(action, cmdResponse)
		sendEvent(name: "deviceError", value: "OK")
	}
}

def actionDirector(action, cmdResponse) {
	switch(action) {
		case "commandResponse":
			commandResponse(cmdResponse)
			break

		case "energyMeterResponse":
			energyMeterResponse(cmdResponse)
			break
			
		case "useTodayResponse":
			useTodayResponse(cmdResponse)
			break
			
		case "currentDateResponse":
			currentDateResponse(cmdResponse)
			break
			
		case "engrStatsResponse":
			engrStatsResponse(cmdResponse)
			break
			
		default:
			log.debug "at default"
	}
}

//	----- CHILD / PARENT INTERCHANGE TASKS -----
def syncAppServerUrl(newAppServerUrl) {
	updateDataValue("appServerUrl", newAppServerUrl)
		log.info "Updated appServerUrl for ${device.name} ${device.label}"
}