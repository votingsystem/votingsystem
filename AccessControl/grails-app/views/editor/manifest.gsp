<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">
</head>
<body>

<div id="contentDiv" style="display:none; padding: 0px 20px 0px 20px;">

    <div class="pageHeader"><h3><g:message code="publishManifestLbl"/></h3></div>

    <form id="mainForm" onsubmit="return submitForm(this);">
        <div class="form-inline">
            <div style="margin:0px 0px 20px 0px">
                <input type="text" name="subject" id="subject" style="width:400px"  required
                       title="<g:message code="subjectLbl"/>" class="form-control"
                       placeholder="<g:message code="subjectLbl"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')" />
                <label style="margin:0 0 0 30px;">${message(code:'dateLbl')}</label>
                <votingSystem:datePicker id="dateFinish" style="margin:0px 0px 0px 35px;"
                                         title="${message(code:'dateLbl')}"
                                         oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
                                         onchange="this.setCustomValidity('')"></votingSystem:datePicker>
            </div>
        </div>

        <div style="position:relative; width:100%;">
            <votingsystem-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></votingsystem-texteditor>
        </div>

        <div style='overflow:hidden;'>
            <div style="float:right; margin:20px 10px 0px 0px;">
                <button id="buttonAccept" type="submit" class="btn btn-default" style="margin:0px 20px 0px 0px;">
                    <g:message code="publishDocumentLbl"/> <i class="fa fa fa-check"></i>
                </button>
            </div>
        </div>

    </form>

    <div id="clientToolMsg" class="" style="color:#6c0404; font-size: 1.2em;"><g:message code="clientToolNeededMsg"/>.
        <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/></div>

</div>

</body>
</html>
<asset:script>
    var textEditor = document.querySelector('#textEditor')


    function submitForm(form) {
        var subject = $( "#subject" ),
        dateFinish = document.getElementById("dateFinish").getValidatedDate(),
        allFields = $( [] ).add( subject );
        allFields.removeClass( "formFieldError" );


        if(dateFinish < new Date() ) {
            showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="dateInitERRORMsg"/>')
            return false
        }

        if(textEditor.getData() == 0) {
            textEditor.classList.add("formFieldError");
            showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="emptyDocumentERRORMsg"/>')
            return false
        }

        var eventVS = new EventVS();
        eventVS.subject = subject.val();
        eventVS.content = textEditor.getData();
        eventVS.dateFinish = dateFinish.format();

        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.MANIFEST_PUBLISHING)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.signedContent = eventVS
        webAppMessage.serviceURL = "${createLink( controller:'eventVSManifest', absolute:true)}"
        webAppMessage.signedMessageSubject = '<g:message code="publishManifestSubject"/>'
        webAppMessage.callerCallback = 'publishDocumentCallback'
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        return false
    }

    var manifestDocumentURL

    function publishDocumentCallback(appMessage) {
        console.log("publishDocumentCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        manifestDocumentURL = null
        if(appMessageJSON != null) {
            var caption = '<g:message code="publishERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '<g:message code="publishOKCaption"/>'
                var msgTemplate = "<g:message code='documentLinkMsg'/>";
                msg = "<p><g:message code='publishOKMsg'/>.</p>" +
                    msgTemplate.format(appMessageJSON.serviceURL);
                manifestDocumentURL = appMessageJSON.message
            }
            showResultDialog(caption, msg, resultCallback)
        }
    }

    function resultCallback() {
        if(manifestDocumentURL != null) window.location.href = manifestDocumentURL
    }

</asset:script>