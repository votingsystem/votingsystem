<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
        <link rel="stylesheet" media="handheld, only screen and (max-device-width: 320px)">                             
        <title>${message(code: 'nombreServidorLabel', null)}</title>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width" />
        <meta name="HandheldFriendly" content="true" />
        <script src="${resource(dir:'ckeditor',file:'ckeditor.js')}"></script>
        <script src="${resource(dir:'js',file:'jquery-1.10.2.min.js')}"></script>
        <script src="${resource(dir:'js',file:'jquery-ui-1.10.3.custom.min.js')}"></script>
        <link rel="stylesheet" href="${resource(dir:'css',file:'jquery-ui-1.10.3.custom.min.css')}">    
        <script src="${resource(dir:'js/i18n',file:'jquery.ui.datepicker-es.js')}"></script>
        <script src="${resource(dir:'app',file:'jsMessages')}"></script>
        <script src="${resource(dir:'js',file:'mobileUtils.js')}"></script>
        <script type="text/javascript">

			CKEDITOR.on( 'instanceReady', function( ev ) {
				$("#contentDiv").fadeIn(500)
			});
		
		 	$(function() {
			    $("#dateFinish").datepicker(pickerOpts);

			    $('#mainForm').submit(function(event){
			    	event.preventDefault();
			    	var subject = $("#subject"),
			    	dateFinish = $("#dateFinish"),
			    	ckeditorDiv = $("#ckeditor"),
			        allFields = $([]).add(subject).add(dateFinish).add(ckeditorDiv);	
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
					webAppMessage.asuntoMensajeFirmado = Message.publishManifestSubject
					
					setMessateToNativeClient(JSON.stringify(webAppMessage))
					return false
				});
			  });   

        </script>
</head>
<body>

<div id="contentDiv" style="display:none;">

	<form id="mainForm">
		<div style="margin:0px 0px 20px 0px">
			<label for="subject"><g:message code="subjectLbl"/></label>
	    	<input type="text" name="subject" id="subject" style="width:300px" 
	    		class="text ui-widget-content ui-corner-all" required/>
	   			
	   			
    	<label for="dateFinish" style="margin:0px 0px 20px 0px"><g:message code="dateFinishLbl"/></label>
		<input type="text" id="dateFinish" class="text ui-widget-content ui-corner-all" style="width:110px;" required readonly/>
		</div>
	
		<div id="ckeditor">
			<script>
				CKEDITOR.appendTo( 'ckeditor', {
	                toolbar: [[ 'Bold', 'Italic', '-', 'NumberedList', 'BulletedList', '-', 'Link', 'Unlink' ],
						[ 'FontSize', 'TextColor', 'BGColor' ]]});
			</script>
		</div>
		<div style="margin:10px 0px 0px 20px;">
			<input id="submitEditorData" type="submit" value="<g:message code="publishDocumentLbl"/>">
		</div>
	</form>

</div>
</body>
</html>