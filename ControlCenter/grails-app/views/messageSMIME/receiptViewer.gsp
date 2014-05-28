<!DOCTYPE html>
<html>
<head>
    <title>
        <g:if test="${receiptPageTitle != null}">${receiptPageTitle}</g:if>
        <g:else><g:message code="receiptPageLbl"/></g:else>
    </title>
    <g:javascript library="jquery" plugin="jquery"/>
    <link rel="stylesheet" href="/ControlCenter/font-awesome/css/font-awesome.min.css" type="text/css"/>

    <asset:stylesheet src="bootstrap.min.css"/>
    <asset:javascript src="bootstrap.min.js"/>

    <asset:stylesheet src="votingSystem.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>

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

    $(function() {
        if(signedContent.operation) {
            if('SEND_SMIME_VOTE' == signedContent.operation) {
                $("#voteReceiptContentDiv").css("display" , "visible")
                $("#pollPage").attr("href", signedContent.eventURL)
                $("#optionSlected").html(signedContent.optionSelected.content)
            }
            if(isJavaFX()) {
                $("#saveReceiptButton").css("display" , "visible")
            }
        }

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