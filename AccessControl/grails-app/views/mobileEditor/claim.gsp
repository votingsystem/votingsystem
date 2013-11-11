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
    	<input type="text" name="subject" id="subject" style="width:500px" required 
				title="<g:message code="subjectLbl"/>"
				placeholder="<g:message code="subjectLbl"/>" 
    			oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
    			onchange="this.setCustomValidity('')" />
   </div>
   <div style="margin:0px 0px 10px 0px">	    			
		<votingSystem:datePicker id="dateFinish" title="${message(code:'dateLbl')}"
			 placeholder="${message(code:'dateLbl')}"
			 oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
			 onchange="this.setCustomValidity('')"></votingSystem:datePicker>   				
	</div>	

	<votingSystem:textEditorMobile id="editorDiv"/>

	<div style="position:relative; height: 50px;display: block;">
		<div style="font-size: 0.9em; margin:10px 0 0 10px;"> 
			<input type="checkbox" id="multipleSignaturesCheckbox"><g:message code="multipleClaimsLbl"/><br>
			<input type="checkbox" id="allowBackupRequestCheckbox"><g:message code="allowBackupRequestLbl"/>
		</div>
	</div>

	<div id="fieldsBox"  style="display:none;">
		<fieldset class="fieldsBox" style="margin:20px 20px 0px 20px;">
			<legend id="fieldsLegend" style="font-size: 1.2em;"><g:message code="claimsFieldLegend"/></legend>
			<div id="fields"></div>
		</fieldset>
	</div>
	
	<div style="position:relative; margin:20px 0px 0px 0px; height:20px;">
		<div style="float:left;">
			<votingSystem:simpleButton id="addClaimFieldButton" imgSrc="${resource(dir:'images',file:'info_16x16.png')}">
					<g:message code="addClaimFieldLbl"/>
			</votingSystem:simpleButton>
		</div>
		<div style="float:right;">
			<votingSystem:simpleButton isButton='true' id="addOptionButton"
				imgSrc="${resource(dir:'images',file:'accept_16x16.png')}">
					<g:message code="publishDocumentLbl"/>
			</votingSystem:simpleButton>	
		</div>	
	</div>	
		
	</form>

</div>
<div style="clear: both;margin:0px 0px 30px 0px;">&nbsp;</div>
<g:include view="/include/dialog/addClaimFieldDialog.gsp"/>
<g:include view="/include/dialog/resultDialog.gsp"/>

	<div id="newFieldTemplate" style="display:none;">
		<g:render template="/template/newField" model="[isTemplate:'true']"/>
	</div> 
	
</body>
</html>
<r:script>
			var numFields = 0

			$(function() {
		 		showEditor()
			    
	    		$("#addClaimFieldButton").click(function () {
	    			hideEditor() 
	    			showAddClaimFieldDialog(addClaimField)
	    		});

				function addClaimField (claimFieldText) {
					showEditor()
					if(claimFieldText == null) return
			        var newFieldTemplate = $('#newFieldTemplate').html()
			        var newFieldHTML = newFieldTemplate.format(claimFieldText);
			        var $newField = $(newFieldHTML)
   					$newField.find('div#deleteFieldButton').click(function() {
							$(this).parent().fadeOut(1000, 
   							function() { $(this).parent().remove(); });
							numFields--
							if(numFields == 0) {
			   					$("#fieldsBox").fadeOut(1000)
				   			}
    					}
					)
	   				$("#fieldsBox #fields").append($newField)
	   				if(numFields == 0) {
	   					$("#fieldsBox").fadeIn(1000)
		   			}
	   				numFields++
	   				$("#claimFieldText").val("");
				}

			    $('#mainForm').submit(function(event){
			    	event.preventDefault();
				    hideEditor()
					if(!validateForm()) {
						showEditor();
						return;
					} 					
			    	var event = new Evento();
			    	event.asunto = $("#subject").val();
			    	event.contenido = htmlEditorContent.trim();
			    	event.fechaFin = $("#dateFinish").datepicker('getDate').format();
			    	
					var claimFields = new Array();
					$("#fieldsBox").children().each(function(){
						var claimField = $(this).find('div.newFieldValueDiv');
						var claimFieldTxt = claimField.text();
						if(claimFieldTxt.length > 0) {
							var claimField = {contenido:claimFieldTxt}
							claimFields.push(claimField)
						}
					});

			    	event.campos = claimFields

					if($("#multipleSignaturesCheckbox").is(':checked') ) {
						event.cardinalidad = "UNA"
					} else {
						event.cardinalidad = "MULTIPLES"
					}
					event.copiaSeguridadDisponible = $("#allowBackupRequestCheckbox").is(':checked')

			    	var webAppMessage = new WebAppMessage(
					    	StatusCode.SC_PROCESANDO, 
					    	Operation.PUBLICACION_RECLAMACION_SMIME)
			    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.VotingSystem.serverName}"
		    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
					webAppMessage.contenidoFirma = event
					webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStamp', absolute:true)}"
					webAppMessage.urlEnvioDocumento = "${createLink( controller:'eventoReclamacion', absolute:true)}"
					webAppMessage.asuntoMensajeFirmado = "${message(code:'publishClaimSubject')}"
					webAppMessage.respuestaConRecibo = true

					votingSystemClient.setMessageToSignatureClient(webAppMessage)
					return false
				 })
			    
			  });

			function validateForm() {
				var subject = $("#subject"),
	    		dateFinish = $("#dateFinish"),
	    		editorDiv = $("#editorDiv"),
	        	allFields = $([]).add(subject).add(dateFinish).add(editorDiv);
				allFields.removeClass( "ui-state-error" );
	
				if(!document.getElementById('subject').validity.valid) {
					subject.addClass( "ui-state-error" );
					showResultDialog('<g:message code="dataFormERRORLbl"/>', 
						'<g:message code="emptyFieldMsg"/>')
					return false
				}
	
				if(!document.getElementById('dateFinish').validity.valid) {
					dateFinish.addClass( "ui-state-error" );
					showResultDialog('<g:message code="dataFormERRORLbl"/>', 
						'<g:message code="emptyFieldMsg"/>')
					return false
				}
				
				if(dateFinish.datepicker("getDate") < new Date() ) {
					dateFinish.addClass( "ui-state-error" );
					showResultDialog('<g:message code="dataFormERRORLbl"/>', 
						'<g:message code="dateInitERRORMsg"/>')
					return false
				}
	
				if(htmlEditorContent.trim() == 0) {
					editorDiv.addClass( "ui-state-error" );
					showResultDialog('<g:message code="dataFormERRORLbl"/>', 
							'<g:message code="emptyDocumentERRORMsg"/>')
					return false;
				}  
				return true
			}
		 	
</r:script>
<r:layoutResources />