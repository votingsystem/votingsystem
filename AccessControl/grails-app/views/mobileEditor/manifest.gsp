<!DOCTYPE html>
<html>
<head>
	<r:require modules="textEditorMobile"/>
	<r:layoutResources />
</head>
<body>
<div class ="contentDiv">
	<form id="mainForm">
		<div style="margin:0px 0px 10px 0px">
    		<input type="text" name="subject" id="subject" style="width:500px"  required
				title="<g:message code="subjectLbl"/>"
				placeholder="<g:message code="subjectLbl"/>"/>
		</div>
   		<div style="margin:0px 0px 10px 0px">
		<votingSystem:datePicker id="dateFinish" title="${message(code:'dateLbl')}"
			 placeholder="${message(code:'dateLbl')}"
			 oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
			 onchange="this.setCustomValidity('')"></votingSystem:datePicker>   				
   		</div>
	
		<votingSystem:textEditorMobile id="editorDiv"/>
		
		<div id="contents" style="display: none">
			<!-- This div will be used to display the editor contents. -->
			<div id="editorContents" class="editorContents">
			</div>
		</div>
	
	
		<div style="margin:15px 20px 0px 0px; display: block;">
			<votingSystem:simpleButton style="float:right;" isButton='true' id="submitEditorData" 
					imgSrc="${resource(dir:'images',file:'accept_16x16.png')}">
					<g:message code="publishDocumentLbl"/>
			</votingSystem:simpleButton>
		</div>
	</form>
</div>
<div style="clear: both;margin:0px 0px 30px 0px;">&nbsp;</div>
</body>
</html>
 <r:script>
		 	$(function() {
		 		showEditor()

			    $('#mainForm').submit(function(event){
			    	event.preventDefault();
			    	var isValidForm = true
				    hideEditor()
    		    	var subject = $("#subject"),
			    	dateFinish = $("#dateFinish"),
			    	editorDiv = $("#editorDiv"),
			        allFields = $([]).add(subject).add(dateFinish).add(editorDiv);	
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
						editorDiv.addClass( "ui-state-error" );
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
			    	event.fechaFin = $("#dateFinish").datepicker('getDate').format();

			    	var webAppMessage = new WebAppMessage(
					    	StatusCode.SC_PROCESSING, 
					    	Operation.MANIFEST_PUBLISHING)
			    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.VotingSystem.serverName}"
			    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
					webAppMessage.contenidoFirma = event
					webAppMessage.urlEnvioDocumento = "${createLink( controller:'eventoFirma', absolute:true)}"
					webAppMessage.asuntoMensajeFirmado = '<g:message code="publishManifestSubject"/>'
					votingSystemClient.setMessageToSignatureClient(webAppMessage)
					return false
				});
			  });   

</r:script>
<r:layoutResources />