metadata {
  definition (name: "Ring Floodlight", namespace: "GvnCampbell", author: "Gavin Campbell") {
	capability "light"
    capability "switch"
    capability "motionSensor"
    capability "sensor"
    capability "refresh"
    capability "polling"
    capability "alarm"
    command "motionDetectionOn"
    command "motionDetectionOff"
    command "sirenOn"
    command "sirenOff"
    command "refresh"
  }

  // UI tile definitions
  tiles {
    standardTile("light", "device.light", width: 2, height: 2, canChangeIcon: false) {
      state "off", label: '${name}', action: "light.on", icon: "st.Lighting.light9", backgroundColor: "#ffffff"
      state "on", label: '${name}', action: "light.off", icon: "st.Lighting.light9", backgroundColor: "#79b821"
      state "offline", label:'${name}', icon:"st.Lighting.light11", backgroundColor:"#ff0000", defaultState: true
    }
    standardTile("motion", "device.motion", width: 1, height: 1, canChangeIcon: false) {
      state "inactive", label: '${name}', icon: "st.motion.motion.inactive", backgroundColor: "#ffffff", action: "motionDetectionOff", defaultState: true
      state "active", label: '${name}', icon: "st.motion.motion.active", backgroundColor: "#79b821", action: "motionDetectionOff"
      state "disabled", label:'${name}', icon:"st.motion.motion.inactive", backgroundColor:"#ff0000", action: "motionDetectionOn"
      state "offline", label:'${name}', icon:"st.motion.motion.inactive", backgroundColor:"#ff0000"
    }
    standardTile("alarm", "device.alarm", width: 1, height: 1, canChangeIcon: false) {
      state "off", label: '${name}', icon: "st.alarm.beep.beep", backgroundColor: "#ffffff", action: "sirenOn", defaultState: true
      state "siren", label: '${name}', icon: "st.alarm.beep.beep", backgroundColor: "#79b821", action: "sirenOff"
      state "offline", label:'${name}', icon:"st.alarm.beep.beep", backgroundColor:"#ff0000"
    }   
    standardTile("refresh", "device.switch", inactiveLabel: false, canChangeIcon: false, decoration: "flat") {
      state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
    }
    main(["light"])
    details(["light", "motion", "motionDetection", "alarm", "refresh"])  }
}

def sirenOn() {
  def logprefix = "[sirenOn] "
  logger logprefix + "Executed."
  parent.sirenOn(this)
  sendEvent(name: "alarm", value: "siren")
}
def sirenOff() {
  def logprefix = "[sirenOff] "
  logger logprefix + "Executed."
  parent.sirenOff(this)
  sendEvent(name: "alarm", value: "off")
}

def parse(description) {
  	def logprefix = "[parse] "
  	logger logprefix + "description: " + description
    
	offlineMonitor()

  def results = []
  def map = description
  if (description instanceof String)  {
    map = stringToMap(description)
    logger logprefix + "stringToMap: " + map
  }

  if (map?.name && map?.value) {
  	logger logprefix + "createEvent"
    if (map?.name == "motion" && device.currentValue("motion") == "disabled") {
    } else {
    	results << createEvent(name: "${map?.name}", value: "${map?.value}")
    }
  }
  results
}

// handle commands
def on() {
  parent.on(this)
  sendEvent(name: "light", value: "on")
}

def off() {
  parent.off(this)
  sendEvent(name: "light", value: "off")
}
def motionDetectionOn() {
  def logprefix = "[motionDetectionOn] "
  logger logprefix + "Executed."
  sendEvent(name: "motion", value: "inactive")
  sendEvent(name: "refresh", value: "default")
}

def motionDetectionOff() {
  def logprefix = "[motionDetectionOff] "
  logger logprefix + "Executed."
  sendEvent(name: "motion", value: "disabled")
}

def refresh() {
  def logprefix = "[refresh] "
  logger logprefix + "Executed."
  parent.poll()
}

// executed when device is first installed
def installed() {
	def logprefix = "[installed] "
    initialize()
}
// executed when device settings are updated
def updated() {
	def logprefix = "[updated] "
	initialize()
}
def initialize() {
    offlineMonitor()
    refresh()
}




// Monitoring the online/offline status
def offlineMonitor() {
	def offlineTimeout = 3600 //time in seconds before a device is marked offline
   	try { unschedule("setOffline") } catch (e) { } //reset the offline monitoring
   	runIn(offlineTimeout, setOffline)
}
def setOffline() {
	def logprefix = "[setOffline] "
	logger logprefix + " ====================> Offline."
    sendEvent(name: "light", value: "offline")
    sendEvent(name: "motion", value: "offline")
    sendEvent(name: "alarm", value: "offline")
    offlineMonitor()
    refresh()
}

private logger(text) {
	def loggingToggle = false
    if (loggingToggle) {
		logger text
	}
}
