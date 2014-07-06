<%@ page import="org.votingsystem.model.CertificateVS" %>
<!DOCTYPE html>
<html>
<head>

    <meta name="layout" content="main" />
    <link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
    <style type="text/css" media="screen">
        .certDiv {
            display:inline;
            width:580px;
            height: 300px;
            padding: 10px;
            background-color: #f2f2f2;
            margin: 10px 5px 5px 10px;
            -moz-border-radius: 5px; border-radius: 5px;
            cursor: pointer;
            max-height:165px;
            overflow:hidden;
            float:left;
        }
    </style>
</head>
<body>
<div class="pageContenDiv" style="max-width: 1300px; margin: 0px auto 0px auto;">
    <div class="row">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="certsPageTitle"/></li>
        </ol>
    </div>
    <div style="display: table;width:90%;vertical-align: middle;margin:0px 0 10px 0px;">
        <div id="certTypeSelectDiv" style="display:table-cell;margin: auto; vertical-align: top;">
            <select id="certTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;" class="form-control" onchange="certTypeSelect(this)">
                <option value="&type=USER&state=OK"> - <g:message code="certUserStateOKLbl"/> - </option>
                <option value="&type=CERTIFICATE_AUTHORITY&state=OK"> - <g:message code="certAuthorityStateOKLbl"/> - </option>
                <option value="&type=USER&state=CANCELLED"> - <g:message code="certUserStateCancelledLbl"/> - </option>
                <option value="&type=CERTIFICATE_AUTHORITY&state=CANCELLED"> - <g:message code="certAuthorityStateCancelledLbl"/> - </option>
            </select>
        </div>
    </div>

    <h3><div id="pageHeader" class="pageHeader text-center"><g:message code="trustedCertsPageTitle"/></div></h3>

    <polymer-element name="cert-list" attributes="url menuType">
        <template>
            <style></style>
            <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" handleAs="json" method="get" contentType="json"></core-ajax>
            <div flex horizontal wrap around-justified layout>
                <template repeat="{{cert in responseData.certList}}">
                    <div class="certDiv card" style="" on-tap="{{showCert}}">
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
                            <a href="{{ cert.issuerSerialNumber | issuerURL}}">{{cert.issuerDN}}</a></div>
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
                    if("${CertificateVS.State.OK.toString()}" == state) stateLbl = "<g:message code="certOKLbl"/>"
                    else if("${CertificateVS.State.CANCELLED.toString()}" ==  state) stateLbl = "<g:message code="certCancelledLbl"/>"
                    else stateLbl = state
                },
                issuerURL: function(issuerSerialNumber) {
                    var issuerTargetURL = "#"
                    if(issuerSerialNumber != null) issuerTargetURL =
                            "${createLink( controller:'certificateVS', action:'cert')}/" + issuerSerialNumber + "?menu=" + menuType
                    return issuerTargetURL
                }
            });
        </script>
    </polymer-element>

    <cert-list id="certList"></cert-list>


</div>
</body>
</html>
<asset:script>
    <g:if test="${CertificateVS.Type.USER.toString().equals(certsMap.type)}">
        document.getElementById("pageHeader").innerHTML = "<g:message code="userCertsPageTitle"/>"
    </g:if><g:elseif test="${CertificateVS.Type.CERTIFICATE_AUTHORITY.toString().equals(certsMap.type)}">
        document.getElementById("pageHeader").innerHTML = "<g:message code="trustedCertsPageTitle"/>"
    </g:elseif><g:else>
        document.getElementById("pageHeader").innerHTML = "${certsMap?.type?.toString()}"
    </g:else>

    var certType = getParameterByName('type')
    var certState = getParameterByName('state')
    var optionValue = "&type=" + ((certType == "")? "USER":certType) + "&state=" + ((certState == "")?"OK":certState)
    setPageHeader(optionValue)

    document.querySelector("#certList").url = updateMenuLink("<g:createLink controller="certificateVS" action="certs"/>", optionValue)

    function certTypeSelect(selected) {
        var optionSelected = selected.value
        console.log("certTypeSelect: " + optionSelected)
        if("" != optionSelected) {
            targetURL = "${createLink(controller: 'certificateVS', action: 'certs')}?menu=" + menuType + optionSelected
            history.pushState(null, null, targetURL);
            setPageHeader(targetURL)
            document.querySelector("#certList").url = targetURL
        }
    }

    function setPageHeader(urlParams) {
        if(urlParams.indexOf("${CertificateVS.Type.USER}") > -1)
            document.getElementById("pageHeader").innerHTML = "<g:message code="userCertsPageTitle"/>"
        else document.getElementById("pageHeader").innerHTML = "<g:message code="trustedCertsPageTitle"/>"
    }

</asset:script>