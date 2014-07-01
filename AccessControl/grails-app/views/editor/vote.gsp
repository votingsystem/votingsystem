<%@ page import="grails.converters.JSON" %>
<!DOCTYPE html>
<html>
<head>
	<meta name="layout" content="main" />
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">
</head>
<body>

<div id="contentDiv" style="display:none;padding: 0px 20px 0px 20px;">

    <div class="pageHeader text-center"><h3><g:message code="publishVoteLbl"/></h3></div>

	<form id="mainForm" onsubmit="return submitForm(this);">
	
	<div style="margin:0px 0px 20px 0px">
		<div class="text-left" style="display: block;">
	    	<input type="text" name="subject" id="subject" style="width:600px" required 
				title="<g:message code="subjectLbl"/>" class="form-control"
				placeholder="<g:message code="subjectLbl"/>"
	    		oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
	   			onchange="this.setCustomValidity('')" />
		</div>
		<div id="dateRangeDiv" class="text-left" style="margin:10px 0px 0px 0px;">
                <label>${message(code:'dateBeginLbl')}</label>
				<votingSystem:datePicker id="dateBegin" title="${message(code:'dateBeginLbl')}"
   					oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
   					onchange="this.setCustomValidity('')"></votingSystem:datePicker>

                <label style="margin:0px 0px 0px 30px;">${message(code:'dateFinishLbl')}</label>
				<votingSystem:datePicker id="dateFinish" title="${message(code:'dateFinishLbl')}"
					style="width:150px; margin: 0px 0px 0px 30px;"
   					oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
   					onchange="this.setCustomValidity('')"></votingSystem:datePicker>
		
		</div>
	</div>

    <div style="position:relative; width:100%;">
        <votingsystem-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></votingsystem-texteditor>
    </div>

    <div class="container-fluid">
        <div style="margin: 15px auto 30px auto;" class="row">
            <div class="col-md-2">
                <label style="">
                    <span id="controlCenterLink" onclick="showVoteControlCenterDialog()" style="font-size:1.1em; color:#02227a;
                    cursor: pointer; cursor: hand;"> <g:message code="controlCenterLbl"/>  <i class="fa fa-info-circle"></i>
                    </span>
                </label>
            </div>
            <div class="col-md-4" style="margin: 0px 0px 0px 30px;">
                <select id="controlCenterSelect" style="" required
                        oninvalid="this.setCustomValidity('<g:message code="selectControlCenterLbl"/>')"
                        onchange="this.setCustomValidity('')" class="form-control">
                    <option value=""> --- <g:message code="selectControlCenterLbl"/> --- </option>
                    <g:each status="i" in="${controlCenters}" var="controlCenterVS">
                        <option value="${controlCenterVS.id}">${controlCenterVS.name}</option>
                    </g:each>
                </select>
            </div>
            <div class="col-md-1 col-md-offset-2 text-right">
                <button id="addOptionButton" type="button" class="btn btn-default" style=""
                        onclick='showAddVoteOptionDialog(addVoteOption)'><g:message code="addOptionLbl"/> <i class="fa fa-plus"></i>
                </button>
            </div>
        </div>
    </div>


    <div id="fieldsDiv" class="fieldsBox" style="display:none;">
        <fieldset id="fieldsBox">
            <legend id="fieldsLegend" style="border: none;"><g:message code="pollFieldLegend"/></legend>
            <div id="fields" style=""></div>
        </fieldset>
    </div>
	
	<div style="position:relative; margin:0px 10px 30px 30px;" class="row">
        <button id="buttonAccept" type="submit" class="btn btn-default" style="position:absolute; right:10px; top:0px;">
            <g:message code="publishDocumentLbl"/> <i class="fa fa fa-check"></i>
        </button>
	</div>
		 
	</form>

    <div id="clientToolMsg" class="text-center" style="color:#6c0404; font-size: 1.2em;"><g:message code="clientToolNeededMsg"/>.
        <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/></div>


</div>
<g:include view="/include/dialog/addControlCenterDialog.gsp"/>
<g:include view="/include/dialog/addVoteOptionDialog.gsp"/>

	<div id="newFieldTemplate" style="display:none;">
		<g:render template="/template/newField" model="[isTemplate:'true']"/>
	</div> 
	
</body>
</html>
<asset:script>
<g:applyCodec encodeAs="none">
    var textEditor = document.querySelector('#textEditor')
    var numVoteOptions = 0
    var controlCenters = {};

    <g:each status="i" in="${controlCenters}" var="controlCenterVS">
        controlCenters["${controlCenterVS.id}"] = ${controlCenterVS as JSON}
    </g:each>

    $(function() {
        if(isClientToolLoaded()) $("#clientToolMsg").css("display", "none")
    });

    function addVoteOption (voteOptionText) {
        if(voteOptionText == null) return
        var newFieldTemplate = $('#newFieldTemplate').html()
        var newFieldHTML = newFieldTemplate.format(voteOptionText);
        var $newField = $(newFieldHTML)
        $newField.find('#deleteFieldButton').click(function() {
            $(this).parent().fadeOut(1000, function() { $(this).parent().remove(); });
            numVoteOptions--
            if(numVoteOptions == 0) { $("#fieldsDiv").fadeOut(500) }
        })
        $("#fieldsBox #fields").append($newField)
        if(numVoteOptions == 0) $("#fieldsDiv").fadeIn(500)
        numVoteOptions++
    }

    function submitForm(form) {
        var subject = $("#subject"),
        dateBegin = document.getElementById("dateBegin").getValidatedDate(),
        dateFinish = document.getElementById("dateFinish").getValidatedDate()
        var pollOptions = getPollOptions()
        if(pollOptions == null) return false;
        var eventVS = new EventVS();
        eventVS.subject = subject.val();
        eventVS.content = textEditor.getData();
        eventVS.dateBegin = dateBegin.format();
        eventVS.dateFinish = dateFinish.format();
        eventVS.controlCenter = controlCenters[$('#controlCenterSelect').val()]
        eventVS.fieldsEventVS = pollOptions
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.VOTING_PUBLISHING)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.signedContent = eventVS
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        webAppMessage.serviceURL = "${createLink(controller:'eventVSElection', absolute:true)}"
        webAppMessage.signedMessageSubject = "<g:message code="publishVoteSubject"/>"
        webAppMessage.callerCallback = 'publishDocumentCallback'
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage)
        return false
    }

    function getPollOptions() {
        var subject = $("#subject"),
        dateRangeDiv = $("#dateRangeDiv"),
        dateBegin = document.getElementById("dateBegin").getValidatedDate(),
        dateFinish = document.getElementById("dateFinish").getValidatedDate(),
        addOptionButton = $("#addOptionButton"),
        allFields = $( [] ).add( subject );
        allFields.removeClass("formFieldError");
        allFields.removeClass("has-error");

        if(!document.getElementById('subject').validity.valid) {
            subject.addClass("formFieldError");
            showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="emptyFieldMsg"/>')
            return null
        }

        if(dateBegin == null) {
            showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="emptyFieldMsg"/>')
            return null
        }

        if(dateFinish == null) {
            showResultDialog('<g:message code="dataFormERRORLbl"/>',  '<g:message code="emptyFieldMsg"/>')
            return null
        }

        if(dateFinish < new Date() ) {
            dateRangeDiv.addClass("has-error");
            showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="dateInitERRORMsg"/>')
            return null
        }

        if(dateBegin > dateFinish) {
            showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="dateRangeERRORMsg"/>')
            dateRangeDiv.addClass("has-error");
            return null
        }

        if(textEditor.getData() == 0) {
            textEditor.classList.add("formFieldError");
            showResultDialog('<g:message code="dataFormERRORLbl"/>','<g:message code="emptyDocumentERRORMsg"/>')
            return null;
        }

        if(!document.getElementById('controlCenterSelect').validity.valid) {
            $("#controlCenterSelect").addClass( "formFieldError" );
            showResultDialog('<g:message code="dataFormERRORLbl"/>',
            '<g:message code="selectControlCenterLbl"/>')
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

    var electionDocumentURL

    function publishDocumentCallback(appMessage) {
        console.log("publishDocumentCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        electionDocumentURL = null
        if(appMessageJSON != null) {
            var caption = '<g:message code="publishERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '<g:message code="publishOKCaption"/>'
                var msgTemplate = "<g:message code='documentLinkMsg'/>";
                msg = "<p><g:message code='publishOKMsg'/>.</p>" +  msgTemplate.format(appMessageJSON.message);
                electionDocumentURL = appMessageJSON.message
            }
            showResultDialog(caption, msg, resultCallback)
        }
    }

    function resultCallback() {
        if(electionDocumentURL != null) window.location.href = electionDocumentURL
    }

</g:applyCodec>
</asset:script>