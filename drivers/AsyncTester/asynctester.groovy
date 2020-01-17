metadata {
    definition (name: "Async Call Tester", namespace: "btk", author: "Brian Keifer") {
        command "refresh"
        command "refreshVolume"
        command "refreshInput"
    }
}

def installed()  {initialize()}
def updated()    {initialize()}
def initialize() {log.debug("Initialized.")}

def refresh() {
    sendCommand("volume")
    sendCommand("input")
}

def refreshVolume() { sendCommand("volume") }
def refreshInput()  { sendCommand("input")  }

private sendCommand(page) {
    if (page == "volume") {
        uri = "http://10.13.13.49:56001/UIC?cmd=%3Cname%3EGetVolume%3C/name%3E"
    } else if (page == "input") {
        uri = "http://10.13.13.49:56001/UIC?cmd=%3Cname%3EGetFunc%3C/name%3E"
    }

    asynchttpGet('processResponse', [uri: uri], [requestType: page])
    log.debug("sendCommand - ${page} - ${uri}")
}

private processResponse(response, data) {
    log.debug(data['requestType'] + " - object - " + response)
    log.debug(data['requestType'] + " - getData - " + response.getData())
}

