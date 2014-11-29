<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <vs:webcss dir="font-awesome/css" file="font-awesome.min.css"/>
    <vs:webscript dir='webcomponentsjs' file="webcomponents.min.js"/>
    <vs:webresource dir="polymer" file="polymer.html"/>
    <vs:webresource dir="font-roboto" file="roboto.html"/>
    <asset:stylesheet src="votingSystem.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <vs:webcomponent path="/element/alert-dialog"/>
    <vs:webresource dir="paper-button" file="paper-button.html"/>
    <!--<script type='text/javascript' src='http://getfirebug.com/releases/lite/1.2/firebug-lite-compressed.js'></script>-->
    <g:layoutHead/>
</head>
<body id="voting_system_page" style="margin:0px auto 0px auto; max-width: 1200px;">
    <div id="pageLayoutDiv" style="display:none;">
        <g:layoutBody/>
    </div>
    <div id="loadingDiv" style="width: 30px;margin: 100px auto 0px auto">
        <i class="fa fa-cog fa-spin" style="font-size:3em;color:#ba0011;"></i>
    </div>

<alert-dialog id="_votingsystemMessageDialog"></alert-dialog>
<core-signals id="coreSignals"></core-signals>
</body>
</html>
<asset:script>
    document.addEventListener('polymer-ready', function() {
        document.querySelector('#pageLayoutDiv').style.display = 'block';
        document.querySelector('#loadingDiv').style.display = 'none';
        updateMenuLinks()
    });
</asset:script>
<asset:deferredScripts/>