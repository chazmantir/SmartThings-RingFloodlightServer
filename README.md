# SmartThings-RingFloodlightServer

This will allow you to add and control features of your your Ring floodlight device.  Viewing the stream is still done through the Ring app but you can tie the notifications into other SmartThings related automations.

This requires the installation of a local server running a node script that utilizes the [doorbot](https://github.com/davglass/doorbot) script to handle communication.

Installation:
* Install doorbot (npm install doorbot -g)
* Copy the ringfloodlightserver.js file to a location on your machine and run it using node.
* Install the device handler in the SmartThings IDE.
* Install the smartapp in the SmartThings IDE.
* Configure the smartapp to point it to the server and add your credentials.  Your devices will start to show up.

Current capabilities:
* Motion Sensor Notification (Notifications as well as turning On/Off)
* Light Control (On/Off)
* Siren Control (On/Off)
