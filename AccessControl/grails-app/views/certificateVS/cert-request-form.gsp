<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">

<polymer-element name="cert-request-form" attributes="caption opened isForAdmins messageToUser">
<template>
    <g:include view="/include/styles.gsp"/>
    <style>
    .formFieldError { background: #f6ccd0;  }
    .messageToUser { font-weight: bold;  margin:10px auto 10px auto; background: #f9f9f9; padding:10px 20px 10px 20px; max-width: 600px;}
    </style>
    <core-signals on-core-signal-messagedialog-accept="{{messagedialogAccept}}"></core-signals>
    <template if="{{messageToUser}}">
        <div style="color: {{status == 200?'#388746':'#ba0011'}};">
            <div class="messageToUser">
                <div  layout horizontal center center-justified >
                    <div id="messageToUser">{{messageToUser}}</div>
                </div>
                <paper-shadow z="1"></paper-shadow>
            </div>
        </div>
    </template>
    <div style="margin: 20px auto 10px auto; text-align: center;width: 600px; ">
        <g:message code="certRequestAdviceMsg"/>
    </div>
    <div layout vertical id="formDataDiv" style="padding: 0px 20px 0px 20px; height: 100%;">
        <div style="width: 600px; margin:10px auto;">
            <input type="text" id="nif" class="form-control" required style="margin: 0 0 10px 0;"
                   title="<g:message code="nifLbl"/>" placeholder="<g:message code="nifLbl"/>"/>
            <div horizontal layout>
                <input type="text" id="name" class="form-control" required style="margin: 0 0 10px 0;"
                       title="<g:message code="nameLbl"/>" placeholder="<g:message code="nameLbl"/>"/>
                <input type="text" id="surname" class="form-control" required style="margin: 0 0 10px 10px;"
                       title="<g:message code="surnameLbl"/>" placeholder="<g:message code="surnameLbl"/>"/>
            </div>

            <input type="text" id="phone" class="form-control" required style="margin: 0 0 10px 0;"
                   title="<g:message code="phoneLbl"/>" placeholder="<g:message code="phoneLbl"/>"/>
            <input type="email" id="email" class="form-control" required style="margin: 0 0 10px 0;"
                   title="<g:message code="emailLbl"/>" placeholder="<g:message code="emailLbl"/>"/>
            <div layout horizontal style="margin:10px 20px 0px 0px;">
                <div flex></div>
                <paper-button raised on-click="{{submitForm}}" style="margin: 20px 0px 0px 5px;">
                    <i class="fa fa-check"></i> <g:message code="acceptLbl"/>
                </paper-button>
            </div>
        </div>
    </div>
</template>
<script>
    Polymer('cert-request-form', {
        ready: function() { },
        submitForm: function () {
            console.log("submitForm")
            this.removeErrorStyle(this.$.formDataDiv)
            this.messageToUser = null
            if(!this.$.nif.validity.valid || validateNIF(this.$.nif.value) == null) {
                this.$.nif.classList.add("formFieldError")
                this.messageToUser = "<g:message code='nifERRORMsg'/>"
                return
            }
            if(!this.$.name.validity.valid) {
                this.$.name.classList.add("formFieldError")
                this.messageToUser = "<g:message code='emptyFieldMsg'/>"
                return
            }
            if(!this.$.surname.validity.valid) {
                this.$.surname.classList.add("formFieldError")
                this.messageToUser = "<g:message code='emptyFieldMsg'/>"
                return
            }
            if(!this.$.phone.validity.valid) {
                this.$.phone.classList.add("formFieldError")
                this.messageToUser = "<g:message code='emptyFieldMsg'/>"
                return
            }
            if(!this.$.email.validity.valid) {
                this.$.email.classList.add("formFieldError")
                this.messageToUser = "<g:message code='emailFieldErrorMsg'/>"
                return
            }
            showMessageVS("<b><g:message code="nifLbl"/>:</b> " + this.$.nif.value +
                    "<br/><b><g:message code="nameLbl"/>:</b> " + this.$.name.value.toUpperCase() +
                    "<br/><b><g:message code="surnameLbl"/>:</b> " + this.$.surname.value.toUpperCase() +
                    "<br/><b><g:message code="phoneLbl"/>:</b> " + this.$.phone.value +
                    "<br/><b><g:message code="emailLbl"/>:</b> " + this.$.email.value,
                    "<g:message code='checkInputMsg'/>", null, true)
        },
        messagedialogAccept: function () {
            console.log("messagedialogAccept")
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.CERT_USER_NEW)
            webAppMessage.serviceURL = "${createLink(controller:'csr', action:'request',absolute:true)}"
            webAppMessage.signedMessageSubject = "<g:message code="certRequestLbl"/>"
            webAppMessage.document = {nif:this.$.nif.value, name:this.$.name.value.toUpperCase(),
                surname:this.$.surname.value.toUpperCase(), phone:this.$.phone.value, email:this.$.email.value}
            webAppMessage.setCallback(function(appMessage) {
                var appMessageJSON = JSON.parse(appMessage)
                var message = appMessageJSON.message
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    message = "<g:message code='certRequestOKMsg'/>"
                }
                showMessageVS(message, '<g:message code="certRequestLbl"/>')
            })
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);

        },
        removeErrorStyle: function (element) {
            var formElements = element.children
            for(var i = 0; i < element.childNodes.length; i++) {
                var child = element.childNodes[i];
                this.removeErrorStyle(child);
                if(child != undefined) {
                    if(child.style != undefined) {
                        //child.style.background = '#fff'
                        child.classList.remove("formFieldError");
                    }
                }
            }
        }
    });
</script>
</polymer-element>
