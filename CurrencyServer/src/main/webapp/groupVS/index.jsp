<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <link href="${elementURL}/groupVS/groupvs-list.vsp" rel="import"/>
</head>
<body>
<vs-innerpage-signal caption="${msg.groupvsLbl}"></vs-innerpage-signal>
<div class="pageContentDiv" style="padding:0px 30px 0px 30px;">
    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
    <groupvs-list id="groupvsList" state="${params.state}" groupVSListMap='${groupVSList}'></groupvs-list>
</div>
</body>
</html>
<script>
    function processSearch(textToSearch) {
        document.querySelector("#pageInfoPanel").innerHTML = "${msg.searchResultLbl} '" + textToSearch + "'"
        document.querySelector("#pageInfoPanel").style.display = 'block'
        document.querySelector("#groupvsList").url = "${restURL}/search/groupVS?searchText=" + textToSearch
    }
</script>