var Operation = {
    BANK:"BANK",
    BANK_NEW:"BANK_NEW",
    CERT_CA_NEW:"CERT_CA_NEW",
    CERT_USER_NEW:"CERT_USER_NEW",
    CERT_EDIT:"CERT_EDIT",
    CONNECT:"CONNECT",
    DISCONNECT:"DISCONNECT",
    FILE_FROM_URL:"FILE_FROM_URL",
    LISTEN_TRANSACTIONS: "LISTEN_TRANSACTIONS",
    MESSAGEVS:"MESSAGEVS",
    CURRENCY_OPEN: "CURRENCY_OPEN",
    FROM_USER:"FROM_USER",
    CURRENCY_REQUEST: "CURRENCY_REQUEST",
    WALLET_OPEN: "WALLET_OPEN",
    WALLET_SAVE: "WALLET_SAVE"
}

function DateUtils(){}

//parse dates with format "2010-08-30 01:02:03" or "2010/08/30 01:02:03"
DateUtils.parse = function (dateStr) {
    var reggie = /(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})/;
    try { (+dateArray[1]) } catch(ex) { reggie = /(\d{4})\/(\d{2})\/(\d{2}) (\d{2}):(\d{2}):(\d{2})/; }

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

//removes year from date with format -> 'dd MMM yyyy HH:mm'
DateUtils.trimYear = function (dateStr) {
    var reggie = /(\d{2}) (\w{3}) (\d{4}) (\d{2}):(\d{2})/;
    var dateArray = reggie.exec(dateStr);
    return dateArray[1] + " " + dateArray[2] + " " + dateArray[4] + ":" + dateArray[5]
}

//parse dates with format "dd/mm/aa"
DateUtils.parseInput = function (dateStr) {
    var reggie = /(\d{2})\/(\d{2})\/(\d{2})/;
    var dateArray = reggie.exec(dateStr);
    var dateObject = new Date(
        (+dateArray[3]) + 2000,
        (+dateArray[2])-1, //Months are zero based
        (+dateArray[1])
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

Date.prototype.toISOStr = function() {
    return this.toISOString().slice(0, 10) + " " + 
        ('0' + this.getHours()).slice(-2) + ":" + ('0' + this.getMinutes()).slice(-2)
}

Number.prototype.getDate = function() {
    return new Date(this)
}

Date.prototype.urlFormatWithTime = function() {//YYYYMMDD_HHmm
    var curr_date = pad(this.getDate(), 2);
    var curr_month = pad(this.getMonth() + 1, 2); //Months are zero based
    var curr_year = this.getFullYear();
    return curr_year + curr_month + curr_date + "_" + ('0' + this.getHours()).slice(-2) + ('0' + this.getMinutes()).slice(-2)
};

Date.prototype.format = function() {
	var curr_date = this.getDate();
    var curr_month = this.getMonth() + 1; //Months are zero based
    var curr_year = this.getFullYear();
    return curr_year + "/" + curr_month + "/" + curr_date
};

Date.prototype.daydiff = function (dateToCompare) {
    return (this - dateToCompare)/(1000*60*60*24);
}

Date.prototype.getURL = function (period) {
    if(!period) return "/" + this.getFullYear() + "/" + pad(this.getMonth() + 1, 2) + "/" + pad(this.getDate(), 2)
    switch (period) {
        case 'WEEK': return "/" + this.getFullYear() + "/" + pad(this.getMonth() + 1, 2) + "/" + pad(this.getDate(), 2)
        case 'MONTH': return "/" + this.getFullYear() + "/" + pad(this.getMonth() + 1, 2)
        case 'YEAR': return "/" + this.getFullYear()
    }
}

Date.prototype.getMonday = function () {
    var day = this.getDay(),
        diff = this.getDate() - day + (day == 0 ? -6:1);
    var result = new Date(this.setDate(diff));
    result.setHours(0,0,0,0);
    return result;
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

//http://stackoverflow.com/questions/149055/how-can-i-format-numbers-as-money-in-javascript
//ej: (123456789.12345).formatMoney(3, '.', ',') ->  123,456,789.123
Number.prototype.formatMoney = function(c, d, t){
    var n = this,
        c = isNaN(c = Math.abs(c)) ? 2 : c,
        d = d == undefined ? "." : d,
        t = t == undefined ? "," : t,
        s = n < 0 ? "-" : "",
        i = parseInt(n = Math.abs(+n || 0).toFixed(c)) + "",
        j = (j = i.length) > 3 ? j % 3 : 0;
    return s + (j ? i.substr(0, j) + t : "") + i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + t) + (c ? d + Math.abs(n - i).toFixed(c).slice(2) : "");
};
	
String.prototype.getDate = function() {
	  var timeMillis = Date.parse(this)
	  return new Date(timeMillis)
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

String.prototype.getElapsedTime = function() {
	  return this.getDate().getElapsedTime()
};


Number.prototype.toAmountStr = function(fractionDigits){
    if(!fractionDigits) fractionDigits = 2
    return this.toLocaleString(undefined, { minimumFractionDigits: fractionDigits})
} 

function toJSON(message){
	if(message) {
		if(typeof message === 'string' ) return JSON.parse(message);
		else  return message
	}
}

var ResponseVS = {
		SC_OK : 200,
		SC_ERROR_REQUEST : 400,
		SC_ERROR_REQUEST_REPEATED : 409,
        SC_PRECONDITION_FAILED:412,
        SC_MESSAGE_FROM_VS:277,
		SC_ERROR : 500,
		SC_PROCESSING : 700,
		SC_CANCELED : 0,
		SC_INITIALIZED : 1,
		SC_PAUSED:10,
        SC_WS_CONNECTION_INIT_OK:800,
        SC_WS_CONNECTION_NOT_FOUND:841
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

function addNumbers(num1, num2) {
    return (new Number(num1) + new Number(num2)).toFixed(2)
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
if(menuType == null) menuType = 'user';

function isNumber(n) {
    return !isNaN(parseFloat(n)) && isFinite(n);
}

//http://stackoverflow.com/questions/901115/how-can-i-get-query-string-values-in-javascript
function getURLParam(name, url) {
    if(!url) url = window.location.href
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),  results = regex.exec(url);
    return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

vs.updateQueryStringParameter = function (uri, key, value) {
    var re = new RegExp("([?&])" + key + "=.*?(&|$)", "i");
    var separator = uri.indexOf('?') !== -1 ? "&" : "?";
    if (uri.match(re)) {
        return uri.replace(re, '$1' + key + "=" + value + '$2');
    } else {
        return uri + separator + key + "=" + value;
    }
}

vs.removeQueryStringParameter = function (uri, key) {
    var re = new RegExp("([?&])" + key + "=.*?(&|$)", "i");
    if (uri.match(re)) {
        return uri.replace(re, '$1' + '$2');
    } else return uri;
}

function getQueryParam(variable, querystring) {
    if(!querystring) querystring = location.search
    var vars = querystring.split('&');
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split('=');
        if (decodeURIComponent(pair[0]) == variable) {
            return decodeURIComponent(pair[1]);
        }
    }
    console.log('Query variable %s not found', variable);
}

function setURLParameter(baseURL, name, value){
    var result;
    if(getURLParam(name, baseURL)){
        result = baseURL.replace(new RegExp('([?|&]'+name + '=)' + '(.+?)(&|$)'),"$1"+encodeURIComponent(value)+"$3");
    }else if(baseURL.length){
        if(baseURL.indexOf("?") < 0) baseURL = baseURL + "?"
        result = baseURL + '&' + name + '=' +encodeURIComponent(value);
    } else {
        result = '?' + name + '=' +encodeURIComponent(value);
    }
    return result
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
            vs.socketElement.showOperationQRCode(vs.socketElement.CURRENCY_SYSTEM, messageJSON)
        });
    }
}

//http://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript?rq=1
vs.getUUID = function() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
        return v.toString(16);
    });
}

vs.getCookie = function(name) {
    var value = "; " + document.cookie;
    var parts = value.split("; " + name + "=");
    if (parts.length == 2) result = parts.pop().split(";").shift();
    return result.split(".")[0];
}
vs.getQRCodeURL = function(operation, operationCode, deviceId, key, size, socketSystem, msg) {
    if(!size) size = "100x100"
    if(!socketSystem) socketSystem = vs.systemCode.CURRENCY_SYSTEM
    var result = vs.contextURL + "/qr?cht=qr&chs=" + size + "&chl=srv=" + socketSystem + ";"
    if(operation != null) result = result + "op=" + operation + ";"
    if(operationCode != null) result = result + "opid=" + operationCode + ";"
    if(deviceId != null) result = result + "did=" + deviceId + ";"
    if(msg != null) result = result + "msg=" + msg + ";"
    if(key != null) result = result + "pk=" + key + ";"
    console.log("getQRCodeURL: " + result)
    return result;
}

vs.QROperationCode = {
    GENERATE_BROWSER_CERTIFICATE:0,
    MESSAGE_INFO:1,
    CURRENCY_SEND:2,
    USER_INFO:3,
    VOTE:4,
    OPERATION_PROCESS:5,
    ANONYMOUS_REPRESENTATIVE_SELECTION:6,
    GET_AES_PARAMS:7
}

vs.MediaType = {
    JSON_SIGNED:"application/json;application/pkcs7-signature"
}

vs.encryptAES = function(textToEncrypt, aesparamsDto) {
    var cipher = forge.cipher.createCipher('AES-CBC', aesparamsDto.key);
    cipher.start({iv: aesparamsDto.iv});
    cipher.update(forge.util.createBuffer(textToEncrypt));
    cipher.finish();
    var encrypted = cipher.output;
    return encrypted.data
}

vs.decryptAES = function(encryptedData, aesparamsDto) {
    var decipher = forge.cipher.createDecipher('AES-CBC', aesparamsDto.key);
    decipher.start({iv: aesparamsDto.iv});
    decipher.update(forge.util.createBuffer(encryptedData));
    decipher.finish();
    return decipher.output.data
}

function querySelector(selector) {
    if(document.querySelector(selector)) return document.querySelector(selector)
    else return document.querySelector('html /deep/ ' + selector)
}

function sendSignalVS(signalData) {
    var div = document.querySelector("#selectedPageTitle")
    if(div) div.innerText = signalData.caption
}

function checkIfClientToolIsConnected() {
    return (clientTool !== undefined) || (window.parent.clientTool !== undefined)
}

//Message -> base64 encoded JSON
//https://developer.mozilla.org/en-US/docs/Web/JavaScript/Base64_encoding_and_decoding#Solution_.232_.E2.80.93_rewriting_atob()_and_btoa()_using_TypedArrays_and_UTF-8
function setClientToolMessage(callerId, message) {
    var b64_to_utf8 = decodeURIComponent(escape(window.atob(message)))
    document.querySelector("#voting_system_page").dispatchEvent(new CustomEvent(callerId, {detail:b64_to_utf8}))
}