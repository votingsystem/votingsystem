<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">

<polymer-element name="cert-list" attributes="url menuType">
    <template>
        <style no-shim>
            .view { :host {position: relative;} }
            .certDiv {
                width:580px;
                height: 200px;
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
                        <a on-click="{{showIssuer}}" href="">{{cert.issuerDN}}</a></div>
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
    </template>
    <script>
        Polymer('cert-list', {
            ready: function() {},
            showCert :  function(e) {
                var targetURL = "${createLink( controller:'certificateVS', action:'cert')}/" +
                e.target.templateInstance.model.cert.serialNumber + "?menu=" + menuType
                window.location.href = targetURL
            },
            getState:function(state){
                var stateLbl
                if("OK" == state) stateLbl = "<g:message code="certOKLbl"/>"
                else if("CANCELLED" ==  state) stateLbl = "<g:message code="certCancelledLbl"/>"
                else stateLbl = state
            },
            showIssuer: function(e) {
                this.fire('core-signal', {name: "cert-selected", data: e.target.templateInstance.model.cert.issuerSerialNumber});
            }
        });
    </script>
</polymer-element>