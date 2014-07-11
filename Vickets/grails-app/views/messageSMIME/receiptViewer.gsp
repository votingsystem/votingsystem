<!DOCTYPE html>
<html>
<head>
    <title>
        <g:if test="${receiptPageTitle != null}">${receiptPageTitle}</g:if>
        <g:else><g:message code="receiptPageLbl"/></g:else>
    </title>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/bootstrap/dist/css', file: 'bootstrap.min.css')}" type="text/css"/>
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
    <button id="saveReceiptButton" type="button" class="btn btn-accept-vs" onclick="saveReceipt();"
            style="display:none; margin:10px 0px 0px 0px; float: right;"><g:message code="saveReceiptLbl"/>
    </button>


    <div id="receipt" style="display:none;">
        ${receipt}
    </div>
</body>
</html>
<asset:script>
    var signedContent = toJSON('${raw(signedContent)}')

    $(function() {
        if(signedContent.operation) {
            //$("#receiptContentDiv").text(JSON.stringify(signedContent))
            $("#operationTypeDiv").text(signedContent.operation)


            if('VICKET_GROUP_NEW' == signedContent.operation) {
                $("#nameDiv").text(signedContent.groupvsName)
                $("#contentDiv").html(signedContent.groupvsInfo)

                //$("#pollPage").attr("href", signedContent.eventURL)
            } else {

            }
            $("#saveReceiptButton").css("display" , "visible")
            $("#receiptContentDiv").css("display" , "visible")
            console.log(signedContent)
        }
    })

    function saveReceipt() {
        console.log("saveReceipt")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.SAVE_RECEIPT)
        webAppMessage.message = document.getElementById("receipt").innerHTML.trim()

        var objectId = Math.random().toString(36).substring(7)
        window[objectId] = {setClientToolMessage: function(appMessage) {
            console.log("saveReceiptCallback - message: " + appMessage);
            var appMessageJSON = toJSON(appMessage)
            console.log("saveReceiptCallback - message: " + appMessage); }}
        webAppMessage.callerCallback = objectId
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

</asset:script>
<asset:deferredScripts/>