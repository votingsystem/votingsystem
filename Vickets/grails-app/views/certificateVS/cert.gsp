<%@ page import="org.votingsystem.model.CertificateVS" %>
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
            <li><a href="${createLink(controller: 'certificateVS', action: 'certs')}"><g:message code="certsPageTitle"/></a></li>
            <li class="active"><g:message code="trustedCertPageTitle"/></li>
        </ol>
    </div>

    <div id="adminButtonsDiv" class=""  style="width: 600px; margin:20px auto 0px auto;">
        <g:if test="${"admin".equals(params.menu) || "superadmin".equals(params.menu)}">
            <button id="cancelCertButton" type="submit" class="btn btn-warning"
                    style="margin:10px 20px 0px 0px;" onclick="showCancelSubscriptionFormDialog(cancelCert)">
                <g:message code="cancelCertLbl"/> <i class="fa fa fa-check"></i>
            </button>
        </g:if>
    </div>

    <h3>
        <div id="pageHeaderDiv" class="pageHeader text-center"></div>
    </h3>

    <div style="width: 100%;">
        <div class="certDiv" style="margin:0px auto 0px auto;">
            <div style="display: inline;">
                <div class='groupvsSubjectDiv' style="display: inline;"><span style="font-weight: bold;">
                    <g:message code="serialNumberLbl"/>: </span>${certMap.serialNumber}</div>
                <div id="certStateDiv" style="display: inline; margin:0px 0px 0px 20px; font-size: 1.2em; font-weight: bold; float: right;"></div>
            </div>
            <div class='groupvsSubjectDiv'><span style="font-weight: bold;"><g:message code="subjectLbl"/>: </span>${certMap.subjectDN}</div>
            <div class=''><span style="font-weight: bold;"><g:message code="issuerLbl"/>: </span>
                <a id="issuerURL" href="#">${certMap.issuerDN}</a>
                </div>
            <div class=''><span style="font-weight: bold;"><g:message code="signatureAlgotithmLbl"/>: </span>${certMap.sigAlgName}</div>
            <div>
                <div class='' style="display: inline;">
                    <span style="font-weight: bold;"><g:message code="noBeforeLbl"/>: </span>${certMap.notBefore}</div>
                <div class='' style="display: inline; margin:0px 0px 0px 20px;">
                    <span style="font-weight: bold;"><g:message code="noAfterLbl"/>: </span>${certMap.notAfter}</div>
            </div>
            <div>
                <g:if test="${certMap.isRoot}">
                    <div class="text-center" style="font-weight: bold; display: inline;
                    margin:0px auto 0px auto;color: #6c0404; float:right; text-decoration: underline;"><g:message code="rootCertLbl"/></div>
                </g:if>
            </div>
        </div>
        <div style="width: 600px; max-height:400px; overflow-y: auto; margin:20px auto 0px auto;">
            <div>${raw(certMap.description)}</div>
        </div>

        <div style="width: 600px; margin:20px auto 0px auto;">
            <label><g:message code="certPublicKeyLbl"/></label>
            <textarea id="pemCertTextArea" class="form-control" rows="20" readonly></textarea>
        </div>
    </div>
</div>
<div id="pemCertDiv" style="display:none;">${certMap.pemCert}</div>

<g:include view="/include/dialog/cancelCertFormDialog.gsp"/>
</body>
</html>
<asset:script>
    $(function() {
        <g:if test="${CertificateVS.Type.CERTIFICATE_AUTHORITY.toString().equals(certMap.type)}">
            document.getElementById('pageHeaderDiv').innerHTML = "<g:message code="trustedCertPageTitle"/>"
        </g:if><g:elseif test="${CertificateVS.Type.USER.toString().equals(certMap.type)}">
            document.getElementById('pageHeaderDiv').innerHTML = "<g:message code="userCertPageTitle"/>"
        </g:elseif>
        <g:if test="${certMap.issuerSerialNumber}">
            document.getElementById('issuerURL').href =
                "${createLink( controller:'certificateVS', action:'cert')}/${certMap.issuerSerialNumber}"
        </g:if>
        document.getElementById('pemCertTextArea').value = document.getElementById("pemCertDiv").innerHTML.trim()
        <g:if test="${CertificateVS.State.OK.toString().equals(certMap.state)}">
            document.getElementById('certStateDiv').innerHTML = "<g:message code="certOKLbl"/>"
        </g:if>
        <g:elseif test="${CertificateVS.State.CANCELLED.toString().equals(certMap.state)}">
            document.getElementById('certStateDiv').innerHTML = "<g:message code="certCancelledLbl"/>"
            if(document.getElementById('cancelCertButton') != null)
                document.getElementById('cancelCertButton').style.display = "none"
        </g:elseif>

    })

    function cancelCert () {
        console.log("cancelCert")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.CERT_EDIT)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.serviceURL = "${createLink(controller:'certificateVS', action:'editCert',absolute:true)}"
        webAppMessage.signedMessageSubject = "<g:message code="cancelCertMessageSubject"/>"
        webAppMessage.signedContent = {operation:Operation.CERT_EDIT, reason:$("#cancelCertReason").val(),
            changeCertToState:"${CertificateVS.State.CANCELLED.toString()}", serialNumber:"${certMap.serialNumber}"}
        //signed and encrypted
        webAppMessage.contentType = 'application/x-pkcs7-signature'
        webAppMessage.callerCallback = 'cancelCertCallback'
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function cancelCertCallback () {
        location.reload();
    }
</asset:script>