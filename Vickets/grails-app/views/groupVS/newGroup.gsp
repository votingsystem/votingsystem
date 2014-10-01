<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/vicketTagVS/tagvs-select-dialog']"/>">
</head>
<body>

<div id="contentDiv" class="pageContentDiv" style="min-height: 1000px; margin:0px auto 0px auto;">
    <div style="margin:0px 30px 0px 30px;">
        <ol class="breadcrumbVS">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li><a href="${createLink(controller: 'groupVS', action: 'index')}"><g:message code="groupvsLbl"/></a></li>
            <li class="active"><g:message code="newGroupVSLbl"/></li>
        </ol>
        <h3>
            <div class="pageHeader text-center">
                <g:message code="newGroupPageTitle"/>
            </div>
        </h3>

        <div class="text-left" style="margin:10px 0 10px 0;">
            <ul>
                <li><g:message code="newGroupVSAdviceMsg3"/></li>
                <li><g:message code="signatureRequiredMsg"/></li>
                <li><g:message code="newGroupVSAdviceMsg2"/></li>
            </ul>
        </div>
        <tagvs-select-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                                        serviceURL="<g:createLink controller="vicketTagVS" action="index" />"></tagvs-select-dialog>
        <form onsubmit="return submitForm();">

            <div layout vertical>
                <div layout horizontal center center-justified style="">
                    <div flex style="margin: 0px 0px 0px 20px;">
                        <paper-input id="groupSubject" floatinglabel style="width:400px;" label="<g:message code="newGroupNameLbl"/>"
                                     validate="" error="<g:message code="requiredLbl"/>" style="" required>
                        </paper-input>
                    </div>
                    <votingsystem-button type="button" onclick="showTagDialog()">
                        <i class="fa fa-tag"></i> <g:message code="addTagLbl" /></votingsystem-button>
                </div>
                <div id="tagsDiv" style="padding:5px 0px 5px 30px; display:none;">
                    <div layout horizontal center>
                        <div style="font-size: 0.9em; font-weight: bold;color:#888; margin:0px 5px 0px 0px;">
                            <g:message code='tagsLbl'/>
                        </div>
                        <template id="selectedTags" is="auto-binding" repeat="{{tag in selectedTags}}">
                            <a class="btn btn-default" on-click="{{removeTag}}" style="font-size: 0.7em; margin:0px 5px 0px 0px;padding:3px;">
                                <i class="fa fa-minus"></i> {{tag.name}}</a>
                        </template>
                    </div>
                </div>
            </div>

            <div style="position:relative; width:100%;">
                <votingsystem-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></votingsystem-texteditor>
            </div>

            <div style="position:relative; margin:10px 10px 60px 0px;height:20px;">
                <div style="position:absolute; right:0;">
                    <votingsystem-button onclick="submitForm()" style="margin: 0px 0px 0px 5px;">
                        <i class="fa fa-check"></i> <g:message code="newGroupVSLbl"/>
                    </votingsystem-button>
                </div>
            </div>

        </form>

    </div>
</div>

</body>
</html>
<asset:script>


    function removeTag(e) {
        var tagDataId = e.target.templateInstance.model.tag.id
        for(tagIdx in document.querySelector('#selectedTags').selectedTags) {
            if(tagDataId == document.querySelector('#selectedTags').selectedTags[tagIdx].id) {
                document.querySelector('#selectedTags').selectedTags.splice(tagIdx, 1)
            }
        }
        if(document.querySelector("#selectedTags").selectedTags.length == 0)
            document.querySelector("#tagsDiv").style.display = 'none'
        else document.querySelector("#tagsDiv").style.display = 'block'
    }

    var tagsInitialized = false
    function showTagDialog() {
        console.log("showTagDialog - tagsInitialized: " + tagsInitialized)
        if(!tagsInitialized) {
            var selectedTagsDiv = document.querySelector("#selectedTags")
            selectedTagsDiv.removeTag = function(e) {
                var tagDataId = e.target.templateInstance.model.tag.id
                for(tagIdx in document.querySelector('#selectedTags').selectedTags) {
                    if(tagDataId == document.querySelector('#selectedTags').selectedTags[tagIdx].id) {
                        document.querySelector('#selectedTags').selectedTags.splice(tagIdx, 1)
                    }
                }
                if(document.querySelector("#selectedTags").selectedTags.length == 0)
                    document.querySelector("#tagsDiv").style.display = 'none'
                else document.querySelector("#tagsDiv").style.display = 'block'
            }

            document.querySelector("#tagDialog").addEventListener('tag-selected', function (e) {
                document.querySelector("#selectedTags").selectedTags = e.detail
                if(e.detail.length == 0) document.querySelector("#tagsDiv").style.display = 'none'
                else document.querySelector("#tagsDiv").style.display = 'block'
            })
            tagsInitialized = true

        }
        document.querySelector("#tagDialog").show(3, document.querySelector('#selectedTags').selectedTags)
    }


    var appMessageJSON;
    function submitForm(){
        var textEditor = document.querySelector('#textEditor')
        textEditor.classList.remove("formFieldError");
        if(document.querySelector('#groupSubject').invalid) {
            showMessageVS('<g:message code="fillAllFieldsERRORLbl"/>', '<g:message code="dataFormERRORLbl"/>')
            return false
        }
        if(textEditor.getData().length == 0) {
            textEditor.classList.add("formFieldError");
            showMessageVS('<g:message code="emptyDocumentERRORMsg"/>', '<g:message code="dataFormERRORLbl"/>')
            return false
        }
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.VICKET_GROUP_NEW)
        webAppMessage.serviceURL = "${createLink( controller:'groupVS', action:"newGroup", absolute:true)}"
        webAppMessage.signedMessageSubject = "<g:message code='newGroupVSMsgSubject'/>"
        webAppMessage.signedContent = {groupvsInfo:textEditor.getData(), tags:document.querySelector('#selectedTags').selectedTags,
            groupvsName:document.querySelector("#groupSubject").value, operation:Operation.VICKET_GROUP_NEW}
        webAppMessage.signedContent.tags = document.querySelector('#selectedTags').selectedTags
        webAppMessage.setCallback(function(appMessage) {
            console.log("newGroupVSCallback - message: " + appMessage);
            appMessageJSON = toJSON(appMessage)
            var callBackResult = null
            var caption = '<g:message code="newGroupERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '<g:message code="newGroupOKCaption"/>'
            }
            showMessageVS(msg, caption)
            window.scrollTo(0,0);
        })
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        appMessageJSON = null
        return false
    }

    document.querySelector("#coreSignals").addEventListener('core-signal-messagedialog-closed', function(e) {
        if(appMessageJSON != null && ResponseVS.SC_OK == appMessageJSON.statusCode)
            window.location.href = appMessageJSON.URL + "?menu=" + menuType
    });

</asset:script>
<asset:deferredScripts/>