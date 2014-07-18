<%@ page import="org.votingsystem.model.CertificateVS" %>
<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/certificateVS/cert-list.gsp']"/>">
    <style type="text/css" media="screen"></style>
</head>
<body>
<div class="pageContenDiv" style="max-width: 1300px; margin: 0px auto 0px auto;">
    <div class="row">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="certsPageTitle"/></li>
        </ol>
    </div>
    <div layout horizontal center center-justified>
        <select id="certTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;" class="form-control" onchange="certTypeSelect(this)">
            <option value="&type=USER&state=OK"> - <g:message code="certUserStateOKLbl"/> - </option>
            <option value="&type=CERTIFICATE_AUTHORITY&state=OK"> - <g:message code="certAuthorityStateOKLbl"/> - </option>
            <option value="&type=USER&state=CANCELLED"> - <g:message code="certUserStateCancelledLbl"/> - </option>
            <option value="&type=CERTIFICATE_AUTHORITY&state=CANCELLED"> - <g:message code="certAuthorityStateCancelledLbl"/> - </option>
        </select>
    </div>

    <h3><div id="pageHeader" class="pageHeader text-center"><g:message code="trustedCertsPageTitle"/></div></h3>

    <cert-list id="certList"></cert-list>

</div>
</body>
</html>
<asset:script>
    document.querySelector("#coreSignals").addEventListener('core-signal-cert-selected', function(e) {
        console.log('core-signal-cert-selected' + e.detail)
        //"${createLink( controller:'certificateVS', action:'cert')}/" + issuerSerialNumber + "?menu=" + menuType
    });


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