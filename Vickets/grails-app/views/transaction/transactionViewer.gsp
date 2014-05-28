<%
    def transactionTypeMsg
    if('VICKET_SEND'.equals(transactionvsMap.type))
        transactionTypeMsg =  message(code: "selectVicketSendLbl")
    else if('USER_ALLOCATION_INPUT'.equals(transactionvsMap.type))
        transactionTypeMsg =  message(code: "selectUserAllocationInputLbl")
    else if('USER_ALLOCATION'.equals(transactionvsMap.type))
        transactionTypeMsg =  message(code: "selectUserAllocationLbl")
    else if('VICKET_REQUEST'.equals(transactionvsMap.type))
        transactionTypeMsg =  message(code: "selectVicketRequestLbl")
    else if('VICKET_CANCELLATION'.equals(transactionvsMap.type))
        transactionTypeMsg =  message(code: "selectVicketCancellationLbl")
%>
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
    <div style="font-size: 1.5em; font-weight: bold;">${transactionTypeMsg}</div>
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

    $(function() {

    })

    function saveReceipt() {
        console.log("saveReceipt")
        VotingSystemClient.setTEXTMessageToSignatureClient($("#receipt").text().trim(), getFnName(saveReceiptCallback))
    }

    function saveReceiptCallback(appMessage) {
        console.log("saveReceiptCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        console.log("saveReceiptCallback - message from native client: " + appMessage);
    }

</asset:script>
<asset:deferredScripts/>