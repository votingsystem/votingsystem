<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">
</head>
<body>

<div id="contentDiv" class="pageContenDiv" style="min-height: 1000px; margin:0px auto 0px auto;">
    <div style="margin:0px 30px 0px 30px;">
        <div class="row">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li><a href="${createLink(controller: 'userVS', action: 'index')}"><g:message code="uservsLbl"/></a></li>
                <li class="active"><g:message code="newVicketSourceLbl"/></li>
            </ol>
        </div>
        <h3>
            <div class="pageHeader text-center">
                <g:message code="newVicketSourceLbl"/>
            </div>
        </h3>

        <div class="text-left" style="margin:10px 0 10px 0;">
            <ul>
                <li><g:message code="systemAdminReservedOperationMsg"/></li>
                <li><g:message code="signatureRequiredMsg"/></li>
                <li><g:message code="newVicketSourceAdviceMsg2"/></li>
                <li><g:message code="newVicketSourceAdviceMsg3"/></li>
            </ul>
        </div>

        <form onsubmit="return submitForm()">
            <div style="position:relative; width:100%;">
                <votingsystem-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></votingsystem-texteditor>
            </div>

            <div class="form-group" style="margin:15px 0px 0px 0px;">
                <label><g:message code="pemCertLbl"/></label>
                <textarea id="pemCert" class="form-control" rows="8" required=""></textarea>
            </div>

            <div style="position:relative; margin:10px 10px 60px 0px;height:20px;">
                <div style="position:absolute; right:0;">
                    <button type="submit" class="btn btn-default">
                        <g:message code="newVicketSourceLbl"/> <i class="fa fa fa-check"></i>
                    </button>
                </div>
            </div>

        </form>
    </div>
</div>
</body>
</html>
<asset:script>
    var textEditor = document.querySelector('#textEditor')
    var appMessageJSON = null

    function submitForm() {
        if(!document.querySelector('#pemCert').validity.valid) {
            showMessageVS('<g:message code="fillAllFieldsERRORLbl"/>', '<g:message code="dataFormERRORLbl"/>')
            return false
        }
        if(textEditor.getData() == 0) {
            textEditor.classList.add("formFieldError");
            showMessageVS('<g:message code="emptyDocumentERRORMsg"/>', '<g:message code="dataFormERRORLbl"/>')
            return false
        }
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.VICKET_SOURCE_NEW)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.serviceURL = "${createLink( controller:'userVS', action:"newVicketSource", absolute:true)}"
        webAppMessage.signedMessageSubject = "<g:message code='newVicketSourceMsgSubject'/>"
        webAppMessage.signedContent = {info:textEditor.getData(),certChainPEM:document.querySelector("#pemCert").value,
            operation:Operation.VICKET_SOURCE_NEW}
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        var objectId = Math.random().toString(36).substring(7)
        window[objectId] = {setClientToolMessage: function(appMessage) {
                console.log("newVicketSourceCallback - message: " + appMessage);
                appMessageJSON = toJSON(appMessage)
                if(appMessageJSON != null) {
                    var caption = '<g:message code="newVicketSourceERRORCaption"/>'
                    var msg = appMessageJSON.message
                    statusCode = appMessageJSON.statusCode
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = '<g:message code="newVicketSourceOKCaption"/>'
                        var msgTemplate = '<g:message code='accessLinkMsg'/>';
                    }
                    showMessageVS(msg, caption)
                }
                window.scrollTo(0,0);
            }}
        webAppMessage.callerCallback = objectId
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        appMessageJSON = null
        return false
    }

    document.querySelector("#_votingsystemMessageDialog").addEventListener('message-accepted', function() {
        if(appMessageJSON != null) {
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                window.location.href = updateMenuLink(appMessageJSON.URL)
            }
        }
    })
</asset:script>