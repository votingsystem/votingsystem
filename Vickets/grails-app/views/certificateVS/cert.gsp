<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
    <style type="text/css" media="screen">
        .certDiv {
            width: 600px;
            background-color: #f2f2f2;
            padding: 10px;
            height: 150px;
            -moz-border-radius: 5px; border-radius: 5px;
            overflow:hidden;
        }
    </style>
</head>
<body>
<div class="pageContenDiv">
    <div class="row" style="max-width: 1300px; margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="trustedCertPageTitle"/></li>
        </ol>
    </div>
    <h3>
        <div class="pageHeader text-center"><g:message code="trustedCertPageTitle"/></div>
    </h3>

    <div style="width: 100%;">
        <div class="certDiv" style="margin:0px auto 0px auto;">
            <a id="targetURL" href="${createLink( controller:'certificateVS', action:'trusted')}"></a>
            <div class='groupvsSubjectDiv'><span style="font-weight: bold;"><g:message code="serialNumberLbl"/>: </span>${certMap.serialNumber}</div>
            <div class='groupvsSubjectDiv'><span style="font-weight: bold;"><g:message code="subjectLbl"/>: </span>${certMap.subjectDN}</div>
            <div class=''><span style="font-weight: bold;"><g:message code="issuerLbl"/>: </span>${certMap.issuerDN}</div>
            <div class=''><span style="font-weight: bold;"><g:message code="signatureAlgotithmLbl"/>: </span>${certMap.sigAlgName}</div>
            <div>
                <div class='' style="display: inline;">
                    <span style="font-weight: bold;"><g:message code="noBeforeLbl"/>: </span>${certMap.notBefore}</div>
                <div class='' style="display: inline; margin:0px 0px 0px 20px;">
                    <span style="font-weight: bold;"><g:message code="noAfterLbl"/>: </span>${certMap.notAfter}</div>
            </div>
            <div>
                <a href="${createLink( controller:'certificateVS', action:'trusted')}/${certMap.serialNumber}?format=pem" style="display: inline; ">
                    <g:message code="downloadPEMCertLbl"/></a>
                <g:if test="${certMap.isRoot}">
                    <div class="text-center" style="font-weight: bold; display: inline;
                    margin:0px auto 0px auto;color: #6c0404; float:right; text-decoration: underline;"><g:message code="rootCertLbl"/></div>
                </g:if>
            </div>
        </div>
    </div>
</div>

</body>

</html>
<asset:script>
    $(function() {

        $('.certDiv').click(function(event) {
            event.preventDefault();

            $("#targetURL").trigger('click');})

    })
</asset:script>