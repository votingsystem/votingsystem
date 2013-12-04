<html>
<head>
        <meta name="layout" content="main" />
   		<r:require modules="textEditorPC"/>
</head>
<body>

<div id="contentDiv" style="display:none;">

	<div class="publishPageTitle">
		<p style="margin: 0px 0px 0px 0px; text-align:center;">
			<g:message code="newRepresentativePageTitle"/>
		</p>
	</div>
	
	<div class="userAdvert" >
		<ul>
			<li><g:message code="newRepresentativeAdviceMsg1"/></li>
			<li><g:message code="newRepresentativeAdviceMsg2"/></li>
			<li><g:message code="newRepresentativeAdviceMsg3"/></li>
			<li><g:message code="newRepresentativeAdviceMsg4"/></li>
		</ul>
	</div>	
	
	<form id="mainForm">

    <div style="position:relative; width:100%;">
        <votingSystem:textEditor id="editorDiv" style="height:300px; width:100%;"/>
    </div>
		
	<div style="position:relative; margin:10px 10px 0px 0px;height:20px;">
		<div style="position:absolute; right:0;">
				<votingSystem:simpleButton isSubmitButton='true'
					imgSrc="${resource(dir:'images/fatcow_16',file:'accept.png')}" style="margin:0px 20px 0px 0px;">
						<g:message code="newRepresentativeLbl"/>
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
			    	var editorDiv = $("#editorDiv")
			    	hideEditor_editorDiv()
					if(editorDivContent.trim() == 0) {
						editorDiv.addClass( "formFieldError" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>',
						    '<g:message code="emptyDocumentERRORMsg"/>')
						showEditor_editorDiv();
						return
					}  

			    	var webAppMessage = new WebAppMessage( ResponseVS.SC_PROCESSING,Operation.NEW_REPRESENTATIVE)
			    	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
		    		webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
					webAppMessage.signedContent = {representativeInfo:editorDivContent.trim(),
							operation:Operation.REPRESENTATIVE_DATA}
					webAppMessage.receiverSignServiceURL = "${createLink( controller:'representative', absolute:true)}"
					webAppMessage.signedMessageSubject = '<g:message code="representativeDataLbl"/>'
					webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStampVS', absolute:true)}"
					votingSystemClient.setMessageToSignatureClient(webAppMessage, newRepresentativeCallback);
			    });
			  });

			function newRepresentativeCallback(appMessage) {
				console.log("newRepresentativeCallback - message from native client: " + appMessage);
				var appMessageJSON = toJSON(appMessage)
				if(appMessageJSON != null) {
					$("#workingWithAppletDialog" ).dialog("close");
					var caption = '<g:message code="publishERRORCaption"/>'
					var msg = appMessageJSON.message
					if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
						caption = '<g:message code="publishOKCaption"/>'
				    	var msgTemplate = "<g:message code='documentLinkMsg'/>";
						msg = "<p><g:message code='publishOKMsg'/>.</p>" + 
							msgTemplate.format(appMessageJSON.message);
					}
					showResultDialog(caption, msg)
				}
			}
</r:script>