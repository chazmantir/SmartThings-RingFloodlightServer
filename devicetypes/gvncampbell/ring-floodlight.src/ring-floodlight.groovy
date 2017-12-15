metadata {
  definition (name: "Ring Floodlight", namespace: "GvnCampbell", author: "Gavin Campbell") {
	capability "light"
    capability "motionSensor"
    capability "sensor"
    capability "refresh"
    capability "polling"
    command "refresh"
  }

  // UI tile definitions
  tiles {
    standardTile("light", "device.light", width: 2, height: 2, canChangeIcon: true) {
      state "off", label: '${name}', action: "light.on", icon: "st.Lighting.light11", backgroundColor: "#ffffff"
      state "on", label: '${name}', action: "light.off", icon: "st.Lighting.light11", backgroundColor: "#79b821"
      state "offline", label:'${name}', icon:"st.Lighting.light11", backgroundColor:"#ff0000"
    }
    standardTile("motion", "device.motion", width: 1, height: 1, canChangeIcon: true) {
      state "inactive", label: '${name}', icon: "st.motion.motion.inactive", backgroundColor: "#ffffff"
      state "active", label: '${name}', icon: "st.motion.motion.active", backgroundColor: "#79b821"
      state "offline", label:'${name}', icon:"st.motion.motion.inactive", backgroundColor:"#ff0000"
    }
    standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
      state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
    }
    main(["light"])
    details(["light", "motion", "refresh"])  }
}

def parse(description) {
  def logprefix = "[parse] "
  log.debug logprefix + "description: " + description

	def offlineTimeout = 300 //time in seconds before a device is marked offline
   try {
     unschedule("setOffline")
   } catch (e) {
   }
   runIn(offlineTimeout, setOffline)


  def results = []
  def map = description
  if (description instanceof String)  {
    map = stringToMap(description)
    log.debug logprefix + "stringToMap: " + map
  }

  if (map?.name && map?.value) {
  	log.debug logprefix + "createEvent"
    results << createEvent(name: "${map?.name}", value: "${map?.value}")
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

def refresh() {
  def logprefix = "[refresh] "
  log.debug logprefix + "Executed."
  parent.poll()
}

def setOffline() {
	log.trace logprefix + " ====================> Offline."
    sendEvent(name: "light", value: "offline")
    sendEvent(name: "motion", value: "offline")
}

