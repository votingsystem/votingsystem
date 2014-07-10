<html>
<head>
    <meta name="layout" content="main" />
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">
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
                <li><g:message code="signatureRequiredMsg"/></li>
                <li><g:message code="newGroupVSAdviceMsg2"/></li>
                <li><g:message code="newGroupVSAdviceMsg3"/></li>
            </ul>
        </div>

        <div style="position:relative; width:100%;">
            <votingsystem-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></votingsystem-texteditor>
        </div>

        <div style="position:relative; margin:10px 10px 60px 0px;height:20px;">
            <div style="position:absolute; right:0;">
                <button onclick="submitForm()" class="btn btn-default">
                    <g:message code="saveChangesLbl"/> <i class="fa fa fa-check"></i>
                </button>
            </div>
        </div>
    </div>


</div>

</html>
<asset:script>
    var textEditor = document.querySelector('#textEditor')

    document.addEventListener('polymer-ready', function() {
        document.querySelector(".pageHeader").innerHTML = "<g:message code="editingGroupMsgTitle"/>".format('${groupvsMap.name}')
        document.querySelector('#textEditor').setData('${raw(groupvsMap?.description)}')
    });

    function submitForm(){
        if(textEditor.getData() == 0) {
            textEditor.classList.add("formFieldError");
            showMessageVS('<g:message code="emptyDocumentERRORMsg"/>', '<g:message code="dataFormERRORLbl"/>')
            return
        }
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.VICKET_GROUP_EDIT)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.serviceURL = "${createLink( controller:'groupVS', action:"edit", absolute:true)}/${groupvsMap.id}"
        webAppMessage.signedMessageSubject = "<g:message code='newGroupVSMsgSubject'/>"
        webAppMessage.signedContent = {groupvsInfo:textEditor.getData(), groupvsName:'${groupvsMap.name}',
            id:'${groupvsMap.id}', operation:Operation.VICKET_GROUP_NEW}
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        webAppMessage.callerCallback = getFnName(editGroupVSCallback)
        //console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

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
            showMessageVS(msg, caption)
        }
        window.scrollTo(0,0);
    }

</asset:script>