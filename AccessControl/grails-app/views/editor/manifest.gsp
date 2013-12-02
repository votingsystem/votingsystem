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

    <div style="position:relative; width:100%;">
        <votingSystem:textEditor id="editorDiv" style="height:300px; width:100%;"/>
    </div>
		
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
		 		showEditor_editorDiv()

			    $('#mainForm').submit(function(event){
			    	event.preventDefault();
			    	hideEditor_editorDiv()
			    	var isValidForm = true
			    	
			    	var subject = $( "#subject" ),
			    	dateFinish = $( "#dateFinish" ),
			    	editorDiv = $( "#editorDiv" ),
			        allFields = $( [] ).add( subject ).add( dateFinish ).add(editorDiv);
			        allFields.removeClass( "formFieldError" );


					if(dateFinish.datepicker("getDate") < new Date() ) {
						dateFinish.addClass( "formFieldError" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
							'<g:message code="dateInitERRORMsg"/>')
						isValidForm = false
					}

					if(editorDivContent.trim() == 0) {
						editorDiv.addClass( "formFieldError" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
								'<g:message code="emptyDocumentERRORMsg"/>')
						isValidForm = false
					}  
					if(!isValidForm) {
						showEditor_editorDiv()
						return 
					}
					
					var eventVS = new EventVS();
			    	eventVS.subject = subject.val();
			    	eventVS.content = editorDivContent.trim();
			    	eventVS.dateFinish = dateFinish.datepicker('getDate').format();

			    	var webAppMessage = new WebAppMessage(
					    	ResponseVS.SC_PROCESSING,
					    	Operation.MANIFEST_PUBLISHING)
			    	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
			    		webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
					webAppMessage.signedContent = eventVS
					webAppMessage.receiverSignServiceURL = "${createLink( controller:'eventVSManifest', absolute:true)}"
					webAppMessage.signedMessageSubject = '<g:message code="publishManifestSubject"/>'

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
					if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
						caption = '<g:message code="publishOKCaption"/>'
				    	var msgTemplate = "<g:message code='documentLinkMsg'/>";
						msg = "<p><g:message code='publishOKMsg'/>.</p>" + 
							msgTemplate.format(appMessageJSON.receiverSignServiceURL);
					}
					showResultDialog(caption, msg)
				}
			}

</r:script>