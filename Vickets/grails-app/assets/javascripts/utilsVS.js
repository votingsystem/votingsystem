function WebAppMessage(statusCode, operation) {
	this.statusCode = statusCode
	this.operation = operation
	this.subject ;
	this.signedContent;
	this.serviceURL;
	this.documentURL;
	this.receiverName;
	this.serverURL;
	this.message;
	this.caption;
	this.callerCallback;
}

var Operation = {
    SAVE_RECEIPT: "SAVE_RECEIPT",
    OPEN_RECEIPT: "OPEN_RECEIPT",
    VICKET_GROUP_NEW : "VICKET_GROUP_NEW",
    VICKET_GROUP_EDIT: "VICKET_GROUP_EDIT",
    VICKET_GROUP_CANCEL: "VICKET_GROUP_CANCEL",
    VICKET_GROUP_USER_ACTIVATE: "VICKET_GROUP_USER_ACTIVATE",
    VICKET_GROUP_USER_DEACTIVATE: "VICKET_GROUP_USER_DEACTIVATE",
    VICKET_GROUP_USER_DEPOSIT: "VICKET_GROUP_USER_DEPOSIT",
    VICKET_GROUP_SUBSCRIBE : "VICKET_GROUP_SUBSCRIBE"
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

DateUtils.checkDate = function (dateInit, dateFinish) {
    var todayDate = new Date();
    if(todayDate > dateInit && todayDate < dateFinish) return true;
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


//https://github.com/sairam/bootstrap-prompts/blob/master/bootstrap-prompts-alert.js
window._originalAlert = window.alert;
window.alert = function(text) {
    var bootStrapAlert = function() {
        if(! $.fn.modal.Constructor) return false;
        if($('#windowAlertModal').length == 1) return true;
    }
    if ( bootStrapAlert() ){
        $('#windowAlertModal .modal-body p').text(text);
        $('#windowAlertModal').modal('show');
    }  else {
        console.log('bootstrap was not found');
        window._originalAlert(text);
    }
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
	  var timeMillis = Date.parse(this)
	  return new Date(timeMillis)
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
		SC_CANCELLED : 0,
		SC_INITIALIZED : 1,
		SC_PAUSED:10
}

var SocketOperation = {
    LISTEN_TRANSACTIONS : "LISTEN_TRANSACTIONS"
}

function getUrlParam(paramName, staticURL, decode){
   var currLocation = (staticURL.length)? staticURL : window.location.search,
       parArr = currLocation.split("?")[1].split("&");
   
   for(var i = 0; i < parArr.length; i++){
        parr = parArr[i].split("=");
        if(parr[0] == paramName){
            return (decode) ? decodeURIComponent(parr[1]) : parr[1];
        }
   }
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

var dynatableParams = {
    dynatable: 'dynatable',
    queries: 'queries',
    sorts: 'sorts',
    page: 'page',
    perPage: 'max',
    offset: 'offset',
    record: null,
  }

var dynatableFeatures =  {
    search: false
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

var menuType = 'user'


function getMenuURL(baseURL) {
    var selectedMenuType = getParameterByName('menu')
    if(baseURL.indexOf("?") < 0) {
        baseURL = baseURL + "?menu=" + menuType;
    } else {
        baseURL = baseURL + "&menu=" + menuType;
    }
    return baseURL
}

function updateMenuLinks() {
    var selectedMenuType = getParameterByName('menu')
    if("" == selectedMenuType.trim()) {
        return
    }
    console.log("updateMenuLinks")
    menuType = selectedMenuType
    var elem = 'a'
    var attr = 'href'
    var elems = document.getElementsByTagName(elem);
    var arrayElements = Array.prototype.slice.call(elems);
    var groupElements = document.getElementsByClassName('linkvs');
    arrayElements.concat(Array.prototype.slice.call(groupElements))
    for (var i = 0; i < elems.length; i++) {
        if(elems[i][attr].indexOf("mailto:") > -1) continue
        if(elems[i][attr].indexOf("menu=" + selectedMenuType) < 0) {
            if(elems[i][attr].indexOf("?") < 0) {
                elems[i][attr] = elems[i][attr] + "?menu=" + menuType;
            } else elems[i][attr] = elems[i][attr] + "&menu=" + menuType;
        }
    }
    for (var j = 0; j < groupElements.length; j++) {
        var attrValue = groupElements[j].getAttribute("data-href")
        if(attrValue == null) continue
        if(attrValue.indexOf("menu=" + selectedMenuType) < 0) {
            if(attrValue.indexOf("?") < 0) {
                groupElements[j].setAttribute("data-href", attrValue + "?menu=" + menuType )
            } else groupElements[j].setAttribute("data-href", attrValue + "&menu=" + menuType );
        }
    }
}


//http://stackoverflow.com/questions/901115/how-can-i-get-query-string-values-in-javascript
function getParameterByName(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(location.search);
    return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

function VotingSystemClient () { }

VotingSystemClient.setJSONMessageToSignatureClient = function (messageJSON) {
    try {
        console.log("setJSONMessageToSignatureClient - clientTool: " + clientTool)
    } catch(e) {
        console.log(e)
        alert(e)
        return
    }
    var messageToSignatureClient = JSON.stringify(messageJSON)
    console.log("setJSONMessageToSignatureClient - messageToSignatureClient: " + messageToSignatureClient);
    clientTool.setJSONMessageToSignatureClient(messageToSignatureClient)
}
