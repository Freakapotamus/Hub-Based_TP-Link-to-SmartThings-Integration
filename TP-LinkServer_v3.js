#!/usr/bin/env node

/*
TP-LinkHub - Version 1.0

COMPATABILITY KEY:  HubVersion 1.0

This java script uses node.js functionality to provide a hub between SmartThings and TP-Link devices.  It works with the following TP-Link integrations:
a.	TP-Link Connect (including Discovery)
b.	TP-Link Smart Things Integration
c.	TP-Link Bridge (OPTIONAL)

07/13/2017 - Update to eliminate the 1.5% comms error rate I am getting.  At same time, updating the Energy Monitor Device Handlers - optional for existing users.
07/13/2017 - Update to add switch "oldNode" to allow working with pre-node.js V6 installations.  Major difference is the depreciated 'new Buffer' command whose replacement 'Buffer.alloc' does not exist in the earlier versions.
07/13/2017 - Removed Bridge Support from this file.  Update Bridge installation accordingly.
07/09/2017 - Updated for commonality with existing (non-connected) TP-Link and Bridge Device Handlers.
*/

//##### Options for this program ###################################
var logFile = "yes"	//	Set to no to disable error.log file.
var hubPort = 8082	//	Synched with Device Handlers.
//##################################################################

//---- Determine if old Node version, act accordingly -------------
console.log("Node.js Version Detected:   " + process.version)
var oldNode = "no"
if (process.version == "v6.0.0-pre") {
	oldNode ="yes"
	logFile = "no"
}

//---- Program set up and global variables -------------------------
var http = require('http')
var net = require('net')
var fs = require('fs')
var server = http.createServer(onRequest)

//---- Start the HTTP Server Listening to SmartThings --------------
server.listen(hubPort)
console.log("TP-Link Hub Console Log")
logResponse("\n\r" + new Date() + "\rTP-Link Hub Error Log")

//---- Command interface to Smart Things ---------------------------
function onRequest(request, response){
	var command = request.headers["command"]
	var deviceIP = request.headers["tplink-iot-ip"]
	var cmdRcvd = "\n\r" + new Date() + "\r\nIP: " + deviceIP + " sent command " + command
	console.log(" ")
	console.log(cmdRcvd)
	switch(command) {
		//---- (BridgeDH - Poll for Server APP ------------------
		case "pollServer":
			response.setHeader("cmd-response", "ok")
			response.end()
			var respMsg = "Server Poll response sent to SmartThings"
			console.log(respMsg)
		break

		//---- TP-Link Device Command ---------------------------
		case "deviceCommand":
			processDeviceCommand(request, response)
			break
	
		//---- Special Case for Energy Meter --------------------
		case "emeterCmd":
			processEmeterCommand(request, response)
			break

		default:
			response.setHeader("cmd-response", "InvalidHubCmd")
			response.end()
			var respMsg = "#### Invalid Command ####"
			var respMsg = new Date() + "\n\r#### Invalid Command from IP" + deviceIP + " ####\n\r"
			console.log(respMsg)
			logResponse(respMsg)
	}
}

//---- Send deviceCommand and send response to SmartThings ---------
function processDeviceCommand(request, response) {
	var command = request.headers["tplink-command"]
	var deviceIP = request.headers["tplink-iot-ip"]
	var respMsg = "deviceCommand sending to IP: " + deviceIP + " Command: " + command
	console.log(respMsg)
	var socket = net.connect(9999, deviceIP)
	socket.setKeepAlive(false)
	socket.setTimeout(6000)  // 6 seconds timeout.  TEST WITHOUT
   	 socket.on('connect', () => {
  		socket.write(TcpEncrypt(command))
   	 })
	socket.on('data', (data) => {
			socket.end()
			data = decrypt(data.slice(4)).toString('ascii')
			response.setHeader("cmd-response", data)
			response.end()
			var respMsg = "Command Response sent to SmartThings"
			console.log(respMsg)
	}).on('timeout', () => {
		response.setHeader("cmd-response", "TcpTimeout")
		response.end()
		socket.end()
		var respMsg = new Date() + "\n#### TCP Timeout in deviceCommand for IP: " + deviceIP + " ,command: " + command
		console.log(respMsg)
		logResponse(respMsg)
	}).on('error', (err) => {
		socket.end()
		var respMsg = new Date() + "\n#### Socket Error in deviceCommand for IP: " + deviceIP + " ,command: " + command
		console.log(respMsg)
		logResponse(respMsg)
	})
}

//---- Send EmeterCmd and send response to SmartThings -------------
function processEmeterCommand(request, response) {
	var command = request.headers["tplink-command"]
	var deviceIP = request.headers["tplink-iot-ip"]
	var respMsg = "EmeterCmd sending to IP:" + deviceIP + " command: " + command
	console.log(respMsg)
	var socket = net.connect(9999, deviceIP)
	socket.setKeepAlive(false)
	socket.setTimeout(4000)
	socket.on('connect', () => {
  		socket.write(TcpEncrypt(command))
	})
	var concatData = ""
	var resp = ""
	setTimeout(mergeData, 3000)  // 3 seconds to capture response
	function mergeData() {
		if (concatData != "") {
			socket.end()
			data = decrypt(concatData.slice(4)).toString('ascii')
			response.setHeader("cmd-response", data)
			response.end()
			var respMsg = "Command Response sent to SmartThings"
			console.log(respMsg)
		} else {
			socket.end()
			response.setHeader("cmd-response", "TcpTimeout")
			response.end()
			var respMsg = new Date() + "\n#### Comms Timeout in EmeterCmd for IP: " + deviceIP + " ,command: " + command
		console.log(respMsg)
		logResponse(respMsg)
		}
	}
	socket.on('data', (data) => {
		concatData += data.toString('ascii')
	}).on('timeout', () => {
		socket.end()
		var respMsg = new Date() + "\n#### TCP Timeout in EmeterCmd for IP: " + deviceIP + " ,command: " + command
		console.log(respMsg)
		logResponse(respMsg)
	}).on('error', (err) => {
		socket.end()
		var respMsg = new Date() + "\n\r#### TCP Error in EmeterCmd for IP: " + deviceIP + " ,command: " + command
		console.log(respMsg)
		logResponse(respMsg)
	})
}

//----- Utility - Response Logging Function ------------------------
function logResponse(respMsg) {
	if (logFile == "yes") {
		fs.appendFileSync("error.log", "\r" + respMsg)
	}
}

//----- Utility - Encrypt TCP Commands to Devices ------------------
function TcpEncrypt(input) {
	if (oldNode == "no"){
		var buf = Buffer.alloc(input.length + 4)
	} else {
		var buf = new Buffer(input.length + 4)
	}
	buf[0] = null
	buf[1] = null
	buf[2] = null
	buf[3] = input.length
	var key = 0xAB
	for (var i = 4; i < input.length+4; i++) {
		buf[i] = input.charCodeAt(i-4) ^ key
		key = buf[i]
	}
	return buf
}

//----- Utility - Decrypt Returns from  Devices --------------------
function decrypt(input, firstKey) {
	if (oldNode == "no") {
		var buf = Buffer.from(input)
	} else {
		var buf = new Buffer(input)
	}
	var key = 0x2B
	var nextKey
	for (var i = 0; i < buf.length; i++) {
		nextKey = buf[i]
		buf[i] = buf[i] ^ key
		key = nextKey
	}
	return buf
}
