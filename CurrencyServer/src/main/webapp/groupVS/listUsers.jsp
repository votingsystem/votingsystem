<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
<head>

    <link href="${config.webURL}/userVS/uservs-list.vsp" rel="import"/>
</head>
<body>
<vs-innerpage-signal caption="${msg.groupvsUserListLbl}"></vs-innerpage-signal>
<div class="pageContentDiv">
    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <h3><div class="pageHeader text-center">
        ${msg.groupvsUserListPageHeader} '${subscriptionMap.groupName}'</div>
    </h3>
    <uservs-list id="userList" url="${config.restURL}/groupVS/listUsers/${subscriptionMap.id}"
               userURLPrefix="user" menuType="${params.menu}"></uservs-list>
</div>
</body>
</html>
<script>
</script>