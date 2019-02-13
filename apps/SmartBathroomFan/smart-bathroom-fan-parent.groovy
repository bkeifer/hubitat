
definition(
    name:"Smart Bathroom Fan Controllers",
    namespace:"btk",
    author:"Brian Keifer",
    description: "Parent app for Smart Bathroom Fan",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)


preferences {
    page name: "mainPage", title: "", install: true, uninstall: true
}

def installed() {
    log.debug "Installed using settings: ${settings}"
    initialize()
}

def initialize() {
    setVersion()
    log.info "Smart Bathroom Fan Parent Initialized"
}


def mainPage() {
    dynamicPage(name: "mainPage") {
        installCheck()

        if (state.appInstalled == 'COMPLETE') {
            display()

            section ("") {
                app(name: "smartBathroomFanApp", appName: "Smart Bathroom Fan", namespace: "btk", title: "Add a new Smart Bathroom Fan", multiple: true)
            }

        }
    }
}


def display() {
    section{paragraph "Version: $state.version"}
}


def installCheck() {
    state.appInstalled = app.getInstallationState()
    if(state.appInstalled != 'COMPLETE'){
        section{paragraph "Please hit 'Done' to install '${app.label}' parent app "}
    } else {
        log.info "Parent Installed OK"
    }
}


def setVersion() {
		state.version = "0.1"
    state.internalName = "SmartBathroomFanControllers"
    state.externalName = "Smart Bathroom Fan Controllers"
}
