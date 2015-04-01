<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
<head>
    <meta name="layout" content="main"/>
    <title>${msg.testAccountsPageCaption}</title>
    <style type="text/css" media="screen"></style>
</head>
<body>
    <div class="pageContent" style="position:relative;">
        <div style="width: 50%;height: 50%;overflow: auto;margin: auto;top: 0; left: 0; bottom: 0; right: 0;">

            <div onclick="initTransaction('${msg.testWebAccountLbl}');"
                 style="font-size: 1.5em; color: #0000ff; cursor: pointer;">${msg.testWebAccountLbl}</div>

        </div>
    </div>
</body>
<script>

    function initTransaction(transactionSubject) {
        var encodedIBAN = encodeURIComponent("")
        var encodedSubject = encodeURIComponent(transactionSubject)
        var encodedRefererURL = encodeURIComponent(window.location.href)
        var encodedReceptor = encodeURIComponent('${msg.receptorTestWebAccountLbl}')
        var uriData = "${config.restURL}/app/androidClient?operation=TRANSACTIONVS&amount=20&currencyCode=EUR" +
            "&tagVS=HIDROGENO&IBAN=" + encodedIBAN + "&subject=" + encodedSubject + "&toUser=" + encodedReceptor +
            "&toUserIBAN=ES8978788989450000000004&refererURL=" + encodedRefererURL
        window.location.href = uriData.replace("\n","")
        return false
    }

</script>
</html>
