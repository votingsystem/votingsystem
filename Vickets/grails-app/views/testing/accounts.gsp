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

                <a href="" onclick="return initTransaction('<g:message code="testWebAccountLbl"/>');" style="font-size: 1.5em;">
                    <g:message code="testWebAccountLbl"/></a>
				
			</div>


            <g:include view="/include/help/userTypes_${request.locale}.gsp"/>
		</div>
	<div style="display:none;">

	</div>
	</body>
	<asset:script>

        function initTransaction(transactionSubject) {
            var encodedIBAN = encodeURIComponent("")
            var encodedSubject = encodeURIComponent(transactionSubject)
            var encodedReceptor = encodeURIComponent('<g:message code="receptorTestWebAccountLbl"/>')
            var uriData = "${createLink(controller:'app', action:'androidClient')}?operation=TRANSACTION&amount=20&currency=EURO&" +
                "IBAN=" + encodedIBAN + "&subject=" + encodedSubject + "&receptor=" + encodedReceptor
			window.location.href = uriData.replace("\n","")
            return false
        }

	</asset:script>
</html>
