<!DOCTYPE html>
<html>
<head>
	<meta name="layout" content="main" />
   	<r:require modules="textEditorPC"/>
</head>
<body>

<div id="contentDiv" style="display:none; padding: 0px 20px 0px 20px;">

	<div class="publishPageTitle">
		<p style="text-align:center; width: 100%;">
			<g:message code="publishClaimLbl"/>
		</p>
	</div>

	<form id="mainForm">
	
	<div style="margin:0px 0px 20px 0px">
    	<input type="text" name="subject" id="subject" style="width:400px;margin:0px 40px 0px 0px" required
				title="<g:message code="subjectLbl"/>"
				placeholder="<g:message code="subjectLbl"/>" 
    			oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
    			onchange="this.setCustomValidity('')" />

		<votingSystem:datePicker id="dateFinish" title="${message(code:'dateLbl')}"
						placeholder="${message(code:'dateLbl')}"
	   					oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
	   					onchange="this.setCustomValidity('')"></votingSystem:datePicker>
	</div>

    <div style="position:relative; width:100%;">
        <votingSystem:textEditor id="editorDiv" style="height:300px; width:100%;"/>
    </div>
	
	<div style="margin:0px 0px 30px 0px;">
		<div style="font-size: 0.9em; margin:10px 0 0 10px; width:60%;display: inline-block;"> 
			<input type="checkbox" id="multipleSignaturesCheckbox"><g:message code="multipleClaimsLbl"/><br>
			<input type="checkbox" id="allowBackupRequestCheckbox"><g:message code="allowBackupRequestLbl"/>
		</div>
	    <div style="float:right; margin:10px 20px 0px 0px;">
			<votingSystem:simpleButton id="addClaimFieldButton" style="margin:0px 20px 0px 0px;">
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
			<votingSystem:simpleButton id="buttonAccept" isSubmitButton='true'
				imgSrc="${resource(dir:'images/fatcow_16',file:'accept.png')}" style="margin:0px 20px 0px 0px;">
					<g:message code="publishDocumentLbl"/>
			</votingSystem:simpleButton>
		</div>	
	</div>

	</form>

	<g:render template="/template/signatureMechanismAdvert"  model="${[advices:[message(code:"onlySignedDocumentsMsg")]]}"/>

</div>

<g:include view="/include/dialog/addClaimFieldDialog.gsp"/>
	
	<div id="newFieldTemplate" style="display:none;">
		<g:render template="/template/newField"/>
	</div> 
</body>
</html>
<r:script>
			var numClaimFields = 0
			
		 	$(function() {
		 		showEditor_editorDiv()

	    		$("#addClaimFieldButton").click(function () {
	    			hideEditor_editorDiv()
	    			showAddClaimFieldDialog(addClaimField)
	    		});
	    		
	    		function addClaimField (claimFieldText) {
	    			showEditor_editorDiv()
					if(claimFieldText == null) return
			        var newFieldTemplate = $('#newFieldTemplate').html()
			        var newFieldHTML = newFieldTemplate.format(claimFieldText);
			        var $newField = $(newFieldHTML)
   					$newField.find('div#deleteFieldButton').click(function() {
							$(this).parent().fadeOut(1000, 
   							function() { $(this).parent().remove(); });
							numClaimFields--
							if(numClaimFields == 0) {
			   					$("#fieldsBox").fadeOut(1000)
				   			}
    					}
					)
	   				$("#fieldsBox #fields").append($newField)
	   				if(numClaimFields == 0) {
	   					$("#fieldsBox").fadeIn(1000)
		   			}
	   				numClaimFields++
	   				$("#claimFieldText").val("");
		    	}

			    $('#mainForm').submit(function(event){
			    	event.preventDefault();
				    hideEditor_editorDiv()
					if(!validateForm()) {
						showEditor_editorDiv();
						return;
					} 					
			    	var eventVS = new EventVS();
			    	eventVS.subject = $("#subject").val();
			    	eventVS.content = editorDivContent.trim();
			    	eventVS.dateFinish = $("#dateFinish").datepicker('getDate').format();
			    	
					var claimFields = new Array();
					$("#fieldsBox").children().each(function(){
						var claimField = $(this).find('div.newFieldValueDiv');
						var claimFieldTxt = claimField.text();
						if(claimFieldTxt.length > 0) {
							var claimField = {content:claimFieldTxt}
							claimFields.push(claimField)
						}
					});

			    	eventVS.fieldsEventVS = claimFields

					if($("#multipleSignaturesCheckbox").is(':checked') ) {
						eventVS.cardinality = "MULTIPLE"
					} else {
						eventVS.cardinality = "EXCLUSIVE"
					}
					eventVS.backupAvailable = $("#allowBackupRequestCheckbox").is(':checked')

			    	var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.CLAIM_PUBLISHING)
			    	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
		    		webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
					webAppMessage.signedContent = eventVS
					webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStampVS', absolute:true)}"
					webAppMessage.receiverSignServiceURL = "${createLink( controller:'eventVSClaim', absolute:true)}"
					webAppMessage.signedMessageSubject = "${message(code:'publishClaimSubject')}"
					webAppMessage.isResponseWithReceipt = true
					votingSystemClient.setMessageToSignatureClient(webAppMessage, publishDocumentCallback)
					return false
				 })
			    
			  });

			function validateForm() {
				var subject = $("#subject"),
	    		dateFinish = $("#dateFinish"),
	    		editorDiv = $("#editorDiv"),
	        	allFields = $([]).add(subject).add(dateFinish).add(editorDiv);
				allFields.removeClass( "formFieldError" );
	
				if(!document.getElementById('subject').validity.valid) {
					subject.addClass( "formFieldError" );
					showResultDialog('<g:message code="dataFormERRORLbl"/>',  '<g:message code="emptyFieldMsg"/>')
					return false
				}
	
				if(!document.getElementById('dateFinish').validity.valid) {
					dateFinish.addClass( "formFieldError" );
					showResultDialog('<g:message code="dataFormERRORLbl"/>',  '<g:message code="emptyFieldMsg"/>')
					return false
				}
				
				if(dateFinish.datepicker("getDate") < new Date() ) {
					dateFinish.addClass( "formFieldError" );
					showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="dateInitERRORMsg"/>')
					return false
				}
	
				if(editorDivContent.trim() == 0) {
					editorDiv.addClass( "formFieldError" );
					showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="emptyDocumentERRORMsg"/>')
					return false;
				}  
				return true
			}

			var claimDocumentURL

			function publishDocumentCallback(appMessage) {
				console.log("publishDocumentCallback - message from native client: " + appMessage);
				var appMessageJSON = toJSON(appMessage)
				claimDocumentURL = null
				if(appMessageJSON != null) {
					$("#workingWithAppletDialog" ).dialog("close");
					var caption = '<g:message code="publishERRORCaption"/>'
					var msg = appMessageJSON.message
					if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
						caption = '<g:message code="publishOKCaption"/>'
				    	var msgTemplate = "<g:message code='documentLinkMsg'/>";
						msg = "<p><g:message code='publishOKMsg'/>.</p>" + 
							msgTemplate.format(appMessageJSON.message);
					    claimDocumentURL = appMessageJSON.message
					} else showEditor_editorDiv()
					showResultDialog(caption, msg, resultCallback)
				}
			}

            function resultCallback() {
                if(claimDocumentURL != null) window.location.href = claimDocumentURL
            }

</r:script>