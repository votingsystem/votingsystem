<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
        <meta name="layout" content="main" />
        <g:render template="/template/js/pcEditor"/>
        <script type="text/javascript">
		
		 	$(function() {
		 		showEditor()
			    $("#dateFinish").datepicker(pickerOpts);

			    
			    $('#mainForm').submit(function(event){
			    	event.preventDefault();
			    	hideEditor() 
			    	var isValidForm = true
			    	
			    	var subject = $( "#subject" ),
			    	dateFinish = $( "#dateFinish" ),
			    	ckeditorDiv = $( "#ckeditor" ),
			        allFields = $( [] ).add( subject ).add( dateFinish ).add(ckeditorDiv);
			        allFields.removeClass( "ui-state-error" );


					if(dateFinish.datepicker("getDate") < new Date() ) {
						dateFinish.addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
							'<g:message code="dateInitERRORMsg"/>')
						isValidForm = false
					}

					if(htmlEditorContent.trim() == 0) {
						ckeditorDiv.addClass( "ui-state-error" );
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
					    	StatusCode.SC_PROCESANDO, 
					    	Operation.PUBLICACION_MANIFIESTO_PDF)
			    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
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
					var msg = appMessageJSON.mensaje
					if(StatusCode.SC_OK == appMessageJSON.codigoEstado) { 
						caption = '<g:message code="publishOKCaption"/>'
				    	var msgTemplate = "<g:message code='documentLinkMsg'/>";
						msg = "<p><g:message code='publishOKMsg'/>.</p>" + 
							msgTemplate.format(appMessageJSON.urlEnvioDocumento);
					}
					showResultDialog(caption, msg)
				}
			}

        </script>
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

		<input type="text" id="dateFinish" style="margin:0px 0px 0px 35px;" required readonly
				title="<g:message code="dateLbl"/>"
				placeholder="<g:message code="dateLbl"/>"
   				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   				onchange="this.setCustomValidity('')"/>
	</div>
	
	<div id="editor"></div>
	<div id="editorContents" class="editorContents"></div>	
		
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