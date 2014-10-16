<%
    def dateFrom =formatDate(date:timePeriod.dateFrom, formatName:'webViewDateFormat')
    def dateTo =formatDate(date:timePeriod.dateTo, formatName:'webViewDateFormat')
%>
<!DOCTYPE html>
<html>
<head>
    <title><g:message code="weekReportsPageTitle"/></title>
    <g:render template="/template/pagevs"/>
    <link rel="stylesheet" type="text/css" href="${resource(dir: '/bower_components/jsoneditor', file: 'jsoneditor.min.css')}">
    <script type="text/javascript" src="${resource(dir: '/bower_components/jsoneditor', file: 'jsoneditor.min.js')}"></script>
    <!-- ace editor -->
    <script type="text/javascript" src="${resource(dir: '/bower_components/jsoneditor/asset/ace', file: 'ace.js')}"></script>
    <!-- json lint -->
    <script type="text/javascript" src="${resource(dir: '/bower_components/jsoneditor/asset/jsonlint', file: 'jsonlint.js')}"></script>
</head>
<body>
    <div class="pageContentDiv" style="max-width:1000px; margin: 20px auto 0px auto;">
        <div class="pageHeader" style="margin:0px auto; text-align: center;">
            <g:message code="periodLbl"/>: ${dateFrom} - ${dateTo}
        </div>
        <div id="reportsFile" style="width:100%; height:1000px;"></div>
    </div>
</body>
</html>
<asset:script>
    var options = { mode: 'code', modes: ['code', 'form', 'text', 'tree', 'view'], // allowed modes
            error: function (err) { alert(err.toString());}
        };
    var reportsFileEditor = new JSONEditor(document.querySelector("#reportsFile"),options);
    <g:applyCodec encodeAs="none">var reportsJSON = ${reportsFile}</g:applyCodec>
    reportsFileEditor.set(reportsJSON);
    reportsFileEditor.expandAll()
</asset:script>
<asset:deferredScripts/>