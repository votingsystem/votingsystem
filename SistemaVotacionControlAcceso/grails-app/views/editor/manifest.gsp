<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
        <meta name="layout" content="main" />
        <script src="${resource(dir:'js/i18n',file:'jquery.ui.datepicker-es.js')}"></script>
        <script src="${resource(dir:'ckeditor',file:'ckeditor.js')}"></script>
        <script type="text/javascript">

			CKEDITOR.on( 'instanceReady', function( ev ) {
				$("#contentDiv").fadeIn(500)
			});
		
		 	$(function() {
			    $("#dateFinish").datepicker(pickerOpts);
			    $('#mainForm').submit(function(event){
			    	event.preventDefault();
				    
			    	var subject = $( "#subject" ),
			    	dateFinish = $( "#dateFinish" ),
			    	ckeditorDiv = $( "#ckeditor" ),
			        allFields = $( [] ).add( subject ).add( dateFinish ).add(ckeditorDiv);
			        allFields.removeClass( "ui-state-error" );


					if(dateFinish.datepicker("getDate") < new Date() ) {
						dateFinish.addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
							'<g:message code="dateInitERRORMsg"/>')
						return false
					}
					
			        var editor = CKEDITOR.instances.editor1;
					if(editor.getData().length == 0) {
						ckeditorDiv.addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
								'<g:message code="emptyDocumentERRORMsg"/>')
						return false;
					}

					var event = new Evento();
			    	event.asunto = subject.val();
			    	event.contenido = editor.getData();
			    	event.fechaFin = dateFinish.val() + " 00:00:00";

			    	var webAppMessage = new WebAppMessage(
					    	StatusCode.SC_PROCESANDO, 
					    	Operation.PUBLICACION_MANIFIESTO_PDF)
			    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
			    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
					webAppMessage.contenidoFirma = event
					webAppMessage.urlEnvioDocumento = "${createLink( controller:'eventoFirma', absolute:true)}"
					webAppMessage.asuntoMensajeFirmado = '<g:message code="publishManifestSubject"/>'

					votingSystemApplet.setMessateToNativeClient(JSON.stringify(webAppMessage));
			    	return false 
			    });

			  });


			function setMessageFromNativeClient(appMessage) {
		        var dataStr = JSON.stringify(appMessage);  
  			    console.log( "setMessageFromNativeClient - dataStr: " + dataStr);
  			    console.log( "setMessageFromNativeClient");
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
					console.log( "setMessageFromNativeClient - statusCode: " + statusCode);
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
			<g:message code="publishManifestLbl"/>
		</p>
	</div>
	
	<form id="mainForm">
	
	<div style="margin:0px 0px 20px 0px">
		<label for="subject"><g:message code="subjectLbl"/></label>
    	<input type="text" name="subject" id="subject" style="width:400px"  required 
    			oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
    			onchange="this.setCustomValidity('')" />

    	<label for="dateFinish" style="margin:0px 0px 20px 20px"><g:message code="dateLbl"/></label>
		<input type="text" id="dateFinish" required readonly
   				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   				onchange="this.setCustomValidity('')"/>
	</div>
	
	<div id="ckeditor" style="display:block;">
		<script>
			CKEDITOR.appendTo( 'ckeditor', {
                toolbar: [[ 'Bold', 'Italic', '-', 'NumberedList', 'BulletedList', '-', 'Link', 'Unlink' ],
					[ 'FontSize', 'TextColor', 'BGColor' ]]});
		</script>
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