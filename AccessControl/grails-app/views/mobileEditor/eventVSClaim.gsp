<!DOCTYPE html>
<html>
<head>
	<r:require modules="textEditorMobile"/>
	<r:layoutResources />
</head>
<body>
<div class ="contentDiv">
	<form id="mainForm" onsubmit="return submitForm(this);">
	
	<div style="margin:0px 0px 10px 0px; display: inline;">
    	<input type="text" name="subject" id="subject" style="width:500px" required 
				title="<g:message code="subjectLbl"/>"
				placeholder="<g:message code="subjectLbl"/>" 
    			oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
    			onchange="this.setCustomValidity('')" />
   </div>
   <div style="margin:0px 0px 10px 20px; display: inline;">
		<votingSystem:datePicker id="dateFinish" title="${message(code:'dateLbl')}"
			 placeholder="${message(code:'dateLbl')}"
			 oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
			 onchange="this.setCustomValidity('')"></votingSystem:datePicker>   				
	</div>

    <div style="position:relative; width:100% ;margin:20px 0 0 0;">
        <votingSystem:textEditor id="editorDiv" type="mobile" style="height:300px; width:100%;"/>
    </div>

	<div style="position:relative; height: 50px;display: block;">
		<div style="font-size: 0.9em; margin:10px 0 0 10px;"> 
			<input type="checkbox" id="multipleSignaturesCheckbox"><g:message code="multipleClaimsLbl"/><br>
			<input type="checkbox" id="allowBackupRequestCheckbox"><g:message code="allowBackupRequestLbl"/>
		</div>
	</div>

	<div id="fieldsBox"  style="display:none;">
		<fieldset class="fieldsBox" style="margin:20px 20px 0px 20px;">
			<legend id="fieldsLegend" style="font-size: 1.2em;"><g:message code="claimsFieldLegend"/></legend>
			<div id="fields"></div>
		</fieldset>
	</div>
	
	<div style="position:relative; margin:20px 0px 0px 0px; height:20px;">
		<div style="float:left;">
			<votingSystem:simpleButton id="addClaimFieldButton">
                <g:message code="addClaimFieldLbl"/>
			</votingSystem:simpleButton>
		</div>
		<div style="float:right; margin:0px 190px 0px 0px;">
			<votingSystem:simpleButton isSubmitButton='true' id="addOptionButton"
				imgSrc="${resource(dir:'images/icon_16',file:'accept.png')}"> <g:message code="publishDocumentLbl"/>
			</votingSystem:simpleButton>	
		</div>	
	</div>	
		
	</form>

</div>
<div style="clear: both;margin:0px 0px 30px 0px;">&nbsp;</div>
<g:include view="/include/dialog/addClaimFieldDialog.gsp"/>
<g:include view="/include/dialog/resultDialog.gsp"/>

	<div id="newFieldTemplate" style="display:none;">
		<g:render template="/template/newField" model="[isTemplate:'true']"/>
	</div> 
	
</body>
</html>
<r:script>
    var numFields = 0

    function addClaimField (claimFieldText) {
        if(claimFieldText == null) return
        var newFieldTemplate = $('#newFieldTemplate').html()
        var newFieldHTML = newFieldTemplate.format(claimFieldText);
        var $newField = $(newFieldHTML)
        $newField.find('#deleteFieldButton').click(function() {
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
    }

    function submitForm(form) {
        if(!isValidForm()) return false;
        var eventVS = new EventVS();
        eventVS.subject = $("#subject").val();
        eventVS.content = getEditor_editorDivData();
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
            eventVS.cardinality = "EXCLUSIVE"
        } else {
            eventVS.cardinality = "MULTIPLE"
        }
        eventVS.backupAvailable = $("#allowBackupRequestCheckbox").is(':checked')

        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.CLAIM_PUBLISHING)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.signedContent = eventVS
        webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStampVS', absolute:true)}"
        webAppMessage.receiverSignServiceURL = "${createLink( controller:'eventVSClaim', absolute:true)}"
        webAppMessage.signedMessageSubject = "${message(code:'publishClaimSubject')}"
        votingSystemClient.setMessageToSignatureClient(webAppMessage)
        return false
    }

    $(function() {
        $("#addClaimFieldButton").click(function () {
            showAddClaimFieldDialog(addClaimField)
        });
    });

    function isValidForm() {
        var subject = $("#subject"),
        dateFinish = $("#dateFinish"),
        editorDiv = $("#editorDiv"),
        allFields = $([]).add(subject).add(dateFinish).add(editorDiv);
        allFields.removeClass( "formFieldError" );

        if(!document.getElementById('subject').validity.valid) {
            subject.addClass( "formFieldError" );
            showResultDialog('<g:message code="dataFormERRORLbl"/>','<g:message code="emptyFieldMsg"/>')
            return false
        }

        if(!document.getElementById('dateFinish').validity.valid) {
            dateFinish.addClass( "formFieldError" );
            showResultDialog('<g:message code="dataFormERRORLbl"/>','<g:message code="emptyFieldMsg"/>')
            return false
        }

        if(dateFinish.datepicker("getDate") < new Date() ) {
            dateFinish.addClass( "formFieldError" );
            showResultDialog('<g:message code="dataFormERRORLbl"/>','<g:message code="dateInitERRORMsg"/>')
            return false
        }

        if(getEditor_editorDivData().length == 0) {
            editorDiv.addClass( "formFieldError" );
            showResultDialog('<g:message code="dataFormERRORLbl"/>','<g:message code="emptyDocumentERRORMsg"/>')
            return false;
        }
        return true
    }
		 	
</r:script>
<r:layoutResources />