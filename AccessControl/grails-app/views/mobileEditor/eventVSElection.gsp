<!DOCTYPE html>
<%@ page import="grails.converters.JSON" %>
<html>
<head>
	<r:require modules="textEditorMobile"/>
	<r:layoutResources />
</head>
<body>
<div class ="contentDiv">
	<form id="mainForm" onsubmit="return submitForm(this);">
	
	<div style="margin:0px 0px 10px 0px; display:inline;">
    	<input type="text" name="subject" id="subject" style="width:500px" required 
			title="<g:message code="subjectLbl"/>"
			placeholder="<g:message code="subjectLbl"/>" 
    		oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   			onchange="this.setCustomValidity('')" />
   </div>
   <div style="margin:0px 0px 10px 30px; display:inline;">
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

    <div style="position:relative; width:100%; margin:20px 0 0 0;">
        <votingSystem:textEditor id="editorDiv" type="mobile" style="height:300px; width:100%;"/>
    </div>

	<div style="margin: 15px auto 30px auto;">
		<div style="display:inline; margin:0 0 0 40px;">
			<span id="controlCenterLink" style="font-size:1.1em; color:#02227a; cursor: pointer; cursor: hand;">
				<g:message code="controlCenterLbl"/>
			</span>
		</div>
		<div style="display:inline;">    	
			<select id="controlCenterSelect" style="margin:0px 0px 0px 20px;display:inline;" required
					oninvalid="this.setCustomValidity('<g:message code="selectControlCenterLbl"/>')"
	   				onchange="this.setCustomValidity('')">
				<g:each status="i" in="${controlCenters}" var="controlCenterVS">
					<option value=""> --- <g:message code="selectControlCenterLbl"/> --- </option>
				  	<option value="${controlCenterVS.id}">${controlCenterVS.name}</option>
				</g:each>
			</select>		
		</div>
	</div>
	
	<fieldset id="fieldsBox" class="fieldsBox" style="display:none;">
		<legend id="fieldsLegend" style="font-size: 1.2em;"><g:message code="pollFieldLegend"/></legend>
		<div id="fields"></div>
	</fieldset>
	
	<div style="position:relative; margin:20px 0px 20px 30px;">
		<votingSystem:simpleButton id="addOptionButton" style="margin:0px 0px 0px 20px;" >
				<g:message code="addOptionLbl"/>
		</votingSystem:simpleButton>
		<votingSystem:simpleButton id="buttonAccept" isSubmitButton='true' style="position:absolute; right:190px; top:0px;"
			imgSrc="${resource(dir:'images/fatcow_16',file:'accept.png')}"><g:message code="publishDocumentLbl"/>
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
<g:include view="/include/dialog/resultDialog.gsp"/>
</html>
<r:script>
<g:applyCodec encodeAs="none">
    var numVoteOptions = 0
    var controlCenters = {};
    <g:each status="i" in="${controlCenters}" var="controlCenterVS">
        controlCenters["${controlCenterVS.id}"] = ${controlCenterVS as JSON}
    </g:each>

    function submitForm(form) {
        var subject = $("#subject"),
        dateBegin = $("#dateBegin"),
        dateFinish = $("#dateFinish")
        var pollOptions = getPollOptions()
        if(pollOptions == null) return false;

        var event = new EventVS();
        event.subject = subject.val();
        event.content = getEditor_editorDivData();
        event.dateBegin = dateBegin.datepicker('getDate').format();
        event.dateFinish = dateFinish.datepicker('getDate').format();
            event.controlCenterVS = controlCenters[$('#controlCenterSelect').val()]

        event.fieldsEventVS = pollOptions
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VOTING_PUBLISHING)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.signedContent = event
        webAppMessage.urlTimeStampServer = "${createLink(controller:'timeStampVS', absolute:true)}"
        webAppMessage.receiverSignServiceURL = "${createLink(controller:'eventVS', absolute:true)}"
        webAppMessage.signedMessageSubject = "${message(code:'publishVoteSubject')}"
        votingSystemClient.setMessageToSignatureClient(webAppMessage)
        return false
    }

    $(function() {

        $("#addOptionButton").click(function () {
            showAddVoteOptionDialog(addVoteOption)
        });


        $("#controlCenterLink").click(function () {
            showVoteControlCenterDialog(addControlCenterDialog)
        });

        function addControlCenterDialog () {

        }

        function addVoteOption (voteOptionText) {
            if(voteOptionText == null) return
            var newFieldTemplate = $('#newFieldTemplate').html()
            var newFieldHTML = newFieldTemplate.format(voteOptionText);
            var $newField = $(newFieldHTML)
            $newField.find('#deleteFieldButton').click(function() {
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
    });

    function getPollOptions() {
        var subject = $("#subject"),
        dateBegin = $("#dateBegin"),
        dateFinish = $("#dateFinish"),
        editorDiv = $("#editorDiv"),
        addOptionButton = $("#addOptionButton"),
        allFields = $( [] ).add( subject ).add(dateBegin).add(dateFinish).add(editorDiv);
        allFields.removeClass("formFieldError");

        if(!document.getElementById('subject').validity.valid) {
            subject.addClass( "formFieldError" );
            showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="emptyFieldMsg"/>')
            return null
        }

        if(dateBegin.datepicker("getDate") === null) {
            dateBegin.addClass( "formFieldError" );
            showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="emptyFieldMsg"/>')
            return null
        }

        if(dateFinish.datepicker("getDate") === null) {
            dateFinish.addClass( "formFieldError" );
            showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="emptyFieldMsg"/>')
            return null
        }

        if(dateFinish.datepicker("getDate") < new Date() ) {
            dateFinish.addClass( "formFieldError" );
            showResultDialog('<g:message code="dataFormERRORLbl"/>',
                '<g:message code="dateInitERRORMsg"/>')
            return null
        }

        if(dateBegin.datepicker("getDate") >
            dateFinish.datepicker("getDate")) {
            showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="dateRangeERRORMsg"/>')
            dateBegin.addClass("formFieldError");
            dateFinish.addClass("formFieldError");
            return null
        }

        if(getEditor_editorDivData().length == 0) {
            editorDiv.addClass( "formFieldError" );
            showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="emptyDocumentERRORMsg"/>')
            return null;
        }

        if(!document.getElementById('controlCenterSelect').validity.valid) {
            $("#controlCenterSelect").addClass( "formFieldError" );
            showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="selectControlCenterLbl"/>')
            return null
        } else $("#controlCenterSelect").removeClass("formFieldError");

        var pollOptions = new Array();
        $("#fieldsBox div").children().each(function(){
            var optionTxt = $(this).find('div.newFieldValueDiv').text();
            if(optionTxt.length > 0) {
                console.log("- adding option: " + optionTxt);
                var claimField = {content:optionTxt}
                pollOptions.push(claimField)
            }
        });
        console.log("- pollOptions.length: " + pollOptions.length);

        if(pollOptions.length < 2) { //two options at least
            showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="optionsMissingERRORMsg"/>')
            addOptionButton.addClass( "formFieldError" );
            return null
        }
        return pollOptions
    }
</g:applyCodec>		
</r:script>  
<r:layoutResources />