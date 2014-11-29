<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-shadow', file: 'paper-shadow.html')}">

<polymer-element name="asciidoc-template-test" attributes="jsonDoc">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
        .messageToUser {
            font-weight: bold;
            margin:10px auto 10px auto;
            background: #f9f9f9;
            padding:10px 20px 10px 20px;
        }
        </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" method="post" contentType="json"
                   on-core-response="{{ajaxResponse}}"></core-ajax>
        <div layout vertical style="margin: 10px 30px; max-width:1000px;">
            <div class="pageHeader"  layout horizontal center center-justified>
                <h3>ASCIIDOC TEST</h3>
            </div>
            <div style="display:{{messageToUser? 'block':'none'}}">
                <div  layout horizontal center center-justified  class="messageToUser">
                    <div>
                        <div id="messageToUser">{{messageToUser}}</div>
                    </div>
                    <paper-shadow z="1"></paper-shadow>
                </div>
            </div>
            <form on-submit="{{submitForm}}">
            <div  layout vertical center center-justified  style="margin:10px 0px 10px 0px;">
                    <input type="text" class="form-control" id="subject" on-keypress="{{inputChanged}}"
                           style="width:400px; margin:10px 0 0 0;" placeholder="<g:message code="subjectLbl"/>" required/>
                    <input type="text" class="form-control" id="section" on-keypress="{{inputChanged}}"
                           style="width:400px; margin:10px 0 0 0;" placeholder="<g:message code="sectionLbl"/>" required/>
                    <input type="text" class="form-control" id="content" on-keypress="{{inputChanged}}"
                           style="width:400px; margin:10px 0 0 0;" placeholder="<g:message code="contentLbl"/>" required/>
            </div>
            <div>
                <input type="checkbox" id="isSignedDocument"/><label><g:message code="sendSignedDocumentLbl"/></label>
            </div>
            <vs-html-echo html="{{responseData}}"></vs-html-echo>
            <div layout horizontal style="margin:0px 20px 0px 0px;">
                <div flex>
                    <input type="text" id="keyEnterListener" style="width: 0px;background-color:white; border: none;" autofocus/>
                </div>
                <div style="margin:10px 0px 10px 0px;">
                    <button type="submit" class="btn btn-warning" style="margin:15px 20px 20px 0px;">
                        <g:message code="acceptLbl"/>
                    </button>
                </div>
            </div>
            </form>
        </div>
        <div>

        </div>
        <!--without tabs !!!-->
        <div id="asciiDocTemplate" style="display: none;">
= {{jsonDoc.subject}}
v1.0, 2013-05-20: First draft
:title: Sample Document
:tags: [document, example]
:metaInfVS: {{jsonDocStr}}

== {{jsonDoc.section.title}}
{{jsonDoc.section.content}}
        </div>
    </template>
    <script>
        Polymer('asciidoc-template-test', {
            publish: {
                jsonDoc: {value: {}}
            },
            messageToUser:null,
            jsonDocStr:null,
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            ajaxResponse:function() {
                console.log(this.tagName + " - responseDataChanged: " + JSON.stringify(this.responseData))
                //this.$.ajax.url = ""
            },
            responseDataChanged:function() {
                console.log(this.tagName + " - responseDataChanged: " + JSON.stringify(this.responseData))
                //this.$.ajax.url = ""
            },
            inputChanged:function() {
                var section = {title:this.$.section.value, content:this.$.content.value}
                this.jsonDoc = {subject: this.$.subject.value, section:section, operation:'COOIN',
                    UUID:Math.random().toString(36).substring(7)}
                this.jsonDocStr = JSON.stringify(this.jsonDoc)
            },
            submitForm: function(e) {
                e.preventDefault()
                var asciiDoc = this.$.asciiDocTemplate.innerHTML.trim()
                var serviceURL = "<g:createLink  controller="asciiDoc" action="test" absolute="true"/>"
                console.log(this.tagName + " - submitForm - asciiDoc: " + asciiDoc)

                if(this.$.isSignedDocument.checked) {
                    var webAppMessage = new WebAppMessage( 'COOIN')
                    webAppMessage.serviceURL = serviceURL
                    webAppMessage.signedMessageSubject = "AsciiDoc TEST"
                    webAppMessage.signedContent = {asciiDoc:asciiDoc, operation:'COOIN'}
                    webAppMessage.asciiDoc = asciiDoc
                    webAppMessage.setCallback(function(appMessage) {
                        console.log("sendCertAuthority - message: " + appMessage);
                        var appMessageJSON = toJSON(appMessage)
                        showMessageVS(appMessageJSON.message, "sendCertAuthority - status: " + appMessageJSON.statusCode)
                    })
                    VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
                } else {
                    this.$.ajax.url = ""
                    this.$.ajax.body= JSON.stringify({asciiDoc:asciiDoc})
                    this.$.ajax.url = serviceURL
                }
            }
        });
    </script>
</polymer-element>