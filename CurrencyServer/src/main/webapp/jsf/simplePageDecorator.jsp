<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://www.opensymphony.com/sitemesh/decorator" prefix="decorator" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
    <title></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="${config.webURL}/css/cryptocurrency.css" media="all" rel="stylesheet" />
    <link href="${config.resourceURL}/font-awesome/css/font-awesome.min.css" media="all" rel="stylesheet" />
    <script src="${config.resourceURL}/webcomponentsjs/webcomponents.min.js" type="text/javascript"></script>
    <link href="${config.resourceURL}/polymer/polymer.html" rel="import"/>
    <link href="${config.resourceURL}/font-roboto/roboto.html" rel="import"/>
    <link href="${config.resourceURL}/core-signals/core-signals.html" rel="import"/>
    <link href="${config.resourceURL}/paper-button/paper-button.html" rel="import"/>
    <script src="${config.webURL}/resources/js/utilsVS.js" type="text/javascript"></script>
    <script src="${config.webURL}/resources/js/utils_js.jsp" type="text/javascript"></script>
    <link href="${config.webURL}/element/alert-dialog.vsp" rel="import"/>
    <link href="${config.resourceURL}/vs-innerpage-signal/vs-innerpage-signal.html" rel="import"/>
    <link href="${config.resourceURL}/core-localstorage/core-localstorage.html" rel="import"/>
    <link href="${config.webURL}/element/alert-dialog.vsp" rel="import"/>
    <decorator:head />
</head>
<body>
<body id="voting_system_page" style="margin:0px auto 0px auto; max-width: 1200px;">
    <div id="appTitle" style="font-size:1.5em;width: 100%; text-align: center; margin:15px auto;"></div>
    <div id="pageLayoutDiv" style="display:none;">
        <decorator:body />
    </div>
    <div id="loadingDiv" style="width: 30px;margin: 100px auto 0px auto">
        <i class="fa fa-cog fa-spin" style="font-size:3em;color:#ba0011;"></i>
    </div>
    <alert-dialog id="_votingsystemMessageDialog"></alert-dialog>
    <core-signals id="coreSignals"></core-signals>
</body>
</body>
</html>
<script>
    document.addEventListener('polymer-ready', function() {
        document.querySelector('#pageLayoutDiv').style.display = 'block';
        document.querySelector('#loadingDiv').style.display = 'none';
        updateMenuLinks()
    });

    document.querySelector('#coreSignals').addEventListener('core-signal-vs-innerpage', function(e) {
        if(e.detail.title) document.querySelector('#appTitle').innerHTML = e.detail.title
        document.dispatchEvent( new Event('innerPageSignal'));
    });
</script>