<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/userVS/uservs-selector"/>
</head>
<body>
<vs-innerpage-signal caption="<g:message code="userSearchPageTitle"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
    <div layout vertical center>
        <uservs-selector id="userVSSelector" style="width:800px; margin:10px 0px 0px 0px;" contactSelector></uservs-selector>
    </div>
</div>
</body>
</html>
<asset:script>
    document.querySelector("#coreSignals").addEventListener('core-signal-user-clicked', function(e) {
        if(document.querySelector("#navBar") != null) {
            document.querySelector("#navBar").url = "${createLink(controller: 'userVS')}/" + e.detail.id + "?menu=" + menuType
        } else window.location.href = "${createLink(controller: 'userVS')}/" + e.detail.id + "?menu=" + menuType
    });
</asset:script>
<asset:deferredScripts/>