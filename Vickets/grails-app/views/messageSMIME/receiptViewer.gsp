<!DOCTYPE html>
<html>
<head>
    <title>
        <g:if test="${receiptPageTitle != null}">${receiptPageTitle}</g:if>
        <g:else><g:message code="receiptPageLbl"/></g:else>
    </title>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <asset:stylesheet src="vickets.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
</head>
<body style="max-width: 600px; margin:30px auto 0px auto;">
    <div id="receiptContentDiv" style="border: 1px solid #6c0404; width: 500px;margin:auto; padding: 15px;">
        <div id="operationTypeDiv" style="font-size: 1.2em; color:#6c0404; font-weight: bold;"></div>
        <div id="nameDiv" style="font-size: 1.2em; color:#6c0404; font-weight: bold;"></div>
        <div id="contentDiv" style=""></div>
    </div>
    <button id="saveReceiptButton" type="button" class="btn btn-default" onclick="saveReceipt();"
            style="display:none; margin:10px 0px 0px 0px; float: right;"><g:message code="saveReceiptLbl"/>
    </button>


    <div id="receipt" style="display:none;">
        ${receipt}
    </div>
</body>
</html>
<asset:script>
    var signedContent = toJSON('${raw(signedContent)}')

    if(signedContent.operation) {
        if('SEND_SMIME_VOTE' == signedContent.operation) {
            document.querySelector('#operationTypeDiv').innerHTML = signedContent.operation
            if('VICKET_GROUP_NEW' == signedContent.operation) {
                document.querySelector('#nameDiv').innerHTML = signedContent.groupvsName
                document.querySelector('#contentDiv').innerHTML = signedContent.groupvsInfo
            } else {

            }
            document.querySelector('#receiptContentDiv').style.display = "block"
            console.log(signedContent)
        }
        if(window['isClientToolConnected']) {
            document.querySelector('#saveReceiptButton').style.display = "block"

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
<asset:deferredScripts/>