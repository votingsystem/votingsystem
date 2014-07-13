<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-select-tag-dialog', file: 'votingsystem-select-tag-dialog.html')}">
</head>
<body>

<div id="contentDiv" class="pageContenDiv" style="min-height: 1000px; margin:0px auto 0px auto;">
    <div style="margin:0px 30px 0px 30px;">
        <div class="row">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li><a href="${createLink(controller: 'groupVS', action: 'index')}"><g:message code="groupvsLbl"/></a></li>
                <li class="active"><g:message code="newGroupVSLbl"/></li>
            </ol>
        </div>
        <h3>
            <div class="pageHeader text-center">
                <g:message code="newGroupPageTitle"/>
            </div>
        </h3>

        <div class="text-left" style="margin:10px 0 10px 0;">
            <ul>
                <li><g:message code="signatureRequiredMsg"/></li>
                <li><g:message code="newGroupVSAdviceMsg2"/></li>
                <li><g:message code="newGroupVSAdviceMsg3"/></li>
            </ul>
        </div>

        <form onsubmit="return submitForm();">

            <div class="form-inline">
                <div style="margin:0px 0px 10px 0px; display: table; height: 10px;">
                    <div style="display: table-cell;padding:0px; margin:0px;">
                        <input type="text" name="subject" id="groupSubject" style="width:400px;"  required
                               title="<g:message code="subjectLbl"/>" class="form-control"
                               placeholder="<g:message code="newGroupNameLbl"/>"
                               onchange="this.setCustomValidity('')" />
                    </div>
                    <div style="width:100%;display: table-cell" class="text-right">
                        <button type="button" onclick="showTagDialog()" class="btn btn-danger">
                        <g:message code="addTagLbl" /></button>
                    </div>
                </div>
                <div id="tagsDiv" style="padding:0px 0px 0px 30px; display:none;">
                    <div style=" display: table-cell; font-size: 1.1em; font-weight: bold; vertical-align: middle;">
                        <g:message code='tagsLbl'/>:</div>
                    <div id="selectedTagDiv" style="margin:0px 0px 15px 0px; padding: 5px; display: table-cell;">
                        <template id="selectedTags" is="auto-binding" repeat="{{tag in selectedTags}}">
                            <button data-tagId='{{tag.id}}' onclick="removeTag(this)" type="button" class="btn btn-default"
                                    style="margin:7px 10px 0px 0px;">{{tag.name}}  <i class="fa fa-minus-circle"></i>
                            </button>
                        </template>
                    </div>
                </div>
            </div>

            <div style="position:relative; width:100%;">
                <votingsystem-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></votingsystem-texteditor>
            </div>

            <div style="position:relative; margin:10px 10px 60px 0px;height:20px;">
                <div style="position:absolute; right:0;">
                    <button type="submit" class="btn btn-default">
                        <g:message code="newGroupVSLbl"/> <i class="fa fa fa-check"></i>
                    </button>
                </div>
            </div>

        </form>

    </div>
</div>
<votingsystem-select-tag-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                serviceURL="<g:createLink controller="vicketTagVS" action="index" />"></votingsystem-select-tag-dialog>
</body>
</html>
<asset:script>
    var textEditor = document.querySelector('#textEditor')

    function removeTag(button) {
        var tagDataId = button.getAttribute('data-tagId')
        for(tagIdx in document.querySelector('#selectedTags').selectedTags) {
            if(tagDataId == document.querySelector('#selectedTags').selectedTags[tagIdx].id) {
                document.querySelector('#selectedTags').selectedTags.splice(tagIdx, 1)
            }
        }
        if(document.querySelector("#selectedTags").selectedTags.length == 0)
            document.querySelector("#tagsDiv").style.display = 'none'
        else document.querySelector("#tagsDiv").style.display = 'block'
    }

    function showTagDialog() {
        document.querySelector("#tagDialog").show(3, document.querySelector('#selectedTags').selectedTags)
    }

    document.querySelector("#tagDialog").addEventListener('tag-selected', function (e) {
            document.querySelector("#selectedTags").selectedTags = e.detail
            if(e.detail.length == 0) document.querySelector("#tagsDiv").style.display = 'none'
            else document.querySelector("#tagsDiv").style.display = 'block'
    })

    var appMessageJSON
    function submitForm(){
        textEditor.classList.remove("formFieldError");
        if(!document.getElementById('groupSubject').validity.valid) {
            showMessageVS('<g:message code="fillAllFieldsERRORLbl"/>', '<g:message code="dataFormERRORLbl"/>')
            return false
        }
        if(textEditor.getData().length == 0) {
            textEditor.classList.add("formFieldError");
            showMessageVS('<g:message code="emptyDocumentERRORMsg"/>', '<g:message code="dataFormERRORLbl"/>')
            return false
        }
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.VICKET_GROUP_NEW)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.serviceURL = "${createLink( controller:'groupVS', action:"newGroup", absolute:true)}"
        webAppMessage.signedMessageSubject = "<g:message code='newGroupVSMsgSubject'/>"
        webAppMessage.signedContent = {groupvsInfo:textEditor.getData(), tags:document.querySelector('#selectedTags').selectedTags,
            groupvsName:document.querySelector("#groupSubject").value, operation:Operation.VICKET_GROUP_NEW}
        webAppMessage.signedContent.tags = document.querySelector('#selectedTags').selectedTags
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"

        var objectId = Math.random().toString(36).substring(7)
        window[objectId] = {setClientToolMessage: function(appMessage) {
            console.log("newGroupVSCallback - message: " + appMessage);
            appMessageJSON = toJSON(appMessage)
            var callBackResult = null
            if(appMessageJSON != null) {
                var caption = '<g:message code="newGroupERRORCaption"/>'
                var msg = appMessageJSON.message
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    caption = '<g:message code="newGroupOKCaption"/>'
                }
                showMessageVS(msg, caption)
            }
            window.scrollTo(0,0); }}
        webAppMessage.callerCallback = objectId
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        appMessageJSON = null
        return false
    }

    document.querySelector("#_votingsystemMessageDialog").addEventListener('message-accepted', function (e) {
        if(appMessageJSON != null) window.location.href = appMessageJSON.URL + "?menu=" + menuType
    })

</asset:script>