<html>
<head>
    <meta name="layout" content="main" />
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">
</head>
<body>
<div style="margin: 0px auto 0px auto; max-width: 1200px;">
    <div style="margin:0px 30px 0px 30px;">
        <div class="row" style="">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li><a href="${createLink(controller: 'representative', action:'main')}"><g:message code="representativesPageLbl"/></a></li>
                <li class="active"><g:message code="editRepresentativeLbl"/></li>
            </ol>
        </div>

        <div id="contentDiv" style="visibility: hidden;">

            <h3><div class="pageHeader text-center"></div></h3>

            <div class="text-left" style="margin:15px 0 0 0;">
                <ul>
                    <li><g:message code="newRepresentativeAdviceMsg2"/></li>
                    <li><g:message code="newRepresentativeAdviceMsg3"/></li>
                    <li><g:message code="newRepresentativeAdviceMsg4"/></li>
                </ul>
            </div>

            <form id="mainForm">
                <div style="position:relative; width:100%;">
                    <votingsystem-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></votingsystem-texteditor>
                </div>

                <div class="" style="margin:10px 10px 10px 10px;">
                    <div class="" style="display: inline">
                        <a href="#" id="selectImageButton" type="button" class="btn btn-default" onclick="selectImage()">
                            <g:message code="selectImageLbl"/> <i class="fa fa-file-image-o"></i>
                        </a>
                    </div>
                    <div class="" style="display: inline; float:right;">
                        <button type="submit" class="btn btn-default">
                            <g:message code="acceptLbl"/> <i class="fa fa fa-check"></i>
                        </button>
                    </div>
                </div>
                <div id="selectedImagePath" style="margin:10px 10px 40px 10px;"></div>

            </form>

            <div id="clientToolMsg" class="text-center" style="color:#6c0404; font-size: 1.2em;"><g:message code="clientToolNeededMsg"/>.
                <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/></div>

        </div>
    </div>
</div>

<g:include view="/include/dialog/editRepresentativeDialog.gsp"/>

</body>
</html>
<asset:script>
    var selectedImagePath = null
    var textEditor = document.querySelector('#textEditor')

    $(function() {

        $("#editRepresentativeDialog").modal("show");

        $('#mainForm').submit(function(event){
            event.preventDefault();
            if(textEditor.getData().length == 0) {
                textEditor.classList.add("formFieldError");
                showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="emptyDocumentERRORMsg"/>')

                return false;
            } else textEditor.classList.remove( "formFieldError" );
            if(selectedImagePath == null) {
                $("#selectImageButton").addClass("btn-danger");
                showResultDialog('<g:message code="dataFormERRORLbl"/>',
                    '<g:message code="missingImageERRORMsg"/>')
                return
            }
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.NEW_REPRESENTATIVE)
            webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
            webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
            webAppMessage.filePath = selectedImagePath
            webAppMessage.signedContent = {representativeInfo:editorContent, operation:Operation.REPRESENTATIVE_DATA}
            webAppMessage.serviceURL = "${createLink( controller:'representative', absolute:true)}"
            webAppMessage.signedMessageSubject = '<g:message code="representativeDataLbl"/>'
            webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
            webAppMessage.callerCallback = getFnName(editRepresentativeCallback)
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            return false
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

    var editRepresentativeHeaderTemplate = "<g:message code="editingRepresentativeMsgTitle"/>"

    function showRepresentativeData(representativeDataJSON) {
        console.log("showRepresentativeData: " + representativeDataJSON)
        var editRepresentativeHeader = editRepresentativeHeaderTemplate.format(representativeDataJSON.fullName)
        $(".pageHeader").append(editRepresentativeHeader)
        textEditor.setData(representativeDataJSON.info)
        $("#contentDiv").css("visibility", "visible" );
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

</asset:script>