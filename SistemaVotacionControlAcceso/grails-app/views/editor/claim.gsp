<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
        <meta name="layout" content="main" />
        <script src="${resource(dir:'ckeditor',file:'ckeditor.js')}"></script>
        <script type="text/javascript">
			var numFields = 0

			CKEDITOR.on( 'instanceReady', function( ev ) {
				$("#contentDiv").fadeIn(500)
			});
			
		 	$(function() {
    		   $("#claimFieldMsgDialog").dialog({
   			   	  width: 450, autoOpen: false, modal: true,
   			      buttons: [
   			           {
   			        		text:"<g:message code="acceptLbl"/>",
   			               	icons: { primary: "ui-icon-check"},
   			             	click:function() {
  	   	   			   				$("#submitClaimFieldText").click() 	   	   			   				
  	   	   			        	}
   			           },
   			           {
   			        		text:"<g:message code="cancelLbl"/>",
   			               	icons: { primary: "ui-icon-closethick"},
   			             	click:function() {
  	   			   				$(this).dialog( "close" );
  	   			       	 	}	
   			           }
   			       ],
   			      show: { effect: "fade", duration: 500 },
   			      hide: { effect: "fade", duration: 500 }
   			    });

			    $("#dateFinish").datepicker(pickerOpts);

	    		$("#addClaimFieldLink").click(function () { 
	    			$("#claimFieldMsgDialog").dialog("open");
	    		});
	    		
			    $('#newFieldClaimForm').submit(function(event){
			        event.preventDefault();
			        var newFieldTemplate = "${votingSystem.newField(isTemplate:true)}"
			        var newFieldHTML = newFieldTemplate.format($("#claimFieldText").val());
			        var $newField = $(newFieldHTML)
   					$newField.find('div#deleteFieldButton').click(function() {
							$(this).parent().fadeOut(500, 
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
					$("#claimFieldMsgDialog").dialog( "close" );
			    });

			    $('#mainForm').submit(function(){
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
			    	
					var claimFields = new Array();
					$("#fieldsBox").children().each(function(){
						var claimFieldTxt = $(this).find('div.newFieldValueDiv').text();
						var claimField = {contenido:claimFieldTxt}
						claimFields.push(claimField)
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
			    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
		    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
					webAppMessage.contenidoFirma = event
					webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStamp', absolute:true)}"
					webAppMessage.urlEnvioDocumento = "${createLink( controller:'eventoReclamacion', absolute:true)}"
					webAppMessage.asuntoMensajeFirmado = "${message(code:'publishClaimSubject')}"
					webAppMessage.respuestaConRecibo = true

					votingSystemApplet.setMessageToSignatureClient(JSON.stringify(webAppMessage));
					return false
				 })
			    
			  });


				function setMessageFromSignatureClient(appMessage) {
					console.log("setMessageFromSignatureClient - message from native client: " + appMessage);
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
								msg = "<p><g:message code='publishOKMsg'/>.</p>" + 
									msgTemplate.format(appMessageJSON.mensaje);
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
			<g:message code="publishClaimLbl"/>
		</p>
	</div>

	<form id="mainForm">
	
	<div style="margin:0px 0px 20px 0px">
		<label for="subject"><g:message code="subjectLbl"/></label>
    	<input type="text" name="subject" id="subject" style="width:400px;margin:0px 40px 0px 0px" required 
    			oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
    			onchange="this.setCustomValidity('')" />

    	<label for="dateFinish"><g:message code="dateLbl"/></label>
		<input type="text" id="dateFinish" required readonly
   				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   				onchange="this.setCustomValidity('')"/>
	</div>
	

	<div id="ckeditor">
		<script>
			CKEDITOR.appendTo( 'ckeditor', {
                toolbar: [[ 'Bold', 'Italic', '-', 'NumberedList', 'BulletedList', '-', 'Link', 'Unlink' ],
					[ 'FontSize', 'TextColor', 'BGColor' ]]});
		</script>
	</div>
	
	
	<div style="margin:0px 0px 30px 0px;">
		<div style="font-size: 0.9em; margin:10px 0 0 10px; width:60%;display: inline-block;"> 
			<input type="checkbox" id="multipleSignaturesCheckbox"><g:message code="multipleClaimsLbl"/><br>
			<input type="checkbox" id="allowBackupRequestCheckbox"><g:message code="allowBackupRequestLbl"/>
		</div>
	    <div style="float:right; margin:10px 20px 0px 0px;">
			<votingSystem:simpleButton id="addClaimFieldLink" style="margin:0px 20px 0px 0px;"
				imgSrc="${resource(dir:'images',file:'info_16x16.png')}">
					<g:message code="addClaimFieldLbl"/>
			</votingSystem:simpleButton>
	    </div>
	</div>


	<fieldset id="fieldsBox" class="fieldsBox" style="display:none;">
		<legend id="fieldsLegend"><g:message code="claimsFieldLegend"/></legend>
		<div id="fields"></div>
	</fieldset>
	
	
	<div style='overflow:hidden;'>
		<div style="float:right; margin:0px 10px 0px 0px;">
			<votingSystem:simpleButton id="buttonAccept" isButton='true' 
				imgSrc="${resource(dir:'images',file:'accept_16x16.png')}" style="margin:0px 20px 0px 0px;">
					<g:message code="publishDocumentLbl"/>
			</votingSystem:simpleButton>
		</div>	
	</div>
	
	
	</form>

	<div class="userAdvert" style="">
		<ul>
			<li><g:message code="onlySignedDocumentsMsg"/></li>
			<li><g:message code="dniConnectedMsg"/></li>
			<li><g:message code="appletAdvertMsg"/></li>
			<li><g:message code="javaInstallAdvertMsg"/></li>
		</ul>
	</div>	
    
    <div id="claimFieldMsgDialog" title="<g:message code="addClaimFieldLbl"/>">
		<p style="text-align: center;">
	  		<g:message code="claimFieldDescriptionMsg"/>
	  	</p>
	  	<span><g:message code="addClaimFieldMsg"/></span>
   		<form id="newFieldClaimForm">
   			<input type="text" id="claimFieldText" style="width:400px" 
   				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   				onchange="this.setCustomValidity('')"
   				class="text ui-widget-content ui-corner-all" required/>
  				<input id="submitClaimFieldText" type="submit" style="display:none;">
   		</form>
    </div> 
</div>

</body>
</html>