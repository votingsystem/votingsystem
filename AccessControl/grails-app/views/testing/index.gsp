<!DOCTYPE html>
<html>
<head>
  	<title>testing</title>
    <g:render template="/template/pagevs"/>
</head>
<body>
<div layout horizontal center center-justified id="progressDiv"
     style="width:100%;height:100%;display:{{isProcessing? 'block':'none'}}">
    <progress></progress>
</div>
</body>
</html>
<asset:script>

</asset:script>