<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/vicket/vicket-request-form']"/>">
    <script type='text/javascript' src='http://getfirebug.com/releases/lite/1.2/firebug-lite-compressed.js'></script>
</head>
<body>
<vs-innerpage-signal title="<g:message code="doVicketRequestLbl"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
    <vicket-request-form></vicket-request-form>
</div>
</body>
</html>