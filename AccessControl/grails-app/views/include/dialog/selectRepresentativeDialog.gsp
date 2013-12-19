<div id="selectRepresentativeDialog" title="<g:message code="selectRepresentativeLbl"/>" style="padding:20px 20px 20px 20px;display:none;">

    <div id="representativeSelectionFormDiv">
        <p><g:message code="delegationIntroLbl"/></p>

        <div style="font-size:1.2em;font-weight: bold;"><g:message code="anonymousDelegationLbl"/></div>
        <div><g:message code="anonymousDelegationMsg"/></div>
        <div style="margin: 0 0 0 40px;">
            <div id="moreDetailsMsgDiv" class="linkVS" onclick="seeMoreDetails()"></div>
            <div id="moreDetailsDiv"><g:message code='anonymousDelegationMoreMsg'/></div>
        </div>

        <div style="margin:40px 0 0 0;font-size:1.2em;font-weight: bold;"><g:message code="publicDelegationLbl"/></div>
        <div><g:message code="publicDelegationMsg"/></div>

        <div style="margin:40px 0 0 0;"><g:message code="selectRepresentationCheckboxMsg"/>:</div>
        <div class="checkBox" style="margin:0 0 0 100px;">
            <div style="margin:0 0 0 40px;display:block;">
                <input type="checkbox" id="anonymousDelegationCheckbox" onclick="setRepresentativeModeCheckBox(this)"/>
                <label for="anonymousDelegationCheckbox"><g:message code="anonymousDelegationCheckboxLbl"/></label>
            </div>
            <div style="margin:0 0 0 40px;display:block;">
                <input type="checkbox" id="publicDelegationCheckbox" onclick="setRepresentativeModeCheckBox(this)"/>
                <label for="publicDelegationCheckbox"><g:message code="publicDelegationCheckboxLbl"/></label>
            </div>
        </div>
    </div>

    <div id="confirmRepresentativeSelectionDiv" style="display: none;">
        <div id="representativeNameDiv"></div>
        <div id="delegationWeeksDiv"  style="margin:25px 0 25px 0;"></div>
        <g:message code="signWithDNIMsg"/>
    </div>

</div> 
<r:script>

var callerCallback
var weeksAnonymousDelegation
var representativeFullName

function showSelectRepresentativeDialog(callback, representativeName) {
	$("#selectRepresentativeDialog").dialog("open");
    $("#confirmRepresentativeSelectionDiv").hide()
    $("#representativeSelectionFormDiv").show()
    weeksAnonymousDelegation = null
    representativeFullName = representativeName
    seeMoreDetails(false)
	if(callback != null) callerCallback = callback
}

$("#selectRepresentativeDialog").dialog({
  width: 770, autoOpen: false, modal: true,
  buttons: [{id: "acceptButton",
            text:"<g:message code="acceptLbl"/>",
            icons: { primary: "ui-icon-check"},
            click:function() { submitSelectRepresentativeForm();  }}, {
                    id: "cancelButton", text:"<g:message code="cancelLbl"/>",
                    icons: { primary: "ui-icon-closethick"},
                    click:function() {proccessCancelRepresentativeSelection() }}],
  show: {effect:"fade", duration: 300},
  hide: {effect: "fade",duration: 300}
 });


function seeMoreDetails(seeDetails) {
    if(seeDetails == null) seeDetails = !$("#moreDetailsDiv").is(":visible")
    if(seeDetails) {
        $("#moreDetailsDiv").show()
        $("#moreDetailsMsgDiv").html("<g:message code="hideDetailsMsg"/>")
    } else {
        $("#moreDetailsDiv").hide()
        $("#moreDetailsMsgDiv").html("<g:message code="showAnoymousDelegationDetailsMsg"/>")
    }
}

function setRepresentativeModeCheckBox(representativeModeCheckBox) {
    console.log("setRepresentativeModeCheckBox: " + representativeModeCheckBox.id)
    if("anonymousDelegationCheckbox" == representativeModeCheckBox.id &&
        $("#publicDelegationCheckbox").is(':checked')) {
        document.getElementById("publicDelegationCheckbox").checked = false
    } else if("publicDelegationCheckbox" == representativeModeCheckBox.id &&
        $("#publicDelegationCheckbox").is(':checked')) {
        document.getElementById("anonymousDelegationCheckbox").checked = false
    }
}

function proccessCancelRepresentativeSelection() {
    if($("#confirmRepresentativeSelectionDiv").is(":visible")) {
        weeksAnonymousDelegation = null
        $("#representativeSelectionFormDiv").show()
        $("#confirmRepresentativeSelectionDiv").hide()
    } else {
        $("#selectRepresentativeDialog").dialog( "close" );
    }
}

var publicLbl = "<g:message code='publicLbl'/>";
var anonymousLbl = "<g:message code='anonymousLbl'/>";

function submitSelectRepresentativeForm() {
	console.log("submitSelectRepresentativeForm")
	var selectedRepresentationLbl
	var representativeOperation
	if(!$("#anonymousDelegationCheckbox").is(':checked') && !$("#publicDelegationCheckbox").is(':checked')) {
		showResultDialog("<g:message code='errorLbl'/>",
				"<g:message code='selectRepresentationCheckboxMsg'/>", showSelectRepresentativeDialog)
	} else {
        if($("#anonymousDelegationCheckbox").is(':checked')) {
            selectedRepresentationLbl = anonymousLbl
            representativeOperation = Operation.ANONYMOUS_REPRESENTATIVE_SELECTION
            if(weeksAnonymousDelegation == null || '' == weeksAnonymousDelegation) {
                showAnonymousRepresentativeDateRangeDialog()
                return
	        }
        } else {
            selectedRepresentationLbl = publicLbl
            representativeOperation = Operation.REPRESENTATIVE_SELECTION
        }
        if($("#confirmRepresentativeSelectionDiv").is(":visible")) {
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, representativeOperation)
            webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
            webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
            webAppMessage.signedContent = {operation:representativeOperation, representativeNif:"${representative.nif}",
                    representativeName:representativeFullName, weeksOperationActive:weeksAnonymousDelegation}
            webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStampVS', absolute:true)}"
            webAppMessage.receiverSignServiceURL = "${createLink(controller:'representative', action:'delegation', absolute:true)}"
            webAppMessage.signedMessageSubject = '<g:message code="representativeDelegationMsgSubject"/>'
            votingSystemClient.setMessageToSignatureClient(webAppMessage, selectRepresentativeCallback);
        } else {
            var representativeNameLbl = "${representativeName}";
            var msgTemplate = "<g:message code='selectRepresentativeConfirmMsg'/>";
            if(representativeOperation == Operation.ANONYMOUS_REPRESENTATIVE_SELECTION) {
                var weeksMsgTemplate = "<g:message code='numWeeksResultAnonymousDelegationMsg'/>";
                $("#delegationWeeksDiv").html(weeksMsgTemplate.format(anonymousLbl, weeksAnonymousDelegation))
                $("#delegationWeeksDiv").show()
            } else $("#delegationWeeksDiv").hide()
            $("#representativeNameDiv").html(msgTemplate.format(selectedRepresentationLbl, representativeNameLbl))
            $("#confirmRepresentativeSelectionDiv").show()
            $("#representativeSelectionFormDiv").hide()
        }
	}
}

</r:script>