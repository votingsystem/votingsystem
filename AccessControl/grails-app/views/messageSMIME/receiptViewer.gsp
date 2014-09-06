<!DOCTYPE html>
<html>
<head>
    <title>
        <g:if test="${receiptPageTitle != null}">${receiptPageTitle}</g:if>
        <g:else><g:message code="receiptPageLbl"/></g:else>
    </title>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
</head>

<body style="max-width: 600px; margin:30px auto 0px auto;">
<div id="voteReceiptContentDiv"  style="display:none; border: 1px solid #6c0404; width: 500px;margin:auto; padding: 15px;">
    <div style="font-size: 1.2em; color:#6c0404; font-weight: bold;"><g:message code="voteReceiptLbl"/></div>
    <a id="pollPage"><g:message code="voteReceiptContentPollPageLbl"/></a>
    <div><g:message code="optionSelectedLbl"/>: <span id="optionSlected"></span></div>
</div>
<button id="saveReceiptButton" type="button" class="btn btn-accept-vs" onclick="saveReceipt();"
        style="display:none; margin:10px 0px 0px 0px; float: right;">
    <g:message code="saveReceiptLbl"/></button>


<div id="receipt" style="display:none;">
    ${receipt}
</div>

</body>
</html>
<asset:script>
    var signedContent = toJSON('${raw(signedContent)}')

    if(signedContent.operation) {
        if('SEND_SMIME_VOTE' == signedContent.operation) {
            document.querySelector('#voteReceiptContentDiv').style.display = "block"
            document.querySelector('#pollPage').href = signedContent.eventURL
            document.querySelector('#optionSlected').innerHTML = signedContent.optionSelected.content
        }
        if(window['isClientToolConnected']) {
            document.querySelector('#voteReceiptContentDiv').style.display = "block"
            $("#saveReceiptButton").css("display" , "visible")
        }
    }

    function saveReceipt() {
        console.log("saveReceipt")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.SAVE_RECEIPT)
        webAppMessage.message = document.getElementById("receipt").innerHTML.trim()

        var objectId = Math.random().toString(36).substring(7)
        window[objectId] = {setClientToolMessage: function(appMessage) {
            console.log("saveReceiptCallback - message: " + appMessage);}}
        webAppMessage.callerCallback = objectId
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }
</asset:script>