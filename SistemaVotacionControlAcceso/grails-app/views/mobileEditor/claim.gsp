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
			var numFields = 0

			CKEDITOR.on( 'instanceReady', function( ev ) {
				$("#contentDiv").fadeIn(500)
			});
			
		 	$(function() {
    		   $("#claimFieldMsgDialog").dialog({
   			   	  width: 450, autoOpen: false, modal: true,
   			      buttons: [{
   			        		text:"<g:message code="acceptLbl"/>",
   			               	icons: { primary: "ui-icon-check"},
   			             	click:function() {
   	   			             	$("#submitClaimFieldText").click() 	   	   			   				
	   	   			        }
   			           },  {
   			        		text:"<g:message code="cancelLbl"/>",
   			               	icons: { primary: "ui-icon-closethick"},
   			             	click:function() {
  	   			   				$(this).dialog( "close" );
  	   			       	 	}	
   			           }],
   			      show: { effect: "fade", duration: 500 },
   			      hide: { effect: "fade", duration: 500 }
   			    });
			    $("#dateFinish").datepicker(pickerOpts);

	    		$("#addClaimFieldButton").click(function () { 
	    			$("#claimFieldMsgDialog").dialog("open");
	    		});
	    		
			    $('#newFieldClaimForm').submit(function(event){
			        event.preventDefault();


					if(!document.getElementById('claimFieldText').validity.valid) {
						$("#claimFieldText").addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
							'<g:message code="emptyFieldMsg"/>')
						return false
					} else $("#claimFieldText").removeClass( "ui-state-error" );
			        
			        var newFieldTemplate = "${votingSystem.newField(isTemplate:true)}"
			        var newFieldHTML = newFieldTemplate.format($("#claimFieldText").val());
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
					$("#claimFieldMsgDialog").dialog( "close" );
			    });

			    $('#mainForm').submit(function(){
					var subject = $( "#subject" ),
			    		dateFinish = $( "#dateFinish" ),
			    		ckeditorDiv = $( "#ckeditor" ),
			        	allFields = $( [] ).add( subject ).add( dateFinish ).add(ckeditorDiv);
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

					setMessateToNativeClient(JSON.stringify(webAppMessage))
					return false
				 })
			    
			  });


        </script>
        <style>
			.button_base {
				display:table;
				position:relative;
			    margin: 0;
			    border: 0;
			    font-size: 1.2em;
			    padding: 0px 10px 0px 0px;
			    height: 30px;
			    text-align: center;
			    box-sizing: border-box;
			    -webkit-box-sizing: border-box;
			    -moz-box-sizing: border-box;
			    -webkit-user-select: none;
			    cursor: pointer; 
			    cursor: hand;
			    vertical-align: middle;
			    text-decoration: none;
			    background-color: #f1f5f8;
			    /*button_simple_rollover*/
			    color: #769ab5;
			    border: #769ab5 solid 1px;
			}
			
			.button_base .buttonImage { 
				display:table-cell;
				vertical-align:middle;
				margin:5px 0px 5px 5px;
			}
			
			.button_base .buttonText {
				display:table-cell;
				vertical-align:middle;
				padding:0px 0px 0px 5px;
			}
			
			.button_base:hover {
				color: #0066cc;
			    background-color: #dae7f1;
			}
  		</style>
</head>
<body>

<div id="contentDiv" style="display:none;">

	<form id="mainForm">
	
	<div style="margin:0px 0px 20px 0px">
		<label for="subject"><g:message code="subjectLbl"/></label>
    	<input type="text" name="subject" id="subject" style="width:300px" 
    		class="text ui-widget-content ui-corner-all" required oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
    			onchange="this.setCustomValidity('')" />
    			
    	<label for="dateFinish" style="margin:0px 0px 20px 0px"><g:message code="dateLbl"/></label>
		<input type="text" id="dateFinish" class="text ui-widget-content ui-corner-all" required readonly
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
	
	
	<div style="position:relative; height: 50px;">
		<div style="font-size: 0.9em; margin:10px 0 0 10px; width:60%;display: inline-block;"> 
			<input type="checkbox" id="multipleSignaturesCheckbox"><g:message code="multipleClaimsLbl"/><br>
			<input type="checkbox" id="allowBackupRequestCheckbox"><g:message code="allowBackupRequestLbl"/>
		</div>
		<div style="float:right; margin:15px 20px 0px 0px;">
			<votingSystem:simpleButton id="addClaimFieldButton" imgSrc="${resource(dir:'images',file:'info_16x16.png')}">
					<g:message code="addClaimFieldLbl"/>
			</votingSystem:simpleButton>
		</div>
	</div>

	<div id="fieldsBox"  style="display:none;">
		<fieldset class="fieldsBox" style="margin:20px 20px 0px 20px;">
			<legend id="fieldsLegend" style="font-size: 1.2em;"><g:message code="claimsFieldLegend"/></legend>
			<div id="fields"></div>
		</fieldset>
	</div>
	
	<div style="position:relative; margin:10px 10px 0px 0px; height:20px;">
		<div style="position:absolute; right:0;">
			<votingSystem:simpleButton isButton='true' id="addOptionButton" style="margin:0px 0px 0px 20px;"
				imgSrc="${resource(dir:'images',file:'accept_16x16.png')}">
					<g:message code="publishDocumentLbl"/>
			</votingSystem:simpleButton>	
		</div>	
	</div>	
		
	</form>
    
    <div id="claimFieldMsgDialog" title="<g:message code="addClaimFieldLbl"/>">
		<p style="text-align: center;">
	  		<g:message code="claimFieldDescriptionMsg"/>
	  	</p>
	  	<span><g:message code="addClaimFieldMsg"/></span>
   		<form id="newFieldClaimForm">
   			<input type="text" id="claimFieldText" style="width:400px" 
   				class="text ui-widget-content ui-corner-all" required/>
			<input id="submitClaimFieldText" type="submit" style="display:none;">
   		</form>
    </div> 

</div>
</body>
</html>