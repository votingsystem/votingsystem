<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="core-ajax" file="core-ajax.html"/>
<vs:webcss dir="font-awesome/css" file="font-awesome.min.css"/>
<vs:webresource dir="paper-fab" file="paper-fab.html"/>

<polymer-element name="votingsystem-cert" attributes="url cert subpage">
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
        <g:include view="/include/styles.gsp"/>
        <core-ajax id="ajax" auto url="{{url}}" response="{{cert}}" handleAs="json" method="get" contentType="json"></core-ajax>
        <div layout vertical center center-justified >
                <div layout horizontal class="pageWidth">
                    <template if="{{subpage != null || subcert != null}}">
                        <div style="margin: 20px 0 0 0;" title="<g:message code="backLbl"/>" >
                            <paper-fab icon="arrow-back" on-click="{{back}}" style="color: white;"></paper-fab>
                        </div>
                    </template>

                    <h3 flex>
                        <div id="pageHeaderDiv" class="pageHeader text-center"></div>
                    </h3>
                </div>

                <div class="certDiv pageWidth">
                    <div style="">
                        <div class='groupvsSubjectDiv' style="display: inline;"><span style="font-weight: bold;">
                            <g:message code="serialNumberLbl"/>: </span>{{certvs.serialNumber}}</div>
                        <div id="certStateDiv" style="display: inline; margin:0px 0px 0px 20px; font-size: 1.2em; font-weight: bold; float: right;"></div>
                    </div>
                    <div class='groupvsSubjectDiv'><span style="font-weight: bold;"><g:message code="subjectLbl"/>: </span>{{certvs.subjectDN}}</div>
                    <div class=''><span style="font-weight: bold;"><g:message code="issuerLbl"/>: </span>
                        <a id="issuerURL" on-click="{{certIssuerClicked}}" style="cursor: pointer;">{{certvs.issuerDN}}</a>
                    </div>
                    <div class=''><span style="font-weight: bold;"><g:message code="signatureAlgotithmLbl"/>: </span>{{certvs.sigAlgName}}</div>
                    <div>
                        <div class='' style="display: inline;">
                            <span style="font-weight: bold;"><g:message code="noBeforeLbl"/>: </span>{{certvs.notBefore}}</div>
                        <div class='' style="display: inline; margin:0px 0px 0px 20px;">
                            <span style="font-weight: bold;"><g:message code="noAfterLbl"/>: </span>{{certvs.notAfter}}</div>
                    </div>
                    <div>
                        <g:if test="{{certvs.isRoot}}">
                            <div class="text-center" style="font-weight: bold; display: inline;
                            margin:0px auto 0px auto;color: #6c0404; float:right; text-decoration: underline;"><g:message code="rootCertLbl"/></div>
                        </g:if>
                    </div>
                </div>
                <div style="width: 600px; max-height:400px; overflow-y: auto; margin:20px auto 0px auto;">
                    <div>{{certvs.description}}</div>
                </div>

                <div style="width: 600px; margin:20px auto 0px auto;">
                    <div><label><g:message code="certPublicKeyLbl"/></label></div>
                    <div>
                        <textarea id="pemCertTextArea" style="width:520px; height:300px;font-family: monospace; font-size:0.8em;" readonly></textarea>
                    </div>
                </div>

        </div>
    </template>
    <script>
        Polymer('votingsystem-cert', {
            publish: {
                certvs: {value: {}}
            },
            certvsChanged: function() {
                this.$.pemCertTextArea.value = this.certvs.pemCert
                if('CERTIFICATE_AUTHORITY' == this.certvs.type) {
                    this.$.pageHeaderDiv.innerHTML = "<g:message code="trustedCertPageTitle"/>"
                } else if ('USER' == this.certvs.type) {
                    this.$.pageHeaderDiv.innerHTML = "<g:message code="userCertPageTitle"/>"
                }
                if('OK' == this.certvs.state) {
                    this.$.certStateDiv.innerHTML = "<g:message code="certOKLbl"/>"
                } else if ('CANCELLED' == this.certvs.state) {
                    this.$.certStateDiv.innerHTML = "<g:message code="certCancelledLbl"/>"
                }
            },
            ready: function() {
                this.certsSelectedStack = []
                if(this.certvs != null) this.certvsChanged()
            },
            back:function() {
                var previousCert = this.certsSelectedStack.pop()
                if(previousCert != null) {
                    this.url = ""
                    this.certvs = previousCert
                    if(this.certsSelectedStack.length == 0) this.subcert = null
                } else {
                    console.log(this.tagName + " - core-signal-votingsystem-cert-closed")
                    this.fire('core-signal', {name: "votingsystem-cert-closed", data: this.certvs.id});
                }
            },
            certIssuerClicked:function(e) {
                var issuerSerialNumber = e.target.templateInstance.model.certvs.issuerSerialNumber
                if(issuerSerialNumber != null) {
                    var certURL = "${createLink( controller:'certificateVS', action:'cert', absolute:true)}/" + issuerSerialNumber
                    console.log(this.tagName + " - certIssuerClicked: " + certURL)
                    this.certsSelectedStack.push(this.certvs)
                    this.url = certURL
                    this.subcert = true
                }
            }
        })
    </script>
</polymer-element>