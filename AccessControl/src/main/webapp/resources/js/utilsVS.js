var Operation = {
    CERT_USER_NEW:"CERT_USER_NEW",
    SAVE_SMIME: "SAVE_SMIME",
    SEND_ANONYMOUS_DELEGATION:"SEND_ANONYMOUS_DELEGATION",
    OPEN_SMIME: "OPEN_SMIME",
    FILE_FROM_URL:"FILE_FROM_URL",
    BACKUP_REQUEST: "BACKUP_REQUEST",
    VOTING_PUBLISHING: "VOTING_PUBLISHING",
    SEND_VOTE: "SEND_VOTE",
    CANCEL_VOTE:"CANCEL_VOTE",
    SELECT_IMAGE:"SELECT_IMAGE",
    TERMINATED: "TERMINATED",
    BROWSER_URL: "BROWSER_URL",
    ACCESS_REQUEST_CANCELLATION:"ACCESS_REQUEST_CANCELLATION",
    EVENT_CANCELLATION: "EVENT_CANCELLATION",
    SIGNAL_VS:"SIGNAL_VS",
    NEW_REPRESENTATIVE:"NEW_REPRESENTATIVE",
    EDIT_REPRESENTATIVE:"EDIT_REPRESENTATIVE",
    REPRESENTATIVE_SELECTION:"REPRESENTATIVE_SELECTION",
    ANONYMOUS_REPRESENTATIVE_SELECTION:"ANONYMOUS_REPRESENTATIVE_SELECTION",
    REPRESENTATIVE_VOTING_HISTORY_REQUEST: "REPRESENTATIVE_VOTING_HISTORY_REQUEST",
    REPRESENTATIVE_ACCREDITATIONS_REQUEST: "REPRESENTATIVE_ACCREDITATIONS_REQUEST",
    REPRESENTATIVE_REVOKE: "REPRESENTATIVE_REVOKE",
    REPRESENTATIVE_DATA:"REPRESENTATIVE_DATA",
    ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION:"ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION",
    REPRESENTATIVE_STATE:"REPRESENTATIVE_STATE"
}

function httpGet(theUrl){
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.open( "GET", theUrl, false );
    xmlHttp.send( null );
    return xmlHttp.responseText;
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



function showMessageVS(message, caption, callerId, isConfirmMessage) {
    if (document.querySelector("#_votingsystemMessageDialog") != null && typeof
            document.querySelector("#_votingsystemMessageDialog").setMessage != 'undefined'){
        document.querySelector("#_votingsystemMessageDialog").setMessage(message, caption, callerId, isConfirmMessage)
    }  else {
        console.log('alert-dialog not found');
        window._originalAlert(message);
    }
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

function loadjsfile(filename){
	var fileref=document.createElement('script')
	fileref.setAttribute("type","text/javascript")
 	fileref.setAttribute("src", filename)
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

function checkInputType(inputType) {
    if(navigator.userAgent.toLowerCase().indexOf("javafx") > -1) return false;
    if(null == inputType || '' == inputType.trim()) return false
    var isSuppported = true
    var elem = document.createElement("input");
    elem.type = inputType;
    if (elem.disabled || elem.type != inputType) isSuppported = false;
    if("text" != inputType.toLowerCase()) {
        try {
            elem.value = "Test";
            if(elem.value == "Test") isSuppported = false;
        } catch(e) { console.log(e) }
    }
    return isSuppported
}


//http://www.mkyong.com/javascript/how-to-detect-ie-version-using-javascript/
function getInternetExplorerVersion() {
// Returns the version of Windows Internet Explorer or a -1
// (indicating the use of another browser).
   var rv = -1; // Return value assumes failure.
   if (navigator.appName == 'Microsoft Internet Explorer')
   {
      var ua = navigator.userAgent;
      var re  = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
      if (re.exec(ua) != null)
         rv = parseFloat( RegExp.$1 );
   }
   return rv;
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

function getFnName(fn) {
	  var f = typeof fn == 'function';
	  var s = f && ((fn.name && ['', fn.name]) || fn.toString().match(/function ([^\(]+)/));
	  return (!f && 'not a function') || (s && s[1] || 'anonymous');
}

function getRadioValue(radioName) {
    var radios = document.getElementsByName(radioName);
    for (var i = 0, length = radios.length; i < length; i++) {
        if (radios[i].checked) {
            return radios[i].value;
            break;
        }
    }
}

var menuType = getURLParam('menu').toLowerCase();
if(menuType == null) menuType = 'user'

//http://stackoverflow.com/questions/901115/how-can-i-get-query-string-values-in-javascript
function getURLParam(name, url) {
    if(!url) url = location.search
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

function VotingSystemClient () { }

var clientTool
VotingSystemClient.setMessage = function (messageJSON) {
    if(window['isClientToolConnected'] || window.parent['isClientToolConnected']) {
        if(clientTool == undefined) clientTool = window.top.clientTool //we're inside vs-iframe
        var messageToSignatureClient = JSON.stringify(messageJSON);
        //https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64.btoa#Unicode_Strings
        clientTool.setMessage(window.btoa(encodeURIComponent( escape(messageToSignatureClient))))
    } else {
        console.log("clientTool undefined")
        if(isAndroid ()) {
            var encodedData = window.btoa(JSON.stringify(messageJSON));
            window.sendAndroidURIMessage(encodedData)
            return
        }
        window.alert("clientTool undefined")
    }
}

VotingSystemClient.call = function (messageJSON) {
    if(window['isClientToolConnected'] || window.parent['isClientToolConnected']) {
        if(clientTool == undefined) clientTool = window.top.clientTool //we're inside vs-iframe
        var messageToSignatureClient = JSON.stringify(messageJSON);
        var resultBase64 = clientTool.call(window.btoa(encodeURIComponent( escape(messageToSignatureClient))))
        var b64_to_utf8 = decodeURIComponent(escape(window.atob(resultBase64)))
        return b64_to_utf8
    } else console.log("clientTool not found")
}

function sendSignalVS(signalData, callback) {
    var result
    var operationVS = new OperationVS(Operation.SIGNAL_VS)
    operationVS.jsonStr = JSON.stringify(signalData)
    operationVS.setCallback(callback)
    try {
        result = VotingSystemClient.call(operationVS);
    } catch(ex) { } finally { return result;}
}

window['isClientToolConnected'] = false

function checkIfClientToolIsConnected() {
    return window['isClientToolConnected'] || window.parent['isClientToolConnected']
}

function setClientToolConnected() {
    console.log("setClientToolConnected");
    window['isClientToolConnected'] = true;
    document.querySelector("#voting_system_page").dispatchEvent(new CustomEvent('votingsystem-client-connected', { }))
}

var coreSignalData = null
function fireCoreSignal(coreSignalDataBase64) {
    if(document.querySelector("#app") != null && document.querySelector("#app").fire != null) {
        var b64_to_utf8 = decodeURIComponent(escape(window.atob(coreSignalDataBase64)))
        document.querySelector("#app").fire('iron-signal', toJSON(b64_to_utf8));
        console.log("fireCoreSignal: " + b64_to_utf8)
    } else {
        coreSignalData = coreSignalDataBase64
        console.log("fireCoreSignal - navBar not found")
    }
}

window.addEventListener('WebComponentsReady', function() {
    if(coreSignalData != null) {
        console.log("utilsVS.js - WebComponentsReady -- sending pending core signal")
        fireCoreSignal(coreSignalData)
    }
    coreSignalData = null
});

//Message -> base64 encoded JSON
//https://developer.mozilla.org/en-US/docs/Web/JavaScript/Base64_encoding_and_decoding#Solution_.232_.E2.80.93_rewriting_atob()_and_btoa()_using_TypedArrays_and_UTF-8
function setClientToolMessage(callerId, message) {
    var b64_to_utf8 = decodeURIComponent(escape(window.atob(message)))
    window[callerId](b64_to_utf8)
}
