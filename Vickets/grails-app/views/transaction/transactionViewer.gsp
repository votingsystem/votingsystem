<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/bootstrap/dist/css', file: 'bootstrap.min.css')}" type="text/css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <asset:stylesheet src="vickets.css"/>
</head>
<body>
<div class="" style="max-width:1000px; margin: 0px auto 0px auto; padding:20px 30px 0px 30px;">
    <div id="transactionTypeMsg" style="font-size: 1.5em; font-weight: bold;"></div>
    <g:if test="${transactionvsMap?.tags != null && !transactionvsMap.tags.isEmpty()}">
        <div id="tagsDiv" style="padding:0px 0px 0px 30px;">
            <div style=" display: table-cell; font-size: 1.1em; font-weight: bold;"><g:message code='tagsLbl'/>:</div>
            <div id="selectedTagDiv" style="margin:0px 0px 15px 0px; padding: 5px 5px 0px 5px; display: table-cell;" class="btn-group-xs">
                <g:each in="${transactionvsMap?.tags}">
                    <a class="btn btn-default" href="#" role="button" style="margin:0px 10px 0px 0px;">${it.name}</a>
                </g:each>
            </div>
        </div>
    </g:if>
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
                    <div style=""><b><g:message code="nifLbl"/>: </b>${transactionvsMap.fromUserVS.nif}</div>
                    <div style=""><b><g:message code="nameLbl"/>: </b>${transactionvsMap.fromUserVS.name}</div>
                </div>
        </g:if>
        <g:else>
            <div style="font-weight: bold;"><g:message code="anonymousPagerLbl"/></div>
        </g:else>
    </div>

<g:if test="${transactionvsMap.childTransactions && !transactionvsMap.childTransactions.isEmpty()}">
    <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold;margin:10px 0px 0px 0px;">
        <g:message code="transactionsTriggeredLbl"/></div>
    <g:each in="${transactionvsMap.childTransactions}">
        <div style="margin:0px 0px 0px 20px;">
            <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold;"><g:message code="receptorLbl"/></div>
            <div id="toUserDiv">
                <div style=""><b><g:message code="nifLbl"/>: </b>${it.toUserVS.nif}</div>
                <div style=""><b><g:message code="nameLbl"/>: </b>${it.toUserVS.name}</div>
                <div style=""><b><g:message code="amountLbl"/>: </b>${it.amount} ${it.currency}</div>
            </div>
        </div >
    </g:each>
</g:if>
<g:elseif test="${transactionvsMap.toUserVS}">
    <div style="margin:20px 0px 0px 20px;">
        <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold;"><g:message code="receptorLbl"/></div>
        <div id="toUserDiv">
            <div style=""><b><g:message code="nifLbl"/>: </b>${transactionvsMap.toUserVS.nif}</div>
            <div style=""><b><g:message code="nameLbl"/>: </b>${transactionvsMap.toUserVS.name}</div>
        </div>
    </div >
</g:elseif>

    <div style="max-width: 600px;">
        <button type="button" class="btn btn-accept-vs" onclick="openReceipt();"
                style="margin:10px 0px 0px 0px; float:right;"><g:message code="openReceiptLbl"/>
        </button>
    </div>
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
    var fromUserTemplate
    var toUserTemplate

    document.getElementById("transactionTypeMsg").innerHTML = getTransactionVSDescription("${transactionvsMap.type}")

    <g:if test="${'VICKET_SOURCE_INPUT'.equals(transactionvsMap.type)}">
        fromUserTemplate = document.getElementById("fromUserVicketSource").innerHTML
        var fromUserIBANInfoURL = "${createLink(uri:'/IBAN')}/from/${transactionvsMap.fromUserVS.payer.fromUserIBAN}"
        document.getElementById("fromUserDiv").innerHTML = fromUserTemplate.format("${transactionvsMap.fromUserVS.payer.fromUser}",
            "${transactionvsMap.fromUserVS.payer.fromUserIBAN}", fromUserIBANInfoURL)
    </g:if>

    <g:if test="${'GROUP'.equals(transactionvsMap.fromUserVS.type)}">
        fromUserTemplate = document.getElementById("groupUserData").innerHTML
        document.getElementById("fromUserDiv").innerHTML = fromUserTemplate.format("${transactionvsMap.fromUserVS.name}")
    </g:if>
    <g:if test="${'GROUP'.equals(transactionvsMap.toUserVS?.type)}">
        toUserTemplate = document.getElementById("groupUserData").innerHTML
        document.getElementById("toUserDiv").innerHTML = toUserTemplate.format("${transactionvsMap.toUserVS.name}",
            "${transactionvsMap.toUserVS.id}")
    </g:if>

    function openReceipt() {
        console.log("openReceipt")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.OPEN_RECEIPT)
        webAppMessage.message = document.getElementById("receipt").innerHTML.trim()
        var objectId = Math.random().toString(36).substring(7)
        window[objectId] = {setClientToolMessage: function(appMessage) {
            console.log("openReceiptCallback - message: " + appMessage);
            var appMessageJSON = toJSON(appMessage)
            console.log("openReceiptCallback - message from native client: " + appMessage);  }}
        webAppMessage.callerCallback = objectId
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function saveReceipt() {
        console.log("saveReceipt")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.SAVE_RECEIPT)
        webAppMessage.message = document.getElementById("receipt").innerHTML.trim()
        var objectId = Math.random().toString(36).substring(7)
        window[objectId] = {setClientToolMessage: function(appMessage) {
            console.log("saveReceiptCallback - message from native client: " + appMessage);
            var appMessageJSON = toJSON(appMessage)
            console.log("saveReceiptCallback - message from native client: " + appMessage); }}
        webAppMessage.callerCallback = objectId
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }
</asset:script>
<asset:deferredScripts/>