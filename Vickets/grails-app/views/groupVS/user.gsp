<%@ page import="org.votingsystem.model.SubscriptionVS" %>
<!DOCTYPE html>
<html>
<head>
    <title><g:message code="groupUserPageLbl"/></title>
    <r:require module="application"/>
    <r:layoutResources />
</head>
<body style="max-width: 600px; margin:30px auto 0px auto;">
    <div id="messageDiv" class="text-center" style="font-size: 1.4em; color:#870000; font-weight: bold;"></div>
    <div id="" style="border: 1px solid #870000; width: 500px;margin:auto; padding: 15px;">
        <div id="" style="font-size: 1.2em; font-weight: bold;">${subscriptionMap.nif}</div>
        <div id="nameDiv" style="font-size: 1.2em;font-weight: bold;">${subscriptionMap.name}</div>
        <div id="contentDiv" style=""><g:message code="subscriptionRequestDateLbl"/>: ${subscriptionMap.subscriptionDateCreated}</div>
    </div>
    <button id="activateUserButton" type="button" class="btn btn-warning" onclick="activateUser();"
            style="margin:10px 0px 0px 0px; "><g:message code="activateUserLbl"/> <i class="fa fa-thumbs-o-up"></i>
    </button>
    <button id="deActivateUserButton" type="button" class="btn btn-warning" onclick="deActivateUser();"
            style="margin:10px 0px 0px 0px; "><g:message code="deActivateUserLbl"/> <i class="fa fa-thumbs-o-down"></i>
    </button>
    <button id="makeDepositButton" type="button" class="btn btn-warning" onclick="makeDeposit();"
            style="margin:10px 0px 0px 0px; "><g:message code="makeDepositLbl"/> <i class="fa fa-money"></i>
    </button>

    <div id="receipt" style="display:none;">

    </div>
</body>
</html>
<r:script>

    $(function() {
        <g:if test="${SubscriptionVS.State.ACTIVE.toString().equals(subscriptionMap.state)}">
            $("#messageDiv").text("<g:message code="userStateActiveLbl"/>")
            $("#activateUserButton").css("display", "none")
        </g:if>
        <g:if test="${SubscriptionVS.State.PENDING.toString().equals(subscriptionMap.state)}">
            $("#messageDiv").text("<g:message code="userStatePendingLbl"/>")
            $("#makeDepositButton").css("display", "none")

        </g:if>
        <g:if test="${SubscriptionVS.State.CANCELLED.toString().equals(subscriptionMap.state)}">
            $("#messageDiv").text("<g:message code="userStateCancelledLbl"/>")
            $("#deActivateUserButton").css("display", "none")
            $("#makeDepositButton").css("display", "none")
        </g:if>
    })

    function activateUser () { }

    function deActivateUser () { }

    function makeDeposit () { }


</r:script>
<r:layoutResources/>