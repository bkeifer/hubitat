definition(
    name: "Smart Porch Lights",
    namespace: "btk",
    author: "Brian Keifer",
    description: "Porch light timer with additional lighting on arrival",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
    page(name: "prefPage", title:"", install: true, uninstall: true)
}


def prefPage() {
    dynamicPage(name: "prefPage", title: "Preferences", uninstall: true, install: true) {
        section("Lights") {
            input "lights", "capability.switchLevel", title: "Which lights will this app manage?", requiured: true
            input "normalLevel", "number", title: "Normal Light Level", required: true
            input "arrivalLevel", "number", title: "Arrival Light Level", required: true
            input "brightTime", "number", title: "For how many minutes should the lights brighten?", required: true, defaultValue: 5
        }

        section("Turn on between...") {
            input "sunsetOffsetValue", "number", title: "Optional Sunset Offset (Minutes)", required: false
            input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
            input "sunriseOffsetValue", "number", title: "Optional Sunrise Offset (Minutes)", required: false
            input "sunriseOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
     	}

        section("Presence Sensors") {
            input "presenceSensors", "capability.presenceSensor", title: "Which presence sensors should we track?", multiple: true, requiured: true
        }

        section("Enable?") {
            input "enabled", "bool", title: "Enable this app?"
        }

        section("Debug") {
            input "debugMode", "bool", title: "Enable debug logging?", required: true, defaultValue: false
        }
    }
}


def installed() { initialize() }
def updated() { initialize() }
def initialize() {
    setVersion()
    log.info ("Smart Porch Lights initializing...")
    logDebug("Using settings: ${settings}")
    createSubscriptions()
}


def createSubscriptions() {
    unsubscribe()
    subscribe(presenceSensors, "presence", handlePresence)

    astroCheck()

    //Update sunrise/sunset each day at 12:01am
    schedule("0 1 0 ? * * *", astroCheck)
}


def handlePresence(evt) {
    if (evt.value == "present"){
        if (settings.enabled) {
            sendEvent(name:"SmartPorchLights", value: "handlePresence", descriptionText:"Sensor arrived - ${evt.device}")
            logDebug("Sensor arrived - ${evt.device}")

           	def both = getSunriseAndSunset(sunriseOffset: state.sunriseOffset, sunsetOffset: state.sunsetOffset)
        	def now = new Date()
            def riseTime = both.sunrise
            def setTime = both.sunset

           	if (riseTime.after(now) || setTime.before(now)) {
                lights.setLevel(settings.arrivalLevel,2)
                runIn(settings.brightTime*60, dimLights, [overwrite: true])
            } else {
                sendEvent(name:"SmartPorchLights", value: "handlePresence", descriptionText:"Currently daytime.  Ignoring arrival of ${evt.device}")
                logDebug("Currently daytime.  Ignoring arrival of ${evt.device}")
            }
        }
    }
}


def dimLights() {
    sendEvent(name:"SmartPorchLights", value: "dimLights", descriptionText:"Dimming lights back to ${settings.normalLevel}.")
    logDebug("Dimming lights back to ${settings.normalLevel}.")
    lights.setLevel(settings.normalLevel, 10)
}


def sunriseHandler(evt) {
    sendEvent(name:"SmartPorchLights", value: "SunriseHandler", descriptionText:"SunriseHandler fired!")
    logDebug("Sun has risen!")
    state.astro = "Risen"
    lights.off()
}


def sunsetHandler(evt) {
    sendEvent(name:"SmartPorchLights", value: "SunsetHandler", descriptionText:"SunsetHandler fired!")
    logDebug("Sun has set!")
    state.astro = "Set"
    lights.setLevel(settings.normalLevel, 10)
}


// Thank you, Cobra!
def astroCheck() {
    state.sunsetOffsetValue = sunsetOffsetValue
    state.sunriseOffsetValue = sunriseOffsetValue
    if(sunsetOffsetDir == "Before"){state.sunsetOffset = -state.sunsetOffsetValue}
    if(sunsetOffsetDir == "After"){state.sunsetOffset = state.sunsetOffsetValue}
    if(sunriseOffsetDir == "Before"){state.sunriseOffset = -state.sunriseOffsetValue}
    if(sunriseOffsetDir == "After"){state.sunriseOffset = state.sunriseOffsetValue}

	def both = getSunriseAndSunset(sunriseOffset: state.sunriseOffset, sunsetOffset: state.sunsetOffset)
	def now = new Date()

	def riseTime = both.sunrise
	def setTime = both.sunset

	logDebug("riseTime: $riseTime")
	logDebug("setTime: $setTime")

	unschedule("sunriseHandler")
	unschedule("sunsetHandler")

	if (riseTime.after(now)) {
        if (settings.enabled) {
            sendEvent(name:"SmartPorchLights", value: "SunriseHandler", descriptionText:"Scheduling sunrise handler for $riseTime")
            logDebug("Scheduling sunrise handler for $riseTime")
            runOnce(riseTime, sunriseHandler)
        }
	}
	if(setTime.after(now)) {
        if (settings.enabled) {
            sendEvent(name:"SmartPorchLights", value: "SunsetHandler", descriptionText:"Scheduling sunset handler for $setTime")
            logDebug("Scheduling sunset handler for $setTime")
            runOnce(setTime, sunsetHandler)
        }
	}
	logDebug("astroCheck() Complete")
}


def logDebug(txt){
    try {
    	if (settings.debugMode) { log.debug("${app.label} - (Version: ${state.version}) - ${txt}") }
    } catch(ex) {
    	log.error("logDebug unable to output requested data!")
    }
}


def setVersion() {
    state.version = "0.2"
}