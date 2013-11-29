var WebAppMessage = function (statusCode, operacion) {
	this.statusCode = statusCode
	this.operacion = operacion
	this.subject ;
	this.signedContent;
	this.receiverSignServiceURL;
	this.urlDocumento;
	this.receiverName;
	this.serverURL;
	this.isResponseWithReceipt;
	this.eventVS;
	this.callerCallback;
}

EventVS.State = {
        ACTIVE:"ACTIVE",
        TERMINATED:"TERMINATED",
        CANCELLED:"CANCELLED",
        AWAITING:"AWAITING",
        PENDING_SIGNATURE:"PENDING_SIGNATURE",
        DELETED_FROM_SYSTEM:"DELETED_FROM_SYSTEM"
}

function EventVS(eventJSON, eventTemplate, subSystem) {


    this.getMessage = function () {
    	var result =  "";
    	if(EventVS.State.ACTIVE == this.state) {
    		result = "<g:message code='openLbl'/>";
    	} else if(EventVS.State.AWAITING == this.state) {
    		result =  "<g:message code='pendingLbl'/>";
    	} else if(EventVS.State.TERMINATED == this.state) {
    		result =  "<g:message code='closedLbl'/>";
    	} else if(EventVS.State.CANCELLED == this.state) {
    		result =  "<g:message code='cancelledLbl'/>";
    	}
    	return result;
    }

    this.subSystem = subSystem
    this.id = eventJSON.id
    this.dateCreated
    this.dateFinish = eventJSON.dateFinish
    this.dateBegin = eventJSON.dateBegin
    this.userVS = eventJSON.userVS
    this.state = eventJSON.state
    this.backupAvailable
    this.type
    this.operation
    this.hashAccessRequestBase64
    this.hashSolicitudAccesoHex
    this.hashCertVoteBase64
    this.hashCertVoteHex
    this.cardinality
    this.accessControl
    this.controlCenterVS
    this.fieldsEventVS
    this.optionSelected
    this.duracion
    this.urlPDF
    this.URL
    this.numSignatures
    this.content
    this.fieldsEventVS
    this.subject = eventJSON.subject
    if(eventTemplate != null) {
        var subjectStr = this.subject.substring(0,50) +  ((this.subject.length > 50)? "...":"");
        this.eventHTML = eventTemplate.format(subjectStr, this.userVS, this.dateBegin,
            this.dateFinish.getElapsedTime(), this.getMessage());
    }

    this.isActive = function () {
    	var result =  false;
    	if(EventVS.State.ACTIVE == this.state) {
    		result = DateUtils.checkDate(this.dateBegin, this.dateFinish);
    	} else if(EventVS.State.AWAITING == this.state) {
    		result =  DateUtils.checkDate(this.dateBegin, this.dateFinish);
    	}
    	return result;
    }

    this.getURL = function() {
        var result
        if(this.subSystem == SubSystem.VOTES) result = "${createLink( controller:'eventVSElection')}/" + this.id
        if(this.subSystem == SubSystem.CLAIMS) result = "${createLink( controller:'eventVSClaim')}/" + this.id
        if(this.subSystem == SubSystem.MANIFESTS) result = "${createLink( controller:'eventVSManifest')}/" + this.id
		return result;
    }

    this.getElement = function() {
        if(eventTemplate == null) console.log("eventTemplate null")

        var $newEvent = $(this.eventHTML)

        if(EventVS.State.ACTIVE == this.state) {
            $newEvent.css('border-color', '#6bad74')
            $newEvent.find(".eventSubjectDiv").css('background-color', '#6bad74')
            $newEvent.find(".eventStateDiv").css('color', '#6bad74')
        }
        if(EventVS.State.TERMINATED == this.state) {
            $newEvent.css('border-color', '#cc1606')
            $newEvent.find(".eventSubjectDiv").css('background-color', '#cc1606')
            $newEvent.find(".eventStateDiv").css('color', '#cc1606')
        }
        if(EventVS.State.CANCELLED == this.state) {
            $newEvent.css('border-color', '#cc1606')
            $newEvent.find(".eventSubjectDiv").css('background-color', '#cc1606')
            $newEvent.find(".eventStateDiv").css('color', '#cc1606')
            $newEvent.find(".cancelMessage").fadeIn(100)

        }
        if(EventVS.State.AWAITING == this.state) {
            $newEvent.css('border-color', '#fba131')
            $newEvent.find(".eventSubjectDiv").css('background-color', '#fba131')
            $newEvent.find(".eventStateDiv").css('color', '#fba131')
        }

        var eventURL = this.getURL()
        $newEvent.click(function() {
            console.log("- eventURL: " + eventURL);
            window.location.href = eventURL;
        });

        return $newEvent
    }

}


var DateUtils = {

	//parse dates with format "2010-08-30 01:02:03" 	
	parse: function (dateStr) {
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
	},
	
	checkDate: function (dateInit, dateFinish) {
		var todayDate = new Date();
		if(todayDate > dateInit && todayDate < dateFinish) return true;
		else return false;
	}
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
	  return this.replace(/{(\d+)}/g, function(match, number) { 
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
		SC_CANCELLATION_REPEATED : 471,
		SC_ERROR_VOTE_REPEATED : 470,
		SC_ERROR : 500,
		SC_PROCESSING : 700,
		SC_CANCELLED : 0,
		SC_INITIALIZED : 1,
		SC_PAUSED:10
}

var Operation = {
		CONTROL_CENTER_ASSOCIATION : "CONTROL_CENTER_ASSOCIATION",
		CONTROL_CENTER_STATE_CHANGE_SMIME: "CONTROL_CENTER_STATE_CHANGE_SMIME",
		BACKUP_REQUEST: "BACKUP_REQUEST", 
		MANIFEST_PUBLISHING: "MANIFEST_PUBLISHING", 
		MANIFEST_SIGN: "MANIFEST_SIGN", 
		CLAIM_PUBLISHING: "CLAIM_PUBLISHING",
		SMIME_CLAIM_SIGNATURE: "SMIME_CLAIM_SIGNATURE",
		VOTING_PUBLISHING: "VOTING_PUBLISHING", 
		SEND_SMIME_VOTE: "SEND_SMIME_VOTE",
		TERMINATED: "TERMINATED",
		SAVE_VOTE_RECEIPT: "SAVE_VOTE_RECEIPT", 
		VOTE_CANCELLATION: "VOTE_CANCELLATION", 
		ACCESS_REQUEST_CANCELLATION:"ACCESS_REQUEST_CANCELLATION", 
		EVENT_CANCELLATION: "EVENT_CANCELLATION",
		MENSAJE_CIERRE_HERRAMIENTA_VALIDACION: "MENSAJE_CIERRE_HERRAMIENTA_VALIDACION",
		NEW_REPRESENTATIVE:"NEW_REPRESENTATIVE",
		REPRESENTATIVE_SELECTION:"REPRESENTATIVE_SELECTION", 
		REPRESENTATIVE_VOTING_HISTORY_REQUEST: "REPRESENTATIVE_VOTING_HISTORY_REQUEST",
		REPRESENTATIVE_ACCREDITATIONS_REQUEST: "REPRESENTATIVE_ACCREDITATIONS_REQUEST", 
		REPRESENTATIVE_REVOKE: "REPRESENTATIVE_REVOKE",
		REPRESENTATIVE_DATA:"REPRESENTATIVE_DATA"
}

var SubSystem = {
		VOTES : "VOTES",
		CLAIMS: "CLAIMS",
		MANIFESTS: "MANIFESTS",
		REPRESENTATIVES:"REPRESENTATIVES"
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