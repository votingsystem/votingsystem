<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="cert-request-form">
<template>
    <style>
    .messageToUser { font-weight: bold;  margin:10px auto 10px auto; background: #f9f9f9; padding:10px 20px 10px 20px; max-width: 600px;}
    </style>
    <div hidden="{{!messageToUser}}">
        <div class="messageToUser layout horizontal center center-justified">
            <div id="messageToUser">{{messageToUser}}</div>
        </div>
    </div>
    <div style="margin: 20px auto 10px auto; text-align: center;max-width: 600px; ">
        ${msg.certRequestAdviceMsg}
    </div>
    <div id="formDataDiv" style="padding: 0px 20px 0px 20px; height: 100%;">
        <div style="max-width: 600px; margin:10px auto;">
            <input type="text" id="nif" class="form-control" required style="margin: 0 0 10px 0;"
                   title="${msg.nifLbl}" placeholder="${msg.nifLbl}"/>
            <div class="horizontal layout">
                <input type="text" id="givenname" class="form-control" required style="margin: 0 0 10px 0;"
                       title="${msg.nameLbl}" placeholder="${msg.nameLbl}"/>
                <input type="text" id="surname" class="form-control" required style="margin: 0 0 10px 10px;"
                       title="${msg.surnameLbl}" placeholder="${msg.surnameLbl}"/>
            </div>

            <input type="text" id="phone" class="form-control" required style="margin: 0 0 10px 0;"
                   title="${msg.phoneLbl}" placeholder="${msg.phoneLbl}"/>
            <input type="email" id="email" class="form-control" required style="margin: 0 0 10px 0;"
                   title="${msg.emailLbl}" placeholder="${msg.emailLbl}"/>
            <div class="layout horizontal" style="margin:10px 20px 0px 0px;">
                <div class="flex"></div>
                <button on-click="submitForm" style="font-size:1.1em;margin: 20px 0px 0px 5px;">
                    <i class="fa fa-check"></i> ${msg.acceptLbl}
                </button>
            </div>
        </div>
    </div>
</template>
<script>
    Polymer({
        is:'cert-request-form',
        properties: {
            messageToUser:{type:String, value:null}
        },
        ready: function() {
            console.log(this.tagName + " - ready")
        },
        submitForm: function () {
            console.log("submitForm")
            this.removeErrorStyle(this.$.formDataDiv)
            this.messageToUser = null
            if(!this.$.nif.validity.valid || validateNIF(this.$.nif.value) == null) {
                this.$.nif.classList.add("formFieldError")
                this.messageToUser = "${msg.nifERRORMsg}"
                return
            }
            if(!this.$.givenname.validity.valid) {
                this.$.givenname.classList.add("formFieldError")
                this.messageToUser = "${msg.emptyFieldMsg}"
                return
            }
            if(!this.$.surname.validity.valid) {
                this.$.surname.classList.add("formFieldError")
                this.messageToUser = "${msg.emptyFieldMsg}"
                return
            }
            if(!this.$.phone.validity.valid) {
                this.$.phone.classList.add("formFieldError")
                this.messageToUser = "${msg.emptyFieldMsg}"
                return
            }
            if(!this.$.email.validity.valid) {
                this.$.email.classList.add("formFieldError")
                this.messageToUser = "${msg.emailFieldErrorMsg}"
                return
            }
            alert("<b>${msg.nifLbl}:</b> " + validateNIF(this.$.nif.value) +
                    "<br/><b>${msg.nameLbl}:</b> " + this.$.givenname.value.toUpperCase() +
                    "<br/><b>${msg.surnameLbl}:</b> " + this.$.surname.value.toUpperCase() +
                    "<br/><b>${msg.phoneLbl}:</b> " + this.$.phone.value +
                    "<br/><b>${msg.emailLbl}:</b> " + this.$.email.value,
                    "${msg.checkInputMsg}", this.messagedialogAccept.bind(this))
        },
        messagedialogAccept: function () {
            console.log("messagedialogAccept")
            var operationVS = new OperationVS(Operation.CERT_USER_NEW)
            operationVS.serviceURL = vs.contextURL + "/rest/csr/request"
            operationVS.subject = "${msg.certRequestLbl}"
            operationVS.jsonStr = JSON.stringify({nif:validateNIF(this.$.nif.value), givenname:this.$.givenname.value.toUpperCase(),
                surname:this.$.surname.value.toUpperCase(), mobilePhone:this.$.phone.value, email:this.$.email.value})
            operationVS.setCallback(function(appMessage) { this.showResponse(appMessage) }.bind(this))
            vs.client.processOperation(operationVS);
        },
        showResponse :function(appMessageJSON) {
            var message = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                message = "${msg.certRequestOKMsg}"
                alert(message, '${msg.certRequestLbl}', 'resultMessage',true)
            } else alert(message, '${msg.certRequestLbl}')
        },
        removeErrorStyle: function (element) {
            var formElements = element.children
            for(var i = 0; i < element.childNodes.length; i++) {
                var child = element.childNodes[i];
                this.removeErrorStyle(child);
                if(child != undefined) {
                    if(child.style != undefined && child.tagName === 'INPUT') {
                        //child.style.background = '#fff'
                        child.classList.remove("formFieldError");
                    }
                }
            }
        }
    });
</script>
</dom-module>
