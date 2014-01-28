<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="appTitle"/></title>
    <r:external uri="/images/euro_16.png"/>
    <style type="text/css" media="screen"></style>
</head>
<body>
<div class="pageContent" style="position:relative;">
    <div style="width: 50%;height: 50%;overflow: auto;margin: auto;top: 0; left: 0; bottom: 0; right: 0;">

        <div style="font-size: 1.5em;margin-bottom: 30px;">
            Para poder completar la operación debe seleccionar la aplicación <b>Votaciones</b></div>
        <a href="" onclick="return goBack();" style="font-size: 1.5em;">Volver a intentar</a>
    </div>
</div>
</body>
<r:script>
    function goBack() {
        window.history.back()
        return false
    }
</r:script>
</html>