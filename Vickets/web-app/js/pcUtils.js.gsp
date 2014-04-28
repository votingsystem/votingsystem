window.onload=function(){
	$("#votingSystemAppletFrame").attr("src", "");
	$("#validationToolAppletFrame").attr("src", "");
	checkIEVersion()
};


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

function checkIEVersion() {
   var ver = getInternetExplorerVersion();
   if ( ver> -1 ) {
      if ( ver<= 10.0 ) {
          alert("<g:message code='browserNosuportedMsg'/>")
	  }
   }
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


function isJavaEnabledClient() {
	if(!(deployJava.versionCheck('1.8') || deployJava.versionCheck('1.7'))) {
		console.log("---- checkJavaEnabled -> browser without Java7 or Java8 ");
		$("#browserWithoutJavaDialog").dialog("open")
		return false
	} else return true
}

function getFnName(fn) {
	  var f = typeof fn == 'function';
	  var s = f && ((fn.name && ['', fn.name]) || fn.toString().match(/function ([^\(]+)/));
	  return (!f && 'not a function') || (s && s[1] || 'anonymous');
}

var SocketService = function () {

    this.socket = null;

    this.connect = function () {
    	var host = "${grailsApplication.config.grails.serverURL}/websocket/service".replace('http', 'ws')
        if ('WebSocket' in window) {
            this.socket = new WebSocket(host);
        } else if ('MozWebSocket' in window) {
            this.socket = new MozWebSocket(host);
        } else {
            console.log('<g:message code="browserWithoutWebsocketSupport"/>');
            return
        }
        this.socket.onopen = function () {
            console.log('Info: WebSocket connection opened');
        };

        this.socket.onclose = function () {
            console.log('Info: WebSocket closed.');
        };
    }

    this.sendMessage = function(message) {
        var messageStr = JSON.stringify(message);
        console.log("sendMessage to simulation service: " + messageStr)
        if(this.socket == null || 3 == this.socket.readyState) {
            console.log("missing message - socket closed")
        } else if(messageStr != ''){
            this.socket.send(messageStr);
        }
    }

    this.close = function() {
        //states: CONNECTING	0, OPEN	1, CLOSING	2, CLOSED	3
        if(this.socket == null || 3 == this.socket.readyState) {
            console.log("socket already closed")
            return
        } else console.log(" closing socket connection")
        this.socket.close()
    }

};

var socketService = new SocketService()
socketService.connect()