<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">

<polymer-element name="cert-data" attributes="url certStr">
    <template>
        <style no-shim>
        .view { :host {position: relative;} }
        .certDiv {
            width: 600px;
            background-color: #f2f2f2;
            padding: 10px;
            height: 150px;
            -moz-border-radius: 5px; border-radius: 5px;
            overflow:hidden;
        }
        </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{cert}}" handleAs="json" method="get" contentType="json"></core-ajax>
        <div flex vertical>
                <h3>
                    <div id="pageHeaderDiv" class="pageHeader text-center"></div>
                </h3>
                <div class="certDiv" style="margin:0px auto 0px auto;">
                    <div style="display: inline;">
                        <div class='groupvsSubjectDiv' style="display: inline;"><span style="font-weight: bold;">
                            <g:message code="serialNumberLbl"/>: </span>---{{cert.serialNumber}}</div>
                        <div id="certStateDiv" style="display: inline; margin:0px 0px 0px 20px; font-size: 1.2em; font-weight: bold; float: right;"></div>
                    </div>
                    <div class='groupvsSubjectDiv'><span style="font-weight: bold;"><g:message code="subjectLbl"/>: </span>{{cert.subjectDN}}</div>
                    <div class=''><span style="font-weight: bold;"><g:message code="issuerLbl"/>: </span>
                        <a id="issuerURL" on-click="{{certIssuerClicked}}">{{cert.issuerDN}}</a>
                    </div>
                    <div class=''><span style="font-weight: bold;"><g:message code="signatureAlgotithmLbl"/>: </span>{{cert.sigAlgName}}</div>
                    <div>
                        <div class='' style="display: inline;">
                            <span style="font-weight: bold;"><g:message code="noBeforeLbl"/>: </span>{{cert.notBefore}}</div>
                        <div class='' style="display: inline; margin:0px 0px 0px 20px;">
                            <span style="font-weight: bold;"><g:message code="noAfterLbl"/>: </span>{{cert.notAfter}}</div>
                    </div>
                    <div>
                        <g:if test="{{cert.isRoot}}">
                            <div class="text-center" style="font-weight: bold; display: inline;
                            margin:0px auto 0px auto;color: #6c0404; float:right; text-decoration: underline;"><g:message code="rootCertLbl"/></div>
                        </g:if>
                    </div>
                </div>
                <div style="width: 600px; max-height:400px; overflow-y: auto; margin:20px auto 0px auto;">
                    <div>{{cert.description}}</div>
                </div>

                <div style="width: 600px; margin:20px auto 0px auto;">
                    <label><g:message code="certPublicKeyLbl"/></label>
                    <textarea id="pemCertTextArea" class="form-control" rows="20" readonly></textarea>
                </div>

        </div>
    </template>
    <script>
        Polymer('cert-data', {
            ready: function() {},
            certStrChanged : function() {
                this.cert = JSON.parse(this.certStr)
                this.$.pemCertTextArea.value = this.cert.pemCert
                if('CERTIFICATE_AUTHORITY' == this.cert.type) {
                    this.$.pageHeaderDiv.innerHTML = "<g:message code="trustedCertPageTitle"/>"
                } else if ('USER' == this.cert.type) {
                    this.$.pageHeaderDiv.innerHTML = "<g:message code="userCertPageTitle"/>"
                }
                if('OK' == this.cert.state) {
                    this.$.certStateDiv.innerHTML = "<g:message code="certOKLbl"/>"
                } else if ('CANCELLED' == this.cert.state) {
                    this.$.certStateDiv.innerHTML = "<g:message code="certCancelledLbl"/>"
                }
            },

            certIssuerClicked:function(e) {
                var issuerSerialNumber = e.target.templateInstance.model.cert.issuerSerialNumber
                if(issuerSerialNumber != null) {
                    console.log(this.tagName + " - certIssuerClicked: " + "${createLink( controller:'certificateVS', action:'cert')}/" + issuerSerialNumber)
                }
            }
        })
    </script>
</polymer-element>