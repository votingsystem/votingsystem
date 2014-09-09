<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-credit-card.png')}" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:message code="signedDocumentLbl"/></title>
    <g:include view="/include/styles.gsp"/>
</head>
<body>
    <div id="pageContent" class="" style="max-width:1000px; margin: 0px auto 0px auto; padding:10px 30px 0px 30px;">
    </div>
<g:if test="${'VICKET_DEPOSIT_FROM_VICKET_SOURCE'.equals(operation)}">
    <div id="contentTemplate" class="" style="display:none;">

    </div>
</g:if>
<g:else>
    <div id="contentTemplate" class="" style="display:none;">
        <div id="transactionTypeMsg" style="font-size: 1.5em; font-weight: bold;"></div>
        <div style=""><b><g:message code="subjectLbl"/>: </b>{0}</div>
        <div style=""><b><g:message code="amountLbl"/>: </b>{1}</div>
        <div style=""><b><g:message code="validToLbl"/>: </b>{2}</div>

        <div style="margin-left: 20px;">
            <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold; margin:10px 0px 0px 0px;">
                <g:message code="pagerLbl"/></div>
            <div id="fromUserDiv">
                <div style=""><b><g:message code="nameLbl"/>: </b>{3}</div>
                <div style=""><b><g:message code="IBANLbl"/>: </b>{4}</div>
            </div>
        </div>
        <div style="margin:20px 0px 0px 20px;">
            <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold;"><g:message code="receptorLbl"/></div>
            <div id="toUserDiv">
                <div style=""><b><g:message code="IBANLbl"/>: </b>{5}</div>
            </div>
        </div >
    </div>
</g:else>
</body>
</html>
<asset:script>


    function showContent(contentStr) {
        var contentJSON =  JSON.parse(contentStr)
        var contentTemplate =  document.getElementById("contentTemplate").innerHTML
        var amount = contentJSON.amount + " " + contentJSON.currency
        document.getElementById("pageContent").innerHTML = contentTemplate.format(contentJSON.subject, amount,
            contentJSON.validTo, contentJSON.fromUser, contentJSON.fromUserIBAN, contentJSON.toUserIBAN)
    }
</asset:script>
<asset:deferredScripts/>

