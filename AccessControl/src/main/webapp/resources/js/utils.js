var Operation = {
    PUBLISH_EVENT:"PUBLISH_EVENT",
    CERT_USER_NEW:"CERT_USER_NEW",
    SEND_ANONYMOUS_DELEGATION:"SEND_ANONYMOUS_DELEGATION",
    OPEN_CMS: "OPEN_CMS",
    FILE_FROM_URL:"FILE_FROM_URL",
    BACKUP_REQUEST: "BACKUP_REQUEST",
    SEND_VOTE: "SEND_VOTE",
    CANCEL_VOTE:"CANCEL_VOTE",
    TERMINATED: "TERMINATED",
    ACCESS_REQUEST_CANCELLATION:"ACCESS_REQUEST_CANCELLATION",
    EVENT_CANCELLATION: "EVENT_CANCELLATION",
    EDIT_REPRESENTATIVE:"EDIT_REPRESENTATIVE",
    ANONYMOUS_REPRESENTATIVE_SELECTION:"ANONYMOUS_REPRESENTATIVE_SELECTION",
    REPRESENTATIVE_VOTING_HISTORY_REQUEST: "REPRESENTATIVE_VOTING_HISTORY_REQUEST",
    REPRESENTATIVE_ACCREDITATIONS_REQUEST: "REPRESENTATIVE_ACCREDITATIONS_REQUEST",
    REPRESENTATIVE_REVOKE: "REPRESENTATIVE_REVOKE",
    REPRESENTATIVE_DATA:"REPRESENTATIVE_DATA",
    ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION:"ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION",
    REPRESENTATIVE_STATE:"REPRESENTATIVE_STATE"
}

function EventVS() {}

EventVS.State = {
    ACTIVE:"ACTIVE",
    TERMINATED:"TERMINATED",
    CANCELED:"CANCELED",
    PENDING:"PENDING",
    DELETED_FROM_SYSTEM:"DELETED_FROM_SYSTEM"
}


function DateUtils(){}

//parse dates with format "2010-08-30 01:02:03"
DateUtils.parse = function (dateStr) {
		var reggie = /(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})/;
		var dateArray = reggie.exec(dateStr);
		var dateObject = new Date(
		    (+dateArray[1]),
		    (+dateArray[2])-1, //Months are zero based
		    (+dateArray[3]),
		    (+dateArray[4]),
		    (+dateArray[5]),
		    (+dateArray[6])
		);
		return dateObject
	}

//parse dates with format "yyyy-mm-dd"
DateUtils.parseInputType = function (dateStr) {
		var reggie = /(\d{4})-(\d{2})-(\d{2})/;
		var dateArray = reggie.exec(dateStr);
		var dateObject = new Date(
		    (+dateArray[1]),
		    (+dateArray[2])-1, //Months are zero based
		    (+dateArray[3])
		);
		return dateObject
	}

DateUtils.checkDate = function (dateBegin, dateFinish) {
		var todayDate = new Date();
		if(todayDate > dateBegin && todayDate < dateFinish) return true;
		else return false;
	}

Date.prototype.formatWithTime = function() {
    var curr_date = this.getDate();
    var curr_month = this.getMonth() + 1; //Months are zero based
    var curr_year = this.getFullYear();
    return curr_year + "/" + curr_month + "/" + curr_date + " " + ('0' + this.getHours()).slice(-2)  + ":" +
        ('0' + this.getMinutes()).slice(-2) + ":" + ('0' + this.getSeconds()).slice(-2)
};

Date.prototype.format = function() {
    var curr_date = this.getDate();
    var curr_month = this.getMonth() + 1; //Months are zero based
    var curr_year = this.getFullYear();
    return curr_year + "/" + curr_month + "/" + curr_date
};

Date.prototype.urlFormat = function() {
    var curr_date = pad(this.getDate(), 2);
    var curr_month = pad(this.getMonth() + 1, 2); //Months are zero based
    var curr_year = this.getFullYear();
    return curr_year + "_" + curr_month + "_" + curr_date
};

function pad(n, width, z) {
    z = z || '0';
    n = n + '';
    return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
}

function FormUtils(){}

FormUtils.checkIfEmpty = function (param) {
    if((param == undefined) || (param == null) || '' == param.trim()) return true;
    else return false
}

String.prototype.format = function() {
	  var args = arguments;
	  var str =  this.replace(/''/g, "'")
	  return str.replace(/{(\d+)}/g, function(match, number) {
	    return typeof args[number] != 'undefined'
	      ? args[number]
	      : match
	    ;
	  });
	};

String.prototype.getDate = function() {
	  return new Date(this)
};

String.prototype.getElapsedTime = function() {
	  return this.getDate().getElapsedTime()
};

function toJSON(message){
	if(message != null) {
		if( Object.prototype.toString.call(message) == '[object String]' ) {
			return JSON.parse(message);
		} else {
			return message
		} 
	}
}

var ResponseVS = {
		SC_OK : 200,
		SC_ERROR_REQUEST : 400,
		SC_ERROR_REQUEST_REPEATED : 409,
		SC_ERROR : 500,
		SC_PROCESSING : 700,
		SC_CANCELED : 0,
		SC_INITIALIZED : 1,
		SC_PAUSED:10
}

function calculateNIFLetter(dni) {
    var  nifLetters = "TRWAGMYFPDXBNJZSQVHLCKET";
    var module= dni % 23;
    return nifLetters.charAt(module);
}

function validateNIF(nif) {
	if(nif == null) return false;
	nif  = nif.toUpperCase();
	if(nif.length < 9) {
        var numZeros = 9 - nif.length;
		for(var i = 0; i < numZeros ; i++) {
			nif = "0" + nif;
		}
	}
	var number = nif.substring(0, 8);
    var letter = nif.substring(8, 9);
    if(letter != calculateNIFLetter(number)) return null;
    else return nif;
}

function openWindow(targetURL) {
    var width = 1000
    var height = 800
    var left = (screen.width/2) - (width/2);
    var top = (screen.height/2) - (height/2);
    var title = ''

    var newWindow =  window.open(targetURL, title, 'toolbar=no, scrollbars=yes, resizable=yes, '  +
        'width='+ width +
        ', height='+ height  +', top='+ top +', left='+ left + '');
}

function isChrome () {
	return (navigator.userAgent.toLowerCase().indexOf("chrome") > - 1);
}

function isAndroid () {
	return (navigator.userAgent.toLowerCase().indexOf("android") > - 1);
}

function isFirefox () {
	return (navigator.userAgent.toLowerCase().indexOf("firefox") > - 1);
}

function isJavaFX () {
	return (navigator.userAgent.toLowerCase().indexOf("javafx") > - 1);
}

var menuType = getURLParam('menu').toLowerCase();
if(menuType == null) menuType = 'user'

//http://stackoverflow.com/questions/901115/how-can-i-get-query-string-values-in-javascript
function getURLParam(name, url) {
    if(!url) url = window.location.href
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),  results = regex.exec(url);
    return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

function setURLParameter(baseURL, name, value){
    var result;
    if(getURLParam(name, baseURL)){
        result = baseURL.replace(new RegExp('([?|&]'+name + '=)' + '(.+?)(&|$)'),"$1"+encodeURIComponent(value)+"$3");
    }else if(baseURL.length){
        if(baseURL.indexOf("?") < 0) baseURL = baseURL + "?"
        result = baseURL +'&'+name + '=' +encodeURIComponent(value);
    } else {
        result = '?'+name + '=' +encodeURIComponent(value);
    }
    return result
}

vs.getHTTPJSON = function(url, callback) {
    var xhr = new XMLHttpRequest()
    xhr.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            callback(this.responseText)
        }
    };
    xhr.open("GET", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send();
}

var clientTool
vs.client = {}
vs.client.processOperation = function (messageJSON) {
    if(clientTool !== undefined) {
        if(messageJSON.jsonStr) {
                var jsonData = toJSON(messageJSON.jsonStr)
                jsonData.uuid =  vs.getUUID()
                messageJSON.jsonStr = JSON.stringify(jsonData)
            }
        var messageToSignatureClient = JSON.stringify(messageJSON);
        //https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64.btoa#Unicode_Strings
        clientTool.setMessage(window.btoa(encodeURIComponent( escape(messageToSignatureClient))))
    } else {
        Polymer.Base.importHref(vs.contextURL + '/element/vs-socket.vsp', function () {
            if (!vs.socketElement) {
                vs.socketElement = document.createElement('vs-socket');
                document.querySelector("#voting_system_page").appendChild(vs.socketElement)
                vs.socketElement.connect()
            }
            vs.socketElement.showOperationQRCode(vs.socketElement.VOTING_SYSTEM, messageJSON)
        });
    }
}


function sendSignalVS(signalData) {
    var div = document.querySelector("#selectedPageTitle")
    if(div) div.innerText = signalData.caption
}

function checkIfClientToolIsConnected() {
    return (clientTool !== undefined) ||  (window.parent.clientTool !== undefined)
}

vs.getUUID = function() {
    //http://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript?rq=1
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
        return v.toString(16);
    });
},

//Message -> base64 encoded JSON
//https://developer.mozilla.org/en-US/docs/Web/JavaScript/Base64_encoding_and_decoding#Solution_.232_.E2.80.93_rewriting_atob()_and_btoa()_using_TypedArrays_and_UTF-8
function setClientToolMessage(callerId, message) {
    var b64_to_utf8 = decodeURIComponent(escape(window.atob(message)))
    document.querySelector("#voting_system_page").dispatchEvent(new CustomEvent(callerId, {detail:b64_to_utf8}))
}
