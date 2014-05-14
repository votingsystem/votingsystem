<!DOCTYPE html>
<html>
<head>
	<meta name="layout" content="main" />
   	<r:require modules="textEditorPC"/>
</head>
<body>

<div id="contentDiv" style="display:none; padding: 0px 20px 0px 20px;">

    <div class="pageHeader text-center"><h3><g:message code="publishClaimLbl"/></h3></div>

	<form id="mainForm" onsubmit="return submitForm(this);">

    <div class="form-inline">
        <div style="margin:0px 0px 20px 0px">
            <input type="text" name="subject" id="subject" style="width:400px;margin:0px 40px 0px 0px" required
                   title="<g:message code="subjectLbl"/>" class="form-control"
                   placeholder="<g:message code="subjectLbl"/>"
                   oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                   onchange="this.setCustomValidity('')" />

            <label>${message(code:'dateLbl')}</label>
            <votingSystem:datePicker id="dateFinish" title="${message(code:'dateLbl')}"
                                     placeholder="${message(code:'dateLbl')}"
                                     oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
                                     onchange="this.setCustomValidity('')"></votingSystem:datePicker>
        </div>
    </div>

    <div style="position:relative; width:100%;">
        <votingSystem:textEditor id="editorDiv" style="height:300px; width:100%;"/>
    </div>
	
	<div style="margin:0px 0px 30px 0px;">
		<div class="text-left" style="font-size: 0.9em; margin:10px 0 0 20px; width:60%;display: inline-block;">
			<input type="checkbox" id="multipleSignaturesCheckbox"><g:message code="multipleClaimsLbl"/><br>
			<input type="checkbox" id="allowBackupRequestCheckbox"><g:message code="allowBackupRequestLbl"/>
		</div>
	    <div style="float:right; margin:10px 20px 0px 0px;">
            <button id="addClaimFieldButton" type="button" class="btn btn-default" style="margin:0px 20px 0px 0px;"
                    onclick='showAddClaimFieldDialog(addClaimField)'><g:message code="addClaimFieldLbl"/> <i class="fa fa-plus"></i>
            </button>
	    </div>
	</div>

        <div id="fieldsDiv" class="fieldsBox" style="display:none;">
            <fieldset id="fieldsBox">
                <legend id="fieldsLegend" style="border: none;"><g:message code="claimsFieldLegend"/></legend>
                <div id="fields" style=""></div>
            </fieldset>
        </div>
	
	<div style='overflow:hidden;'>
		<div style="float:right; margin:0px 10px 0px 0px;">
            <button id="buttonAccept" type="submit" class="btn btn-default" style="margin:0px 20px 0px 0px;">
                <g:message code="publishDocumentLbl"/> <i class="fa fa fa-check"></i>
            </button>
		</div>	
	</div>

	</form>

    <div id="clientToolMsg" class="text-center" style="color:#870000; font-size: 1.2em;"><g:message code="clientToolNeededMsg"/>.
        <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/></div>

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
        if(isClientToolLoaded()) $("#clientToolMsg").css("display", "none")
     });

    function submitForm(form) {
        if(!validateForm()) return false;
        var eventVS = new EventVS();
        eventVS.subject = $("#subject").val();
        eventVS.content = getEditor_editorDivData();
        eventVS.dateFinish = document.getElementById("dateFinish").getValidatedDate().format();
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
        if($("#multipleSignaturesCheckbox").is(':checked') ) eventVS.cardinality = "MULTIPLE"
        else eventVS.cardinality = "EXCLUSIVE"
        eventVS.backupAvailable = $("#allowBackupRequestCheckbox").is(':checked')
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.CLAIM_PUBLISHING)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.signedContent = eventVS
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        webAppMessage.serviceURL = "${createLink( controller:'eventVSClaim', absolute:true)}"
        webAppMessage.signedMessageSubject = "${message(code:'publishClaimSubject')}"
        votingSystemClient.setMessageToSignatureClient(webAppMessage, publishDocumentCallback)
        return false
    }

    function addClaimField (claimFieldText) {
        if(claimFieldText == null) return
        var newFieldTemplate = $('#newFieldTemplate').html()
        var newFieldHTML = newFieldTemplate.format(claimFieldText);
        var $newField = $(newFieldHTML)
        $newField.find('#deleteFieldButton').click(function() {
                $(this).parent().fadeOut(1000,
                function() { $(this).parent().remove(); });
                numClaimFields--
                if(numClaimFields == 0) $("#fieldsDiv").fadeOut(500)
            }
        )
        $("#fieldsBox #fields").append($newField)
        if(numClaimFields == 0) $("#fieldsDiv").fadeIn(500)
        numClaimFields++
        $("#claimFieldText").val("");
    }

    function validateForm() {
        var subject = $("#subject"),
        dateFinish = document.getElementById("dateFinish").getValidatedDate(),
        editorDiv = $("#editorDiv"),
        allFields = $([]).add(subject).add(dateFinish).add(editorDiv);
        allFields.removeClass( "formFieldError" );

        if(!document.getElementById('subject').validity.valid) {
            subject.addClass( "formFieldError" );
            showResultDialog('<g:message code="dataFormERRORLbl"/>',  '<g:message code="emptyFieldMsg"/>')
            return false
        }

        if(dateFinish == null) {
            showResultDialog('<g:message code="dataFormERRORLbl"/>',  '<g:message code="emptyFieldMsg"/>')
            return false
        }

        if(dateFinish < new Date() ) {
            showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="dateInitERRORMsg"/>')
            return false
        }

        if(getEditor_editorDivData().length == 0) {
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
            var caption = '<g:message code="publishERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '<g:message code="publishOKCaption"/>'
                var msgTemplate = "<g:message code='documentLinkMsg'/>";
                msg = "<p><g:message code='publishOKMsg'/>.</p>" + msgTemplate.format(appMessageJSON.message);
                claimDocumentURL = appMessageJSON.message
            }
            showResultDialog(caption, msg, resultCallback)
        }
    }

    function resultCallback() {
        if(claimDocumentURL != null) window.location.href = claimDocumentURL
    }

</r:script>