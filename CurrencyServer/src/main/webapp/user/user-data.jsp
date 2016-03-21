<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="user-data">
<template>
    <style></style>
    <div style="max-width: 1200px; margin: 0 auto">
        <div class="layout horizontal center center-justified"
             style="font-size: 1.5em;margin:5px 0 15px 0;font-weight: bold; color:#6c0404;">
            <div data-user-id$="{{user.id}}" style="text-align: center;">{{userName}}</div>
            <div style="font-size: 0.7em; color: #888; font-weight: normal;margin: 2px 0 0 30px;">{{user.nif}}</div>
        </div>
        <div class="layout horizontal center center-justified" style="margin:0px 0px 10px 0px;">
            <div>
                <img id="qrImg"/>
            </div>
            <div hidden="{{!user.description}}" style="margin:0 0 20px 0;">
                <div id="userDescriptionDiv" class="contentDiv" style=" border: 1px solid #c0c0c0;padding:10px;"></div>
            </div>
        </div>
    </div>
</template>
<script>
    Polymer({
        is:'user-data',
        properties: {
            user: {type:Object, observer:'userChanged'},
            isBank: {type:Boolean, value:false},
            url:{type:String, observer:'getHTTP'},
            message: {type:String}
        },
        ready: function() {
            console.log(this.tagName + " - ready - menuType: " + this.menuType)
            if(this.message) alert(this.message, "${msg.messageLbl}")
        },
        userChanged:function() {
            console.log(this.tagName + " - userChanged - user: ", this.user)
            if(this.user.name) {
                if('BANK' === this.user.type || 'SYSTEM' == this.user.type) {
                    this.userName = this.user.name
                } else if('USER' == this.user.type) {
                    this.userName = this.user.firstName + " " + this.user.lastName
                }
            }
            if(this.user.connectedDevices && this.user.connectedDevices.length > 0) {
                console.log("fetching only first connected device!")
                var cert = forge.pki.certificateFromPem(this.user.connectedDevices[0].x509CertificatePEM);
                var publicKeyBase64 = vs.getPublicKeyBase64(cert.publicKey)
                this.$.qrImg.src = vs.getQRCodeURL('USER_INFO', null, this.user.connectedDevices[0].id, null,
                        publicKeyBase64, "150x150")
            }
            this.$.userDescriptionDiv.innerHTML = this.user.description
         },
        getHTTP: function (targetURL) {
            if(!targetURL) targetURL = this.url
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                this.user = toJSON(rawData.response)
            }.bind(this));
        }
    });
</script>
</dom-module>
