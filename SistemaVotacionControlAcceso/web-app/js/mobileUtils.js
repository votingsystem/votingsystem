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

var StatusCode = {
		SC_OK : 200,
		SC_PING : 0,
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
		REPRESENTATIVE_REVOKE: "REPRESENTATIVE_REVOKE"
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

function showResultDialog(caption, message) {
	console.log("showResultDialog - caption: " + caption + " - message: "+ message);
	$('#resultMessage').html(message);
	$('#resultDialog').dialog('option', 'title', caption);
	$("#resultDialog").dialog( "open" );
}

var resultDialog = $("<div id='resultDialog' title=''>" +
		  "<p id='resultMessage' style='text-align: center;'></p></div>");

var VotingSystemClient = function () {
	
	
	function setMessateToNativeClient (message) {
		console.log("---- setMessateToNativeClient: " + message);
		androidClient.setVotingWebAppMessage(message);
	}

	window.onload = function(){
		window.setMessateToNativeClient = setMessateToNativeClient
		$(document.body).append(resultDialog);
		$("#resultDialog").dialog({
		   	  width: 400, autoOpen: false, modal: true,
		      buttons: [{
		        		text:Message.acceptLbl,
		               	icons: { primary: "ui-icon-check"},
		             	click:function() {
		             		$(this).dialog( "close" );	   	   			   				
   			        	}
		           }],
		      //show: {effect: "fade",duration: 100},
		      //hide: { effect: "fade", duration: 100}
		    });
		
	};

}

var pickerOpts = {showOn: 'both', buttonImage: '/SistemaVotacionControlAcceso/images/appointment.png', 
		buttonImageOnly: true, dateFormat: 'yy/MM/dd'};

var votingSystemClient = new VotingSystemClient()