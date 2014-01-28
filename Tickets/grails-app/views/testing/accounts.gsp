<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main"/>
		<title><g:message code="testAccountsPageCaption"/></title>
        <r:external uri="/images/euro_16.png"/>
		<style type="text/css" media="screen"></style>
	</head>
	<body>
		<div class="pageContent" style="position:relative;">
			<div style="width: 50%;height: 50%;overflow: auto;margin: auto;top: 0; left: 0; bottom: 0; right: 0;">

                <a href="" onclick="return initTransaction('<g:message code="testWebAccountLbl"/>');" style="font-size: 1.5em;">
                    <g:message code="testWebAccountLbl"/></a>
				
			</div>
		</div>
	<div style="display:none;">

	</div>
	</body>
	<r:script>


        function initTransaction(transactionSubject) {
            var encodedIBAN = encodeURIComponent("ESkk bbbb gggg xxcc cccc cccc")
            var encodedSubject = encodeURIComponent(transactionSubject)
            var encodedReceptor = encodeURIComponent('<g:message code="receptorTestWebAccountLbl"/>')
            var uriData = "${createLink(controller:'app', action:'androidClient')}?operation=TRANSACTION&amount=20&currency=Euro&" +
                "IBAN=" + encodedIBAN + "&subject=" + encodedSubject + "&receptor=" + encodedReceptor
			window.location.href = uriData.replace("\n","")
            return false
        }

	</r:script>
</html>
