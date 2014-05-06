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
    <r:require module="bootstrap"/>
</head>
<r:layoutResources />
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
    <div style="margin:20px 0px 0px 20px; font-size: 1.2em;">
        <a href="${transactionvsMap.messageSMIMEURL}"><g:message code="proofLbl"/></a>
    </div>
</div>

</body>
</html>
<r:script>

    $(function() {

    })

</r:script>
<r:layoutResources />
