<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
<head>

    <link href="${elementURL}/groupVS/groupvs-details.vsp" rel="import"/>
</head>
<body>
    <vs-innerpage-signal caption="${msg.groupLbl}"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <groupvs-details groupvs="${groupvsMap}"></groupvs-details>
    </div>
</body>
</html>
