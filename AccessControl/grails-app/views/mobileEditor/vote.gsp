<!DOCTYPE html>
<%@ page import="grails.converters.JSON" %>
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
		<votingSystem:datePicker id="dateBegin" title="${message(code:'dateBeginLbl')}"
			 style="width:150px;"
			 placeholder="${message(code:'dateBeginLbl')}"
			 oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
			 onchange="this.setCustomValidity('')"></votingSystem:datePicker>   

		<votingSystem:datePicker id="dateFinish" title="${message(code:'dateFinishLbl')}"
			 style="width:150px; margin:0px 0px 0px 20px;"
			 placeholder="${message(code:'dateFinishLbl')}"
			 oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
			 onchange="this.setCustomValidity('')"></votingSystem:datePicker>   
	</div>

	<votingSystem:textEditorMobile id="editorDiv"/>

	<div style="margin: 15px auto 30px auto;">
		<div style="display:inline;">
			<img src="${resource(dir:'images',file:'info_16x16.png')}"></img>
			<span id="controlCenterLink" style="font-size:1.1em; color:#02227a; cursor: pointer; cursor: hand;">
				<g:message code="controlCenterLbl"/>
			</span>
		</div>
		<div style="display:inline;">    	
			<select id="controlCenterSelect" style="margin:0px 0px 0px 20px;display:inline;" required
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
</div>
<div style="clear: both;margin:0px 0px 30px 0px;">&nbsp;</div>
<g:include view="/include/dialog/addControlCenterDialog.gsp"/>
<g:include view="/include/dialog/addVoteOptionDialog.gsp"/>

	<div id="newFieldTemplate" style="display:none;">
		<g:render template="/template/newField" model="[isTemplate:'true']"/>
	</div> 
	
</body>
</html>
<r:script>
<g:applyCodec encodeAs="none">
		var numVoteOptions = 0
		var controlCenters = {};
		<g:each status="i" in="${controlCenters}" var="controlCenter">
			controlCenters["${controlCenter.id}"] = ${controlCenter as JSON}
		</g:each>
		
		$(function() {
			showEditor()

    		$("#addOptionButton").click(function () { 
    			hideEditor() 
    			showAddVoteOptionDialog(addVoteOption)
    		});
    		

	  		$("#controlCenterLink").click(function () {
	  			hideEditor()  
	  			showVoteControlCenterDialog(addControlCenterDialog)
			});

	  		function addControlCenterDialog () {
	  			showEditor()
		  	}
	  		
			function addVoteOption (voteOptionText) {
				showEditor()
				if(voteOptionText == null) return
		        var newFieldTemplate = $('#newFieldTemplate').html()
	            var newFieldHTML = newFieldTemplate.format(voteOptionText);
	            var $newField = $(newFieldHTML)
		      	$newField.find('div#deleteFieldButton').click(function() {
	      			$(this).parent().fadeOut(1000, 
      						function() { $(this).parent().remove(); });
		      		numVoteOptions--
		      		if(numVoteOptions == 0) {
       					$("#fieldsBox").fadeOut(1000)
        			}
		      	})
		      	$("#fieldsBox #fields").append($newField)
		      	if(numVoteOptions == 0) {
		      		$("#fieldsBox").fadeIn(1000)
		      	}
		      	numVoteOptions++
			}


			$('#mainForm').submit(function(event){	
		    	event.preventDefault();
			    hideEditor()		    	
				var subject = $("#subject"),
				dateBegin = $("#dateBegin"),
				dateFinish = $("#dateFinish")
				var pollOptions = getPollOptions()
				if(pollOptions == null) {
					showEditor();
					return false;
				}
				
			  	var event = new Evento();
			  	event.asunto = subject.val();
			  	event.contenido = htmlEditorContent;
			  	event.fechaInicio = dateBegin.datepicker('getDate').format();
			  	event.fechaFin = dateFinish.datepicker('getDate').format();
				  	event.centroControl = controlCenters[$('#controlCenterSelect').val()]
		
			  	event.opciones = pollOptions
			  	var webAppMessage = new WebAppMessage(
			    	StatusCode.SC_PROCESSING, 
			    	Operation.VOTING_PUBLISHING)
				webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.VotingSystem.serverName}"
				webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
				webAppMessage.contenidoFirma = event
				webAppMessage.urlTimeStampServer = "${createLink(controller:'timeStamp', absolute:true)}"
				webAppMessage.urlEnvioDocumento = "${createLink(controller:'eventoVotacion', absolute:true)}"
				webAppMessage.asuntoMensajeFirmado = "${message(code:'publishVoteSubject')}"
				webAppMessage.respuestaConRecibo = true
			
				votingSystemClient.setMessageToSignatureClient(webAppMessage)
				return false
			})


		});
		function getPollOptions() {	    	
			var subject = $("#subject"),
			dateBegin = $("#dateBegin"),
			dateFinish = $("#dateFinish"),
			editorDiv = $("#editorDiv"),
			addOptionButton = $("#addOptionButton"), 
			allFields = $( [] ).add( subject ).add(dateBegin).add(dateFinish).add(editorDiv);
			allFields.removeClass("ui-state-error");
			
			if(!document.getElementById('subject').validity.valid) {
				subject.addClass( "ui-state-error" );
				showResultDialog('<g:message code="dataFormERRORLbl"/>', 
					'<g:message code="emptyFieldMsg"/>')
				return null
			}
			
			if(dateBegin.datepicker("getDate") === null) {
				dateBegin.addClass( "ui-state-error" );
				showResultDialog('<g:message code="dataFormERRORLbl"/>', 
					'<g:message code="emptyFieldMsg"/>')
				return null
			}
			
			if(dateFinish.datepicker("getDate") === null) {
				dateFinish.addClass( "ui-state-error" );
				showResultDialog('<g:message code="dataFormERRORLbl"/>', 
					'<g:message code="emptyFieldMsg"/>')
				return null
			}
			
			if(dateFinish.datepicker("getDate") < new Date() ) {
				dateFinish.addClass( "ui-state-error" );
				showResultDialog('<g:message code="dataFormERRORLbl"/>', 
					'<g:message code="dateInitERRORMsg"/>')
				return null
			}
								
			if(dateBegin.datepicker("getDate") > 
				dateFinish.datepicker("getDate")) {
				showResultDialog('<g:message code="dataFormERRORLbl"/>',
						'<g:message code="dateRangeERRORMsg"/>') 
				dateBegin.addClass("ui-state-error");
				dateFinish.addClass("ui-state-error");
				return null
			}
			     	
			if(htmlEditorContent.trim() == 0) {
				editorDiv.addClass( "ui-state-error" );
				showResultDialog('<g:message code="dataFormERRORLbl"/>', 
						'<g:message code="emptyDocumentERRORMsg"/>')
				return null;
			}  
			
			if(!document.getElementById('controlCenterSelect').validity.valid) {
				$("#controlCenterSelect").addClass( "ui-state-error" );
				showResultDialog('<g:message code="dataFormERRORLbl"/>', 
					'<g:message code="selectControlCenterLbl"/>')
				return null
			} else $("#controlCenterSelect").removeClass("ui-state-error");

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
				return null
			}
			return pollOptions
		}
</g:applyCodec>		
</r:script>  
<r:layoutResources />