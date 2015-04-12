<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<html>
<head>
    <title>${pageTitle}</title>
</head>
<body>
<f:view>
    <h:outputText value="#{msg.representativeAccreditationsMailBody}"/>
</f:view>
</body>
</html>