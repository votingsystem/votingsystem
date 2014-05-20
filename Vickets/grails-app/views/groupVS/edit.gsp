<html>
<head>
        <meta name="layout" content="main" />
   		<r:require modules="textEditor"/>
</head>
<body>

<div id="contentDiv" class="pageContenDiv" style="min-height: 1000px; margin:0px auto 0px auto;">
    <div style="margin:0px 30px 0px 30px;">
        <div class="row">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li><a href="${createLink(controller: 'groupVS', action: 'index')}"><g:message code="groupvsLbl"/></a></li>
                <li class="active"><g:message code="editGroupVSLbl"/></li>
            </ol>
        </div>
        <h3>
            <div class="pageHeader text-center"></div>
        </h3>

        <div class="text-left" style="margin:10px 0 10px 0;">
            <ul>
                <li><g:message code="newGroupVSAdviceMsg1"/></li>
                <li><g:message code="newGroupVSAdviceMsg2"/></li>
                <li><g:message code="newGroupVSAdviceMsg3"/></li>
            </ul>
        </div>

        <form id="mainForm">
            <div style="position:relative; width:100%;">
                <votingSystem:textEditor id="editorDiv" style="height:300px; width:100%;"/>
            </div>

            <div style="position:relative; margin:10px 10px 60px 0px;height:20px;">
                <div style="position:absolute; right:0;">
                    <button type="submit" class="btn btn-default">
                        <g:message code="saveChangesLbl"/> <i class="fa fa fa-check"></i>
                    </button>
                </div>
            </div>

        </form>
    </div>


</div>

<g:include view="/include/dialog/resultDialog.gsp"/>
</body>
</html>
<r:script>

    $(function() {
        showGroupData()

        $('#mainForm').submit(function(event){
            event.preventDefault();
            var editorDiv = $("#editorDiv")
            var editorContent = getEditor_editorDivData()
            if(editorContent.length == 0) {
                editorDiv.addClass( "formFieldError" );
                showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="emptyDocumentERRORMsg"/>')
                return
            }
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.VICKET_GROUP_EDIT)
            webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
            webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
            webAppMessage.serviceURL = "${createLink( controller:'groupVS', action:"edit", absolute:true)}/${groupvsMap.id}"
            webAppMessage.signedMessageSubject = "<g:message code='newGroupVSMsgSubject'/>"
            webAppMessage.signedContent = {groupvsInfo:getEditor_editorDivData(), groupvsName:'${groupvsMap.name}',
                id:'${groupvsMap.id}', operation:Operation.VICKET_GROUP_NEW}
            webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
            webAppMessage.callerCallback = getFnName(editGroupVSCallback)
            //console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        });

      });

    function editGroupVSCallback(appMessage) {
        console.log("editGroupVSCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
            var caption = '<g:message code="editGroupERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '<g:message code="editGroupOKCaption"/>'
                var msgTemplate = '<g:message code='accessLinkMsg'/>';
                msg = msg + '</br></br>' + msgTemplate.format(appMessageJSON.URL + "?menu=admin")
            }
            showResultDialog(caption, msg)
        }
        window.scrollTo(0,0);
    }

    var editGroupHeaderTemplate = "<g:message code="editingGroupMsgTitle"/>"

    function showGroupData() {
        console.log("showGroupData")
        var editRepresentativeHeader = editGroupHeaderTemplate.format('${groupvsMap.name}')
        $(".pageHeader").append(editRepresentativeHeader)
        setDataEditor_editorDiv('${raw(groupvsMap?.description)}')
    }
</r:script>