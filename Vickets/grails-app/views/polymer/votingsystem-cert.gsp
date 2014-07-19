<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">

<polymer-element name="votingsystem-cert" attributes="url certmap-data cert subpage">
    <template>
        <style no-shim>
        .view { :host {position: relative;} }
        .pageWidth {width: 600px;}
        .certDiv {
            background-color: #fefefe;
            padding: 10px;
            height: 150px;
            -moz-border-radius: 5px; border-radius: 5px;
            overflow:hidden;
        }
        </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{cert}}" handleAs="json" method="get" contentType="json"></core-ajax>
        <div layout vertical center center-justified >
                <div layout horizontal class="pageWidth">
                    <template if="{{subpage != null || subcert != null}}">
                        <votingsystem-button isFab="true" on-click="{{back}}" style="font-size: 1.5em; margin:5px 0px 0px 0px;">
                            <i class="fa fa-arrow-left"></i></votingsystem-button>
                    </template>

                    <h3 flex>
                        <div id="pageHeaderDiv" class="pageHeader text-center"></div>
                    </h3>
                </div>

                <div class="certDiv pageWidth">
                    <div style="">
                        <div class='groupvsSubjectDiv' style="display: inline;"><span style="font-weight: bold;">
                            <g:message code="serialNumberLbl"/>: </span>{{cert.serialNumber}}</div>
                        <div id="certStateDiv" style="display: inline; margin:0px 0px 0px 20px; font-size: 1.2em; font-weight: bold; float: right;"></div>
                    </div>
                    <div class='groupvsSubjectDiv'><span style="font-weight: bold;"><g:message code="subjectLbl"/>: </span>{{cert.subjectDN}}</div>
                    <div class=''><span style="font-weight: bold;"><g:message code="issuerLbl"/>: </span>
                        <a id="issuerURL" on-click="{{certIssuerClicked}}" style="cursor: pointer;">{{cert.issuerDN}}</a>
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
        Polymer('votingsystem-cert', {
            certChanged: function() {
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
            ready: function() {
                this.certsSelectedStack = []
                if(this['certmap-data'] != null) {
                    this.cert = JSON.parse(this['certmap-data'])
                }
            },
            back:function() {
                var previousCert = this.certsSelectedStack.pop()
                if(previousCert != null) {
                    this.url = ""
                    this.cert = previousCert
                    if(this.certsSelectedStack.length == 0) this.subcert = null
                } else {
                    console.log(this.tagName + " - core-signal-votingsystem-cert-closed")
                    this.fire('core-signal', {name: "votingsystem-cert-closed", data: this.cert.id});
                }
            },
            certIssuerClicked:function(e) {
                var issuerSerialNumber = e.target.templateInstance.model.cert.issuerSerialNumber
                if(issuerSerialNumber != null) {
                    var certURL = "${createLink( controller:'certificateVS', action:'cert', absolute:true)}/" + issuerSerialNumber
                    console.log(this.tagName + " - certIssuerClicked: " + certURL)
                    this.certsSelectedStack.push(this.cert)
                    this.url = certURL
                    this.subcert = true
                }
            }
        })
    </script>
</polymer-element>