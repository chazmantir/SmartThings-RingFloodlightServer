const RingAPI = require('doorbot');
const http = require('http');
const url = require('url');
const httpPort = 5000;

let ring = null;
let motionList = [];
let deviceList = [];

const checkMotionInterval = 2;  // in seconds
const checkLightInterval = 30;  // in seconds
let checkMotionTimer = null;
let checkLightTimer = null;
const motionNotificationDuration = 30; //in seconds

// Handle all URL requests.
const requestHandler = (request, response) => {
	var getMotionStatus = function (ml, id) {
		let ret = 'off';
		for (i=0; i < ml.length; i+=1) {
			if (ml[i].id == id) {
				ret = motionList[i].motion;
			};
		};
		return ret;
	};

	var resetMotionStatus = function (dl) {
		let ret = dl;
		for (i=0; i < ret.length; i+=1) {
			let elapsedTime = (Date.now() - ret[i].motiontime)/1000;
			if (elapsedTime > motionNotificationDuration) {
				ret[i].motion = 'off';
				ret[i].motiontime = Date.now();
				console.log('Motion stopped: ' + ret[i].id);
			};
		};
		return ret;
	};

	var findDevice = function (dl, id) {
		let foundDeviceIndex = -1;
		for (i=0; i < deviceList.length; i+=1) {
			if (deviceList[i].id == id) {
				foundDeviceIndex = i;
			};
		};
		return foundDeviceIndex;
	};

	var setLight = function (dl, id, s) {
		let ret = dl;
		for (i=0; i < ret.length; i+=1) {
			if (ret[i].id == id) {
				ret[i].led_status = s;
			};
		};
		return ret;
	}


	const checkMotion = () => {
		deviceList = resetMotionStatus(deviceList);
		ring.dings((e, json) => {
			if (json) {
				for (let device of json) {
					let fd = findDevice(deviceList,device.doorbot_id);
					if (fd > -1) {
						console.log('Motion started: ' + device.doorbot_id);
						deviceList[fd].motion = 'on';
						deviceList[fd].motiontime = Date.now();
					};
				};
			};
		});
	};
	const checkLight = () => {
		ring.devices((e, devices) => {
			if (devices.hasOwnProperty('stickup_cams')) {
				for (i=0; i<devices.stickup_cams.length;i+=1) {
					let device = devices.stickup_cams[i];
					let fd = findDevice(deviceList,device.id);
					if (fd > -1) {
						console.log('Led Status updated for: ' + device.id);
						deviceList[fd].led_status = device.led_status;
					};
				};
			};
		});
	};


    response.writeHead(200, {'Content-Type': 'application/json'});

	console.log('[requestHandler] request.url: ' + request.url);
	console.log('[requestHandler] request.url decoded: ' + decodeURIComponent(request.url));


    let q = url.parse(request.url, true).query;

	let sockets = {};
	sockets.service = "ringfloodlightserver";
	sockets.status = "ok";


	if (q.u != null && q.p != null) {
		if (ring == null || (ring != null && (ring.username != q.u || ring.password != q.p) ) ) { // Initialize the API if it hasn't been initialized yet.
			console.log("[requestHandler] Initializing Ring API.");
			console.log('[requestHandler]     q.u: ' + q.u);
			console.log('[requestHandler]     q.p: ' + q.p);
			console.log('[requestHandler]     q.r: ' + q.r);
			console.log('[requestHandler]     q.ua: ' + q.ua);
			ring = RingAPI({ // the api will handle auth issues
				email: q.u,
				password: q.p,
				retries: q.r,
				userAgent: q.ua
			});

		};


		console.log('[requestHandler] q.q: ' + q.q);
		console.log('[requestHandler] checkLightTimer: ' + checkLightTimer);
		console.log('[requestHandler] checkMotionTimer: ' + checkMotionTimer);
		if (q.q == 'init' || q.q == 'refresh' || checkLightTimer == null || checkMotionTimer == null) { // Initialize

			console.log('[requestHandler] Initialize.');
			if (q.u != null && q.p != null) {

				// Get device list.
				ring.devices((e, devices) => {
					if (devices.hasOwnProperty('stickup_cams')) {
						for (i=0; i<devices.stickup_cams.length;i+=1) {
							d = devices.stickup_cams[i];
							d.motion = getMotionStatus(motionList,d.id);
							deviceList.push(d);
						};
					} else {
						sockets.status = "error";
					};
				});

				// Start polling of device motion status.
				checkMotion();
				if (checkMotionTimer != null) {
					clearInterval(checkMotionTimer);
					checkMotionTimer = null;
				};
				checkMotionTimer = setInterval(checkMotion, checkMotionInterval * 1000);

				// Start polling of device light status.
				checkLight();
				if (checkLightTimer != null) {
					clearInterval(checkLightTimer);
					checkLightTimer = null;
				};
				checkLightTimer = setInterval(checkLight, checkLightInterval * 1000);

			} else {
				sockets.status = "error";
			};

			if (deviceList.length > 0) {
				sockets.devices = deviceList;
			};
			response.end(JSON.stringify(sockets));
		};

		if (q.q == 'trigger') {
			console.log('[requestHandler] Triggering motion.');
			if (deviceList.length > 0) {
				deviceList[0].motion = 'on';
				deviceList[0].motiontime = Date.now();
			};
			if (deviceList.length > 0) {
				sockets.devices = deviceList;
			};
			response.end(JSON.stringify(sockets));
		} else if (q.q == 'lights' && q.id != null && q.state != null) { // turn the lights on/off
			if (q.state == 'on') {
				console.log('[requestHandler] Turning on light.');
				ring.lightOn(q, (e) => {
					deviceList = setLight(deviceList, q.id, "on");
					response.end(JSON.stringify(sockets));
				});
			} else if (q.state == 'off') {
				console.log('[requestHandler] Turning off light.');
				ring.lightOff(q, (e) => {
					deviceList = setLight(deviceList, q.id, "off");
					response.end(JSON.stringify(sockets));
				});
			};
		} else {
			console.log('[requestHandler] Returning device list and status.');
			if (deviceList.length > 0) {
				sockets.devices = deviceList;
			};
			response.end(JSON.stringify(sockets));
		};
	} else {
		console.log('[requestHandler] Returning device list and status.');
		if (deviceList.length > 0) {
			sockets.devices = deviceList;
		};
		response.end(JSON.stringify(sockets));
	};

};


const httpServer = http.createServer(requestHandler);

httpServer.listen(httpPort, (err) => {
    if (err) {
        return console.log('something bad happened', err)
    }
    console.log(`http server is listening on ${httpPort}`)
});




