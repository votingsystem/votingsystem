<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../element/reason-dialog.vsp" rel="import"/>

<dom-module name="votingsystem-cert">
    <template>
        <style>
        .certDiv {
            background-color: #fefefe;
            padding: 10px;
            height: 150px;
            -moz-border-radius: 5px; border-radius: 5px;
            overflow:hidden;
        }
        </style>
        <iron-ajax auto url="{{url}}" last-response="{{certvs}}" handle-as="json" method="get" content-type="application/json"></iron-ajax>
        <reason-dialog id="reasonDialog" caption="${msg.cancelCertFormCaption}"></reason-dialog>
        <div class="layout vertical center center-justified">
            <div style="width: 600px;">
                <div class="layout horizontal center center-justified" style="width: 100%;">
                    <div hidden="{{!fabVisible}}">
                        <paper-fab mini icon="arrow-back" on-click="back"
                                   style="color: #f9f9f9;margin: 0 0 0 20px;background: #ba0011;"></paper-fab>
                    </div>
                    <div class="flex">
                        <h3>
                            <div id="pageHeaderDiv" class="pageHeader text-center"></div>
                        </h3>
                    </div>
                </div>

                <div class="certDiv">
                    <div>
                        <div class='groupvsSubjectDiv' style="display: inline;"><span style="font-weight: bold;">
                            ${msg.serialNumberLbl}: </span>{{certvs.serialNumber}}</div>
                        <div id="certStateDiv" style="display: inline; margin:0px 0px 0px 20px; font-size: 1.2em; font-weight: bold; float: right;"></div>
                    </div>
                    <div class='groupvsSubjectDiv'><span style="font-weight: bold;">${msg.subjectLbl}: </span>{{certvs.subjectDN}}</div>
                    <div><span style="font-weight: bold;">${msg.issuerLbl}: </span>
                        <a id="issuerURL" on-click="certIssuerClicked" style="cursor: pointer; text-decoration: underline;">{{certvs.issuerDN}}</a>
                    </div>
                    <div><span style="font-weight: bold;">${msg.signatureAlgotithmLbl}: </span>{{certvs.sigAlgName}}</div>
                    <div>
                        <div style="display: inline;">
                            <span style="font-weight: bold;">${msg.noBeforeLbl}: </span>{{getDate(certvs.notBefore)}}</div>
                        <div style="display: inline; margin:0px 0px 0px 20px;">
                            <span style="font-weight: bold;">${msg.noAfterLbl}: </span>{{getDate(certvs.notAfter)}}</div>
                    </div>
                    <div hidden="{{!certvs.isRoot}}" class="text-center" style="font-weight: bold; display: inline;
                            margin:0px auto 0px auto;color: #6c0404; float:right; text-decoration: underline;">${msg.rootCertLbl}
                    </div>
                </div>
                <div style="width: 600px; max-height:400px; overflow-y: auto; margin:20px auto 0px auto;">
                    <div>{{certvs.description}}</div>
                </div>
                <button id="cancelCertButton" on-click="openReasonDialog" style="display: none;">
                    ${msg.cancelCertLbl}
                </button>
                <div class="vertical layout center center-justified" style="width: 600px; margin:0px auto 0px auto;">
                    <div><label>${msg.certPublicKeyLbl}</label></div>
                    <div>
                        <textarea id="pemCertTextArea" style="width:520px; height:300px;font-family: monospace; font-size:0.8em;" readonly></textarea>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'votingsystem-cert',
            properties: {
                fabVisible:{type:Boolean, value:false},
                url:{type:String},
                certvs:{type:Object, value:{}, observer:'certvsChanged'}
            },
            ready: function() {
                this.certsSelectedStack = []
                if(menuType === "admin" || menuType === "superuser") this.$.cancelCertButton.style.display = "block"
                this.$.reasonDialog.addEventListener('on-submit', function (e) {
                        var operationVS = new OperationVS(Operation.CERT_EDIT)
                        operationVS.serviceURL = contextURL + "/rest/certificateVS/editCert"
                        operationVS.signedMessageSubject = "${msg.cancelCertMessageSubject}"
                        var signedContent = {operation:Operation.CERT_EDIT, reason:e.detail,
                            changeCertToState:"${CertificateVS.State.CANCELED.toString()}", serialNumber:"${certMap.serialNumber}"}
                        operationVS.jsonStr = JSON.stringify(signedContent)
                        operationVS.setCallback(function(appMessage) {
                            this.url = contextURL + "/rest/certificateVS/cert/" + certMap.serialNumber + "?menu=" + menuType
                        })
                        VotingSystemClient.setMessage(operationVS);
                }.bind(this))
            },
            openReasonDialog: function() {
                this.$.reasonDialog.show()
            },
            certvsChanged: function() {
                this.$.pemCertTextArea.value = this.certvs.pemCert
                if('CERTIFICATE_AUTHORITY' == this.certvs.type) {
                    this.$.pageHeaderDiv.innerHTML = "${msg.trustedCertPageTitle}"
                } else if ('USER' == this.certvs.type) {
                    this.$.pageHeaderDiv.innerHTML = "${msg.userCertPageTitle}"
                }
                if('OK' == this.certvs.state) {
                    this.$.certStateDiv.innerHTML = "${msg.certOKLbl}"
                } else if ('CANCELED' == this.certvs.state) {
                    this.$.certStateDiv.innerHTML = "${msg.certCancelledLbl}"
                }
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            back:function() {
                var previousCert = this.certsSelectedStack.pop()
                if(previousCert != null) {
                    this.url = ""
                    this.certvs = previousCert
                    if(this.certsSelectedStack.length == 0) this.subcert = null
                } else {
                    console.log(this.tagName + " - iron-signal-votingsystem-cert-closed")
                    this.fire('cert-closed');
                }
            },
            certIssuerClicked:function(e) {
                var issuerSerialNumber = this.certvs.issuerSerialNumber
                if(issuerSerialNumber != null) {
                    var certURL = contextURL + "/rest/certificateVS/cert/" + issuerSerialNumber
                    console.log(this.tagName + " - certIssuerClicked: " + certURL)
                    this.certsSelectedStack.push(this.certvs)
                    this.url = certURL
                    this.subcert = true
                }
            }
        })
    </script>
</dom-module>