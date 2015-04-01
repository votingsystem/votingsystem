<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:choose>
    <c:when test="${'simplePage'.equals(param['mode'])}" >
        <jsp:include page="/jsf/simplePageDecorator.jsp"/>
    </c:when>
    <c:when test="${'innerPage'.equals(param['mode'])}" ></c:when>
    <c:otherwise>
        <jsp:include page="/jsf/mainDecorator.jsp"/>
    </c:otherwise>
</c:choose>