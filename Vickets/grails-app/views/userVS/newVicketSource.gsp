<html>
<head>
    <meta name="layout" content="main" />
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
                <li><g:message code="newVicketSourceAdviceMsg1"/></li>
                <li><g:message code="newVicketSourceAdviceMsg2"/></li>
                <li><g:message code="newVicketSourceAdviceMsg3"/></li>
            </ul>
        </div>

        <form id="mainForm">
            <div style="position:relative; width:100%;">
                <votingSystem:textEditor id="editorDiv" style="height:300px; width:100%;"/>
            </div>

            <div class="form-group" style="margin:15px 0px 0px 0px;">
                <label><g:message code="vicketSourcePEMCertMsg"/></label>
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
<g:include view="/include/dialog/resultDialog.gsp"/>
</body>
</html>
<asset:script>

    $(function() {
        $('#mainForm').submit(function(event){
            event.preventDefault();
            if(!document.getElementById('pemCert').validity.valid) {
                showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="fillAllFieldsERRORLbl"/>')
                return
            }
            var editorDiv = $("#editorDiv")
            var editorContent = getEditor_editorDivData()
            if(editorContent.length == 0) {
                editorDiv.addClass( "formFieldError" );
                showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="emptyDocumentERRORMsg"/>')
                return
            }

            console.log("newGroup - sendSignature ")
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.VICKET_SOURCE_NEW)
            webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
            webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
            webAppMessage.serviceURL = "${createLink( controller:'userVS', action:"newVicketSource", absolute:true)}"
            webAppMessage.signedMessageSubject = "<g:message code='newVicketSourceMsgSubject'/>"
            webAppMessage.signedContent = {info:getEditor_editorDivData(),certChainPEM:$("#pemCert").val(),
                        operation:Operation.VICKET_SOURCE_NEW}
            webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
            webAppMessage.callerCallback = 'newVicketSourceCallback'
            //console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        });

      });

    var vicketSourceURL = null

    function resultOKCallback() {
        window.location.href = vicketSourceURL + "?menu=superadmin"
    }

    function newVicketSourceCallback(appMessage) {
        console.log("newGroupVSCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        var callBackResult = null
        if(appMessageJSON != null) {
            var caption = '<g:message code="newVicketSourceERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '<g:message code="newVicketSourceOKCaption"/>'
                var msgTemplate = '<g:message code='accessLinkMsg'/>';
                callBackResult = resultOKCallback
                vicketSourceURL = appMessageJSON.URL
            }
            showResultDialog(caption, msg, callBackResult)
        }
        window.scrollTo(0,0);
    }

</asset:script>