definition(
    name: "Bath Sequence Controller",
    namespace: "btk",
    author: "Brian Keifer",
    description: "Controls bedroom/bathroom lights in sequence for bath time",
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

        section("Sensor Selection") {
            input "switchStartSequence", "capability.switch", title: "Switch to start the sequence", required: true
            input "motionStartShower", "capability.motionSensor", title: "Start shower when this sensor activates", required: true
            input "motionStartBedtime", "capability.motionSensor", title: "Start bed time when this sensor activates", required: true
            input "motionResetRoutine", "capability.motionSensor", title: "Reset system when this sensor deactivates", required: true
        }
        section("Scene Selection") {
            input "sceneBath", "capability.switch", title: "Scene to turn on for bath time", required: true
            input "sceneShower", "capability.switch", title: "Scene to turn on for shower time", required: true
            input "sceneBed", "capability.switch", title: "Scene to turn on for bed time", required: true
        }
        section("Semaphore Switch") {
            input "switchSemaphore", "capability.switch", title: "Switch to turn on when this app is in control of the lights", required: true
        }
        section("Enable?") {
            input "enabled", "bool", title: "Enable this SmartApp?"
        }
        section("Debug") {
            input "debugMode", "bool", title: "Enable debug logging?", required: true, defaultValue: false
        }
    }
}


def installed() {
    log.debug "Installed using settings: ${settings}"
    initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  unsubscribe()
  initialize()
}

def initialize() {
    log.info "Bath Sequence Controller intitializing..."

    subscribe(switchStartSequence, "switch.on", handleSequenceStart)
    subscribe(motionStartShower, "motion.active", handleMotionStartShower)
    subscribe(motionStartBedtime, "motion.active", handleMotionStartBedtime)
    subscribe(motionResetRoutine, "motion.inactive", handleMotionResetRoutine)

    log.info "Subscriptions created!"

    state.phase = "idle"
    // Phases
    // idle
    // bath
    // shower
    // bed

    log.info "State set to idle!"

    switchSemaphore.off()
    log.info "Semaphore turned off."
}


def handleSequenceStart(evt) {
    if (state.phase == "idle") {
        state.phase = "bath"
        log.info("New phase: BATH")
        log.debug("Turning activation switch off")
        switchStartSequence.off()
        log.debug("Turning semaphore switch on")
        switchSemaphore.on()
        log.debug("Setting BATH scene")
        sceneBath.on()
    }
}


def handleMotionStartShower(evt) {
    if (state.phase == "bath") {
        state.phase = "shower"
        log.info("New phase: SHOWER")
        log.debug("Setting SHOWER scene")
        sceneShower.on()
    }
}


def handleMotionStartBedtime(evt) {
    if (state.phase == "shower") {
        state.phase = "bed"
        log.info("New phase: BED")
        log.debug("Setting BED scene")
        sceneBed.on()
    }
}


def handleMotionResetRoutine(evt) {
    if (state.phase == "bed") {
        state.phase = "idle"
        log.info("New phase: IDLE")
        log.debug("Turning semaphore switch off")
        switchSemaphore.off()
    }
}
