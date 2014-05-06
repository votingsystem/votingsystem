<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
</head>
<body>
<div class="pageContenDiv">
    <div class="row">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li><a href="${createLink(controller: 'groupVS', action: 'index')}"><g:message code="groupvsLbl"/></a></li>
            <li class="active"><g:message code="groupvsAdminLbl"/></li>
        </ol>
    </div>

    <div style="display: table;width:90%;vertical-align: middle;margin:0px 0 10px 0px;">
        <div style="display:table-cell;margin: auto; vertical-align: top;">
            <select id="transactionvsTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;" class="form-control">
                <option value="" style="color:black;"> - <g:message code="selectTransactionTypeLbl"/> - </option>
                <option value="USER_ALLOCATION"> - <g:message code="selectUserAllocationLbl"/> - </option>
                <option value="USER_ALLOCATION_INPUT"> - <g:message code="selectUserAllocationInputLbl"/> - </option>
                <option value="VICKET_REQUEST"> - <g:message code="selectVicketRequestLbl"/> - </option>
                <option value="VICKET_SEND"> - <g:message code="selectVicketSendLbl"/> - </option>
                <option value="VICKET_CANCELLATION"> - <g:message code="selectVicketCancellationLbl"/> - </option>
            </select>
        </div>
    </div>


    <a id="initUserBaseDataButton" href="#" onclick="testMessage();"
       class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:400px;">
        <g:message code="makeDepositButtonLbl"/>
    </a>

    <div id="messageDiv"></div>

</div>

</body>
</html>
<r:script>

    $(function() {

    })


    function testMessage() {
        setJavafxClientMessage("Hello from browser")
    }

    function setJavafxClientMessage(javafxClientMessage) {
        console.log("javafxClientMessage: " + javafxClientMessage)
        $("#messageDiv").text("### " + javafxClientMessage)
    }

</r:script>

