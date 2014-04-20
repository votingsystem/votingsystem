var WebAppMessage = function (statusCode, operation) {
	this.statusCode = statusCode
	this.operation = operation
	this.subject ;
	this.signedContent;
	this.serviceURL;
	this.documentURL;
	this.receiverName;
	this.serverURL;
	this.eventVS;
	this.message;
	this.caption;
	this.callerCallback;
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

Date.prototype.format = function() {
	var curr_date = this.getDate();
    var curr_month = this.getMonth() + 1; //Months are zero based
    var curr_year = this.getFullYear();
    return curr_year + "/" + curr_month + "/" + curr_date + " 00:00:00"
};

//http://jsfiddle.net/cckSj/5/
Date.prototype.getElapsedTime = function() {
    // time difference in ms
    var timeDiff = this - new Date();

    if(timeDiff <= 0) {
    	return "<g:message code='timeFinsishedLbl'/>"	
    }
    
    // strip the miliseconds
    timeDiff /= 1000;

    // get seconds
    var seconds = Math.round(timeDiff % 60);

    // remove seconds from the date
    timeDiff = Math.floor(timeDiff / 60);

    // get minutes
    var minutes = Math.round(timeDiff % 60);

    // remove minutes from the date
    timeDiff = Math.floor(timeDiff / 60);

    // get hours
    var hours = Math.round(timeDiff % 24);

    // remove hours from the date
    timeDiff = Math.floor(timeDiff / 24);

    // the rest of timeDiff is number of days
    var resultStr
    var days = timeDiff;
    if(days > 0) {
    	resultStr = days + " " + "<g:message code="daysLbl"/>" + " " + "<g:message code="andLbl"/>" + " " + hours + " " + "<g:message code="hoursLbl"/>"
    } else if (hours > 0) {
    	resultStr = hours + " " + "<g:message code="hoursLbl"/>" + " " + "<g:message code="andLbl"/>" + " " + minutes + " " + "<g:message code="minutesLbl"/>"
    } else if (minutes > 0) {
    	resultStr = minutes + " " + "<g:message code="minutesLbl"/>" + " " + "<g:message code="andLbl"/>" + " " + seconds + " " + "<g:message code="secondsLbl"/>"
    }
    return resultStr
};


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

var dynatableInputs = {
        queries: null,
        sorts: null,
        multisort: ['ctrlKey', 'shiftKey', 'metaKey'],
        page: null,
        queryEvent: 'blur change',
        recordCountTarget: null,
        recordCountPlacement: 'after',
        paginationLinkTarget: null,
        paginationLinkPlacement: 'after',
        paginationPrev: '«',
        paginationNext: '»',
        paginationGap: [1,2,2,1],
        searchTarget: null,
        searchPlacement: 'before',
        perPageTarget: null,
        perPagePlacement: 'before',
        perPageText: '',
        recordCountText: '',
        pageText:'',
        recordCountPageBoundTemplate: '{pageLowerBound} a {pageUpperBound} de',
        recordCountTotalTemplate: '{recordsQueryCount}',
        processingText: '<span class="dynatableLoading">"<g:message code="updatingLbl"/><i class="fa fa-refresh fa-spin"></i></span>'
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