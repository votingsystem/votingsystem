<%
    def dateFrom =formatDate(date:timePeriod.dateFrom, formatName:'webViewDateFormat')
    def dateTo =formatDate(date:timePeriod.dateTo, formatName:'webViewDateFormat')
%>
<!DOCTYPE html>
<html>
<head>
    <title><g:message code="weekReportsPageTitle"/></title>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="${resource(dir: '/bower_components/juicy-jsoneditor/src', file: 'juicy-jsoneditor.html')}">
    <!-- josdejong/jsoneditor#104 workaround-->
    <link rel="stylesheet" type="text/css" href="${resource(dir: '/bower_components/jsoneditor', file: 'jsoneditor.min.css')}">
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="reportsPageTitle"/>"></vs-innerpage-signal>
    <div class="pageContentDiv" style="max-width:1000px; margin: 20px auto 0px auto;">
        <div class="pageHeader" style="margin:0px auto; text-align: center;">
            <g:message code="periodLbl"/>: ${dateFrom} - ${dateTo}
        </div>
        <juicy-jsoneditor json="${reportsFile}" id="jsoneditor" modes="['code', 'form', 'text', 'tree', 'view']"
                      style="width:100%; height:1000px;"></juicy-jsoneditor>
    </div>
</body>
</html>
<asset:script>
    var options = { mode: 'code', modes: ['code', 'form', 'text', 'tree', 'view'], // allowed modes
            error: function (err) { alert(err.toString());}
        };

    document.addEventListener('innerPageSignal', function() {
        document.querySelector('#jsoneditor').editor.expandAll();
    });
</asset:script>
<asset:deferredScripts/>