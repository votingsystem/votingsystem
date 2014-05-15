<html>
<head>
        <meta name="layout" content="main" />
   		<r:require modules="textEditorPC"/>
</head>
<body>
<div id="contentDiv" style="margin: 0px auto 0px auto; max-width: 1200px;">
    <div style="margin:0px 30px 0px 30px;">
        <div class="row" style="">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li><a href="${createLink(controller: 'representative', action:'main')}"><g:message code="representativesPageLbl"/></a></li>
                <li class="active"><g:message code="newRepresentativeLbl"/></li>
            </ol>
        </div>

        <div class="pageHeader text-center"><h3><g:message code="newRepresentativePageTitle"/></h3></div>

        <div class="text-left" style="margin:15px 0 0 0;">
            <ul>
                <li><g:message code="newRepresentativeAdviceMsg2"/></li>
                <li><g:message code="newRepresentativeAdviceMsg3"/></li>
                <li><g:message code="newRepresentativeAdviceMsg4"/></li>
            </ul>
        </div>
        <form id="mainForm">
            <div style="position:relative; width:100%;">
                <votingSystem:textEditor id="editorDiv" style="height:300px; width:100%;"/>
            </div>

            <div class="" style="margin:10px 10px 10px 10px;">
                <div class="" style="display: inline">
                    <a href="#" id="selectImageButton" type="button" class="btn btn-default" onclick="selectImage()">
                        <g:message code="selectImageLbl"/> <i class="fa fa-file-image-o"></i>
                    </a>
                </div>
                <div class="" style="display: inline; float:right;">
                    <button type="submit" class="btn btn-default">
                        <g:message code="newRepresentativeLbl"/> <i class="fa fa fa-check"></i>
                    </button>
                </div>
            </div>
            <div id="selectedImagePath" style="margin:10px 10px 40px 10px;"></div>

        </form>
        <div id="clientToolMsg" class="text-center" style="color:#870000; font-size: 1.2em;"><g:message code="clientToolNeededMsg"/>.
            <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/></div>
    </div>
</div>
</body>
</html>
<r:script>

    var selectedImagePath = null

    $(function() {

        $('#mainForm').submit(function(event){
            event.preventDefault();
            var editorDiv = $("#editorDiv")
            var editorContent = getEditor_editorDivData()
            if(editorContent.length == 0) {
                editorDiv.addClass( "formFieldError" );
                showResultDialog('<g:message code="dataFormERRORLbl"/>',
                    '<g:message code="emptyDocumentERRORMsg"/>')
                return
            } else editorDiv.removeClass( "formFieldError" );
            if(selectedImagePath == null) {
                $("#selectImageButton").addClass("btn-danger");
                showResultDialog('<g:message code="dataFormERRORLbl"/>',
                    '<g:message code="missingImageERRORMsg"/>')
                return
            }
            $("#selectImageButton").removeClass("btn-danger");
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.NEW_REPRESENTATIVE)
            webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
            webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
            webAppMessage.filePath = selectedImagePath
            webAppMessage.signedContent = {representativeInfo:getEditor_editorDivData(), operation:Operation.REPRESENTATIVE_DATA}
            webAppMessage.serviceURL = "${createLink( controller:'representative', absolute:true)}"
            webAppMessage.signedMessageSubject = '<g:message code="representativeDataLbl"/>'
            webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
            webAppMessage.callerCallback = getFnName(newRepresentativeCallback)
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        });

        if(isClientToolLoaded()) $("#clientToolMsg").css("display", "none")
      });

    function selectImage() {
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.SELECT_IMAGE)
        webAppMessage.callerCallback = getFnName(selectImageCallback)
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function selectImageCallback(appMessage) {
        console.log("selectImageCallback - appMessage: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        console.log("selectImageCallback - appMessageJSON: " + appMessageJSON);
         if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
            selectedImagePath = appMessageJSON.message
            $("#selectedImagePath").html('<b><g:message code="selectedImageLbl"/>: </b>' + selectedImagePath)
         } else if(appMessageJSON.message){
            showResultDialog('<g:message code='errorLbl'/>', appMessageJSON.message)
         }
    }

    function newRepresentativeCallback(appMessage) {
        console.log("newRepresentativeCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
            var caption = '<g:message code="newRepresentativeERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '<g:message code="newRepresentativeOKCaption"/>';
            }
            showResultDialog(caption, msg)
        }
    }
</r:script>