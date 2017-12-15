definition(
  name: "RingFloodlightServer Connector",
  namespace: "GvnCampbell",
  author: "Gavin Campbell",
  description: "Manage RingFloodlightServer devices through a local server.",
  category: "SmartThings Labs",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
  page(name: "config")
}

def config() {
	dynamicPage(name: "config", title: "RingFloodlightServer Local Server Settings", install: true, uninstall: true) {

		section("Please enter the details of the running copy of the local RingFloodlightServer server you want to connect to") {
		  input(name: "ip", type: "text", title: "IP", description: "RingFloodlightServer Server IP", required: true, submitOnChange: false)
		  input(name: "port", type: "text", title: "Port", description: "RingFloodlightServer Server Port", required: true, submitOnChange: true)
		  input(name: "username", type: "text", title: "Username", description: "Ring username", required: true, submitOnChange: true)
		  input(name: "password", type: "text", title: "Password", description: "Ring password", required: true, submitOnChange: true)
		}

		doDeviceSync()

		def options = getDevices().collect { s ->
			s.description
		}
		options.removeAll([null])

		def numFound = options.size() ?: 0

		if (ip && port && username && password) {
			section("Select your devices below.") {
		  		input name: "selectedDevices", type: "enum", required:false, title:"Select Devices (${numFound} found)", multiple:true, options:options
			}
		}

  	}
}

def doDeviceSync(){
	def logprefix = "[doDeviceSync] "
  	log.trace logprefix + "Starting..."

	poll()

  	if(!state.subscribe) {
  		log.trace logprefix + "Subscribing."
    	subscribe(location, null, locationHandler, [filterEvents:false])
    	state.subscribe = true
  	}

    runIn(10, doDeviceSync)
}

def getDevices() {
  def logprefix = "[getDevices] "
  log.trace logprefix + "Started..."
  log.debug logprefix + "Return: " + state.devices ?: [:]
  state.devices ?: [:]
}

def installed() {
  log.debug "---------------------->> Installed..."
  initialize()
}

def updated() {
  log.debug "---------------------->> Updated..."
  initialize()
}

def initialize() {
	def logprefix = "[initialize] "
	log.debug logprefix + "Started..."

	state.subscribe = false
	unsubscribe()


    if (state.devices == null) {
    	state.devices = []
    }

	if (ip && port) {
		doDeviceSync()
	}

	if (selectedDevices) {
    	addDevices()
    }
}

def uninstalled() {
  unschedule()
}

def locationHandler(evt) {
	def logprefix = "[locationHandler] "
  	log.debug logprefix + "Starting..."

  	def description = evt.description
  	def hub = evt?.hubId

  	def parsedEvent = parseLanMessage(description)
  	parsedEvent << ["hub":hub]

  	if (parsedEvent.headers && parsedEvent.body && parsedEvent?.data?.service == 'ringfloodlightserver') {
    	def body = new groovy.json.JsonSlurper().parseText(parsedEvent.body)
    	if (body instanceof java.util.HashMap)
    	{
			//POST /devices/ response
			log.debug logprefix + "Body is a map."

			body.devices.each {
				def dni = app.id + "/" + it.id
				def d = getChildDevice(dni)
				if (d) {
        			log.trace logprefix + "Child device found."
					if (it.led_status == "on") {
            			log.trace logprefix + " ====================> Switch on."
						sendEvent(d.deviceNetworkId, [name: "light", value: "on"])
					} else if (it.led_status == "off") {
            			log.trace logprefix + " ====================> Switch off."
						sendEvent(d.deviceNetworkId, [name: "light", value: "off"])
					}
                    if (it.motion == "on") {
            			log.trace logprefix + " ====================> Motion on."
						sendEvent(d.deviceNetworkId, [name: "motion", value: "active"])
                    } else if (it.motion == "off") {
            			log.trace logprefix + " ====================> Motion off."
						sendEvent(d.deviceNetworkId, [name: "motion", value: "inactive"])
                    }
        		} else {
        			log.trace logprefix + "Child device not found. Adding."
                    state.devices.add(it)
    	    		state.devices.unique()
				}
        	}
    	} else if (body instanceof java.util.List) {
			//GET /devices response (application/json)
			log.debug logprefix + "Body is JSON."
    	} else {
    	}
  	} else {
  	}
}



private def parseEventMessage(Map event) {
  def logprefix = "[parseEventMessage Map] "
  log.debug logprefix + "Started..."
  return event
}

def addDevices() {
	def logprefix = "[addDevices] "
	log.trace logprefix + "Started..."

	def devices = getDevices()
	log.trace logprefix + "devices: " + devices

	state.devices.each {
     	def dni = app.id + "/" + it.id
      	def d = getChildDevice(dni)
      	if(!d) {
        	d = addChildDevice("GvnCampbell", "Ring Floodlight", dni, null, ["label": "${it.description}"])
        	log.trace logprefix + "Created ${d.displayName} with id $dni"
        	d.refresh()
      	} else {
        	log.trace logprefix + "found ${d.displayName} with id $dni already exists, type: '$d.typeName'"
      	}
    }
}

def on(childDevice) {
  	def logprefix = "[on] "
  	log.debug logprefix + "Started..."
	def un = java.net.URLEncoder.encode(username, "UTF-8")
    def pw = java.net.URLEncoder.encode(password, "UTF-8")
    def id = getId(childDevice)
	put("", "", "", "?u=${un}&p=${pw}&q=lights&id=${id}&state=on")
}

def off(childDevice) {
  	def logprefix = "[off] "
  	log.debug logprefix + "Started..."
	def un = java.net.URLEncoder.encode(username, "UTF-8")
    def pw = java.net.URLEncoder.encode(password, "UTF-8")
    def id = getId(childDevice)
	put("", "", "", "?u=${un}&p=${pw}&q=lights&id=${id}&state=off")
}

private getId(childDevice) {
  def logprefix = "[getId] "
  log.debug logprefix + "Started..."
  return childDevice?.device?.deviceNetworkId.split("/")[-1]
}

private poll() {
	def logprefix = "[poll] "
	log.trace logprefix + "Started..."

    def un = java.net.URLEncoder.encode(username, "UTF-8")
    def pw = java.net.URLEncoder.encode(password, "UTF-8")
	put("", "", "", "?u=${un}&p=${pw}")
}


//************************************************************************************
//
//  Hanndle HTTP Communication
//
//************************************************************************************

private put(path, text, dni, q = "") {
  def logprefix = "[put] "
  log.debug logprefix + "Started..."

  def hubaction = new physicalgraph.device.HubAction([
        method: "PUT",
        path: path + "/" + q,
        body: text,
        headers: [ HOST: "$ip:$port", "Content-Type": "application/json" ]]
    )
    log.debug logprefix + "hubaction: " + hubaction
    sendHubCommand(hubaction)
}