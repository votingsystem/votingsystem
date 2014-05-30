<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="/Vickets/font-awesome/css/font-awesome.min.css" type="text/css"/>
    <g:javascript library="jquery" plugin="jquery"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <asset:stylesheet src="bootstrap.min.css"/>
    <asset:javascript src="bootstrap.min.js"/>
    <asset:stylesheet src="vickets.css"/>
</head>
<body>
<div class="" style="margin: 20px;">
    <div id="transactionTypeMsg" style="font-size: 1.5em; font-weight: bold;"></div>
    <div style=""><b><g:message code="subjectLbl"/>: </b>${transactionvsMap.subject}</div>
    <div style=""><b><g:message code="amountLbl"/>: </b>${transactionvsMap.amount} ${transactionvsMap.currency}</div>
    <div style=""><b><g:message code="dateCreatedLbl"/>: </b>${transactionvsMap.dateCreated}</div>
    <g:if test="${transactionvsMap.validTo}">
        <div style=""><b><g:message code="validToLbl"/>: </b>${transactionvsMap.validTo}</div>
    </g:if>
<div style="margin-left: 20px;">
    <g:if test="${transactionvsMap.fromUserVS}">
        <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold; margin:10px 0px 0px 0px;">
            <g:message code="pagerLbl"/></div>
            <div style=""><b><g:message code="nameLbl"/>: </b>${transactionvsMap.fromUserVS.name}</div>
            <div style=""><b><g:message code="nifLbl"/>: </b>${transactionvsMap.fromUserVS.nif}</div>
    </g:if>
    <g:else>
        <div style="font-weight: bold;"><g:message code="anonymousPagerLbl"/></div>
    </g:else>
</div>
<div style="margin:20px 0px 0px 20px;">
    <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold;"><g:message code="receptorLbl"/></div>
    <div style=""><b><g:message code="nameLbl"/>: </b>${transactionvsMap.toUserVS.name}</div>
    <div style=""><b><g:message code="nifLbl"/>: </b>${transactionvsMap.toUserVS.nif}</div>
</div>
    <button id="saveReceiptButton" type="button" class="btn btn-accept-vs" onclick="saveReceipt();"
            style="margin:10px 0px 0px 0px;"><g:message code="saveReceiptLbl"/>
    </button>
    <div style="margin:20px 0px 0px 20px; font-size: 1.2em;">
        <a href="${transactionvsMap.messageSMIMEURL}" oncli><g:message code="proofLbl"/></a>
    </div>
</div>

<div id="receipt" style="display:none;">
    ${receipt}
</div>
</body>
</html>
<asset:script>

    $(function() { })

    <g:if test="${'VICKET_SEND'.equals(transactionvsMap.type)}">
        document.getElementById("transactionTypeMsg").innerHTML = "<g:message code="selectVicketSendLbl"/>"
    </g:if>
    <g:elseif test="${'USER_ALLOCATION_INPUT'.equals(transactionvsMap.type)}">
        document.getElementById("transactionTypeMsg").innerHTML = "<g:message code="selectUserAllocationInputLbl"/>"
    </g:elseif>
    <g:elseif test="${'USER_ALLOCATION'.equals(transactionvsMap.type)}">
        document.getElementById("transactionTypeMsg").innerHTML = "<g:message code="selectUserAllocationLbl"/>"
    </g:elseif>
    <g:elseif test="${'VICKET_REQUEST'.equals(transactionvsMap.type)}">
        document.getElementById("transactionTypeMsg").innerHTML = "<g:message code="selectVicketRequestLbl"/>"
    </g:elseif>
    <g:elseif test="${'VICKET_CANCELLATION'.equals(transactionvsMap.type)}">
        document.getElementById("transactionTypeMsg").innerHTML = "<g:message code="selectVicketCancellationLbl"/>"
    </g:elseif>

    function saveReceipt() {
        console.log("saveReceipt")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.SAVE_RECEIPT)
        webAppMessage.message = document.getElementById("receipt").innerHTML
        webAppMessage.callerCallback = 'saveReceiptCallback'
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function saveReceiptCallback(appMessage) {
        console.log("saveReceiptCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        console.log("saveReceiptCallback - message from native client: " + appMessage);
    }

</asset:script>
<asset:deferredScripts/>