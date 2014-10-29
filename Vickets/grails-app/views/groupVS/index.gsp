<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <asset:stylesheet src="vickets_groupvs.css"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/groupVS/groupvs-list']"/>">

</head>
<body>
<vs-innerpage-signal title="<g:message code="groupvsLbl"/>"></vs-innerpage-signal>
<div class="pageContentDiv" style="padding:0px 30px 0px 30px;">
    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
    <groupvs-list id="groupvsList" state="${params.state}" groupVSListMap="${groupVSList as grails.converters.JSON}"></groupvs-list>
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