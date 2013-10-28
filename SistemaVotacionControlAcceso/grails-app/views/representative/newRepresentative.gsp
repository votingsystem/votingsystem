<html>
<head>
        <meta name="layout" content="main" />
        <script src="${resource(dir:'ckeditor',file:'ckeditor.js')}"></script>
        <script type="text/javascript">

			CKEDITOR.on( 'instanceReady', function( ev ) {
				$("#contentDiv").fadeIn(500)
			});
		
		 	$(function() {

			    $('#mainForm').submit(function(event){
			    	event.preventDefault();
			    	var ckeditorDiv = $( "#ckeditor" )
			    	ckeditorDiv.removeClass( "ui-state-error" );
					
			        var editor = CKEDITOR.instances.editor1;
					if(editor.getData().length == 0) {
						ckeditorDiv.addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
								'<g:message code="emptyDocumentERRORMsg"/>')
						return false;
					}


			    	var webAppMessage = new WebAppMessage(
					    	StatusCode.SC_PROCESANDO, 
					    	Operation.NEW_REPRESENTATIVE)
			    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
		    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
					webAppMessage.contenidoFirma = {representativeInfo:editor.getData(), operation:Operation.REPRESENTATIVE_DATA}
					webAppMessage.urlEnvioDocumento = "${createLink( controller:'representative', absolute:true)}"
					webAppMessage.asuntoMensajeFirmado = '<g:message code="representativeDataLbl"/>'
					webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStamp', absolute:true)}"
					votingSystemClient.setMessageToSignatureClient(JSON.stringify(webAppMessage));
			    	return false 

			    });

			  });


			function setMessageFromSignatureClient(appMessage) {
		        var dataStr = JSON.stringify(appMessage);  
  			    console.log( "setMessageFromSignatureClient - dataStr: " + dataStr);
  			    console.log( "setMessageFromSignatureClient");
				$("#loadingVotingSystemAppletDialog").dialog("close");
				if(appMessage != null) {
					signatureClientToolLoaded = true;
					var appMessageJSON
					if( Object.prototype.toString.call(appMessage) == '[object String]' ) {
						appMessageJSON = JSON.parse(appMessage);
					} else {
						appMessageJSON = appMessage
					} 
					var statusCode = appMessageJSON.codigoEstado
					console.log( "setMessageFromSignatureClient - statusCode: " + statusCode);
					if(StatusCode.SC_PROCESANDO == statusCode){
						$("#loadingVotingSystemAppletDialog").dialog("close");
						$("#workingWithAppletDialog").dialog("open");
					} else {
						$("#workingWithAppletDialog" ).dialog("close");
						var caption = '<g:message code="publishERRORCaption"/>'
						var msg = appMessageJSON.mensaje
						if(StatusCode.SC_OK == statusCode) { 
							caption = '<g:message code="publishOKCaption"/>'
					    	var msgTemplate = "<g:message code='documentLinkMsg'/>";
							msg = "<p><g:message code='publishOKMsg'/>.</p>" + msgTemplate.format(appMessageJSON.mensaje);
						}
						showResultDialog(caption, msg)
					}
				}
			}

        </script>
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
	
	<div id="ckeditor" style="display:block;">
		<script>
			CKEDITOR.appendTo( 'ckeditor', {
                toolbar: [[ 'Bold', 'Italic', '-', 'NumberedList', 'BulletedList', '-', 'Link', 'Unlink' ],
					[ 'FontSize', 'TextColor', 'BGColor' ]]});
		</script>
	</div>	
		
	<div style="position:relative; margin:10px 10px 0px 0px;height:20px;">
		<div style="position:absolute; right:0;">
				<votingSystem:simpleButton isButton='true' 
					imgSrc="${resource(dir:'images',file:'accept_16x16.png')}" style="margin:0px 20px 0px 0px;">
						<g:message code="newRepresentativeLbl"/>
				</votingSystem:simpleButton>
		</div>	
	</div>	
		
		
	</form>
		
	<div class="userAdvert" >
		<ul>
			<li><g:message code="onlySignedDocumentsMsg"/></li>
			<li><g:message code="dniConnectedMsg"/></li>
			<li><g:message code="appletAdvertMsg"/></li>
			<li><g:message code="javaInstallAdvertMsg"/></li>
		</ul>
	</div>	
	

</div>

</body>
</html>