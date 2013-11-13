<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
</head>
<body>

<div id="contentDiv" style="display:none; padding: 0px 20px 0px 20px;">

	<div class="publishPageTitle">
		<p style="text-align:center; width: 100%;">
			<g:message code="publishManifestLbl"/>
		</p>
	</div>
	
	<form id="mainForm">
	
	<div style="margin:0px 0px 20px 0px">
    	<input type="text" name="subject" id="subject" style="width:400px"  required 
				title="<g:message code="subjectLbl"/>"
				placeholder="<g:message code="subjectLbl"/>"
    			oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
    			onchange="this.setCustomValidity('')" />

		<votingSystem:datePicker id="dateFinish" style="margin:0px 0px 0px 35px;" 
						title="${message(code:'dateLbl')}"
						placeholder="${message(code:'dateLbl')}"
	   					oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
	   					onchange="this.setCustomValidity('')"></votingSystem:datePicker>
	</div>
	
	<votingSystem:textEditorPC id="editorDiv"/>
		
	<div style='overflow:hidden;'>
		<div style="float:right; margin:20px 10px 0px 0px;">
			<votingSystem:simpleButton id="buttonAccept" isButton='true' 
				imgSrc="${resource(dir:'images',file:'accept_16x16.png')}" style="margin:0px 20px 0px 0px;">
					<g:message code="publishDocumentLbl"/>
			</votingSystem:simpleButton>
		</div>	
	</div>	
		
		
	</form>
		
	<g:render template="/template/signatureMechanismAdvert"  model="${[advices:[message(code:"onlySignedDocumentsMsg")]]}"/>
	
</div>

</body>
</html>
<r:script>
		
		 	$(function() {
		 		showEditor()

			    $('#mainForm').submit(function(event){
			    	event.preventDefault();
			    	hideEditor() 
			    	var isValidForm = true
			    	
			    	var subject = $( "#subject" ),
			    	dateFinish = $( "#dateFinish" ),
			    	editorDiv = $( "#editorDiv" ),
			        allFields = $( [] ).add( subject ).add( dateFinish ).add(editorDiv);
			        allFields.removeClass( "ui-state-error" );


					if(dateFinish.datepicker("getDate") < new Date() ) {
						dateFinish.addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
							'<g:message code="dateInitERRORMsg"/>')
						isValidForm = false
					}

					if(htmlEditorContent.trim() == 0) {
						editorDiv.addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
								'<g:message code="emptyDocumentERRORMsg"/>')
						isValidForm = false
					}  
					if(!isValidForm) {
						showEditor()
						return 
					}
					
					var event = new Evento();
			    	event.asunto = subject.val();
			    	event.contenido = htmlEditorContent.trim();
			    	event.fechaFin = dateFinish.datepicker('getDate').format();

			    	var webAppMessage = new WebAppMessage(
					    	StatusCode.SC_PROCESSING, 
					    	Operation.MANIFEST_PUBLISHING)
			    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.VotingSystem.serverName}"
			    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
					webAppMessage.contenidoFirma = event
					webAppMessage.urlEnvioDocumento = "${createLink( controller:'eventoFirma', absolute:true)}"
					webAppMessage.asuntoMensajeFirmado = '<g:message code="publishManifestSubject"/>'

					votingSystemClient.setMessageToSignatureClient(webAppMessage, publishDocumentCallback);
			    	return false 
			    });

			  });


			function publishDocumentCallback(appMessage) {
				console.log("publishDocumentCallback - message from native client: " + appMessage);
				var appMessageJSON = toJSON(appMessage)
				if(appMessageJSON != null) {
					$("#workingWithAppletDialog" ).dialog("close");
					var caption = '<g:message code="publishERRORCaption"/>'
					var msg = appMessageJSON.message
					if(StatusCode.SC_OK == appMessageJSON.statusCode) { 
						caption = '<g:message code="publishOKCaption"/>'
				    	var msgTemplate = "<g:message code='documentLinkMsg'/>";
						msg = "<p><g:message code='publishOKMsg'/>.</p>" + 
							msgTemplate.format(appMessageJSON.urlEnvioDocumento);
					}
					showResultDialog(caption, msg)
				}
			}

</r:script>