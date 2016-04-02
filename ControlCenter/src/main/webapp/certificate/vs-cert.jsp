<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../element/reason-dialog.vsp" rel="import"/>

<dom-module name="vs-cert">
    <template>
        <reason-dialog id="reasonDialog" caption="${msg.cancelCertFormCaption}"></reason-dialog>
        <div class="layout vertical center center-justified">
            <div>
                <div class="layout horizontal center center-justified" style="width: 100%;">
                    <h3>
                        <div id="pageHeaderDiv" class="pageHeader text-center"></div>
                    </h3>
                </div>

                <div>
                    <div>
                        <div style="display: inline;"><span style="font-weight: bold;">
                            ${msg.serialNumberLbl}: </span>{{certvs.serialNumber}}</div>
                        <div id="certStateDiv" style="display: inline; margin:0px 0px 0px 20px; font-size: 1.2em; font-weight: bold; float: right;"></div>
                    </div>
                    <div><span style="font-weight: bold;">${msg.subjectLbl}: </span>{{certvs.subjectDN}}</div>
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
                <div style="max-height:400px; overflow-y: auto; margin:20px auto 0px auto;">
                    <div>{{certvs.description}}</div>
                </div>
                <button id="cancelCertButton" on-click="openReasonDialog">
                    ${msg.cancelCertLbl}
                </button>
                <div class="vertical layout center center-justified" style="margin:0px auto 0px auto;">
                    <div><label>${msg.certPublicKeyLbl}</label></div>
                    <div>
                        <textarea id="pemCertTextArea" style="width:400px; height:300px;font-family: monospace; font-size:0.8em;" readonly></textarea>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'vs-cert',
            properties: {
                url:{type:String, observer:'getHTTP'},
                certvs:{type:Object, value:{}, observer:'certvsChanged'}
            },
            ready: function() {
                this.certsSelectedStack = []
                document.querySelector("#voting_system_page").addEventListener('on-submit-reason',
                        function() {
                            var operationVS = new OperationVS(Operation.CERT_EDIT)
                            operationVS.serviceURL = vs.contextURL + "/rest/certificate/editCert"
                            operationVS.subject = "${msg.cancelCertMessageSubject}"
                            var signedContent = {operation:Operation.CERT_EDIT, reason:e.detail,
                                changeCertToState:"${Certificate.State.CANCELED.toString()}", serialNumber:"${certMap.serialNumber}"}
                            operationVS.jsonStr = JSON.stringify(signedContent)
                            operationVS.setCallback(function() {
                                this.url = vs.contextURL + "/rest/certificate/serialNumber/${certMap.serialNumber}"
                            })
                            vs.client.processOperation(operationVS);
                        }.bind(this))
            },
            openReasonDialog: function() {
                this.$.reasonDialog.show()
            },
            certvsChanged: function() {
                this.$.pemCertTextArea.value = this.certvs.x509CertificatePEM
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
            certIssuerClicked:function(e) {
                var issuerSerialNumber = this.certvs.issuerSerialNumber
                if(issuerSerialNumber != null) {
                    var certURL = vs.contextURL + "/rest/certificate/serialNumber/" + issuerSerialNumber
                    console.log(this.tagName + " - certIssuerClicked: " + certURL)
                    this.certsSelectedStack.push(this.certvs)
                    this.url = certURL
                }
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL);
                vs.getHTTPJSON(targetURL, function(responseText){
                    this.certvs = toJSON(responseText)
                }.bind(this));
            }
        })
    </script>
</dom-module>