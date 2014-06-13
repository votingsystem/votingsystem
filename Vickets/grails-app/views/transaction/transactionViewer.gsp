<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="${resource(dir: 'font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
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
    <div style=""><b><g:message code="dateCreatedLbl"/>: </b>${formatDate(date:transactionvsMap.dateCreated, formatName:'webViewDateFormat')}</div>
    <g:if test="${transactionvsMap.validTo}">
        <div style=""><b><g:message code="validToLbl"/>: </b>${formatDate(date:transactionvsMap.validTo, formatName:'webViewDateFormat')}</div>
    </g:if>
<div style="margin-left: 20px;">
    <g:if test="${transactionvsMap.fromUserVS}">
        <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold; margin:10px 0px 0px 0px;">
            <g:message code="pagerLbl"/></div>
            <div id="fromUserDiv">
                <div style=""><b><g:message code="nameLbl"/>: </b>${transactionvsMap.fromUserVS.name}</div>
                <div style=""><b><g:message code="nifLbl"/>: </b>${transactionvsMap.fromUserVS.nif}</div>
            </div>
    </g:if>
    <g:else>
        <div style="font-weight: bold;"><g:message code="anonymousPagerLbl"/></div>
    </g:else>
</div>
<div style="margin:20px 0px 0px 20px;">
    <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold;"><g:message code="receptorLbl"/></div>
    <div id="toUserDiv">
        <div style=""><b><g:message code="nameLbl"/>: </b>${transactionvsMap.toUserVS.name}</div>
        <div style=""><b><g:message code="nifLbl"/>: </b>${transactionvsMap.toUserVS.nif}</div>
    </div>
</div>
    <button id="saveReceiptButton" type="button" class="btn btn-accept-vs" onclick="saveReceipt();"
            style="margin:10px 0px 0px 0px; float:right;"><g:message code="saveReceiptLbl"/>
    </button>
</div>

<div id="receipt" style="display:none;">
    ${receipt}
</div>
<div id="fromUserVicketSource" style="display:none;">
    <div style=""><b><g:message code="nameLbl"/>: </b>{0}</div>
    <div style=""><b><g:message code="IBANLbl"/>: </b>
        <a href="{2}">{1}</a></div>
</div>
<div id="groupUserData" style="display:none;">
    <div style=""><b><g:message code="groupLbl"/>: </b><a href="${createLink(uri:'/groupVS')}/{1}">{0}</a></div>
</div>
</body>
</html>
<asset:script>

    $(function() { })
    var fromUserTemplate
    var toUserTemplate

    <g:if test="${'VICKET_SEND'.equals(transactionvsMap.type)}">
        document.getElementById("transactionTypeMsg").innerHTML = "<g:message code="selectVicketSendLbl"/>"
    </g:if>
    <g:elseif test="${'VICKET_SOURCE_INPUT'.equals(transactionvsMap.type)}">
        document.getElementById("transactionTypeMsg").innerHTML = "<g:message code="vicketSourceInputLbl"/>"
        fromUserTemplate = document.getElementById("fromUserVicketSource").innerHTML
        var fromUserIBANInfoURL = "${createLink(uri:'/IBAN')}/from/${transactionvsMap.fromUserVS.payer.fromUserIBAN}"
        document.getElementById("fromUserDiv").innerHTML = fromUserTemplate.format("${transactionvsMap.fromUserVS.payer.fromUser}",
            "${transactionvsMap.fromUserVS.payer.fromUserIBAN}", fromUserIBANInfoURL)


    </g:elseif>
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


    <g:if test="${'GROUP'.equals(transactionvsMap.fromUserVS.type)}">
        fromUserTemplate = document.getElementById("groupUserData").innerHTML
        document.getElementById("fromUserDiv").innerHTML = fromUserTemplate.format("${transactionvsMap.fromUserVS.name}")
    </g:if>
    <g:if test="${'GROUP'.equals(transactionvsMap.toUserVS.type)}">
        toUserTemplate = document.getElementById("groupUserData").innerHTML
        document.getElementById("toUserDiv").innerHTML = toUserTemplate.format("${transactionvsMap.toUserVS.name}",
            "${transactionvsMap.toUserVS.id}")
    </g:if>


       function saveReceipt() {
           console.log("saveReceipt")
           var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.SAVE_RECEIPT)
           webAppMessage.message = document.getElementById("receipt").innerHTML.trim()
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