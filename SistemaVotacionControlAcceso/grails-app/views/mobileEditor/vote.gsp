<%@ page import="grails.converters.JSON" %>
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
			var controlCenters = {};

			<g:each status="i" in="${controlCenters}" var="controlCenter">
				controlCenters["${controlCenter.id}"] = ${controlCenter as JSON}
			</g:each>

			CKEDITOR.on( 'instanceReady', function( ev ) {
				$("#contentDiv").fadeIn(500)
			});
			
		 	$(function() {	
		 			 	
    		   $("#controlCenterDialog").dialog({
   			   	  width: 600, autoOpen: false, modal: true,
   			      buttons: [{
   			        		text:"<g:message code="acceptLbl"/>",
   			               	icons: { primary: "ui-icon-check"},
   			             	click:function() {
  	   	   			   				$("#submitControlCenter").click() 	   	   			   				
  	   	   			        	}
   			           }, {
   			        		text:"<g:message code="cancelLbl"/>",
   			               	icons: { primary: "ui-icon-closethick"},
   			             	click:function() {
  	   			   				$(this).dialog( "close" );
  	   			       	 	}	
   			           }],
   			      show: {effect:"fade", duration: 700},
   			      hide: {effect: "fade",duration: 700}
   			    });

    		   $("#newOptionDialog").dialog({
    			   	  width: 450, autoOpen: false, modal: true,
    			      buttons: [{
    			        		text:"<g:message code="acceptLbl"/>",
    			               	icons: { primary: "ui-icon-check"},
    			             	click:function() {
   	   	   			   				$("#submitOption").click() 	   	   			   				
   	   	   			        	}
    			           }, {
    			        		text:"<g:message code="cancelLbl"/>",
    			               	icons: { primary: "ui-icon-closethick"},
    			             	click:function() {
   	   			   				$(this).dialog( "close" );
   	   			       	 	}	
    			           }],
    			      show: { effect: "fade", duration: 100 },
    			      hide: { effect: "fade", duration: 100 }
    			    });

	    		$("#addOptionButton").click(function () { 
	    			$("#newOptionDialog").dialog("open");
	    		});

			    $("#dateFinish").datepicker(pickerOpts);
			    $("#dateBegin").datepicker(pickerOpts);

			    $('#newOptionForm').submit(function(event){
			        event.preventDefault();

					if(!document.getElementById('newOptionText').validity.valid) {
						$("#newOptionText").addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
							'<g:message code="emptyFieldMsg"/>')
						return false
					} else $("#newOptionText").removeClass( "ui-state-error" );
			        
			        var newFieldTemplate = "${votingSystem.newField(isTemplate:true)}"
			        var newFieldHTML = newFieldTemplate.format($("#newOptionText").val());
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
	   				$("#newOptionText").val("");
					$("#newOptionDialog").dialog( "close" );
					$("#addOptionButton").removeClass("ui-state-error");
			    });

			    $('#newControlCenter').submit(function(event){
			        event.preventDefault();
			        
					if(!document.getElementById('controlCenterURL').validity.valid) {
						$("#controlCenterURL").addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
							'<g:message code="emptyFieldMsg"/>')
							return false
					}
			        
			        var controlCenterURL = $('#controlCenterURL').val()
		          	var suffix = "/"
					if((controlCenterURL.indexOf(suffix, controlCenterURL.length - suffix.length) == -1)) {
						controlCenterURL = controlCenterURL + "/"
					}
			        controlCenterURL = controlCenterURL + "infoServidor"
		  			var jqxhr = $.getJSON(controlCenterURL, function() {});
			        $('#controlCenterURL').val("")
		  			jqxhr.done(function(data) {
	  			        //var dataStr = JSON.stringify(data);  
		  			    //console.log( "second success - dataStr: " + dataStr);
		      			if(DataType.CENTRO_CONTROL == data.tipoServidor) { 
		      				associateControlCenter(data.serverURL)
			      		} else {
			      			console.log( "Server type wrong -> " + data.tipoServidor);
							showResultDialog('<g:message code="errorLbl"/>',
								'<g:message code="controlCenterURLERRORMsg"/>') 
				      	}
		  			  }).fail(function(data) {
		  			  	console.log("error asssociating Control Center");
						showResultDialog('<g:message code="errorLbl"/>',
							'<g:message code="controlCenterURLERRORMsg"/>') 
		  			  }).always(function() {});
					$("#controlCenterDialog").dialog( "close" );
			    });

			    $('#mainForm').submit(function(event){			    	
					var subject = $("#subject"),
						dateBegin = $("#dateBegin"),
			    		dateFinish = $("#dateFinish"),
			    		ckeditorDiv = $("#ckeditor"),
			    		addOptionButton = $("#addOptionButton"), 
			        	allFields = $( [] ).add( subject ).add(dateBegin).add(dateFinish).add(ckeditorDiv);
					allFields.removeClass("ui-state-error");

					if(!document.getElementById('subject').validity.valid) {
						subject.addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
							'<g:message code="emptyFieldMsg"/>')
						return false
					}

					if(dateBegin.datepicker("getDate") == null) {
						dateBegin.addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
							'<g:message code="emptyFieldMsg"/>')
						return false
					}

					if(dateFinish.datepicker("getDate") == null) {
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
										
					if(dateBegin.datepicker("getDate") > 
						dateFinish.datepicker("getDate")) {
						showResultDialog('<g:message code="dataFormERRORLbl"/>',
								'<g:message code="dateRangeERRORMsg"/>') 
						dateBegin.addClass("ui-state-error");
						dateFinish.addClass("ui-state-error");
						return false
					}
		        	
			        var editor = CKEDITOR.instances.editor1;
					if(editor.getData().length == 0) {
						ckeditorDiv.addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
								'<g:message code="emptyDocumentERRORMsg"/>')
						return false;
					}  

					if(!document.getElementById('controlCenterSelect').validity.valid) {
						$("#controlCenterSelect").addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
							'<g:message code="selectControlCenterLbl"/>')
						return false
					} else $("#controlCenterSelect").removeClass("ui-state-error");

					 

			    	var event = new Evento();
			    	event.asunto = subject.val();
			    	event.contenido = editor.getData();
			    	event.fechaInicio = dateBegin.val() + " 00:00:00";
			    	event.fechaFin = dateFinish.val() + " 00:00:00";
	  			  	event.centroControl = controlCenters[$('#controlCenterSelect').val()]

					var pollOptions = new Array();
					$("#fieldsBox div").children().each(function(){
						var optionTxt = $(this).find('div.newFieldValueDiv').text();
						if(optionTxt.length > 0) {
							console.log("- adding option: " + optionTxt);
							var claimField = {contenido:optionTxt}
							pollOptions.push(claimField)
						}
					});
					console.log("- pollOptions.length: " + pollOptions.length);
					
					if(pollOptions.length < 2) { //two options at least 
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
								'<g:message code="optionsMissingERRORMsg"/>')
						addOptionButton.addClass( "ui-state-error" );
						return false
					}

			    	event.opciones = pollOptions
			    	var webAppMessage = new WebAppMessage(
					    	StatusCode.SC_PROCESANDO, 
					    	Operation.PUBLICACION_VOTACION_SMIME)
			    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
		    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
					webAppMessage.contenidoFirma = event
					webAppMessage.urlTimeStampServer = "${createLink(controller:'timeStamp', absolute:true)}"
					webAppMessage.urlEnvioDocumento = "${createLink(controller:'eventoVotacion', absolute:true)}"
					webAppMessage.asuntoMensajeFirmado = "${message(code:'publishVoteSubject')}"
					webAppMessage.respuestaConRecibo = true

					setMessateToNativeClient(JSON.stringify(webAppMessage))
					return false
				 })

	    		$("#controlCenterLink").click(function () { 
	    			$("#controlCenterDialog").dialog("open");
	    		});
			    
			  });


		  	function associateControlCenter(controlCenterURL){ 
		  		console.log( "associateControlCenter - controlCenterURL: " + controlCenterURL);
		    	var webAppMessage = new WebAppMessage(
				    	StatusCode.SC_PROCESANDO, 
				    	Operation.ASOCIAR_CENTRO_CONTROL)

		    	var signatureContent = {
					serverURL:controlCenterURL,
					operation:Operation.ASOCIAR_CENTRO_CONTROL}

		    	
		    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
	    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
				webAppMessage.contenidoFirma = signatureContent
				webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStamp', absolute:true)}"
				webAppMessage.urlEnvioDocumento = "${createLink( controller:'subscripcion', absolute:true)}"
				setMessateToNativeClient(JSON.stringify(webAppMessage))
			} 	

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
    		class="text ui-widget-content ui-corner-all" required 
    		oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   			onchange="this.setCustomValidity('')" />
    			
    			
    	<label for="dateBegin" style="margin:0px 0px 20px 20px"><g:message code="dateBeginLbl"/></label>
		<input type="text" id="dateBegin" class="text ui-widget-content ui-corner-all" style="width:110px;" required readonly/>    			
    			

    	<label for="dateFinish" style="margin:0px 0px 20px 20px"><g:message code="dateFinishLbl"/></label>
		<input type="text" id="dateFinish" class="text ui-widget-content ui-corner-all" style="width:110px;" required readonly/>
	</div>
	 

	<div id="ckeditor">
		<script>
			CKEDITOR.appendTo( 'ckeditor', {
                toolbar: [[ 'Bold', 'Italic', '-', 'NumberedList', 'BulletedList', '-', 'Link', 'Unlink' ],
					[ 'FontSize', 'TextColor', 'BGColor' ]]});
		</script>
	</div>

	
	<div style="margin: 15px auto 30px auto; width:80%;">
		<div style="display:inline;">
			<img src="${resource(dir:'images',file:'info_16x16.png')}"></img>
			<span id="controlCenterLink" style="font-size:1.1em; color:#02227a; cursor: pointer; cursor: hand;">
				<g:message code="controlCenterLbl"/>
			</span>
		</div>
		<div style="display:inline;">    	
			<select id="controlCenterSelect" style="margin:0px 0px 0px 40px;display:inline;" required
					oninvalid="this.setCustomValidity('<g:message code="selectControlCenterLbl"/>')"
	   				onchange="this.setCustomValidity('')">
				<g:each status="i" in="${controlCenters}" var="controlCenter">
					<option value=""> --- <g:message code="selectControlCenterLbl"/> --- </option>
				  	<option value="${controlCenter.id}">${controlCenter.nombre}</option>
				</g:each>
			</select>		
		</div>
	</div>
	
	<fieldset id="fieldsBox" class="fieldsBox" style="display:none;">
		<legend id="fieldsLegend" style="font-size: 1.2em;"><g:message code="pollFieldLegend"/></legend>
		<div id="fields"></div>
	</fieldset>
	
	<div style="position:relative; margin:20px 0px 20px 0px;">
		<votingSystem:simpleButton id="addOptionButton" style="margin:0px 0px 0px 20px;"
			imgSrc="${resource(dir:'images',file:'poll_16x16.png')}">
				<g:message code="addOptionLbl"/>
		</votingSystem:simpleButton>
		<votingSystem:simpleButton id="buttonAccept" isButton='true' style="position:absolute; right:10px; top:0px;"
			imgSrc="${resource(dir:'images',file:'accept_16x16.png')}">
				<g:message code="publishDocumentLbl"/>
		</votingSystem:simpleButton>
	</div>
		 
	</form>
    
    <div id="controlCenterDialog" title="<g:message code="addClaimFieldLbl"/>">
		<p style="text-align: center;">
			<g:message code="controlCenterDescriptionMsg"/>
	  	</p>
	  	<div>
    		<span><g:message code="controlCenterURLLbl"/></span>
    		<form id="newControlCenter">
    			<input type="text" id="controlCenterURL" style="width:500px; margin:0px auto 0px auto;"
    				class="text ui-widget-content ui-corner-all" required/>
   				<input id="submitControlCenter" type="submit" style="display:none;">
    		</form>

	  	</div>
    </div> 
	
    <div id="newOptionDialog" title="<g:message code="addOptionLbl"/>" style="width:80%;margin:20px auto 0px auto;">
    	<form id="newOptionForm">
			<g:message code="pollOptionContentMsg"/>
   			<input type="text" id="newOptionText" style="width:350px; margin:10px auto 0px auto;"
   				class="text ui-widget-content ui-corner-all" required/>
  				<input id="submitOption" type="submit" style="display:none;">
   		</form>
    </div> 

</div>
</body>
</html>