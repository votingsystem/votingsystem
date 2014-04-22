<!DOCTYPE html>
<html>
<head>
  	<title>pruebaTemplate</title>
   	<r:require modules="application"/>
</head>
<r:layoutResources />
<body>
<div id="dialog" title="Basic dialog">
    <p>This is the default dialog which is useful for displaying information. The dialog window can be moved, resized and closed with the 'x' icon.</p>
</div>
</body>
</html>
<r:script>
    $(function() {
        $( "#dialog" ).dialog();
    });
</r:script>
<r:layoutResources />