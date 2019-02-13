/**
 *  Dimmer Sync
 *
 *  Copyright 2018 Brian Keifer
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
  name: "Dimmer Sync",
  namespace: "bkeifer",
  author: "Brian Keifer",
  description: "Sync a virtual dimmer with a physical dimmer",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)

preferences {
  section("Dimmers:") {
    input "physDim", "capability.switchLevel", title: "Physical Dimmer", multiple: false, required: true
    input "virtDim", "capability.switchLevel", title: "Virtual Dimmer", multiple: false, required: true
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  unsubscribe()
  initialize()
}

def initialize() {
  subscribe(physDim, "switch.on", physOnHandler)
  subscribe(physDim, "switch.off", physOffHandler)
  subscribe(physDim, "level", physLevelHandler)
  subscribe(physDim, "switch", physLevelHandler)

  subscribe(virtDim, "switch.on", virtOnHandler)
  subscribe(virtDim, "switch.off", virtOffHandler)
  subscribe(virtDim, "level", virtLevelHandler)
  subscribe(virtDim, "switch", virtLevelHandler)
}

def physLevelHandler(evt) {
  log.debug "physLevelHandler Event: ${evt}"
  if ((evt.value == "on") || (evt.value == "off"))
    return
  def level = evt.value.toFloat()
  level = level.toInteger()
  virtDim?.setLevel(level)
}

def virtLevelHandler(evt) {
  log.debug "virtLevelHandler Event: ${evt}"
  if ((evt.value == "on") || (evt.value == "off"))
    return
  def level = evt.value.toFloat()
  level = level.toInteger()
  physDim?.setLevel(level)
}

def physOnHandler(evt) {
  log.debug "physOnHandler Event: ${evt.value}"
  virtDim?.on()
}

def virtOnHandler(evt) {
  log.debug "virtOnHandler Event: ${evt.value}"
  physDim?.on()
}

def physOffHandler(evt) {
  log.debug "physOffHandler Event: ${evt.value}"
  virtDim?.off()
}

def virtOffHandler(evt) {
  log.debug "virtOffHandler Event: ${evt.value}"
  physDim?.off()
}
