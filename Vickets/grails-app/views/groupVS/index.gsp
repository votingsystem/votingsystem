<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <asset:stylesheet src="vickets_groupvs.css"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/groupVS/groupvs-list']"/>">
</head>
<body>
<votingsystem-innerpage-signal title="<g:message code="groupvsLbl"/>"></votingsystem-innerpage-signal>
<div class="pageContentDiv" style="padding:0px 30px 0px 30px;">
    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
    <groupvs-list id="groupvsList" url="${createLink(controller: 'groupVS')}?menu=${params.menu}"></groupvs-list>
</div>
</body>
</html>
<asset:script>
    function processSearch(textToSearch) {
        document.querySelector("#pageInfoPanel").innerHTML = "<g:message code="searchResultLbl"/> '" + textToSearch + "'"
        document.querySelector("#pageInfoPanel").style.display = 'block'
        document.querySelector("#groupvsList").url = "${createLink(controller: 'search', action: 'groupVS')}?searchText=" + textToSearch
    }
</asset:script>