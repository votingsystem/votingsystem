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

                <a href="" onclick="return initTransaction('Cuenta Web de pruebas');" style="font-size: 1.5em;">Cuenta de pruebas</a>
				
			</div>
		</div>
	<div style="display:none;">

	</div>
	</body>
	<r:script>


        function initTransaction(transactionSubject) {
            var encodedIBAN = encodeURIComponent("ESkk bbbb gggg xxcc cccc cccc")
            var redirectURL = "${createLink(controller:'app', action:'androidClient')}?msg=" + encodeURIComponent(transactionSubject) +
				"&operation=TRANSACTION&amount=1234&currency=Euro&IBAN=" + encodedIBAN
			window.location.href = redirectURL.replace("\n","")
            return false
        }

	</r:script>
</html>
