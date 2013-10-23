<%@ page import="grails.converters.JSON" %>
<%@ page import="org.sistemavotacion.controlacceso.modelo.*" %>
<%
	def messageToUser = null
	def eventClass = null
	if(Evento.Estado.FINALIZADO.toString().equals(eventMap?.estado)) {
		messageToUser =  message(code: 'claimEventFinishedLbl')
		eventClass = "eventFinishedBox"
	} else if(Evento.Estado.PENDIENTE_COMIENZO.toString().equals(eventMap?.estado)) {
		messageToUser = message(code: 'claimEventPendingLbl')
		eventClass = "eventPendingBox"
	} else if(Evento.Estado.CANCELADO.toString().equals(eventMap?.estado)) {
		messageToUser = message(code: 'claimEventCancelledLbl')
		eventClass = "eventFinishedBox"
	}
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
        <meta name="layout" content="main" />
        <script type="text/javascript">
        	var claimEvent = ${eventMap as JSON} 
        	var pendingOperation
        	var fieldsArray = new Array();
		 	$(function() {
		    		$("#adminDocumentLink").click(function () {
		    			$("#adminDocumentDialog").dialog("open");
			    	})

					if(${messageToUser != null?true:false}) { 
						$("#eventMessagePanel").addClass("${eventClass}");
					}

				    $('#submitClaimForm').submit(function(event){
				        event.preventDefault();
				        sendSiganture()
				    });
		    		$("#requestBackupButton").click(function () {
		    			$("#requestEventBackupDialog").dialog("open");
			    	})

    			   	$('#requestEventBackupForm').submit(function(event){
		 				event.preventDefault();
				    	var webAppMessage = new WebAppMessage(
						    	StatusCode.SC_PROCESANDO, 
						    	Operation.SOLICITUD_COPIA_SEGURIDAD)
				    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
			    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
		    			webAppMessage.urlEnvioDocumento = "${createLink(controller:'solicitudCopia', absolute:true)}"
		   				webAppMessage.asuntoMensajeFirmado = "${eventMap.asunto}"
						webAppMessage.evento = claimEvent
						claimEvent.operation = Operation.SOLICITUD_COPIA_SEGURIDAD
						webAppMessage.contenidoFirma = claimEvent
						webAppMessage.emailSolicitante = $("#eventBackupUserEmailText").val()
						webAppMessage.urlTimeStampServer = "${createLink(controller:'timeStamp', absolute:true)}"
						webAppMessage.respuestaConRecibo = true
						pendingOperation = Operation.FIRMA_RECLAMACION_SMIME
						//console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
						votingSystemApplet.setMessageToSignatureClient(JSON.stringify(webAppMessage)); 
		 			});
				    
			 });

			function sendSiganture() {
				console.log("sendSiganture")
		    	var webAppMessage = new WebAppMessage(
				    	StatusCode.SC_PROCESANDO, 
				    	Operation.FIRMA_RECLAMACION_SMIME)
		    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
	    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
    			webAppMessage.urlEnvioDocumento = "${createLink( controller:'recolectorReclamacion', absolute:true)}"
   				webAppMessage.asuntoMensajeFirmado = "${eventMap.asunto}"
				webAppMessage.evento = claimEvent

				var fieldsArray = new Array();
				<g:each in="${eventMap?.campos}" status="i" var="claimField">
					fieldsArray[${i}] = {id:${claimField?.id}, contenido:'${claimField?.contenido}', valor:$("#claimField${claimField?.id}").val()}
				</g:each>
				claimEvent.campos = fieldsArray
				claimEvent.operation = Operation.FIRMA_RECLAMACION_SMIME
				webAppMessage.contenidoFirma = claimEvent
				webAppMessage.urlTimeStampServer = "${createLink(controller:'timeStamp', absolute:true)}"
				webAppMessage.respuestaConRecibo = true
				pendingOperation = Operation.FIRMA_RECLAMACION_SMIME
				//console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
				votingSystemApplet.setMessageToSignatureClient(JSON.stringify(webAppMessage)); 
			}


			function setMessageFromSignatureClient(appMessage) {
				console.log("setMessageFromSignatureClient - message from native client: " + appMessage);
				$("#loadingVotingSystemAppletDialog").dialog("close");
				if(appMessage != null) {
					votingSystemAppletLoaded = true;
					var appMessageJSON
					if( Object.prototype.toString.call(appMessage) == '[object String]' ) {
						appMessageJSON = JSON.parse(appMessage);
					} else {
						appMessageJSON = appMessage
					} 
					var statusCode = appMessageJSON.codigoEstado
					if(StatusCode.SC_PROCESANDO == statusCode){
						$("#loadingVotingSystemAppletDialog").dialog("close");
						$("#workingWithAppletDialog").dialog("open");
					} else {
						$("#workingWithAppletDialog" ).dialog("close");
						var caption
						var msgTemplate
						var callBack
						var msg = appMessageJSON.mensaje
						if(Operation.FIRMA_RECLAMACION_SMIME == pendingOperation) {
							caption = '<g:message code="operationERRORCaption"/>'
							msg = appMessageJSON.mensaje
							if(StatusCode.SC_OK == statusCode) { 
								caption = "<g:message code='operationOKCaption'/>"
							}
						} else if(Operation.CANCELAR_EVENTO == pendingOperation) {
							if(StatusCode.SC_OK == statusCode) { 
								caption = "<g:message code='operationOKCaption'/>"
								msgTemplate = "<g:message code='documentCancellationOKMsg'/>"
								msg = msgTemplate.format('${eventMap?.asunto}');
								callBack = function() {
									window.location.href = "${createLink(controller:'eventoReclamacion')}/" + claimEvent.id;
								}
							}
						}
						showResultDialog(caption, msg, callBack)
					}
				}
			}
        </script>
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
	
	<div style="width:100%; font-size:0.8em; margin:2px 0px 25px 0px;">
		<div style="display:inline;margin:0px 20px 0px 20px;">
			<b><g:message code="dateLimitLbl"/>: </b>${eventMap?.fechaFin}
		</div>
		<g:if test="${Evento.Estado.ACTIVO.toString().equals(eventMap?.estado) ||
			Evento.Estado.PENDIENTE_COMIENZO.toString().equals(eventMap?.estado)}">			
			<div id="adminDocumentLink" class="appLink" style="float:right;margin:0px 20px 0px 0px;">
				<g:message code="adminDocumentLinkLbl"/>
			</div>
		</g:if>
	</div>

	<div class="eventPageContentDiv">
		<div style="">
			<div class="eventContentDiv">${eventMap?.contenido}</div>
		</div>
		
		<div style="width:100%; height: 50px;">
			<g:if test="${eventMap?.numeroFirmas > 0}">
				<div style="float:left;margin:0px 0px 0px 40px;">
					<votingSystem:simpleButton id="requestBackupButton"  
						style="margin:0px 20px 0px 0">
						<g:message code="numClaimsForEvent" args="${[eventMap?.numeroFirmas]}"/>
					</votingSystem:simpleButton>
				</div>
			</g:if>
			<div id="eventAuthorDiv" style="float:right;top:0px;">
				<b><g:message code="publisshedByLbl"/>:</b>${eventMap?.usuario}
			</div>
		</div>
		
		<form id="submitClaimForm">
		<g:if test="${eventMap?.campos.size() > 0}">
			<div class="eventOptionsDiv">
				<fieldset id="fieldsBox" style="">
					<legend id="fieldsLegend"><g:message code="claimsFieldLegend"/></legend>
					<div id="fields" style="width:100%;">
						<g:if test="${Evento.Estado.ACTIVO.toString().equals(eventMap?.estado)}">
							<g:each in="${eventMap?.campos}">
				  				<input type='text' id='claimField${it.id}' required 
				  					class='claimFieldInput'
	  								title='${it.contenido}' placeholder='${it.contenido}'
	   								oninvalid="this.setCustomValidity('<g:message code='emptyFieldLbl'/>')"
	   								onchange="this.setCustomValidity('')" />
							</g:each>
						</g:if>
						<g:if test="${Evento.Estado.CANCELADO.toString().equals(eventMap?.estado) ||
							Evento.Estado.FINALIZADO.toString().equals(eventMap?.estado) ||
							Evento.Estado.PENDIENTE_COMIENZO.toString().equals(eventMap?.estado)}">			
							<g:each in="${eventMap?.campos}">
								<div class="voteOption" style="width: 90%;margin: 10px auto 0px auto;">
									 - ${it.contenido}
								</div>
							</g:each>
						</g:if>
					</div>
				</fieldset>
			</div>
		</g:if>
		<g:if test="${Evento.Estado.ACTIVO.toString().equals(eventMap?.estado)}">			
			<div style="overflow: hidden;">
				<votingSystem:simpleButton id="signClaimFieldButton"  isButton='true'  
					style="margin:0px 20px 0px 0px; float:right;"
					imgSrc="${resource(dir:'images',file:'claim_22.png')}">
						<g:message code="signClaim"/>
				</votingSystem:simpleButton>
			</div>
		</g:if>
		</form>
	</div>
	
	<div class="userAdvert">
		<ul>
			<li><g:message code="dniConnectedMsg"/></li>
			<li><g:message code="appletAdvertMsg"/></li>
			<li><g:message code="javaInstallAdvertMsg"/></li>
		</ul>
	</div>		

<g:include controller="gsp" action="index" params="[pageName:'adminDocumentDialog']"/> 
<g:include controller="gsp" action="index" params="[pageName:'requestEventBackupDialog']"/> 		
</body>
</html>