<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webresource dir="vs-texteditor" file="vs-texteditor.html"/>
    <vs:webcomponent path="/tagVS/tagvs-select-dialog"/>
</head>
<body>
<innerpage-signal title="<g:message code="newGroupVSLbl"/>"></innerpage-signal>
<div class="pageContentDiv" style="min-height: 1000px; margin:0px auto 0px auto;">
    <div style="margin:0px 30px 0px 30px;">
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
                                        serviceURL="<g:createLink controller="tagVS" action="index" />"></tagvs-select-dialog>
        <form onsubmit="return submitForm();">

            <div layout vertical>
                <div layout horizontal center center-justified style="">
                    <div flex style="margin: 0px 0px 0px 20px;">
                        <paper-input id="groupSubject" floatinglabel style="width:400px;" label="<g:message code="newGroupNameLbl"/>"
                                     validate="" error="<g:message code="requiredLbl"/>" style="" required>
                        </paper-input>
                    </div>
                    <paper-button raised type="button" onclick="showTagDialog()">
                        <i class="fa fa-tag"></i> <g:message code="addTagLbl" /></paper-button>
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
                <vs-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></vs-texteditor>
            </div>

            <div style="position:relative; margin:10px 10px 60px 0px;height:20px;">
                <div style="position:absolute; right:0;">
                    <paper-button raised onclick="submitForm()" style="margin: 0px 0px 0px 5px;">
                        <i class="fa fa-check"></i> <g:message code="newGroupVSLbl"/>
                    </paper-button>
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
        var webAppMessage = new WebAppMessage(Operation.COOIN_GROUP_NEW)
        webAppMessage.serviceURL = "${createLink( controller:'groupVS', action:"newGroup", absolute:true)}"
        webAppMessage.signedMessageSubject = "<g:message code='newGroupVSMsgSubject'/>"
        webAppMessage.signedContent = {groupvsInfo:textEditor.getData(), tags:document.querySelector('#selectedTags').selectedTags,
            groupvsName:document.querySelector("#groupSubject").value, operation:Operation.COOIN_GROUP_NEW}
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