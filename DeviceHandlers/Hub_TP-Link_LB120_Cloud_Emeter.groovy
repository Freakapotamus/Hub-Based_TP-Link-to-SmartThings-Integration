/*
TP-Link LB120 with Energy Meter Device Handler

Copyright 2017 Dave Gutheinz

Compatable with 'TP-LinkServer_v3.js' and 'TP-LinkServer_oldNode.js' files with any of the following 'COMPATABILITY KEY' on line 4:
	a.	HubVersion 1.0

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

		http://www.apache.org/licenses/LICENSE-2.0
		
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

Supported models and functions:  This device supports the TP-Link LB120 with Emeter Functions

Update History
07-04-2017	- Initial release of this Emeter Version
*/

metadata {
	definition (
		name: "TP-Link LB120 Emeter Hub", namespace: "modify", author: "Dave Gutheinz") {
		capability "switch"
		capability "Switch Level"
		capability "Color Temperature"
		capability "refresh"
		capability "polling"
		capability "Sensor"
		capability "Actuator"
		command "setModeNormal"
		command "setModeCircadian"
		attribute "bulbMode", "string"
		command "setCurrentDate"
		capability "powerMeter"
		attribute "monthTotalE", "string"
		attribute "monthAvgE", "string"
		attribute "weekTotalE", "string"
		attribute "weekAvgE", "string"
		attribute "engrToday", "string"
		attribute "dateUpdate", "string"
	}
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"waiting"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"waiting"
				attributeState "waiting", label:'${name}', action:"switch.on", icon:"st.switches.light.on", backgroundColor: "#15EE10", nextState:"on"
				attributeState "offline", label:'Comms Error', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#e86d13", nextState:"waiting"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {attributeState "level", action:"switch level.setLevel"}
		}
		controlTile("colorTempSliderControl", "device.colorTemperature", "slider", width: 4, height: 1, range:"(2700..6500)") {
			state "colorTemperature", action:"color temperature.setColorTemperature"
		}
		valueTile("colorTemp", "device.colorTemperature", decoration: "flat", height: 1, width: 2) {
			state "colorTemp", label: 'Color Temp ${currentValue}K'
		}
		standardTile("bulbMode", "bulbMode", width: 2, height: 1, decoration: "flat") {
			state "normal", label:'Mode:   Normal', action:"setModeCircadian", backgroundColor:"#ffffff", nextState: "circadian"
			state "circadian", label:'Mode:   Circadian', action:"setModeNormal", backgroundColor:"#00a0dc", nextState: "normal"
		}
		standardTile("refresh", "capability.refresh", width: 2, height: 1,  decoration: "flat") {
			state ("default", label:"Refresh", action:"refresh.refresh", backgroundColor:"#ffffff")
		}		 
		standardTile("refreshStats", "Refresh Statistics", width: 2, height: 1,  decoration: "flat") {
			state ("refreshStats", label:"Refresh Stats", action:"setCurrentDate", backgroundColor:"#ffffff")
		}		 
		valueTile("power", "device.power", decoration: "flat", height: 1, width: 2) {
			state "power", label: 'Current Power \n\r ${currentValue} W'
		}
		valueTile("engrToday", "device.engrToday", decoration: "flat", height: 1, width: 2) {
			state "engrToday", label: 'Todays Usage\n\r${currentValue} KWH'
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
		main("switch")
		details(["switch", "colorTempSliderControl", "colorTemp", "power", "weekTotal", "monthTotal", "engrToday", "weekAverage", "monthAverage", "bulbMode", "refresh" ,"refreshStats"])
	}
	def rates = [:]
    rates << ["5" : "Refresh every 5 minutes"]
    rates << ["10" : "Refresh every 10 minutes"]	
    rates << ["15" : "Refresh every 15 minutes"]
    rates << ["30" : "Refresh every 30 minutes"]
	preferences {
	input("deviceIP", "text", title: "Device IP", required: true, displayDuringSetup: true)
	input("gatewayIP", "text", title: "Gateway IP", required: true, displayDuringSetup: true)
        input name: "refreshRate", type: "enum", title: "Refresh Rate", options: rates, description: "Select Refresh Rate", required: false
    }
}


def installed() {
	updated()
}

def updated() {
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
	runIn(2, refresh)
    schedule("0 30 0 * * ?", setCurrentDate)
	runIn(6, setCurrentDate)
}

//	----- BASIC BULB COMMANDS ------------------------------------
def on() {
	sendCmdtoServer('{"smartlife.iot.smartbulb.lightingservice":{"transition_light_state":{"on_off":1}}}', "deviceCommand", "commandResponse")
}
def off() {
	sendCmdtoServer('{"smartlife.iot.smartbulb.lightingservice":{"transition_light_state":{"on_off":0}}}', "deviceCommand", "commandResponse")
}

def setLevel(percentage) {
	percentage = percentage as int
	sendCmdtoServer("""{"smartlife.iot.smartbulb.lightingservice":{"transition_light_state":{"ignore_default":1,"on_off":1,"brightness":${percentage}}}}""", "deviceCommand", "commandResponse")
}

def setColorTemperature(kelvin) {
	kelvin = kelvin as int
	sendCmdtoServer("""{"smartlife.iot.smartbulb.lightingservice":{"transition_light_state":{"ignore_default":1,"on_off":1,"color_temp": ${kelvin},"hue":0,"saturation":0}}}""", "deviceCommand", "commandResponse")
}

def setModeNormal() {
	sendCmdtoServer("""{"smartlife.iot.smartbulb.lightingservice":{"transition_light_state":{"mode":"normal"}}}""", "deviceCommand", "commandResponse")
}

def setModeCircadian() {
	sendCmdtoServer("""{"smartlife.iot.smartbulb.lightingservice":{"transition_light_state":{"mode":"circadian"}}}""", "deviceCommand", "commandResponse")
}

def commandResponse(response){
	if (response.headers["cmd-response"] == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR - OffLine - mod commandResponse", isStateChange: true)
	} else {
		def cmdResponse = parseJson(response.headers["cmd-response"])
		state =  cmdResponse["smartlife.iot.smartbulb.lightingservice"]["transition_light_state"]
		parseStatus(state)
	}
}

//	----- REFRESH ------------------------------------------------
def refresh(){
	sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "deviceCommand", "refreshResponse")
}

def refreshResponse(response){
	if (response.headers["cmd-response"] == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR - OffLine - mod refreshResponse", isStateChange: true)
	 } else {
		def cmdResponse = parseJson(response.headers["cmd-response"])
		state = cmdResponse.system.get_sysinfo.light_state
		parseStatus(state)
	}
}

//	----- Parse State from Bulb Responses ------------------------
def parseStatus(state){
	def status = state.on_off
	if (status == 1) {
		status = "on"
	} else {
		status = "off"
		state = state.dft_on_state
	}
	def mode = state.mode
	def level = state.brightness
	def color_temp = state.color_temp
	log.info "$device.name $device.label: Power: ${status} / Mode: ${mode} / Brightness: ${level}% / Color Temp: ${color_temp}K"
	sendEvent(name: "switch", value: status, isStateChange: true)
	sendEvent(name: "bulbMode", value: mode, isStateChange: true)
	sendEvent(name: "level", value: level, isStateChange: true)
	sendEvent(name: "colorTemperature", value: color_temp, isStateChange: true)
	getEngeryMeter()
}

//	----- Get Current Energy Use Rate ----------------------------
def getEngeryMeter(){
	sendCmdtoServer('{"smartlife.iot.common.emeter":{"get_realtime":{}}}', "deviceCommand", "energyMeterResponse")
}

def energyMeterResponse(response) {
	if (response.headers["cmd-response"] == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR - OffLine - mod refreshResponse", isStateChange: true)
	 } else {
		def cmdResponse = parseJson(response.headers["cmd-response"])
		   def realtime = cmdResponse["smartlife.iot.common.emeter"]["get_realtime"]
		def powerConsumption = realtime.power_mw / 1000
		sendEvent(name: "power", value: powerConsumption, isStateChange: true)
		log.info "$device.name $device.label: Updated CurrentPower to $powerConsumption"
		getUseToday()
	}
}

//	----- Get Today's Consumption --------------------------------
def getUseToday(){
	getDateData()
	sendCmdtoServer("""{"smartlife.iot.common.emeter":{"get_daystat":{"month": ${state.monthToday}, "year": ${state.yearToday}}}}""", "emeterCmd", "useTodayResponse")
}

def useTodayResponse(response) {
	if (response.headers["cmd-response"] == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR - OffLine - mod refreshResponse", isStateChange: true)
	 } else {
		def engrToday
		def cmdResponse = parseJson(response.headers["cmd-response"])
		def dayList = cmdResponse["smartlife.iot.common.emeter"]["get_daystat"].day_list
		for (int i = 0; i < dayList.size(); i++) {
			def engrData = dayList[i]
			if(engrData.day == state.dayToday) {
				engrToday = engrData.energy_wh/1000
			}
	   }
		sendEvent(name: "engrToday", value: engrToday, isStateChange: true)
		log.info "$device.name $device.label: Updated Today's Usage to $engrToday"
	}
}

//	----- Get Weekly and Monthly Stats ---------------------------
def getWkMonStats() {
	state.monTotEnergy = 0
	state.monTotDays = 0
	state.wkTotEnergy = 0
	getDateData()
	sendCmdtoServer("""{"smartlife.iot.common.emeter":{"get_daystat":{"month": ${state.monthToday}, "year": ${state.yearToday}}}}""", "emeterCmd", "engrStatsResponse")
	runIn(4, getPrevMonth)
}

def getPrevMonth() {
	getDateData()
	if (state.dayToday < 31) {
		def month = state.monthToday
		def year = state.yearToday
		if (month == 1) {
			year -= 1
			month = 12
			sendCmdtoServer("""{"smartlife.iot.common.emeter":{"get_daystat":{"month": ${month}, "year": ${year}}}}""", "emeterCmd", "engrStatsResponse")
		} else {
			month -= 1
			sendCmdtoServer("""{"smartlife.iot.common.emeter":{"get_daystat":{"month": ${month}, "year": ${year}}}}""", "emeterCmd", "engrStatsResponse")
		}
	}
}

def engrStatsResponse(response) {
	if (response.headers["cmd-response"] == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR - OffLine - mod refreshResponse", isStateChange: true)
	 } else {
		getDateData()
		def monTotEnergy = state.monTotEnergy
		def wkTotEnergy = state.wkTotEnergy
		def monTotDays = state.monTotDays
		Calendar calendar = GregorianCalendar.instance
		calendar.set(state.yearToday, state.monthToday, 1)
		def prevMonthDays = calendar.getActualMaximum(GregorianCalendar.DAY_OF_MONTH)
		def weekEnd = state.dayToday + prevMonthDays - 1
		def weekStart = weekEnd - 6
		def cmdResponse = parseJson(response.headers["cmd-response"])
		def dayList = cmdResponse["smartlife.iot.common.emeter"]["get_daystat"].day_list
		def dataMonth = dayList[0].month
		def currentMonth = state.monthToday
		def addedDays = 0
		if (currentMonth == dataMonth) {
			addedDays = prevMonthDays
		} else {
			addedDays = 0
		}
		for (int i = 0; i < dayList.size(); i++) {
			def engrData = dayList[i]
			if(engrData.day == state.dayToday && engrData.month == state.monthToday) {
				monTotDays -= 1
			} else {
				monTotEnergy += engrData.energy_wh
			}
			def adjustDay = engrData.day + addedDays
			if (adjustDay <= weekEnd && adjustDay >= weekStart) {
				wkTotEnergy += engrData.energy_wh
			}
		}
		monTotDays += dayList.size()
		state.monTotDays = monTotDays
		state.monTotEnergy = monTotEnergy
		state.wkTotEnergy = wkTotEnergy
		if (state.dayToday == 31 || state.monthToday -1 == dataMonth) {
			log.info "$device.name $device.label: Updated 7 and 30 day energy consumption statistics"
			def monAvgEnergy = Math.round(monTotEnergy/(monTotDays-1))/1000
			def wkAvgEnergy = Math.round(wkTotEnergy/7)/1000
			sendEvent(name: "monthTotalE", value: monTotEnergy/1000, isStateChange: true)
			sendEvent(name: "monthAvgE", value: monAvgEnergy, isStateChange: true)
			sendEvent(name: "weekTotalE", value: wkTotEnergy/1000, isStateChange: true)
			sendEvent(name: "weekAvgE", value: wkAvgEnergy, isStateChange: true)
		}
	}
}

//	----- Update date data ---------------------------------------
def setCurrentDate() {
	sendCmdtoServer('{"smartlife.iot.common.timesetting":{"get_time":null}}', "deviceCommand", "currentDateResponse")
}

def currentDateResponse(response) {
	if (response.headers["cmd-response"] == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR - OffLine - mod refreshResponse", isStateChange: true)
	 } else {
		def cmdResponse = parseJson(response.headers["cmd-response"])
		def setDate =  cmdResponse["smartlife.iot.common.timesetting"]["get_time"]
		updateDataValue("dayToday", "$setDate.mday")
		updateDataValue("monthToday", "$setDate.month")
		updateDataValue("yearToday", "$setDate.year")
		sendEvent(name: "dateUpdate", value: "${setDate.year}/${setDate.month}/${setDate.mday}")
		log.info "$device.name $device.label: Current Date Updated to ${setDate.year}/${setDate.month}/${setDate.mday}"
	}
	getWkMonStats()
}

def getDateData(){
	state.dayToday = getDataValue("dayToday") as int
	state.monthToday = getDataValue("monthToday") as int
	state.yearToday = getDataValue("yearToday") as int
}

//	----- Send the Command to the Bridge -------------------------
private sendCmdtoServer(command, hubCommand, action){
	def headers = [:]
	headers.put("HOST", "$gatewayIP:8082")   // Same as on Hub.
	headers.put("tplink-iot-ip", deviceIP)
	headers.put("tplink-command", command)
	headers.put("command", hubCommand)
	sendHubCommand(new physicalgraph.device.HubAction([
		headers: headers],
		device.deviceNetworkId,
		[callback: action]
	))
}
