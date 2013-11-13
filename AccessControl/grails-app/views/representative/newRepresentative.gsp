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
	
	<votingSystem:textEditorPC id="editorDiv"/>
		
	<div style="position:relative; margin:10px 10px 0px 0px;height:20px;">
		<div style="position:absolute; right:0;">
				<votingSystem:simpleButton isButton='true' 
					imgSrc="${resource(dir:'images',file:'accept_16x16.png')}" style="margin:0px 20px 0px 0px;">
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
		 		showEditor()
		 		
			    $('#mainForm').submit(function(event){
			    	event.preventDefault();
			    	var editorDiv = $("#editorDiv")
			    	hideEditor()
					if(htmlEditorContent.trim() == 0) {
						editorDiv.addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
								'<g:message code="emptyDocumentERRORMsg"/>')
						showEditor();
						return
					}  

			    	var webAppMessage = new WebAppMessage(
					    	StatusCode.SC_PROCESSING, 
					    	Operation.NEW_REPRESENTATIVE)
			    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.VotingSystem.serverName}"
		    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
					webAppMessage.contenidoFirma = {representativeInfo:htmlEditorContent.trim(), 
							operation:Operation.REPRESENTATIVE_DATA}
					webAppMessage.urlEnvioDocumento = "${createLink( controller:'representative', absolute:true)}"
					webAppMessage.asuntoMensajeFirmado = '<g:message code="representativeDataLbl"/>'
					webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStamp', absolute:true)}"
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
					if(StatusCode.SC_OK == appMessageJSON.statusCode) { 
						caption = '<g:message code="publishOKCaption"/>'
				    	var msgTemplate = "<g:message code='documentLinkMsg'/>";
						msg = "<p><g:message code='publishOKMsg'/>.</p>" + 
							msgTemplate.format(appMessageJSON.message);
					}
					showResultDialog(caption, msg)
				}
			}
</r:script>