/**
*  Shit Happens
*
*  Copyright 2015 Brian Keifer
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*/

definition(
    name: "Shit Happens",
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "Use one (or more) Hue lights to warn you about bad things happening.",
    importUrl: "https://raw.githubusercontent.com/bkeifer/hubitat/master/apps/ShitHappens/ShitHappens.groovy",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""

)


preferences {
    section("Control these bulbs...") {
        input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:true, multiple:true
    }
    section("Light level...") {
        input "level", "number", title: "In percent", required: false
    }
    section("Warn for these contact sensors...") {
        input "contacts", "capability.contactSensor", title: "Contacts", required: false, multiple: true
    }
    section("Library Alert Switch") {
        input "library", "capability.switch", title: "Switch", required: false, multiple:false
    }
    section("Swim Class Alert Switch") {
        input "swimclass", "capability.switch", title: "Switch", required: false, multiple:false
    }
    section("Low temperature to trigger warning...") {
        input "lowtemp", "number", title: "In degrees F", required: false
    }
    section("How far out should we look for freezing temps?") {
        input "hours", "number", title: "Hours", required: false, description: 24
    }
    section("Days between calendar alerts...") {
        input "caldays", "number", title: "Days", required: false
    }
    section("Switch to use to reset calendar alert") {
        input "calswitch", "capability.switch", title: "Switch", required: false, multiple: false
    }
    section("Forecast API Key") {
        input "apikey", "text", required: false
    }
}



def installed() {
    log.debug "Installed with settings: ${settings}"
    state.alerts = [:]
    state.contacts = [:]
    state.switches = [:]
    state.forecast = []
    state.dogmeds = -1
    initialize()
}


def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}


def initialize() {
    log.trace("initialize")

    if (!state.alerts) {
        state.alerts = [:]
    } else {
        log.debug("state.alerts already set.")
    }

    if (!state.contacts) {
        state.contacts = [:]
    } else {
        log.debug("state.contacts already set.")
    }

    if (!state.switches) {
        state.switches = [:]
    } else {
        log.debug("state.switches already set.")
    }

    if (!state.forecast) {
        state.forecast = []
    } else {
        log.debug("state.forecast already set.")
    }

    if (!state.dogmeds) {
        state.dogmeds = -1
    } else {
        log.debug("state.dogmeds already set.")
    }

    subscribe(app, appTouch)
    subscribe(library, "switch.on", libraryOnHandler)
    subscribe(library, "switch.off", libraryOffHandler)
    subscribe(swimclass, "switch.on", swimclassOnHandler)
    subscribe(swimclass, "switch.off", swimclassOffHandler)
    subscribe(contacts, "contact.open", contactOpenHandler)
    subscribe(contacts, "contact.closed", contactClosedHandler)
    subscribe(calswitch, "switch.on", calendarResetHandler)
    createSchedule()
}


def createSchedule() {
	unschedule()
    checkAll()
    getForecast()
    runEvery5Minutes(checkAll)
    runEvery15Minutes(getForecast)
}


def contactOpenHandler(evt) {
    log.trace("contactOpenHandler")
    state.contacts[evt.displayName] = false
    state.alerts["contact"] = true
    flashToOn("Red")
    log.debug(state)
}


def contactClosedHandler(evt) {
    log.trace("contactClosedHandler")
    state.contacts[evt.displayName] = true
    log.debug(state.contacts)
    checkDoors()
    flash("Green")
    log.debug("continuing")
    updateHues()
}


def switchOnHandler(evt) {
    log.trace("switchOnHandler")
    state.switches[evt.displayName] = false
    log.debug(state)
}


def switchOffHandler(evt) {
    log.trace("switchOffHandler")
    state.switches[evt.displayName] = true
    log.debug(state)
}


def calendarResetHandler(evt) {
    log.trace("calendarResetHandler")
    state.dogmeds = now()
    state.alerts["dogmeds"] = false
    checkAll()
}

def flash(color) {
    3.times {
        turnOnToColor(color)
        pause(500)
        hues*.off()
        pause(500)
    }
}


def flashToOn(color) {
    2.times {
        turnOnToColor(color)
        pause(500)
        hues*.off()
        pause(500)
    }
    turnOnToColor(color)
}


def checkCalendar() {
    log.trace("checkCalendar")
    log.debug("CAL: ${state.alerts["dogmeds"]}")

    def elapsed = (now() - state["dogmeds"]) / 1000 / 60 / 60 / 24
    log.debug("Calendar Alert Elapsed Time: ${elapsed} days" )
    if (state.dogmeds == -1 || state.dogmeds == null || elapsed > caldays) {
        state.alerts["dogmeds"] = true
    } else {
        state.alerts["dogmeds"] = false
    }
}


def checkDoors() {
    log.trace("checkDoors")
    int openContacts = 0
    contacts.each {
        if (it.currentState("contact").value == "closed") {
            state.contacts[it.displayName] == true
        } else {
            state.contacts[it.displayName] == false
            openContacts++
        }
    }

    if (openContacts > 0) {
        state.alerts["contact"] = true
        log.debug("Doors still open")
    } else {
        log.debug("All doors closed")
        state.alerts["contact"] = false
    }
    log.debug(state)
}


def checkSwitches() {
    log.trace("checkSwitches")
    if (swimclass.currentState("switch").value == "on") {
        state.switches[swimclass.displayName] = false
    } else {
        state.switches[swimclass.displayName] = true
    }
    if (library.currentState("switch").value == "on") {
        state.switches[library.displayName] = false
    } else {
        state.switches[library.displayName] = true
    }
    log.debug(state)
}


def turnOnToColor(color, delay = 0) {
    log.trace("turnOnToColor(${color}) delay: ${delay}")
    def hueColor = 70
    def saturation = 100
    def lightLevel = 100
    switch(color) {
        case "Blue":
            hueColor = 70
            break;
        case "Green":
            hueColor = 39
            break;
        case "Yellow":
            hueColor = 25
            break;
        case "Orange":
            hueColor = 10
            break;
        case "Purple":
            hueColor = 75
            break;
        case "Pink":
            hueColor = 83
            break;
        case "Red":
            hueColor = 100
            break;
    }
    if (color != "On - Custom Color") {
        def newValue = [hue: hueColor, saturation: saturation, level: lightLevel as Integer ?: 100]
        hues*.setColor(newValue)
        log.debug "new value = $newValue"
    } else {
        hues*.on(delay: delay)
    }
}


def getLowTemp() {
    def params = [
    uri: "https://api.forecast.io",
    path: "/forecast/${apikey}/40.496754,-75.438682"
    ]
    int lowForecast = 1000  // Ow!

    try {
        httpGet(params) { resp ->
            if (resp.data) {
                //log.debug "Response Data = ${resp.data}"
                //log.debug "Response Status = ${resp.status}"

                // resp.headers.each {
                //     log.debug "header: ${it.name}: ${it.value}"
                // }
                resp.getData().each {
                    if (it.key == "hourly") {
                        def x = it.value
                        x.each { xkey ->
                            if (xkey.key == "data") {
                                def y = xkey.value
                                def templist = y["temperature"]
                                def timelist = y["time"]

                                for (int i = 0; i <= 12; i++) {
                                    if ( templist[i] < lowForecast) {
                                        lowForecast = templist[i]
                                        //log.info("new low: ${templist[i]} at ${new Date( ((long)timelist[i])*1000 ) }")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(resp.status == 200) {
                log.debug "getLowTemp Request was OK"
            } else {
                log.error "getLowTemp Request got http status ${resp.status}"
            }
        }
    } catch(e) {
        log.debug e
    }
    return lowForecast
}


def checkAll() {
    log.trace("checkAll")
    checkForFreeze()
    checkDoors()
    checkSwitches()
    checkCalendar()
    updateHues()
    atomicState.timestamp = now()
}


def updateHues(delay = 0) {
    log.trace("updateHues delay:${delay}")
    log.debug("state.alerts[\"contact\"]: ${state.alerts["contact"]}")
    if(state.alerts["contact"] == true) {
        flash("Red")
    } else if (state.alerts["freeze"]) {
        turnOnToColor("Blue", delay)
    } else if(state.alerts["dogmeds"] == true) {
        turnOnToColor("Purple", delay)
    } else {
        log.debug("turning off!  delay: ${delay}")
        hues*.off(delay: delay)
    }
}


def checkForFreeze() {
    log.trace("checkForFreeze")
    if (getLowTemp() < lowtemp ) {
        log.debug("Freeze warning found, turning on lights.")
        state.alerts["freeze"] = true
    } else {
        state.alerts["freeze"] = false
        log.debug("The pipes are safe.  For now...")
    }
    log.trace(state)
}


def getForecast() {
    def params = [
        uri: "https://api.forecast.io",
        path: "/forecast/${apikey}/40.496754,-75.438682"
    ]

	try {
        httpGet(params) { resp ->
            if (resp.data) {
                //log.debug "Response Data = ${resp.data}"
                //log.debug "Response Status = ${resp.status}"
                // resp.headers.each {
                //     log.debug "header: ${it.name}: ${it.value}"
                // }
                resp.getData().each {
                    if (it.key == "hourly") {
                        def x = it.value
                        x.each { xkey ->
                            if (xkey.key == "data") {
                                def y = xkey.value
                                def templist = y["temperature"]

                                for (int i = 0; i <= 48; i++) {
                                    state.forecast[i] = templist[i]
                                }
                            }
                        }
                    }
                }
            }
            if(resp.status == 200) {
                log.debug "getForecast Request was OK"
            } else {
                log.error "getForecast Request got http status ${resp.status}"
            }
        }
    } catch(e) {
        log.debug e
    }
}