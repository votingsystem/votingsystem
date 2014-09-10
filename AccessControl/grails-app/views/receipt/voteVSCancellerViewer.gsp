<!DOCTYPE html>
<html>
<head>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/receipt/receipt-votevs-canceller.gsp']"/>">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/votingsystem-message-dialog.gsp']"/>">

    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-credit-card.png')}" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:message code="signedDocumentLbl"/></title>
    <g:include view="/include/styles.gsp"/>
    <!--<script type='text/javascript' src='http://getfirebug.com/releases/lite/1.2/firebug-lite-compressed.js'></script>-->
</head>
<body>

<div id="voting_system_page">
    <receipt-votevs-canceller id="receiptViewer" receipt="${signedContentMap as grails.converters.JSON}"
                              smimeMessage="${smimeMessage}"></receipt-votevs-canceller>
</div>
<votingsystem-message-dialog id="_votingsystemMessageDialog"></votingsystem-message-dialog>
</body>
</html>
<asset:script>
    var viewerLoaded = false
    var clientToolConnected = false
    var receiptJSON = null

    document.querySelector("#voting_system_page").addEventListener('votingsystem-clienttoolconnected', function() {
        clientToolConnected = true
    })

    document.querySelector("#receiptViewer").addEventListener('attached', function() {
        console.log('addEventListener - receiptViewer attached - clientToolConnected: ' + clientToolConnected)
        viewerLoaded = true
        console.log("receiptViewer - ready - isClientToolConnected: " + isClientToolConnected)
        document.querySelector("#receiptViewer").isClientToolConnected = clientToolConnected
        if(receiptJSON != null) {
            document.querySelector("#receiptViewer").receipt = receiptJSON
            receiptJSON = null
        }
    })


    function showContent(contentStr) {
        receiptJSON = JSON.parse(contentStr)
        if(viewerLoaded) {
            document.querySelector("#receiptViewer").receipt = receiptJSON
            receiptJSON = null
        }
    }

</asset:script>
<asset:deferredScripts/>

