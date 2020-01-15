def driverVer() { return "0.1" }

metadata {
    definition (name: "Samsung HW-90Q", namespace: "btk", author: "Brian Keifer") {
        capability "Audio Volume"
        capability "Music Player"

        command "inputHDMI1"
        command "inputHDMI2"
        command "inputARC"
        command "inputWifi"
        command "inputOptical"
    }

    preferences {
        input "ipaddr", "string", title: "Soundbar IP Address:", required: true, defaultValue: false
    }
}

def installed() {
    initialize()
}

def initialize() {
    log.debug("Initialized.")
}


private sendSyncCmd(command){
    log.debug("ipaddr: ${ipaddr}")
	def host = "http://${ipaddr}:56001"
	logDebug("sendSyncCmd: Command= ${command}, host = ${host}")
	try {
		httpGet([uri: "${host}${command}", contentType: "text/xml", timeout: 5]) { resp ->
		if(resp.status != 200) {
			logWarn("sendSyncCmd, Command ${command}: Error return: ${resp.status}")
			return
		} else if (resp.data == null){
			logWarn("sendSyncCmd, Command ${command}: No data in command response.")
			return
		}
		def respMethod = resp.data.method
		def respData = resp.data.response
		extractData(respMethod, respData)
		}
	} catch (error) {
		if (command == "/UIC?cmd=%3Cname%3EGetPlayStatus%3C/name%3E") { return }
		logWarn("sendSyncCmd, Command ${command}: No response received.  Error = ${error}")
		return "commsError"
	}
}

private setInput(input) {
    log.trace("setInput($input) command received.")
    if (input != null) {
        sendSyncCmd("/UIC?cmd=%3Cname%3ESetFunc%3C/name%3E%3Cp%20type=%22str%22%20name=%22function%22%20val=%22${input}%22/%3E")
    }
}

public inputHDMI1() {
    setInput("hdmi1")
}

public inputHDMI2() {
    setInput("hdmi2")
}

public inputARC() {
    setInput("arc")
}

public inputOptical() {
    setInput("optical")
}

public setVolume(vol) {
    log.trace("setVolume($vol) command received.")
    sendSyncCmd("/UIC?cmd=%3Cname%3ESetVolume%3C/name%3E%3Cp%20type=%22dec%22%20name=%22volume%22%20val=%22${vol}%22/%3E")
}


def logInfo(msg) {
	if (descriptionText == true) { log.info "${device.label} ${driverVer()} ${msg}" }
}

def logDebug(msg){
	if(debug == true) { log.debug "${device.label} ${driverVer()} ${msg}" }
}

def logWarn(msg){ log.warn "${device.label} ${driverVer()} ${msg}" }