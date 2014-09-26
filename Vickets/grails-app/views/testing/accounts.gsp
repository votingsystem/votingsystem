<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="testAccountsPageCaption"/></title>
    <style type="text/css" media="screen"></style>
</head>
<body>
    <div class="pageContent" style="position:relative;">
        <div style="width: 50%;height: 50%;overflow: auto;margin: auto;top: 0; left: 0; bottom: 0; right: 0;">

            <div onclick="initTransaction('<g:message code="testWebAccountLbl"/>');"
                 style="font-size: 1.5em; color: #0000ff; cursor: pointer;"><g:message code="testWebAccountLbl"/></div>

        </div>
    </div>
</body>
<asset:script>

    function initTransaction(transactionSubject) {
        var encodedIBAN = encodeURIComponent("")
        var encodedSubject = encodeURIComponent(transactionSubject)
        var encodedRefererURL = encodeURIComponent(window.location.href)
        var encodedReceptor = encodeURIComponent('<g:message code="receptorTestWebAccountLbl"/>')
        var uriData = "${createLink(controller:'app', action:'androidClient')}?operation=TRANSACTIONVS&amount=20&currencyCode=EUR" +
            "&tagVS=HIDROGENO&IBAN=" + encodedIBAN + "&subject=" + encodedSubject + "&toUser=" + encodedReceptor +
            "&toUserIBAN=ES8978788989450000000004&refererURL=" + encodedRefererURL
        window.location.href = uriData.replace("\n","")
        return false
    }

</asset:script>
</html>
