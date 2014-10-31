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
    <div style="margin: 10px auto 10px auto; text-align: center;"><g:message code="certRequestAdviceMsg"/></div>
    <div layout vertical id="formDataDiv" style="padding: 0px 20px 0px 20px; height: 100%;">
        <div style="width: 600px; margin:10px auto;">
            <input type="text" id="nif" class="form-control" required style="margin: 0 0 10px 0;"
                   title="<g:message code="nifLbl"/>" placeholder="<g:message code="nifLbl"/>"/>
            <input type="text" id="nameAndSurname" class="form-control" required style="margin: 0 0 10px 0;"
                   title="<g:message code="nameAndSurnameLbl"/>" placeholder="<g:message code="nameAndSurnameLbl"/>"/>
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
            this.removeErrorStyle(this.$.formDataDiv)
            this.messageToUser = null
            if(!this.$.nif.validity.valid || validateNIF(his.$.nif.value) == null) {
                this.$.email.classList.add("formFieldError")
                this.messageToUser = "<g:message code='emailFieldErrorMsg'/>"
                return
            }
            if(!this.$.nameAndSurname.validity.valid) {
                this.$.nameAndSurname.classList.add("formFieldError")
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
            showMessageVS("<b><g:message code="nameAndSurnameLbl"/>:</b> " + this.$.nameAndSurname.value +
                    "<br/><b><g:message code="phoneLbl"/>:</b> " + this.$.phone.value +
                    "<br/><b><g:message code="emailLbl"/>:</b> " + this.$.email.value,
                    "<g:message code='checkInputMsg'/>", null, true)
        },
        messagedialogAccept: function () {
            console.log("messagedialogAccept")
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
