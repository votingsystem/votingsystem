<!DOCTYPE html>
<html>
<head>
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
    <div id="pageContent" class="" style="max-width:1000px; margin: 0px auto 0px auto; padding:10px 30px 0px 30px;">
    </div>
<g:if test="${'VICKET_DEPOSIT_FROM_VICKET_SOURCE'.equals(operation)}">
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
</g:if>
</body>
</html>
<asset:script>
    var contentStr = '{"operation":"VICKET_DEPOSIT_FROM_VICKET_SOURCE","fromUser":"Implantación proyecto Vickets","fromUserIBAN":"ES5378788989451111111111","toUserIBAN":"ES8978788989450000000004","amount":"101010.10","validTo":"2014/06/16 00:00:00","subject":"Ingreso de fuente externa","currency":"EUR","UUID":"21489925-7a50-4453-af8a-cb6b579b1602"}'
    $(function() {

    })

    function showContent(contentStr) {
        var contentJSON = toJSON(contentStr)
        var contentTemplate =  document.getElementById("contentTemplate").innerHTML
        var amount = contentJSON.amount + " " + contentJSON.currency
        document.getElementById("pageContent").innerHTML = contentTemplate.format(contentJSON.subject, amount,
            contentJSON.validTo, contentJSON.fromUser, contentJSON.fromUserIBAN, contentJSON.toUserIBAN)
    }

</asset:script>
<asset:deferredScripts/>

