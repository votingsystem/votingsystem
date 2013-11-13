var WebAppMessage = function (statusCode, operacion) {
	this.codigoEstado = statusCode
	this.operacion = operacion
	this.asunto ;
	this.contenidoFirma;
	this.urlEnvioDocumento;
	this.urlDocumento;
	this.nombreDestinatarioFirma;
	this.urlServer;
	this.respuestaConRecibo;
	this.evento;
	this.redirectURL;
	this.callerCallback;
}

var Evento = function () {
    this.id
    this.fechaCreacion
    this.fechaFin
    this.fechaInicio
    this.usuario
    this.estado
    this.votante
    this.copiaSeguridadDisponible
    this.tipo
    this.operation
    this.hashSolicitudAccesoBase64
    this.hashSolicitudAccesoHex
    this.hashCertificadoVotoBase64
    this.hashCertificadoVotoHex
    this.cardinalidad
    this.urlSolicitudAcceso
    this.urlRecolectorVotosCentroControl
    this.controlAcceso
    this.centroControl
    this.opciones
    this.opcionSeleccionada
    this.campos
    this.duracion
    this.urlPDF
    this.URL
    this.numeroFirmas
    this.contenido
    this.asunto
    this.etiquetas
    this.opciones
    
    this.isActive = function () {
    	var result =  false;
    	if(EstadoEvento.ACTIVO == estado) {
    		result = DateUtils.checkDate(fechaFin, fechaFin);
    	} else if(EstadoEvento.PENDIENTE_COMIENZO == estado) {
    		result =  DateUtils.checkDate(fechaFin, fechaFin);
    	}
    	return result; 	
    }
    
    this.getMessage = function () {
    	var result =  "";
    	if(EstadoEvento.ACTIVO == estado) {
    		result = "<g:message code='openLbl'/>";
    	} else if(EstadoEvento.PENDIENTE_COMIENZO == estado) {
    		result =  "<g:message code='pendingLbl'/>";
    	} else if(EstadoEvento.FINALIZADO == estado) {
    		result =  "<g:message code='closedLbl'/>";
    	} else if(EstadoEvento.CANCELADO == estado) {
    		result =  "<g:message code='cancelledLbl'/>";
    	} else if(EstadoEvento.ACTORES_PENDIENTES_NOTIFICACION == estado) {
    		result =  "<g:message code='withoutNotificationsLbl'/>";
    	}
    	return result; 	
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

var DocumentState = {
		BORRADO_DE_SISTEMA : "BORRADO_DE_SISTEMA",
		CANCELADO:"CANCELADO",
		PENDIENTE_COMIENZO:"PENDIENTE_COMIENZO",
		FINALIZADO:"FINALIZADO"		
}

var StatusCode = {
		SC_OK : 200,
		SC_ERROR_REQUEST : 400,
		SC_CANCELLATION_REPEATED : 471,
		SC_ERROR_VOTE_REPEATED : 470,
		SC_ERROR : 500,
		SC_ERROR_ENVIO_VOTO : 570,
		SC_PROCESSING : 700,
		SC_TERMINATED :710,
		SC_CANCELLED : 0,
		
		SC_SIMULATION_INITIATED:275,
		SC_SIMULATION_RUNNING: 475
}


var Operation = {
		INIT_SIMULATION : "INIT_SIMULATION",
		CANCEL_SIMULATION:"CANCEL_SIMULATION",
		LISTEN:"LISTEN",
		
		
		CONTROL_CENTER_ASSOCIATION : "CONTROL_CENTER_ASSOCIATION",
		CONTROL_CENTER_STATE_CHANGE_SMIME: "CONTROL_CENTER_STATE_CHANGE_SMIME",
		BACKUP_REQUEST: "BACKUP_REQUEST", 
		MANIFEST_PUBLISHING: "MANIFEST_PUBLISHING", 
		MANIFEST_SIGN: "MANIFEST_SIGN", 
		CLAIM_PUBLISHING: "CLAIM_PUBLISHING",
		SMIME_CLAIM_SIGNATURE: "SMIME_CLAIM_SIGNATURE",
		VOTING_PUBLISHING: "VOTING_PUBLISHING", 
		SEND_SMIME_VOTE: "SEND_SMIME_VOTE",
		APPLET_MESSAGE: "APPLET_MESSAGE", 
		APPLET_PAUSED_MESSAGE: "APPLET_PAUSED_MESSAGE", 
		SAVE_VOTE_RECEIPT: "SAVE_VOTE_RECEIPT", 
		VOTE_CANCELLATION: "VOTE_CANCELLATION", 
		ACCESS_REQUEST_CANCELLATION:"ACCESS_REQUEST_CANCELLATION", 
		EVENT_CANCELLATION: "EVENT_CANCELLATION", 
		MENSAJE_HERRAMIENTA_VALIDACION:"MENSAJE_HERRAMIENTA_VALIDACION", 
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

var DataType = {
		CONTROL_CENTER : "CONTROL_CENTER",
		ACCESS_CONTROL : "ACCESS_CONTROL"
}

var EstadoEvento = {
		ACTIVO:"ACTIVO", 
		FINALIZADO:"FINALIZADO", 
		CANCELADO:"CANCELADO", 
		ACTORES_PENDIENTES_NOTIFICACION:"ACTORES_PENDIENTES_NOTIFICACION", 
		PENDIENTE_COMIENZO:"PENDIENTE_COMIENZO",
		PENDIENTE_DE_FIRMA:"PENDIENTE_DE_FIRMA", 
		BORRADO_DE_SISTEMA:"BORRADO_DE_SISTEMA"
}

function getEstadoEventoMsg(estadoEvento) { 
	
	if(EstadoEvento.ACTIVO == estadoEvento) {
		return "<g:message code='openLbl'/>"
	}
	if(EstadoEvento.FINALIZADO == estadoEvento) {
		return "<g:message code='closedLbl'/>"
	}
	if(EstadoEvento.CANCELADO == estadoEvento) {
		return  "<g:message code='closedLbl'/>"
	}
	if(EstadoEvento.PENDIENTE_COMIENZO == estadoEvento) {
		return  "<g:message code='pendingLbl'/>"
	}
	console.log("utils.getEstadoEventoMsg() - UNKNOWN STATE")
	return "UNKNOWN STATE"
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
