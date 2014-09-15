<%@ page import="org.votingsystem.model.CertificateVS" %>
<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/certificateVS/votingsystem-cert']"/>">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">

<polymer-element name="cert-list" attributes="url menuType">
    <template>
        <style no-shim>
            .view { :host {position: relative;} }
            .certDiv {
                width:540px;
                height: 140px;
                padding: 10px;
                border:1px solid #ccc;
                background-color: #fefefe;
                margin: 10px 5px 5px 10px;
                -moz-border-radius: 5px; border-radius: 5px;
                cursor: pointer;
                overflow:hidden;
                position: relative;
                display: inline-block;
                vertical-align: top;
                box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
            }
        </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" handleAs="json" method="get" contentType="json"></core-ajax>
        <core-signals on-core-signal-votingsystem-cert-closed="{{closeCertDetails}}"></core-signals>
        <core-animated-pages id="pages" flex selected="{{page}}" on-core-animated-pages-transition-end="{{transitionend}}"
                             transitions="cross-fade-all">
            <section id="page1">
                <div cross-fade>
                    <div layout horizontal center center-justified>
                        <select id="certTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;" on-change="{{certTypeSelect}}">
                            <option value="&type=USER&state=OK"> - <g:message code="certUserStateOKLbl"/> - </option>
                            <option value="&type=CERTIFICATE_AUTHORITY&state=OK"> - <g:message code="certAuthorityStateOKLbl"/> - </option>
                            <option value="&type=USER&state=CANCELLED"> - <g:message code="certUserStateCancelledLbl"/> - </option>
                            <option value="&type=CERTIFICATE_AUTHORITY&state=CANCELLED"> - <g:message code="certAuthorityStateCancelledLbl"/> - </option>
                        </select>
                    </div>

                    <h3><div id="pageHeader" class="pageHeader text-center"><g:message code="trustedCertsPageTitle"/></div></h3>

                    <div flex horizontal wrap around-justified layout>
                        <template repeat="{{cert in responseData.certList}}">
                            <div class="certDiv" style="" on-tap="{{showCert}}">
                                <div>
                                    <div style="display: inline;">
                                        <div class='groupvsSubjectDiv' style="display: inline;"><span style="font-weight: bold;">
                                            <g:message code="serialNumberLbl"/>: </span>{{cert.serialNumber}}</div>
                                        <div id="certStateDiv" style="display: inline; margin:0px 0px 0px 20px; font-size: 1.2em;
                                        font-weight: bold; float: right;">{{cert.state | getState}}</div>
                                    </div>
                                </div>
                                <div class='groupvsSubjectDiv'><span style="font-weight: bold;"><g:message code="subjectLbl"/>: </span>{{cert.subjectDN}}</div>
                                <div class=''><span style="font-weight: bold;"><g:message code="issuerLbl"/>: </span>
                                    {{cert.issuerDN}}</div>
                                <div class=''><span style="font-weight: bold;"><g:message code="signatureAlgotithmLbl"/>: </span>{{cert.sigAlgName}}</div>
                                <div>
                                    <div class='' style="display: inline;">
                                        <span style="font-weight: bold;"><g:message code="noBeforeLbl"/>: </span>{{cert.notBefore}}</div>
                                    <div class='' style="display: inline; margin:0px 0px 0px 20px;">
                                        <span style="font-weight: bold;"><g:message code="noAfterLbl"/>: </span>{{cert.notAfter}}</div>
                                </div>
                                <div>
                                    <div class="text-center" style="font-weight: bold; display: {{cert.isRoot ? 'inline': 'none'}};
                                    margin:0px auto 0px auto;color: #6c0404; float:right; text-decoration: underline;"><g:message code="rootCertLbl"/></div>
                                </div>
                            </div>
                        </template>
                    </div>
                </div>
            </section>
            <section id="page2">
                <div cross-fade>
                    <votingsystem-cert id="certDetails" subpage cert="{{cert}}"></votingsystem-cert>
                </div>
            </section>
        </core-animated-pages>
    </template>
    <script>
        Polymer('cert-list', {
            ready: function() {
                console.log(this.tagName + " - ready")
                this.page = 0;
                var certType = getParameterByName('type')
                var certState = getParameterByName('state')
                var optionValue = "&type=" + ((certType == "")? "USER":certType) + "&state=" + ((certState == "")?"OK":certState)
                if(this.url != null) {
                    this.url = updateMenuLink("<g:createLink controller="certificateVS" action="certs"/>", optionValue)
                }
            },
            closeCertDetails:function(e, detail, sender) {
                console.log(this.tagName + " - closeCertDetails")
                this.page = 0;
            },
            showCert :  function(e) {
                console.log(this.tagName + " - showCertDetails")
                this.$.certDetails.certvs = e.target.templateInstance.model.cert;
                this.page = 1;
            },
            responseDataChanged:function() {
                console.log(this.tagName + " - responseDataChanged")
                if("${CertificateVS.Type.USER.toString()}" == this.responseData.type) {
                     this.$.pageHeader.innerHTML = "<g:message code="userCertsPageTitle"/>"
                } else if("${CertificateVS.Type.CERTIFICATE_AUTHORITY.toString()}" == this.responseData.type) {
                    this.$.pageHeader.innerHTML = "<g:message code="trustedCertsPageTitle"/>"
                } else this.$.pageHeader.innerHTML = this.responseData.type


                var certType = getParameterByName('type')
                var certState = getParameterByName('state')
                var optionValue = "&type=" + ((certType == "")? "USER":certType) + "&state=" + ((certState == "")?"OK":certState)
                this.setPageHeader(optionValue)
            },
            getState:function(state){
                var stateLbl
                if("OK" == state) stateLbl = "<g:message code="certOKLbl"/>"
                else if("CANCELLED" ==  state) stateLbl = "<g:message code="certCancelledLbl"/>"
                else stateLbl = state
            },
            setPageHeader: function (urlParams) {
                if(urlParams.indexOf("${CertificateVS.Type.USER}") > -1)
                    this.$.pageHeader.innerHTML = "<g:message code="userCertsPageTitle"/>"
                else this.$.pageHeader.innerHTML = "<g:message code="trustedCertsPageTitle"/>"
            },
            certTypeSelect: function () {
                var optionSelected = this.$.certTypeSelect.value
                console.log("certTypeSelect: " + optionSelected)
                if("" != optionSelected) {
                    targetURL = "${createLink(controller: 'certificateVS', action: 'certs')}?menu=" + menuType + optionSelected
                    history.pushState(null, null, targetURL);
                    this.setPageHeader(targetURL)
                    this.$.ajax.url = targetURL
                }
            }
        });
    </script>
</polymer-element>