<script src="${resource(dir:'js',file:'deployJava.js')}"></script>
<script src="${resource(dir:'js',file:'jquery-1.10.2.min.js')}"></script>  
<script src="${resource(dir:'js',file:'jquery-ui-1.10.3.custom.min.js')}"></script>
<link rel="stylesheet" href="${resource(dir:'css',file:'jquery-ui-1.10.3.custom.min.css')}">  
<link rel="stylesheet" href="${resource(dir:'css',file:'votingSystem.css')}">
<script type="text/javascript">

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
    		result = "Recibiendo solicitudes";
    	} else if(EstadoEvento.PENDIENTE_COMIENZO == estado) {
    		result =  "Pendiente de abrir";
    	} else if(EstadoEvento.FINALIZADO == estado) {
    		result =  "Finalizado";
    	} else if(EstadoEvento.CANCELADO == estado) {
    		result =  "Suspendido";
    	} else if(EstadoEvento.ACTORES_PENDIENTES_NOTIFICACION == estado) {
    		result =  "Falta notificación a participantes";
    	}
    	return result; 	
    }
}


var DateUtils = {

	format: function (date) {
		var curr_date = date.getDate();
	    var curr_month = date.getMonth() + 1; //Months are zero based
	    var curr_year = date.getFullYear();
	    return curr_date + "-" + curr_month + "-" + curr_year
	},
	
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

String.prototype.format = function() {
	  var args = arguments;
	  return this.replace(/{(\d+)}/g, function(match, number) { 
	    return typeof args[number] != 'undefined'
	      ? args[number]
	      : match
	    ;
	  });
	};

var FormUtils = {

	checkEmptyField: function (field) {
	      if (field.val().length == 0) {
	    	  field.addClass( "ui-state-error" );
  	          return false;
  	      } else {
  	          return true;
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
		SC_ERROR_PETICION : 400,
		SC_ANULACION_REPETIDA : 471,
		SC_ERROR_VOTO_REPETIDO : 470,
		SC_ERROR : 500,
		SC_ERROR_ENVIO_VOTO : 570,
		SC_PROCESANDO : 700,
		SC_CANCELADO : 0
}

var Operation = {
		ASOCIAR_CENTRO_CONTROL : "ASOCIAR_CENTRO_CONTROL",
		CAMBIO_ESTADO_CENTRO_CONTROL_SMIME: "CAMBIO_ESTADO_CENTRO_CONTROL_SMIME",
		SOLICITUD_COPIA_SEGURIDAD: "SOLICITUD_COPIA_SEGURIDAD", 
		PUBLICACION_MANIFIESTO_PDF: "PUBLICACION_MANIFIESTO_PDF", 
		FIRMA_MANIFIESTO_PDF: "FIRMA_MANIFIESTO_PDF", 
		PUBLICACION_RECLAMACION_SMIME: "PUBLICACION_RECLAMACION_SMIME",
		FIRMA_RECLAMACION_SMIME: "FIRMA_RECLAMACION_SMIME", 
		PUBLICACION_VOTACION_SMIME: "PUBLICACION_VOTACION_SMIME", 
		ENVIO_VOTO_SMIME: "ENVIO_VOTO_SMIME",
		MENSAJE_APPLET: "MENSAJE_APPLET", 
		MENSAJE_CIERRE_APPLET: "MENSAJE_CIERRE_APPLET", 
		GUARDAR_RECIBO_VOTO: "GUARDAR_RECIBO_VOTO", 
		ANULAR_VOTO: "ANULAR_VOTO", 
		ANULAR_SOLICITUD_ACCESO:"ANULAR_SOLICITUD_ACCESO", 
		CANCELAR_EVENTO: "CANCELAR_EVENTO", 
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
		CENTRO_CONTROL : "CENTRO_CONTROL",
		CONTROL_ACCESO : "CONTROL_ACCESO"
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
      if ( ver<= 8.0 ) {
    	  alert("Navegador no soportado, actualizate")
	  }
   }
}

function loadjsfile(filename){
	var fileref=document.createElement('script')
	fileref.setAttribute("type","text/javascript")
 	fileref.setAttribute("src", filename)
 }

function updateSubsystem(selectedSubsystem) {
	console.log(" - selectedSubsystem: " + selectedSubsystem)
	var subsystem_0_0_Link
	var subsystem_0_0_Text
	var subsystem_0_1_Link
	var subsystem_0_1_Text
	var subsystem_0_2_Link
	var subsystem_0_2_Text
	var selectedSubsystemLink
	var selectedSubsystemText
	if(SubSystem.VOTES == selectedSubsystem) {
		subsystem_0_0_Link = "<g:message code="manifestSystemURL"/>"
		subsystem_0_0_Text = "<g:message code="manifestLbl"/>"
		subsystem_0_1_Link = "<g:message code="claimSystemURL"/>"
		subsystem_0_1_Text = "<g:message code="claimLbl"/>"
		selectedSubsystemLink = "<g:message code="votingSystemURL"/>"
		selectedSubsystemText = "<g:message code="votingSystemLbl"/>"

	} else if(SubSystem.CLAIMS == selectedSubsystem) {
		subsystem_0_0_Link = "<g:message code="votingSystemURL"/>"
		subsystem_0_0_Text = "<g:message code="votingLbl"/>"
		subsystem_0_1_Link = "<g:message code="manifestSystemURL"/>"
		subsystem_0_1_Text = "<g:message code="manifestLbl"/>"
		selectedSubsystemLink = "<g:message code="claimSystemURL"/>"
		selectedSubsystemText = "<g:message code="claimSystemLbl"/>"
	} else if(SubSystem.MANIFESTS == selectedSubsystem) {
		subsystem_0_0_Link = "<g:message code="votingSystemURL"/>"
		subsystem_0_0_Text = "<g:message code="votingLbl"/>"
		subsystem_0_1_Link = "<g:message code="claimSystemURL"/>"
		subsystem_0_1_Text = "<g:message code="claimLbl"/>"
		selectedSubsystemLink = "<g:message code="manifestSystemURL"/>"
		selectedSubsystemText = "<g:message code="manifestSystemLbl"/>"
	} else if(SubSystem.REPRESENTATIVES == selectedSubsystem) {
		subsystem_0_0_Link = "<g:message code="votingSystemURL"/>"
		subsystem_0_0_Text = "<g:message code="votingLbl"/>"
		subsystem_0_1_Link = "<g:message code="claimSystemURL"/>"
		subsystem_0_1_Text = "<g:message code="claimLbl"/>"
		subsystem_0_2_Link = "<g:message code="manifestSystemURL"/>"
		subsystem_0_2_Text = "<g:message code="manifestSystemLbl"/>"
		selectedSubsystemLink = "<g:message code="representativeSystemURL"/>"
		selectedSubsystemText = "<g:message code="representativeSystemLbl"/>"
	} else {
		console.log("### updateSubsystem - unknown subsytem -> " + selectedSubsystem)
	}
	$('#subsystem_0_0_Link').attr('href',subsystem_0_0_Link);
	$('#subsystem_0_0_Link').text(subsystem_0_0_Text)
	$('#subsystem_0_1_Link').attr('href',subsystem_0_1_Link);
	$('#subsystem_0_1_Link').text(subsystem_0_1_Text)
	$('#subsystem_0_2_Link').attr('href',subsystem_0_2_Link);
	$('#subsystem_0_2_Link').text(subsystem_0_2_Text)
	$('#selectedSubsystemLink').attr('href',selectedSubsystemLink);
	$('#selectedSubsystemLink').text(selectedSubsystemText)
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

function setAppMessage(appMessage) {
	androidClient.setAppMessage(appMessage);
}

//http://jsfiddle.net/cckSj/5/
function getElapsedTime(endTime) { 
	
    // time difference in ms
    var timeDiff = endTime - new Date();

    if(timeDiff < 0) {
    	return "<g:message code="timeFinsishedLbl"/>"	
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


function printPaginate (offset, numItems, numMaxItemsForPage) {
	console.log("utils.printPaginate - offset:" + offset + " - numItems: " + numItems + 
			" - numMaxItemsForPage: " + numMaxItemsForPage)
	var numPages = ( (numItems -numItems%numMaxItemsForPage)/numMaxItemsForPage) + 1
	var offsetPage = ( (offset -offset%numMaxItemsForPage)/numMaxItemsForPage) + 1
	console.log("printPaginate - numPages:" + numPages + " - offsetPage: " + offsetPage)
	$("#paginationDiv").paginate({
		count 		: numPages,
		start 		: offsetPage,
		display     : 8,
		border					: true,
		border_color			: '#09287e',
		text_color  			: '#09287e',
		background_color    	: '',	
		background_hover_color  : '#09287e',
		border_hover_color		: '#ccc',
		text_hover_color  		: '#fff',
		images					: false,
		mouse					: 'press', 
		onChange				: window.paginate
	});
}

function showResultDialog(caption, message) {
	console.log("showResultDialog - caption: " + caption + " - message: "+ message);
	$('#resultMessage').html(message);
	$('#resultDialog').dialog('option', 'title', caption);
	$("#resultDialog").dialog( "open" );
}

var workingWithAppletDialog = $("${votingSystem.workingWithAppletDialog()}");

var resultDialog = $("<div id='resultDialog' title=''>" +
		  "<p id='resultMessage' style='text-align: center;'></p></div>");


var browserWithoutJavaDialogParams = {
	width: 600, autoOpen: false, modal: true,
	buttons: [{text:"<g:message code="acceptLbl"/>",
	          	icons: { primary: "ui-icon-check"},
	        	click:function() {$(this).dialog( "close" );}}],
	show: {effect:"fade", duration: 300},
	hide: {effect: "fade",duration: 300}
}

var loadingVotingSystemAppletDialogParams = {
	width: 330, autoOpen: false, modal: true,
    show: {effect: "fade", duration: 1000},
    hide: {effect: "fade", duration: 1000}}

var workingWithAppletDialogParams = {
   	  width: 330, autoOpen: false, modal: true,
      show: {effect: "fade",duration: 1000},
      hide: {effect: "fade", duration: 1000}
}

var resultDialogParams = {
	  width: 600, autoOpen: false, modal: true,
	   buttons: [{
	  		text:"<g:message code="acceptLbl"/>",
	         	icons: { primary: "ui-icon-check"},
	       	click:function() {
	       		$(this).dialog( "close" );	   	   			   				
		        	}
	     }],
	show: {effect: "fade",duration: 1000},
	hide: { effect: "fade", duration: 1000}
}

function isJavaEnabledClient() {
	if(!(deployJava.versionCheck('1.8') || deployJava.versionCheck('1.7'))) {
		console.log("---- checkJavaEnabled -> browser without Java7 or Java8 ");
		$(document.body).append("${votingSystem.browserWithoutJavaDialog()}");
		$("#browserWithoutJavaDialog").dialog(browserWithoutJavaDialogParams);
		$("#browserWithoutJavaDialog").dialog("open");
		return false
	} else return true
}

function showLoadingVotingSystemAppletDialog() {
	if (!($("#loadingVotingSystemAppletDialog").length > 0)) { 
    	$(document.body).append("${votingSystem.loadingAppletDialog()}");
    	$("#loadingVotingSystemAppletDialog").dialog(loadingVotingSystemAppletDialogParams);
	}
	$("#loadingVotingSystemAppletDialog").dialog("open");
}


var VotingSystemApplet = function () {
	
	var messageToSignatureClient = null;
	var messageToValidationTool = null;
	
	
	function getMessageToSignatureClient (appMessage) {
		var result
		if(messageToSignatureClient != null) {
			console.log("getMessageToSignatureClient - delivering message to applet");
			result = messageToSignatureClient
			messageToSignatureClient = null
		}
		return result
	}

	function getMessageToValidationTool (appMessage) {
		var result
		if(messageToValidationTool != null) {
			console.log("getMessageToValidationTool - delivering message to applet: " + appMessage);
			result = messageToValidationTool
			messageToValidationTool = null
		}
		return result
	}
	
	this.setMessateToValidationTool = function (message) {
		console.log("jsUtils.js - setMessateToValidationTool - message:" + message);
		messageToValidationTool = message;
		if(!validationToolAppletLoaded) {
			if(isJavaEnabledClient()) {
				console.log("Loading validationTool")
				$("#validationToolAppletFrame").attr("src", '${createLink(controller:'applet', action:'herramientaValidacion')}');
				showLoadingVotingSystemAppletDialog()
				window.getMessageToValidationTool = getMessageToValidationTool
			}
    	} else {
    		console.log("setMessateToValidationTool - validationToolAppletLoaded already loaded");
    		$("#workingWithAppletDialog").dialog("open");
	    } 
	}
	
	this.setMessageToSignatureClient = function (message) {
		console.log("---- setMessageToSignatureClient: " + message);
		messageToSignatureClient = message;
		if(!votingSystemAppletLoaded) {
			if(isJavaEnabledClient()) {
				console.log("Loading signature client");
				window.getMessageToSignatureClient = getMessageToSignatureClient
				$("#votingSystemAppletFrame").attr("src", '${createLink(controller:'applet', action:'cliente')}');
				showLoadingVotingSystemAppletDialog()
			} 
    	} else {
    		console.log("signature client already loaded");
    		$("#workingWithAppletDialog").dialog("open");
	    } 
	}

	window.onload=function(){
		$("#votingSystemAppletFrame").attr("src", "");
		$("#validationToolAppletFrame").attr("src", "");


		$(document.body).append(workingWithAppletDialog);
		$("#workingWithAppletDialog").dialog(workingWithAppletDialogParams);
				
		$(document.body).append(resultDialog);
	    $("#resultDialog").dialog(resultDialogParams);
	};

}

//"yy/MM/dd 12:00:00"
var pickerOpts = {showOn: 'both', buttonImage: "${createLinkTo(dir: 'images', file: 'appointment.png')}", 
		buttonImageOnly: true, dateFormat: 'yy/MM/dd'};

var numMaxEventsForPage = 6
var numMaxItemsForPage = 6

var validationToolAppletLoaded = false
var votingSystemAppletLoaded = false

var votingSystemApplet = new VotingSystemApplet()
</script> 
 
