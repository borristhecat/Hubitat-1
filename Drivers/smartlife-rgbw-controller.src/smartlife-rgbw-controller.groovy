/**
 *  Copyright 2020 Eric Maycock
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
 *  SmartLife RGBW Controller
 *
 *  Author: Eric Maycock (erocm123)
 *  Date: 2020-07-28
 */

import groovy.json.JsonSlurper

private getCOLOR_TEMP_MIN() { 2700 }
private getCOLOR_TEMP_MAX() { 6500 }
private getWARM_WHITE() { "warmWhite" }
private getCOLD_WHITE() { "coldWhite" }
private getWHITE_NAMES() { [WARM_WHITE, COLD_WHITE] }
private getCOLOR_TEMP_DIFF() { COLOR_TEMP_MAX - COLOR_TEMP_MIN }

metadata {
    definition (name: "SmartLife RGBW Controller", namespace: "erocm123", author: "Eric Maycock") {
        capability "Switch Level"
        capability "Actuator"
        capability "Color Control"
        capability "Color Temperature"
        capability "Switch"
        capability "Refresh"
        capability "Sensor"
        capability "Configuration"
        capability "Health Check"
		
		attribute   "needUpdate", "string"
        attribute   "uptime", "string"
        attribute   "ip", "string"
		attribute   "firmware", "string"
        
        command "reset"
        command "setProgram"
        command "setWhiteLevel"
        
        command "redOn"
        command "redOff"
        command "greenOn"
        command "greenOff"
        command "blueOn"
        command "blueOff"
        command "white1On"
        command "white1Off"
        command "white2On"
        command "white2Off"
        
        command "setRedLevel", ["number"]
        command "setGreenLevel", ["number"]
        command "setBlueLevel", ["number"]
        command "setWhite1Level", ["number"]
        command "setWhite2Level", ["number"]
		
		// Flash green for 2 seconds, then off for 2 seconds. Repeat for 10 minutes. Call off to stop.
        command "flashGreen"
        
        // Flash red for 2 seconds, then off for 2 seconds. Repeat for 10 minutes. Call off to stop.
        command "flashRed"
        
        // Flash blue for 2 seconds, then off for 2 seconds. Repeat for 10 minutes. Call off to stop.
        command "flashBlue"
    }

    simulator {
    }
    
    preferences {
        input description: "Once you change values on this page, the corner of the \"configuration\" icon will change orange until all configuration parameters are updated.", title: "Settings", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        //input "childDevices", "enum", title: "Child Devices\n\nSelect which child devices you want enabled", description: "Tap to set", required: false, options:[["1": "Program 1"], ["2": "Program 2"], ["3": "Program 3"], ["4": "Program 4"], ["5": "Program 5"], ["6": "Program 6"], ["7": "Red Channel"], ["8": "Green Channel"], ["9": "Blue Channel"], ["10": "White1 Channel"], ["11": "White2 Channel"]], defaultValue: "0", multiple: true
        input "p1Child", "bool", title: "Program 1 Child Device", description: "", required: false, defaultValue: false
        input "p2Child", "bool", title: "Program 2 Child Device", description: "", required: false, defaultValue: false
        input "p3Child", "bool", title: "Program 3 Child Device", description: "", required: false, defaultValue: false
        input "p4Child", "bool", title: "Program 4 Child Device", description: "", required: false, defaultValue: false
        input "p5Child", "bool", title: "Program 5 Child Device", description: "", required: false, defaultValue: false
        input "p6Child", "bool", title: "Program 6 Child Device", description: "", required: false, defaultValue: false
        input "rChild", "bool", title: "Red Channel Child Device", description: "", required: false, defaultValue: false
        input "gChild", "bool", title: "Green Channel Child Device", description: "", required: false, defaultValue: false
        input "bChild", "bool", title: "Blue Channel Child Device", description: "", required: false, defaultValue: false
        input "w1Child", "bool", title: "White1 Channel Child Device", description: "", required: false, defaultValue: false
        input "w2Child", "bool", title: "White2 Channel Child Device", description: "", required: false, defaultValue: false
        generate_preferences(configuration_model())
    }

    tiles (scale: 2){      
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
            tileAttribute ("device.color", key: "COLOR_CONTROL") {
                attributeState "color", action:"setColor"
            }
        }

        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
       standardTile("configure", "device.needUpdate", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "NO" , label:'', action:"configuration.configure", icon:"http://cdn.device-icons.smartthings.com/secondary/configure@2x.png"
            state "YES", label:'', action:"configuration.configure", icon:"https://github.com/erocm123/SmartThingsPublic/raw/master/devicetypes/erocm123/qubino-flush-1d-relay.src/configure@2x.png"
        }
        
        standardTile("red", "device.red", height: 1, width: 1, inactiveLabel: false, decoration: "flat", canChangeIcon: false) {
            state "off", label:"R", action:"redOn", icon:"st.illuminance.illuminance.dark", backgroundColor:"#cccccc"
            state "on", label:"R", action:"redOff", icon:"st.illuminance.illuminance.bright", backgroundColor:"#FF0000"
        }
        controlTile("redSliderControl", "device.redLevel", "slider", height: 1, width: 4, inactiveLabel: false) {
            state "redLevel", action:"setRedLevel"
        }
        valueTile("redValueTile", "device.redLevel", height: 1, width: 1) {
            state "redLevel", label:'${currentValue}%'
        }     
        
        standardTile("green", "device.green", height: 1, width: 1, inactiveLabel: false, decoration: "flat", canChangeIcon: false) {
            state "off", label:"G", action:"greenOn", icon:"st.illuminance.illuminance.dark", backgroundColor:"#cccccc"
            state "on", label:"G", action:"greenOff", icon:"st.illuminance.illuminance.bright", backgroundColor:"#00FF00"
        }
        controlTile("greenSliderControl", "device.greenLevel", "slider", height: 1, width: 4, inactiveLabel: false) {
            state "greenLevel", action:"setGreenLevel"
        }
        valueTile("greenValueTile", "device.greenLevel", height: 1, width: 1) {
            state "greenLevel", label:'${currentValue}%'
        }    
        
        standardTile("blue", "device.blue", height: 1, width:1, inactiveLabel: false, decoration: "flat", canChangeIcon: false) {
            state "off", label:"B", action:"blueOn", icon:"st.illuminance.illuminance.dark", backgroundColor:"#cccccc"
            state "on", label:"B", action:"blueOff", icon:"st.illuminance.illuminance.bright", backgroundColor:"#0000FF"
        }
        controlTile("blueSliderControl", "device.blueLevel", "slider", height: 1, width: 4, inactiveLabel: false) {
            state "blueLevel", action:"setBlueLevel"
        }
        valueTile("blueValueTile", "device.blueLevel", height: 1, width: 1) {
            state "blueLevel", label:'${currentValue}%'
        }  
        
        standardTile("white1", "device.white1", height: 1, width: 1, inactiveLabel: false, decoration: "flat", canChangeIcon: false) {
            state "off", label:"W1", action:"white1On", icon:"st.illuminance.illuminance.dark", backgroundColor:"#cccccc"
            state "on", label:"W1", action:"white1Off", icon:"st.illuminance.illuminance.bright", backgroundColor:"#FFFFFF"
        }
        controlTile("white1SliderControl", "device.white1Level", "slider", height: 1, width: 4, inactiveLabel: false) {
            state "white1Level", action:"setWhite1Level"
        }
        valueTile("white1ValueTile", "device.white1Level", height: 1, width: 1) {
            state "white1Level", label:'${currentValue}%'
        } 
        standardTile("white2", "device.white2", height: 1, width: 1, inactiveLabel: false, decoration: "flat", canChangeIcon: false) {
            state "off", label:"W2", action:"white2On", icon:"st.illuminance.illuminance.dark", backgroundColor:"#cccccc"
            state "on", label:"W2", action:"white2Off", icon:"st.illuminance.illuminance.bright", backgroundColor:"#FFFFFF"
        }
        controlTile("white2SliderControl", "device.white2Level", "slider", height: 1, width: 4, inactiveLabel: false) {
            state "white2Level", action:"setWhite2Level"
        }
        valueTile("white2ValueTile", "device.white2Level", height: 1, width: 1) {
            state "white2Level", label:'${currentValue}%'
        } 
        valueTile("ip", "ip", width: 2, height: 1) {
            state "ip", label:'IP Address\r\n${currentValue}'
        }
        valueTile("firmware", "firmware", width: 2, height: 1) {
            state "firmware", label:'Firmware ${currentValue}'
        }
        
    }

    main(["switch"])
    details(["switch", "levelSliderControl",
             "red", "redSliderControl", "redValueTile", 
             "green", "greenSliderControl", "greenValueTile",
             "blue", "blueSliderControl", "blueValueTile",
             "white1", "white1SliderControl", "white1ValueTile",
             "white2", "white2SliderControl", "white2ValueTile",
             "switch1", "switch2", "switch3",
             "switch4", "switch5", "switch6",
             "refresh", "configure", "ip", "firmware" ])
}


def installed() {
    logging("installed()",1)
    createChildDevices()
    configure()
}

def configure() {
    logging("configure()", 1)
    def cmds = []
    cmds = update_needed_settings()
    if (cmds != []) cmds
}

def updated()
{
    logging("updated()", 1)
    createChildDevices()
    def cmds = [] 
    cmds = update_needed_settings()
    sendEvent(name: "checkInterval", value: 12 * 60 * 2, data: [protocol: "lan", hubHardwareId: device.hub.hardwareID], displayed: false)
    sendEvent(name:"needUpdate", value: device.currentValue("needUpdate"), displayed:false, isStateChange: true)
    if (cmds != []) cmds
}

private void createChildDevices() {
    if (p1Child && !childExists("ep1")) {
        addChildDevice("Switch Child Device", "${device.deviceNetworkId}-ep1", [completedSetup: true, label: "${device.label} - Program 1",
            isComponent: false, componentName: "ep1", componentLabel: "Program 1"
        ])
    } else if (!p1Child && childExists("ep1")) {
        logging("Trying to delete child device ep1. If this fails it is likely that there is an App using the child device in question.",1)
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep1")}
            if(childDevice) deleteChildDevice(childDevice.deviceNetworkId)
    }
    if (p2Child && !childExists("ep2")) {
        addChildDevice("Switch Child Device", "${device.deviceNetworkId}-ep2", [completedSetup: true, label: "${device.label} - Program 2",
            isComponent: false, componentName: "ep2", componentLabel: "Program 2"
        ])
    } else if (!p2Child && childExists("ep2")) {
        logging("Trying to delete child device ep2. If this fails it is likely that there is an App using the child device in question.",1)
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep2")}
            if(childDevice) deleteChildDevice(childDevice.deviceNetworkId)
    }
    if (p3Child && !childExists("ep3")) {
        addChildDevice("Switch Child Device", "${device.deviceNetworkId}-ep3", [completedSetup: true, label: "${device.label} - Program 3",
            isComponent: false, componentName: "ep3", componentLabel: "Program 3"
        ])
    } else if (!p3Child && childExists("ep3")) {
        logging("Trying to delete child device ep3. If this fails it is likely that there is an App using the child device in question.",1)
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep3")}
            if(childDevice) deleteChildDevice(childDevice.deviceNetworkId)
    }
    if (p4Child && !childExists("ep4")) {
        addChildDevice("Switch Child Device", "${device.deviceNetworkId}-ep4", [completedSetup: true, label: "${device.label} - Program 4",
            isComponent: false, componentName: "ep4", componentLabel: "Program 4"
        ])
    } else if (!p4Child && childExists("ep4")) {
        logging("Trying to delete child device ep4. If this fails it is likely that there is an App using the child device in question.",1)
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep4")}
            if(childDevice) deleteChildDevice(childDevice.deviceNetworkId)
    }
    if (p5Child && !childExists("ep5")) {
        addChildDevice("Switch Child Device", "${device.deviceNetworkId}-ep5", [completedSetup: true, label: "${device.label} - Program 5",
            isComponent: false, componentName: "ep5", componentLabel: "Program 5"
        ])
    } else if (!p5Child && childExists("ep5")) {
        logging("Trying to delete child device ep5. If this fails it is likely that there is an App using the child device in question.",1)
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep5")}
            if(childDevice) deleteChildDevice(childDevice.deviceNetworkId)
    }
    if (p6Child && !childExists("ep6")) {
        addChildDevice("Switch Child Device", "${device.deviceNetworkId}-ep6", [completedSetup: true, label: "${device.label} - Program 6",
            isComponent: false, componentName: "ep6", componentLabel: "Program 6"
        ])
    } else if (!p6Child && childExists("ep6")) {
        logging("Trying to delete child device ep6. If this fails it is likely that there is an App using the child device in question.",1)
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep6")}
            if(childDevice) deleteChildDevice(childDevice.deviceNetworkId)
    }
    if (rChild && !childExists("ep7")) {
        addChildDevice("Switch Level Child Device", "${device.deviceNetworkId}-ep7", [completedSetup: true, label: "${device.label} - Red Channel",
            isComponent: false, componentName: "ep7", componentLabel: "Red Channel"
        ])
    } else if (!rChild && childExists("ep7")) {
        logging("Trying to delete child device ep7. If this fails it is likely that there is an App using the child device in question.",1)
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep7")}
            if(childDevice) deleteChildDevice(childDevice.deviceNetworkId)
    }
    if (gChild && !childExists("ep8")) {
        addChildDevice("Switch Level Child Device", "${device.deviceNetworkId}-ep8", [completedSetup: true, label: "${device.label} - Green Channel",
            isComponent: false, componentName: "ep8", componentLabel: "Green Channel"
        ])
    } else if (!gChild && childExists("ep8")) {
        logging("Trying to delete child device ep8. If this fails it is likely that there is an App using the child device in question.",1)
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep8")}
            if(childDevice) deleteChildDevice(childDevice.deviceNetworkId)
    }
    if (bChild && !childExists("ep9")) {
        addChildDevice("Switch Level Child Device", "${device.deviceNetworkId}-ep9", [completedSetup: true, label: "${device.label} - Blue Channel",
            isComponent: false, componentName: "ep9", componentLabel: "Blue Channel"
        ])
    } else if (!bChild && childExists("ep9")) {
        logging("Trying to delete child device ep9. If this fails it is likely that there is an App using the child device in question.",1)
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep9")}
            if(childDevice) deleteChildDevice(childDevice.deviceNetworkId)
    }
    if (w1Child && !childExists("ep10")) {
        addChildDevice("Switch Level Child Device", "${device.deviceNetworkId}-ep10", [completedSetup: true, label: "${device.label} - White1 Channel",
            isComponent: false, componentName: "ep10", componentLabel: "White1 Channel"
        ])
    } else if (!w1Child && childExists("ep10")) {
        logging("Trying to delete child device ep10. If this fails it is likely that there is an App using the child device in question.",1)
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep10")}
            if(childDevice) deleteChildDevice(childDevice.deviceNetworkId)
    }
    if (w2Child && !childExists("ep11")) {
        addChildDevice("Switch Level Child Device", "${device.deviceNetworkId}-ep11", [completedSetup: true, label: "${device.label} - White2 Channel",
            isComponent: false, componentName: "ep11", componentLabel: "White2 Channel"
        ])
    } else if (!w2Child && childExists("ep11")) {
        logging("Trying to delete child device ep11. If this fails it is likely that there is an App using the child device in question.",1)
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep11")}
            if(childDevice) deleteChildDevice(childDevice.deviceNetworkId)
    }
    childDevices.each {
        if (it.label == "${state.oldLabel} (CH${channelNumber(it.deviceNetworkId)})") {
            def newLabel = "${device.displayName} (CH${channelNumber(it.deviceNetworkId)})"
            it.setLabel(newLabel)
        }
    }
    state.oldLabel = device.label
}
    
def childExists(ep) {
    def children = childDevices
    def childDevice = children.find{it.deviceNetworkId.endsWith(ep)}
    if (childDevice) 
        return true
    else
        return false
}

private def logging(message, level) {
    if (logLevel != "0"){
    switch (logLevel) {
       case "1":
          if (level > 1)
             log.debug "$message"
       break
       case "99":
          log.debug "$message"
       break
    }
    }
}

def getDefault(){
    if(settings.dcolor == "Previous") {
        return "Previous"
    } else if(settings.dcolor == "Random") {
        return "${transition == "false"? "d~" : "f~"}${getHexColor(settings.dcolor)}"
    } else if(settings.dcolor == "Custom" && settings.custom.length() == 10) {
    	//Here we have a 10 char value, i.e. exact colour choice
        return "${settings.custom}"
    } else if(settings.dcolor == "Custom" && settings.dcolor.length() != 10) {
        return "${transition == "false"? "d~" : "f~"}${settings.custom}"
    } else if(settings.dcolor == "Soft White" || settings.dcolor == "Warm White") {
        if (settings.level == null || settings.level == "0") {
            return "${transition == "false"? "x~" : "w~"}${getDimmedColor(getHexColor(settings.dcolor), "100")}"
        } else {
            return "${transition == "false"? "x~" : "w~"}${getDimmedColor(getHexColor(settings.dcolor), settings.level)}"
        }
    } else if(settings.dcolor == "W1") {
        if (settings.level == null || settings.level == "0") {
            return "${transition == "false"? "x~" : "w~"}${getDimmedColor(getHexColor(settings.dcolor), "100")}"
        } else {
            return "${transition == "false"? "x~" : "w~"}${getDimmedColor(getHexColor(settings.dcolor), settings.level)}"
        }
    } else if(settings.dcolor == "W2") {
        if (settings.level == null || settings.level == "0") {
            return "${transition == "false"? "z~" : "y~"}${getDimmedColor(getHexColor(settings.dcolor), "100")}"
        } else {
            return "${transition == "false"? "z~" : "y~"}${getDimmedColor(getHexColor(settings.dcolor), settings.level)}"
        }
    } else {
        if (settings.level == null || settings.dcolor == null){
           return "Previous"
        } else if (settings.level == null || settings.level == "0") {
            return "${transition == "false"? "d~" : "f~"}${getDimmedColor(getHexColor(settings.dcolor), "100")}"
        } else {
            return "${transition == "false"? "d~" : "f~"}${getDimmedColor(getHexColor(settings.dcolor), settings.level)}"
        }
    }
}

def parse(description) {
    def map = [:]
    def events = []
    def cmds = []
    
    if(description == "updated") return
    def descMap = parseDescriptionAsMap(description)

    if (descMap["mac"] != null && (!state.mac || state.mac != descMap["mac"])) {
        logging("Mac address of device found ${descMap["mac"]}",1)
        state.mac = descMap["mac"]
    }
    
    if (state.mac != null && state.dni != state.mac) state.dni = setDeviceNetworkId(state.mac)
    
    def body = new String(descMap["body"].decodeBase64())
    if(body.startsWith("{") || body.startsWith("[")) {
    
    def slurper = new JsonSlurper()
    def result = slurper.parseText(body)
    
    if (result.containsKey("type")) {
        if (result.type == "configuration")
            events << update_current_properties(result)
    }
    if (result.containsKey("power")) {
        events << createEvent(name: "switch", value: result.power)
        toggleTiles("all")
    }
    if (result.containsKey("rgb")) {
       events << createEvent(name:"color", value:"#$result.rgb")
       def rgb = hexToRgb("#$result.rgb") 
       def hsv = rgbwToHSV(rgb)
       events << createEvent(name:"hue", value:hsv.hue)
       events << createEvent(name:"saturation", value:hsv.saturation)

       // only store the previous value if the response did not come from a power-off command
       if (result.power != "off")
         state.previousRGB = result.rgb
    }
    if (result.containsKey("r")) {
       events << createEvent(name:"redLevel", value: Integer.parseInt(result.r,16)/255 * 100 as Integer, displayed: false)
       if ((Integer.parseInt(result.r,16)/255 * 100 as Integer) > 0 ) {
          events << createEvent(name:"red", value: "on", displayed: false)
       } else {
          events << createEvent(name:"red", value: "off", displayed: false)
       }
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep7")}
        if (childDevice) {
            childDevice.sendEvent(name: "switch", value: (Integer.parseInt(result.r,16)/255 * 100 as Integer) > 0 ? "on" : "off")
            childDevice.sendEvent(name: "level", value: Integer.parseInt(result.r,16)/255 * 100 as Integer)            
        }
    }
    if (result.containsKey("g")) {
       events << createEvent(name:"greenLevel", value: Integer.parseInt(result.g,16)/255 * 100 as Integer, displayed: false)
       if ((Integer.parseInt(result.g,16)/255 * 100 as Integer) > 0 ) {
          events << createEvent(name:"green", value: "on", displayed: false)
       } else {
          events << createEvent(name:"green", value: "off", displayed: false)
       }
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep8")}
        if (childDevice) {
            childDevice.sendEvent(name: "switch", value: (Integer.parseInt(result.g,16)/255 * 100 as Integer) > 0 ? "on" : "off")
            childDevice.sendEvent(name: "level", value: Integer.parseInt(result.g,16)/255 * 100 as Integer)            
        }
    }
    if (result.containsKey("b")) {
       events << createEvent(name:"blueLevel", value: Integer.parseInt(result.b,16)/255 * 100 as Integer, displayed: false)
       if ((Integer.parseInt(result.b,16)/255 * 100 as Integer) > 0 ) {
          events << createEvent(name:"blue", value: "on", displayed: false)
       } else {
          events << createEvent(name:"blue", value: "off", displayed: false)
       }
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep9")}
        if (childDevice) {
            childDevice.sendEvent(name: "switch", value: (Integer.parseInt(result.b,16)/255 * 100 as Integer) > 0 ? "on" : "off")
            childDevice.sendEvent(name: "level", value: Integer.parseInt(result.b,16)/255 * 100 as Integer)            
        }
    }
    if (result.containsKey("w1")) {
       events << createEvent(name:"white1Level", value: Integer.parseInt(result.w1,16)/255 * 100 as Integer, displayed: false)
       if ((Integer.parseInt(result.w1,16)/255 * 100 as Integer) > 0 ) {
          events << createEvent(name:"white1", value: "on", displayed: false)
       } else {
          events << createEvent(name:"white1", value: "off", displayed: false)
       }
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep10")}
        if (childDevice) {
            childDevice.sendEvent(name: "switch", value: (Integer.parseInt(result.w1,16)/255 * 100 as Integer) > 0 ? "on" : "off")
            childDevice.sendEvent(name: "level", value: Integer.parseInt(result.w1,16)/255 * 100 as Integer)            
        }

       // only store the previous value if the response did not come from a power-off command
       if (result.power != "off")
          state.previousW1 = result.w1
    }
    if (result.containsKey("w2")) {
       events << createEvent(name:"white2Level", value: Integer.parseInt(result.w2,16)/255 * 100 as Integer, displayed: false)
       if ((Integer.parseInt(result.w2,16)/255 * 100 as Integer) > 0 ) {
          events << createEvent(name:"white2", value: "on", displayed: false)
       } else {
          events << createEvent(name:"white2", value: "off", displayed: false)
       }
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep11")}
        if (childDevice) {
            childDevice.sendEvent(name: "switch", value: (Integer.parseInt(result.w2,16)/255 * 100 as Integer) > 0 ? "on" : "off")
            childDevice.sendEvent(name: "level", value: Integer.parseInt(result.w2,16)/255 * 100 as Integer)            
        }

       // only store the previous value if the response did not come from a power-off command
       if (result.power != "off")
          state.previousW2 = result.w2
    }
    if (result.containsKey("version")) {
       events << createEvent(name:"firmware", value: result.version + "\r\n" + result.date, displayed: false)
    }
		
    if (result.containsKey("uptime")) {
        events << createEvent(name: "uptime", value: result.uptime, displayed: false)
    }

    if (result.containsKey("success")) {
       if (result.success == "true") state.configSuccess = "true" else state.configSuccess = "false" 
    }
    if (result.containsKey("program")) {
        def childDevice = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-ep$result.program"}
            if (childDevice) {         
                childDevice.sendEvent(name: "switch", value: result.running == "true"? "on" : "off")
        }
        
        toggleTiles(result.program)
        
    }
    } else {    
        //logging("Response is not JSON: $body"    ,1)
    }
    
    if (!device.currentValue("ip") || (device.currentValue("ip") != getDataValue("ip"))) events << createEvent(name: 'ip', value: getDataValue("ip"))

    return events
}

private toggleTiles(value) {
   for (int i = 1; i <= 6; i++){
       if ("${i}" != value){
           def childDevice = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-ep$i"}
           if (childDevice) {         
                childDevice.sendEvent(name: "switch", value: "off")
           }
       }
   }
}

private getScaledColor(color) {
   def rgb = color.findAll(/[0-9a-fA-F]{2}/).collect { Integer.parseInt(it, 16) }
   def maxNumber = 1
   for (int i = 0; i < 3; i++){
     if (rgb[i] > maxNumber) {
        maxNumber = rgb[i]
     }
   }
   def scale = 255/maxNumber
   for (int i = 0; i < 3; i++){
     rgb[i] = rgb[i] * scale
   }
   def myred = rgb[0]
   def mygreen = rgb[1]
   def myblue = rgb[2]
   return rgbToHex([r:myred, g:mygreen, b:myblue])
}

def on() {
    logging("on()",1)
    getAction("/on?transition=$dtransition")
}

def off() {
    logging("off()",1)
    getAction("/off?transition=$dtransition")
}

def setLevel(level) {
    setLevel(level, 1)
}

def setLevel(level, duration) {
    logging("setLevel() level = ${level}",1)
    if(level > 100) level = 100
    if (level == 0) { off() }
    else if (device.latestValue("switch") == "off") { on() }
    sendEvent(name: "level", value: level)
    sendEvent(name: "setLevel", value: level, displayed: false)
    setColor(aLevel: level)
}
def setSaturation(percent) {
    logging("setSaturation($percent)",1)
    setColor(saturation: percent)
}
def setHue(value) {
    logging("setHue($value)",1)
    setColor(hue: value)
}
def getWhite(value) {
    logging("getWhite($value)",1)
    def level = Math.min(value as Integer, 99)    
    level = 255 * level/99 as Integer
    logging("level: ${level}",1)
    return hex(level)
}

def setColorTemperature(temp) {
    logging("setColorTemperature being called with ${temp}",1)
    def cmds = []
    if (temp < COLOR_TEMP_MIN) temp = COLOR_TEMP_MIN
	if (temp > COLOR_TEMP_MAX) temp = COLOR_TEMP_MAX
    def warmValue = ((COLOR_TEMP_MAX - temp) / COLOR_TEMP_DIFF * 100) as Integer
    def coldValue = 100 - warmValue
    if (hasW2 == false) {
        coldValue = hex((100 - warmValue)/100 * 255) 
        def uri = "/rgb?value=$coldValue$coldValue$coldValue"
        cmds.push(getAction("$uri&channels=false&transition=$dtransition"))  
    } else {
        cmds.push(getAction("/w2?value=$coldValue&channels=$channels&transition=$dtransition"))
    }
    cmds.push(getAction("/w1?value=${hex(warmValue)}&channels=false&transition=$dtransition"))
    sendEvent(name: "colorTemperature", value: temp)
    return cmds
}

def setColor(value) {
    logging("setColor being called with ${value}",1)
    def uri
    def validValue = true
    def hex
    
    if ((value.saturation != null) && (value.hue != null)) {
        def hue = (value.hue != null) ? value.hue : 13
        def saturation = (value.saturation != null) ? value.saturation : 13
        def rgb = huesatToRGB(hue as Integer, saturation as Integer)
        hex = rgbToHex([r:rgb[0], g:rgb[1], b:rgb[2]])
    } 
    
    if (value.hue == 5 && value.saturation == 4) {
       logging("setting color Soft White - Default",1)
       def whiteLevel = getWhite(value.level)
       uri = "/w1?value=${whiteLevel}"
       state.previousColor = "${whiteLevel}"
    }
    /* Letting White - Concentrate adjust RGB values
    else if (value.hue == 63 && value.saturation == 28) {
       logging("setting color White - Concentrate",1)
       def whiteLevel = getWhite(value.level)
       uri = "/w1?value=${whiteLevel}"
       state.previousColor = "${whiteLevel}"
    } */
    else if (value.hue == 63 && value.saturation == 43) {
       logging("setting color Daylight - Energize",1)
       def whiteLevel = getWhite(value.level)
       uri = "/w2?value=${whiteLevel}"
       state.previousColor = "${whiteLevel}"
    }
    else if (value.hue == 79 && value.saturation == 7) {
       logging("setting color Warm White - Relax",1)
       def whiteLevel = getWhite(value.level)
       uri = "/w1?value=${whiteLevel}"
       state.previousColor = "${whiteLevel}"
    } 
    else if (value.colorTemperature) {
       logging("setting color with color temperature",1)
       def whiteLevel = getWhite(value.level)
       uri = "/w1?value=${whiteLevel}"
       state.previousColor = "${whiteLevel}"
    }
    else if (hex) {
       logging("setting color with hex",1)
       if (!hex ==~ /^\#([A-Fa-f0-9]){6}$/) {
           logging("$hex is not valid",1)
           validValue = false
       } else {
           def rgb = hex.findAll(/[0-9a-fA-F]{2}/).collect { Integer.parseInt(it, 16) }
           def myred = rgb[0] < 40 ? 0 : rgb[0]
           def mygreen = rgb[1] < 40 ? 0 : rgb[1]
           def myblue = rgb[2] < 40 ? 0 : rgb[2]
           def dimmedColor = getDimmedColor(rgbToHex([r:myred, g:mygreen, b:myblue]))
           uri = "/rgb?value=${dimmedColor}"
       }
    }
    else if (value.hex) {
       logging("setting color with hex",1)
       if (!value.hex ==~ /^\#([A-Fa-f0-9]){6}$/) {
           logging("$value.hex is not valid",1)
           validValue = false
       } else {
           def rgb = value.hex.findAll(/[0-9a-fA-F]{2}/).collect { Integer.parseInt(it, 16) }
           def myred = rgb[0] < 40 ? 0 : rgb[0]
           def mygreen = rgb[1] < 40 ? 0 : rgb[1]
           def myblue = rgb[2] < 40 ? 0 : rgb[2]
           def dimmedColor = getDimmedColor(rgbToHex([r:myred, g:mygreen, b:myblue]))
           uri = "/rgb?value=${dimmedColor}"
       }
    }
    else if (value.white) {
       uri = "/w1?value=${value.white}"
    }
    else if (value.aLevel) {
        def actions = []
        if (channels == "true") {
           def skipColor = false
           // Handle white channel dimmers if they're on or were not previously off (excluding power-off command)
           if (device.currentValue("white1") == "on" || state.previousW1 != "00") {
              actions.push(setWhite1Level(value.aLevel))
              skipColor = true
           }
           if (device.currentValue("white2") == "on" || state.previousW2 != "00") {
              actions.push(setWhite2Level(value.aLevel))
              skipColor = true
           }
        if (skipColor == false) {
        logging(state.previousRGB,1)
           // if the device is currently on, scale the current RGB values; otherwise scale the previous setting
           uri = "/rgb?value=${getDimmedColor(device.latestValue("switch") == "on" ? device.currentValue("color").substring(1) : state.previousRGB)}"
           actions.push(getAction("$uri&channels=$channels&transition=$dtransition"))
        }
        } else {
           // Handle white channel dimmers if they're on or were not previously off (excluding power-off command)
           if (device.currentValue("white1") == "on" || state.previousW1 != "00")
              actions.push(setWhite1Level(value.aLevel))
           if (device.currentValue("white2") == "on" || state.previousW2 != "00")
              actions.push(setWhite2Level(value.aLevel))
        
           // if the device is currently on, scale the current RGB values; otherwise scale the previous setting
           uri = "/rgb?value=${getDimmedColor(device.latestValue("switch") == "on" ? device.currentValue("color").substring(1) : state.previousRGB)}"
           actions.push(getAction("$uri&channels=$channels&transition=$dtransition"))
        }
        return actions
    }
    else {
       // A valid color was not chosen. Setting to white
       uri = "/w1?value=ff"
    }

    if (uri != null && validValue != false) getAction("$uri&channels=$channels&transition=$dtransition")

}

private getDimmedColor(color, level) {
   if(color.size() > 2){
      def scaledColor = getScaledColor(color)
      def rgb = scaledColor.findAll(/[0-9a-fA-F]{2}/).collect { Integer.parseInt(it, 16) }
    
      def r = hex(rgb[0] * (level.toInteger()/100))
      def g = hex(rgb[1] * (level.toInteger()/100))
      def b = hex(rgb[2] * (level.toInteger()/100))

      return "${r + g + b}"
   }else{
      color = Integer.parseInt(color, 16)
      return hex(color * (level.toInteger()/100))
   }
}

private getDimmedColor(color) {
   if (device.latestValue("level")) {
      getDimmedColor(color, device.latestValue("level"))
   } else {
      return color.replaceAll("#","")
   }
}

def reset() {
    logging("reset()",1)
    setColor(white: "ff")
}

def refresh() {
    logging("refresh()",1)
    getAction("/status")
}

def ping() {
    logging("ping()",1)
    refresh()
}

def setWhiteLevel(value) {
    logging("setwhiteLevel: ${value}",1)
    def level = Math.min(value as Integer, 99)    
    level = 255 * level/99 as Integer
    logging("level: ${level}",1)
    if ( value > 0 ) {
        if (device.latestValue("switch") == "off") { on() }
        sendEvent(name: "white", value: "on")
    } else {
        sendEvent(name: "white", value: "off")
    }
    def whiteLevel = hex(level)
    setColor(white: whiteLevel)
}

def hexToRgb(colorHex) {
    def rrInt = Integer.parseInt(colorHex.substring(1,3),16)
    def ggInt = Integer.parseInt(colorHex.substring(3,5),16)
    def bbInt = Integer.parseInt(colorHex.substring(5,7),16)
    
    def colorData = [:]
    colorData = [r: rrInt, g: ggInt, b: bbInt]
    colorData
}

// huesatToRGB Changed method provided by daved314
def huesatToRGB(float hue, float sat) {
    if (hue <= 100) {
        hue = hue * 3.6
    }
    sat = sat / 100
    float v = 1.0
    float c = v * sat
    float x = c * (1 - Math.abs(((hue/60)%2) - 1))
    float m = v - c
    int mod_h = (int)(hue / 60)
    int cm = Math.round((c+m) * 255)
    int xm = Math.round((x+m) * 255)
    int zm = Math.round((0+m) * 255)
    switch(mod_h) {
        case 0: return [cm, xm, zm]
           case 1: return [xm, cm, zm]
        case 2: return [zm, cm, xm]
        case 3: return [zm, xm, cm]
        case 4: return [xm, zm, cm]
        case 5: return [cm, zm, xm]
    }       
}

private rgbwToHSV(Map colorMap) {
    //logging("rgbwToHSV(): colorMap: ${colorMap}",1)
    if (colorMap.containsKey("r") & colorMap.containsKey("g") & colorMap.containsKey("b")) { 

        float r = colorMap.r / 255f
        float g = colorMap.g / 255f
        float b = colorMap.b / 255f
        float w = (colorMap.white) ? colorMap.white / 255f : 0.0
        float max = [r, g, b].max()
        float min = [r, g, b].min()
        float delta = max - min

        float h,s,v = 0

        if (delta) {
            s = delta / max
            if (r == max) {
                h = ((g - b) / delta) / 6
            } else if (g == max) {
                h = (2 + (b - r) / delta) / 6
            } else {
                h = (4 + (r - g) / delta) / 6
            }
            while (h < 0) h += 1
            while (h >= 1) h -= 1
        }

        v = [max,w].max() 

        return colorMap << [ hue: h * 100, saturation: s * 100, level: Math.round(v * 100) ]
    }
    else {
        log.error "rgbwToHSV(): Cannot obtain color information from colorMap: ${colorMap}"
    }
}

private hex(value, width=2) {
    def s = new BigInteger(Math.round(value).toString()).toString(16)
    while (s.size() < width) {
        s = "0" + s
    }
    s
}
def rgbToHex(rgb) {
    def r = hex(rgb.r)
    def g = hex(rgb.g)
    def b = hex(rgb.b)
    def hexColor = "#${r}${g}${b}"
    
    hexColor
}

def sync(ip, port) {
    def existingIp = getDataValue("ip")
    def existingPort = getDataValue("port")
    if (ip && ip != existingIp) {
        updateDataValue("ip", ip)
        sendEvent(name: 'ip', value: ip)
    }
    if (port && port != existingPort) {
        updateDataValue("port", port)
    }
}

private encodeCredentials(username, password){
    def userpassascii = "${username}:${password}"
    def userpass = "Basic " + userpassascii.bytes.encodeBase64().toString()
    return userpass
}

private getAction(uri){ 
  updateDNI()
  def userpass
  logging(uri,1)
  if(password != null && password != "") 
    userpass = encodeCredentials("admin", password)
    
  def headers = getHeader(userpass)

  def hubAction = new hubitat.device.HubAction(
    method: "GET",
    path: uri,
    headers: headers
  )
  return hubAction    
}

private postAction(uri, data){ 
  updateDNI()
  
  def userpass
  
  if(password != null && password != "") 
    userpass = encodeCredentials("admin", password)
  
  def headers = getHeader(userpass)
  
  def hubAction = new hubitat.device.HubAction(
    method: "POST",
    path: uri,
    headers: headers,
    body: data
  )
  return hubAction    
}

private setDeviceNetworkId(ip, port = null){
    def myDNI
    if (port == null) {
        myDNI = ip
    } else {
          def iphex = convertIPtoHex(ip)
          def porthex = convertPortToHex(port)
        
        myDNI = "$iphex:$porthex"
    }
    logging("Device Network Id set to ${myDNI}",1)
    return myDNI
}

private updateDNI() { 
    if (state.dni != null && state.dni != "" && device.deviceNetworkId != state.dni) {
       device.deviceNetworkId = state.dni
    }
}

private getHostAddress() {
    if(getDeviceDataByName("ip") && getDeviceDataByName("port")){
        return "${getDeviceDataByName("ip")}:${getDeviceDataByName("port")}"
    }else{
        return "${ip}:80"
    }
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}

def parseDescriptionAsMap(description) {
    description.split(",").inject([:]) { map, param ->
        def nameAndValue = param.split(":")
        if (nameAndValue.length == 2) map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
        else map += [(nameAndValue[0].trim()):""]
    }
}

private getHeader(userpass = null){
    def headers = [:]
    headers.put("Host", getHostAddress())
    headers.put("Content-Type", "application/x-www-form-urlencoded")
    if (userpass != null)
       headers.put("Authorization", userpass)
    return headers
}

def childOn(String dni) {
    logging("childOn($dni)",1)
    def uri = ""
    if(state."program${channelNumber(dni)}" != null) {
        uri = "/program?value=${state."program${channelNumber(dni)}"}&number=${channelNumber(dni)}"
    } else {
        //default programs if user hasn't set them up
        switch(channelNumber(dni)){
        case "1":
            uri = "/program?value=g~ff0000~100_g~0000ff~100&repeat=-1&off=true&number=${channelNumber(dni)}"
            break;
        case "2":
            uri = "/program?value=f~ff0000~6000_f~0000ff~6000_f~00ff00~6000_f~ffff00~6000_f~5a00ff~6000_f~ff00ff~6000_f~00ffff~6000&repeat=-1&off=false&number=${channelNumber(dni)}"
            break;
        case "3":
            uri = "/program?value=f~xxxxxx~100-3000&repeat=-1&off=false&number=${channelNumber(dni)}"
            break;
        case "4":
            uri = "/program?value=f~800000~100-3000_f~662400~100-2000_f~330000~100-3000_f~4d1b00~100-2000_f~990000~100-3000_f~1a0900~100-2000&repeat=-1&off=true&number=${channelNumber(dni)}"
            break;
        case "5":
            uri = "/program?value=f~00004d~100-15000_x~b3~100_g~000000~100_x~cc~100&repeat=-1&off=true&number=${channelNumber(dni)}"
            break;
        case "6":
            uri = "/program?value=f~xxxxxx~100-3000&repeat=-1&off=false&number=${channelNumber(dni)}"
            break;
        case "7":
            childSetLevel(dni, 100)
            break;
        case "8":
            childSetLevel(dni, 100)
            break;
        case "9":
            childSetLevel(dni, 100)
            break;
        case "10":
            childSetLevel(dni, 100)
            break;
        case "11":
            childSetLevel(dni, 100)
            break;
        default:
            uri = "/program?value=f~xxxxxx~100-3000&repeat=-1&off=false"
            break;
        }
        
    }
    sendHubCommand(getAction(uri))
}

def childSetLevel(String dni, value) {
    def level = Math.min(value as Integer, 99)  
	def uri = ""
    level = 255 * level/99 as Integer
    logging("level: ${level}",1)
    level = hex(level)
    switch (channelNumber(dni)) {
        case "7":
            uri = "/r?value=$level&channels=$channels&transition=$dtransition"
        break
        case "8":
            uri = "/g?value=$level&channels=$channels&transition=$dtransition"
        break
        case "9":
            uri = "/b?value=$level&channels=$channels&transition=$dtransition"
        break
        case "10":
            uri = "/w1?value=$level&channels=$channels&transition=$dtransition"
        break
        case "11":
            uri = "/w2?value=$level&channels=$channels&transition=$dtransition"
        break
    }
    sendHubCommand(getAction(uri))
}


def childOff(String dni) {
    logging("childOff($dni)",1)
	def uri = ""
     if (channelNumber(dni) in [1,2,3,4,5,6]) {
         uri = "/stop"
         sendHubCommand(getAction(uri))
     } else {
        switch (channelNumber(dni)) {
        case "7":
            childSetLevel(dni, 0)
            break;
        case "8":
            childSetLevel(dni, 0)
            break;
        case "9":
            childSetLevel(dni, 0)
            break;
        case "10":
            childSetLevel(dni, 0)
            break;
        case "11":
            childSetLevel(dni, 0)
            break;
        }
     }
    
}

def childRefresh(String dni) {
    logging("childRefresh($dni)",1)
    //Not needed right now
}

private channelNumber(String dni) {
    dni.split("-ep")[-1] as Integer
}

def setProgram(value, program){
   state."program${program}" = value
}

def hex2int(value){
   return Integer.parseInt(value, 10)
}

def redOn() {
    logging("redOn()",1)
    getAction("/r?value=ff&channels=$channels&transition=$dtransition")
}
def redOff() {
    logging("redOff()",1)
    getAction("/r?value=00&channels=$channels&transition=$dtransition")
}

def setRedLevel(value) {
    logging("setRedLevel: ${value}",1)
    def level = Math.min(value as Integer, 99)    
    level = 255 * level/99 as Integer
    logging("level: ${level}",1)
    level = hex(level)
    getAction("/r?value=$level&channels=$channels&transition=$dtransition")
}
def greenOn() {
    logging("greenOn()",1)
    getAction("/g?value=ff&channels=$channels&transition=$dtransition")
}
def greenOff() {
    logging("greenOff()",1)
    getAction("/g?value=00&channels=$channels&transition=$dtransition")
}

def setGreenLevel(value) {
    logging("setGreenLevel: ${value}",1)
    def level = Math.min(value as Integer, 99)    
    level = 255 * level/99 as Integer
    logging("level: ${level}",1)
    level = hex(level)
    getAction("/g?value=$level&channels=$channels&transition=$dtransition")
}
def blueOn() {
    logging("blueOn()",1)
    getAction("/b?value=ff&channels=$channels&transition=$dtransition")
}
def blueOff() {
    logging("blueOff()",1)
    getAction("/b?value=00&channels=$channels&transition=$dtransition")
}

def setBlueLevel(value) {
    logging("setBlueLevel: ${value}",1)
    def level = Math.min(value as Integer, 99)    
    level = 255 * level/99 as Integer
    logging("level: ${level}",1)
    level = hex(level)
    getAction("/b?value=$level&channels=$channels&transition=$dtransition")
}
def white1On() {
    logging("white1On()",1)
    getAction("/w1?value=ff&channels=$channels&transition=$dtransition")
}
def white1Off() {
    logging("white1Off()",1)
    getAction("/w1?value=00&channels=$channels&transition=$dtransition")
}

def setWhite1Level(value) {
    logging("setwhite1Level: ${value}",1)
    def level = Math.min(value as Integer, 99)    
    level = 255 * level/99 as Integer
    logging("level: ${level}",1)
    def whiteLevel = hex(level)
    getAction("/w1?value=$whiteLevel&channels=$channels&transition=$dtransition")
}
def white2On() {
    logging("white2On()",1)
    getAction("/w2?value=ff&channels=$channels&transition=$dtransition")
}
def white2Off() {
    logging("white2Off()",1)
    getAction("/w2?value=00&channels=$channels&transition=$dtransition")
}

def setWhite2Level(value) {
    logging("setwhite2Level: ${value}",1)
    def level = Math.min(value as Integer, 99)    
    level = 255 * level/99 as Integer
    logging("level: ${level}",1)
    def whiteLevel = hex(level)
    getAction("/w2?value=$whiteLevel&channels=$channels&transition=$dtransition")
}

private getHexColor(value){
def color = ""
  switch(value){
    case "Previous":
    color = "Previous"
    break;
    case "White":
    color = "ffffff"
    break;
    case "Daylight":
    color = "ffffff"
    break;
    case "Soft White":
    color = "ff"
    break;
    case "Warm White":
    color = "ff"
    break;
    case "W1":
    color = "ff"
    break;
    case "W2":
    color = "ff"
    break;
    case "Blue":
    color = "0000ff"
    break;
    case "Green":
    color = "00ff00"
    break;
    case "Yellow":
    color = "ffff00"
    break;
    case "Orange":
    color = "ff5a00"
    break;
    case "Purple":
    color = "5a00ff"
    break;
    case "Pink":
    color = "ff00ff"
    break;
    case "Cyan":
    color = "00ffff"
    break;
    case "Red":
    color = "ff0000"
    break;
    case "Off":
    color = "000000"
    break;
    case "Random":
    color = "xxxxxx"
    break;
}
   return color
}

def generate_preferences(configuration_model)
{
    def configuration = new XmlSlurper().parseText(configuration_model)
   
    configuration.Value.each
    {
        if(it.@hidden != "true" && it.@disabled != "true"){
        switch(it.@type)
        {   
            case ["number"]:
                input "${it.@index}", "number",
                    title:"${it.@label}\n" + "${it.Help}",
                    range: "${it.@min}..${it.@max}",
                    defaultValue: "${it.@value}",
                    displayDuringSetup: "${it.@displayDuringSetup}"
            break
            case "list":
                def items = []
                it.Item.each { items << ["${it.@value}":"${it.@label}"] }
                input "${it.@index}", "enum",
                    title:"${it.@label}\n" + "${it.Help}",
                    defaultValue: "${it.@value}",
                    displayDuringSetup: "${it.@displayDuringSetup}",
                    options: items
            break
            case ["password"]:
                input "${it.@index}", "password",
                    title:"${it.@label}\n" + "${it.Help}",
                    displayDuringSetup: "${it.@displayDuringSetup}"
            break
            case "decimal":
               input "${it.@index}", "decimal",
                    title:"${it.@label}\n" + "${it.Help}",
                    range: "${it.@min}..${it.@max}",
                    defaultValue: "${it.@value}",
                    displayDuringSetup: "${it.@displayDuringSetup}"
            break
            case "boolean":
               input "${it.@index}", "bool",
                    title:"${it.@label}\n" + "${it.Help}",
                    defaultValue: "${it.@value}",
                    displayDuringSetup: "${it.@displayDuringSetup}"
            break
            case "text":
               input "${it.@index}", "text",
                    title:"${it.@label}\n" + "${it.Help}",
                    defaultValue: "${it.@value}",
                    displayDuringSetup: "${it.@displayDuringSetup}"
            break
        }
        }
    }
}

 /*  Code has elements from other community source @CyrilPeponnet (Z-Wave Parameter Sync). */

def update_current_properties(cmd)
{
    def currentProperties = state.currentProperties ?: [:]
    currentProperties."${cmd.name}" = cmd.value

    if (settings."${cmd.name}" != null)
    {
        if (convertParam("${cmd.name}", settings."${cmd.name}").toString() == cmd.value)
        {
            sendEvent(name:"needUpdate", value:"NO", displayed:false, isStateChange: true)
        }
        else
        {
            sendEvent(name:"needUpdate", value:"YES", displayed:false, isStateChange: true)
        }
    }
    state.currentProperties = currentProperties
}


def update_needed_settings()
{
    def cmds = []
    def currentProperties = state.currentProperties ?: [:]
     
    def configuration = new XmlSlurper().parseText(configuration_model())
    def isUpdateNeeded = "NO"
    
    cmds << getAction("/configSet?name=haip&value=${device.hub.getDataValue("localIP")}")
    cmds << getAction("/configSet?name=haport&value=${device.hub.getDataValue("localSrvPortTCP")}")
    
    configuration.Value.each
    {     
        if ("${it.@setting_type}" == "lan" && it.@disabled != "true"){
            if (currentProperties."${it.@index}" == null)
            {
               if (it.@setonly == "true"){
                  logging("Setting ${it.@index} will be updated to ${convertParam("${it.@index}", it.@value)}", 2)
                  cmds << getAction("/configSet?name=${it.@index}&value=${convertParam("${it.@index}", it.@value)}")
               } else {
                  isUpdateNeeded = "YES"
                  logging("Current value of setting ${it.@index} is unknown", 2)
                  cmds << getAction("/configGet?name=${it.@index}")
               }
            }
            else if ((settings."${it.@index}" != null || it.@hidden == "true") && currentProperties."${it.@index}" != (settings."${it.@index}"? convertParam("${it.@index}", settings."${it.@index}".toString()) : convertParam("${it.@index}", "${it.@value}")))
            { 
                isUpdateNeeded = "YES"
                logging("Setting ${it.@index} will be updated to ${convertParam("${it.@index}", settings."${it.@index}")}", 2)
                cmds << getAction("/configSet?name=${it.@index}&value=${convertParam("${it.@index}", settings."${it.@index}")}")
            } 
        }
    }
    
    sendEvent(name:"needUpdate", value: isUpdateNeeded, displayed:false, isStateChange: true)
    return cmds
}

def convertParam(name, value) {
    switch (name){
        case "dcolor":
            getDefault()
        break
        default:
            value
        break
    }
}

def configuration_model()
{
'''
<configuration>
<Value type="password" byteSize="1" index="password" label="Password" min="" max="" value="" setting_type="preference" fw="">
<Help>
</Help>
</Value>
<Value type="list" byteSize="1" index="pos" label="Boot Up State" min="0" max="2" value="0" setting_type="lan" fw="">
<Help>
Default: Off
</Help>
    <Item label="Off" value="0" />
    <Item label="On" value="1" />
</Value>
<Value type="list" byteSize="1" index="dtransition" label="Default Transition" min="1" max="2" value="1" setting_type="lan" fw="">
<Help>
Default: Fade
</Help>
    <Item label="Fade" value="1" />
    <Item label="Flash" value="2" />
</Value>
<Value type="list" byteSize="1" index="dcolor" label="Default Color" min="" max="" value="" setting_type="lan" fw="">
<Help>
Default: Previous
</Help>
    <Item label="Previous" value="Previous" />
    <Item label="Soft White - Default" value="Soft White" />
    <Item label="White - Concentrate" value="White" />
    <Item label="Daylight - Energize" value="Daylight" />
    <Item label="Warm White - Relax" value="Warm White" />
    <Item label="Red" value="Red" />
    <Item label="Green" value="Green" />
    <Item label="Blue" value="Blue" />
    <Item label="Yellow" value="Yellow" />
    <Item label="Orange" value="Orange" />
    <Item label="Purple" value="Purple" />
    <Item label="Pink" value="Pink" />
    <Item label="Cyan" value="Random" />
    <Item label="W1" value="W1" />
    <Item label="W2" value="W2" />
    <Item label="Custom" value="Custom" />
</Value>
<Value type="text" byteSize="1" index="custom" label="Custom Color in Hex" min="" max="" value="" setting_type="preference" fw="">
<Help>
Use a 10 digit hex value rrggbbw1w2 (ie for white1 channel = 100% and red channel = 100% ff0000ff00)
If \"Custom\" is chosen above as the default color, default level does not apply.
</Help>
</Value>
<Value type="number" byteSize="1" index="level" label="Default Level" min="1" max="100" value="" setting_type="preference" fw="">
<Help>
</Help>
</Value>
<Value type="boolean" byteSize="1" index="channels" label="Mutually Exclusive RGB / White.\nOnly allow one or the other" min="" max="" value="false" setting_type="preference" fw="">
<Help>
</Help>
</Value>
<Value type="boolean" byteSize="1" index="hasW2" label="Use W2 for cold white instead of RGB" min="" max="" value="false" setting_type="preference" fw="">
<Help>
</Help>
</Value>
<Value type="list" byteSize="1" index="transitionspeed" label="Transition Speed" min="1" max="3" value="1" setting_type="lan" fw="">
<Help>
Default: Slow
</Help>
    <Item label="Slow" value="1" />
    <Item label="Medium" value="2" />
    <Item label="Fast" value="3" />
</Value>
<Value type="number" byteSize="1" index="autooff" label="Auto Off" min="0" max="65536" value="0" setting_type="lan" fw="" disabled="true">
<Help>
Automatically turn the switch off after this many seconds.
Range: 0 to 65536
Default: 0 (Disabled)
</Help>
</Value>
<Value type="list" index="logLevel" label="Debug Logging Level?" value="0" setting_type="preference" fw="">
<Help>
</Help>
    <Item label="None" value="0" />
    <Item label="Reports" value="1" />
    <Item label="All" value="99" />
</Value>
</configuration>
'''
}
