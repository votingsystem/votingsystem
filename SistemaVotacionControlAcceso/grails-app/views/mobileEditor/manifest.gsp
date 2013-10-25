<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<g:render template="/template/js/mobileEditor"/>
        <script type="text/javascript">
		
		 	$(function() {
		 		showEditor()
			    $("#dateFinish").datepicker(pickerOpts);

			    $('#mainForm').submit(function(event){
			    	event.preventDefault();
			    	var isValidForm = true
				    hideEditor()
    		    	var subject = $("#subject"),
			    	dateFinish = $("#dateFinish"),
			    	ckeditorDiv = $("#editor"),
			        allFields = $([]).add(subject).add(dateFinish).add(ckeditorDiv);	
			        allFields.removeClass( "ui-state-error" );
					if(!document.getElementById('subject').validity.valid) {
						subject.addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
							'<g:message code="emptyFieldMsg"/>')
						isValidForm = false
					}
					if(!document.getElementById('dateFinish').validity.valid) {
						dateFinish.addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
							'<g:message code="emptyFieldMsg"/>')
						isValidForm = false
					}
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
						isValidForm = false;
					}  
					if(!isValidForm) {
						showEditor();
						return false;
					} 		
			    	var event = new Evento();
			    	event.asunto = subject.val();
			    	event.contenido = htmlEditorContent.trim();
			    	event.fechaFin = DateUtils.format($("#dateFinish").datepicker('getDate')) + " 00:00:00";

			    	var webAppMessage = new WebAppMessage(
					    	StatusCode.SC_PROCESANDO, 
					    	Operation.PUBLICACION_MANIFIESTO_PDF)
			    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
			    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
					webAppMessage.contenidoFirma = event
					webAppMessage.urlEnvioDocumento = "${createLink( controller:'eventoFirma', absolute:true)}"
					webAppMessage.asuntoMensajeFirmado = '<g:message code="publishManifestSubject"/>'
					votingSystemClient.setMessageToSignatureClient(JSON.stringify(webAppMessage))
					return false
				});
			  });   

        </script>
</head>
<body>

<div id="contentDiv" style="display:none; padding: 20px 20px 20px 20px;">

	<form id="mainForm">
		<div style="margin:0px 0px 20px 0px">
	    	<input type="text" name="subject" id="subject" style="width:600px"  required
				title="<g:message code="subjectLbl"/>"
				placeholder="<g:message code="subjectLbl"/>"/>
	   			
	   			
		<input type="text" id="dateFinish" required readonly
				style="margin:0px 0px 0px 30px"
				title="<g:message code="dateLbl"/>"
				placeholder="<g:message code="dateLbl"/>"/>
		</div>
	
		<div id="editor"></div>
		<div id="editorContents" class="editorContents"  style="display: none"></div>
		
		<div id="contents" style="display: none">
			<!-- This div will be used to display the editor contents. -->
			<div id="editorContents" class="editorContents">
			</div>
		</div>
	
	
		<div style="float:right; margin:15px 20px 0px 0px;">
			<votingSystem:simpleButton isButton='true' id="submitEditorData" 
					imgSrc="${resource(dir:'images',file:'accept_16x16.png')}">
					<g:message code="publishDocumentLbl"/>
			</votingSystem:simpleButton>
		</div>
	</form>

</div>
</body>
</html>