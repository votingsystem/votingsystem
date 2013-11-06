<%@ page import="grails.converters.JSON" %>
<%@ page import="org.sistemavotacion.controlacceso.modelo.*" %>
<%
	def messageToUser = null
	def eventClass = null
	if(Evento.Estado.FINALIZADO.toString().equals(eventMap?.estado)) {
		messageToUser =  message(code: 'eventFinishedLbl')
		eventClass = "eventFinishedBox"
	} else if(Evento.Estado.PENDIENTE_COMIENZO.toString().equals(eventMap?.estado)) {
		messageToUser = message(code: 'eventPendingLbl')
		eventClass = "eventPendingBox"
	} else if(Evento.Estado.CANCELADO.toString().equals(eventMap?.estado)) {
		messageToUser = message(code: 'eventCancelledLbl')
		eventClass = "eventFinishedBox"
	}
%>
<!DOCTYPE html>
<html>
<head>
        <meta name="layout" content="main" />
</head>
<body>

	<g:if test="${messageToUser != null}">
		<div id="eventMessagePanel" class="eventMessagePanel">
			<p class="messageContent">
				${messageToUser}
			</p>
		</div>
	</g:if>

	<div class="publishPageTitle" style="margin:0px 0px 0px 0px;">
		<p style="margin: 0px 0px 0px 0px; text-align:center;">
			${eventMap?.asunto}
		</p>
	</div>
	
	<div style="display:inline-block; width:100%; font-size:0.8em;">
		<div style="display:inline;margin:0px 20px 0px 20px;">
			<b><g:message code="dateLimitLbl"/>: </b>${eventMap?.fechaFin}
		</div>
		
		<g:if test="${Evento.Estado.ACTIVO.toString() == eventMap?.estado ||
			Evento.Estado.PENDIENTE_COMIENZO.toString()}">			
			<div id="adminDocumentLink" class="appLink" style="float:right;margin:0px 20px 0px 0px;">
				<g:message code="adminDocumentLinkLbl"/>
			</div>
		</g:if>
	</div>

	<div class="eventPageContentDiv">
		<div style="width:100%;position:relative;">
			<div class="eventContentDiv">${eventMap?.contenido}</div>
		</div>
		
		<div style="width:100%; height: 50px;">
			<g:if test="${eventMap?.numeroFirmas > 0}">
				<div style="float:left;margin:0px 0px 0px 40px;">
					<votingSystem:simpleButton id="requestBackupButton"  
						style="margin:0px 20px 0px 0">
						<g:message code="numSignaturesForEvent" args="${[eventMap?.numeroFirmas]}"/>
					</votingSystem:simpleButton>
				</div>
			</g:if>
			<div id="eventAuthorDiv" style="float:right;top:0px;">
				<b><g:message code="publisshedByLbl"/>:</b>${eventMap?.usuario}
			</div>
		</div>
		
		<g:if test="${Evento.Estado.ACTIVO.toString().equals(eventMap?.estado)}">			
			<votingSystem:simpleButton id="signManifestButton"  isButton='true'  
				style="margin:15px 20px 0px 0px; float:right;"
				imgSrc="${resource(dir:'images',file:'claim_22.png')}">
					<g:message code="subscripcion.firmarManifiesto"/>
			</votingSystem:simpleButton>
		</g:if>
	</div>

	<g:render template="/template/signatureMechanismAdvert"/>


<g:include view="/include/dialog/adminDocumentDialog.gsp"/>
<g:include view="/include/dialog/requestEventBackupDialog.gsp"/>
</body>
</html>
<r:script>
<g:applyCodec encodeAs="none">
        	var pageEvent = ${eventMap as JSON} 
		 	$(function() {
				if(${messageToUser != null?true:false}) { 
					$("#eventMessagePanel").addClass("${eventClass}");
				}
			 	
	    		$("#adminDocumentLink").click(function () {
	    			showAdminDocumentDialog(adminDocumentCallback)
		    	})
		    	
	    		$("#signManifestButton").click(function () {
	    			sendManifest();
		    	})
		    	
	    		$("#requestBackupButton").click(function () {
	    			showRequestEventBackupDialog(requestBackupCallback)
		    	})
		    	
			 });

			function sendManifest() {
				console.log("sendManifest")
		    	var webAppMessage = new WebAppMessage(
				    	StatusCode.SC_PROCESANDO, 
				    	Operation.FIRMA_MANIFIESTO_PDF)
		    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
	    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
    			webAppMessage.urlEnvioDocumento = "${createLink( controller:'recolectorFirma', absolute:true)}/${eventMap.id}"
   				webAppMessage.asuntoMensajeFirmado = "${eventMap.asunto}"
		    	//signed and encrypted
    			webAppMessage.contentType = 'application/x-pkcs7-signature, application/x-pkcs7-mime'
   				webAppMessage.respuestaConRecibo = true
	    		webAppMessage.evento = pageEvent
				webAppMessage.urlTimeStampServer = "${createLink(controller:'timeStamp', absolute:true)}"
				webAppMessage.urlDocumento = pageEvent.URL
				votingSystemClient.setMessageToSignatureClient(webAppMessage, sendManifestCallback); 
			}

			function requestBackupCallback(appMessage) {
				console.log("requestBackupCallback");
				var appMessageJSON = toJSON(appMessage)
				if(appMessageJSON != null) {
					$("#workingWithAppletDialog" ).dialog("close");
					var caption = '<g:message code="operationERRORCaption"/>'
					if(StatusCode.SC_OK == appMessageJSON.codigoEstado) { 
						caption = "<g:message code='operationOKCaption'/>"
					}
					var msg = appMessageJSON.mensaje
					showResultDialog(caption, msg)
				}
			}

			function sendManifestCallback(appMessage) {
				console.log("eventSignatureCallback - message from native client: " + appMessage);
				var appMessageJSON = toJSON(appMessage)
				if(appMessageJSON != null) {
					$("#workingWithAppletDialog" ).dialog("close");
					var caption = '<g:message code="operationERRORCaption"/>'
					if(StatusCode.SC_OK == appMessageJSON.codigoEstado) { 
						caption = "<g:message code='operationOKCaption'/>"
					} else if (StatusCode.SC_CANCELADO== appMessageJSON.codigoEstado) {
						caption = "<g:message code='operationCANCELLEDLbl'/>"
					}
					var msg = appMessageJSON.mensaje
					showResultDialog(caption, msg)
				}
			}

			function adminDocumentCallback(appMessage) {
				console.log("adminDocumentCallback - message from native client: " + appMessage);
				var appMessageJSON = toJSON(appMessage)
				if(appMessageJSON != null) {
					$("#workingWithAppletDialog").dialog("close");
					var callBack
					if(StatusCode.SC_OK == appMessageJSON.codigoEstado) { 
						caption = "<g:message code='operationOKCaption'/>"
						msgTemplate = "<g:message code='documentCancellationOKMsg'/>"
						msg = msgTemplate.format('${eventMap?.asunto}');
						callBack = function() {
							window.location.href = "${createLink(controller:'eventoReclamacion')}/" + claimEvent.id;
						}
					}
					showResultDialog(caption, msg, callBack)
				}
			}
</g:applyCodec>
</r:script>