/**
 * Dark Sky Virtual Weather Station
 *
 *  Copyright 2019 Brian Keifer
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
 **/


 metadata {
    definition (name: "Dark Sky Virtual Weather Station", namespace: "btk", author: "Brian Keifer") {
        capability "Sensor"
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "UltravioletIndex"

        command "poll"

        attribute "temperature", "number"
        attribute "humidity", "number"
        attribute "forecastHigh", "number"
        attribute "forecastLow", "number"
        attribute "ultravioletIndex", "number"
    }
}

preferences {
    input "apiKey", "text", required: true, title: "Dark Sky API Key"
    input "lat", "text", title: "Latitude", required: true
    input "lon", "text", title: "Longitude", required: true
    input "debugLogging", "bool", required: false, title: "Enable Debug Logging?"
}


def updated() {
    debug("CALL: updated()")
    unschedule()
    runEvery5Minutes(poll)
}


def poll() {
    debug("CALL: poll()")

    def params = [
        uri: "https://api.darksky.net/forecast/${apiKey}/${lat},${lon}"
    ]
    debug(params)
    try {
        httpGet(params) { res ->
            res.headers.each {
                debug("Response: ${it.name} : ${it.value}")
            }

            sendEvent(name: "humidity", value: res.data.currently.humidity * 100.0, unit: "%", isStateChange: true)
            sendEvent(name: "temperature", value: res.data.currently.temperature, unit: "F", isStateChange: true)
            sendEvent(name: "dewPoint", value: res.data.currently.dewPoint, isStateChange: true)
            sendEvent(name: "pressure", value: res.data.currently.pressure, isStateChange: true)
            sendEvent(name: "windSpeed", value: res.data.currently.windSpeed, unit: "MPH", isStateChange: true)
            sendEvent(name: "windGust", value: res.data.currently.windGust, unit: "MPH", isStateChange: true)
            sendEvent(name: "windBearing", value: res.data.currently.windBearing, unit: "Degrees", isStateChange: true)
            sendEvent(name: "apparentTemperature", value: res.data.currently.apparentTemperature, unit: "F", isStateChange: true)
            sendEvent(name: "ultravioletIndex", value: res.data.currently.uvIndex, isStateChange: true)
            
            getForecastHighLow(res.data.hourly.data)

            log.info("Successful run!")
        }
    } catch (e) {
        log.error "Failed: ${e}"
    }
}


def getForecastHighLow(data) {
    highTemp = null
    lowTemp = null
    highWind = null
    highGust = null

    count = 0
    while (count < 24) {
        hour = data[count]
        time = new Date( (long)hour.time * 1000)
        temp = hour.temperature
        windSpeed = hour.windSpeed
        windGust = hour.windGust

        if (highTemp == null || temp > highTemp) {
            highTemp = temp
        }
        if (lowTemp == null || temp < lowTemp) {
            lowTemp = temp
        }
        if (highWind == null || windSpeed > highWind) {
            highWind = windSpeed
        }
        if (highGust == null || windGust > highGust) {
            highGust = windGust
        }
        debug("Loop ${count}")
        count++
    }
    sendEvent(name: "forecastHighTemperature", value: highTemp, unit: "F", isStateChange: true)
    sendEvent(name: "forecastLowTemperature", value: lowTemp, unit: "F", isStateChange: true)
    sendEvent(name: "forecastHighWindSpeed", value: highWind, unit: "MPH", isStateChange: true)
    sendEvent(name: "forecastHighWindGust", value: highGust, unit: "MPH", isStateChange: true)
}


def debug(msg) {
    if (debugLogging) {
        log.debug(msg)
    }
}