<html>
<head>
        <meta name="layout" content="main" />
       	<r:require modules="textEditorPC"/>
</head>
<body>
<div class="row" style="">
    <ol class="breadcrumbVS pull-left">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li><a href="${createLink(controller: 'representative', action:'main')}"><g:message code="representativesPageLbl"/></a></li>
        <li class="active"><g:message code="editRepresentativeLbl"/></li>
    </ol>
</div>

<div id="contentDiv" style="visibility:hidden;">

    <h3><div class="pageHeader text-center"></div></h3>
	
	<div class="text-left" style="margin:15px 0 0 0;">
		<ul>
			<li><g:message code="newRepresentativeAdviceMsg1"/></li>
			<li><g:message code="newRepresentativeAdviceMsg2"/></li>
			<li><g:message code="newRepresentativeAdviceMsg3"/></li>
			<li><g:message code="newRepresentativeAdviceMsg4"/></li>
		</ul>
	</div>	
	
	<form id="mainForm">
        <div style="position:relative; width:100%;">
            <votingSystem:textEditor id="editorDiv" style="height:300px; width:100%;"/>
        </div>

        <div style="position:relative; margin:10px 10px 60px 0px;height:20px;">
            <div style="position:absolute; right:0;">
                <button type="submit" class="btn btn-default" style="margin:0px 20px 0px 0px;">
                    <g:message code="acceptLbl"/> <i class="fa fa fa-check"></i>
                </button>
            </div>
        </div>
	</form>

    <div id="clientToolMsg" class="text-center" style="color:#870000; font-size: 1.2em;"><g:message code="clientToolNeededMsg"/>.
        <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/></div>

</div>

<g:include view="/include/dialog/editRepresentativeDialog.gsp"/>

</body>
</html>
<r:script>
    $(function() {

        $("#editRepresentativeDialog").dialog("open");

    	var editorDiv = $("#editorDiv")
        $('#mainForm').submit(function(event){
            event.preventDefault();
            var editorContent = getEditor_editorDivData()
            if(editorContent.length == 0) {
                editorDiv.addClass( "formFieldError" );
                showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="emptyDocumentERRORMsg"/>')

                return false;
            } else editorDiv.removeClass( "formFieldError" );
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.NEW_REPRESENTATIVE)
            webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
            webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
            webAppMessage.signedContent = {representativeInfo:editorContent, operation:Operation.REPRESENTATIVE_DATA}
            webAppMessage.serviceURL = "${createLink( controller:'representative', absolute:true)}"
            webAppMessage.signedMessageSubject = '<g:message code="representativeDataLbl"/>'
            webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
            votingSystemClient.setMessageToSignatureClient(webAppMessage, editRepresentativeCallback);
            return false
        });

        if(isClientToolLoaded()) $("#clientToolMsg").css("display", "none")
      });

    var editRepresentativeHeaderTemplate = "<g:message code="editingRepresentativeMsgTitle"/>"

    function showRepresentativeData(representativeDataJSON) {
        console.log("showRepresentativeData: " + representativeDataJSON)
        var editRepresentativeHeader = editRepresentativeHeaderTemplate.format(representativeDataJSON.fullName)
        $(".pageHeader").append(editRepresentativeHeader)
        setDataEditor_editorDiv(representativeDataJSON.info)
        $("#contentDiv").css( "visibility", "visible" );
    }


    function editRepresentativeCallback(appMessage) {
        console.log("editRepresentativeCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
            var caption = '<g:message code="operationERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "<g:message code='operationOKCaption'/>"
            } else if (ResponseVS.SC_CANCELLED== appMessageJSON.statusCode) {
                caption = "<g:message code='operationCANCELLEDLbl'/>"
            }
            showResultDialog(caption, msg)
        }
    }

</r:script>