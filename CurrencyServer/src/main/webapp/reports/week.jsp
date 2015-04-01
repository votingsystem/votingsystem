<%
    def dateFrom =formatDate(date:timePeriod.dateFrom, formatName:'webViewDateFormat')
    def dateTo =formatDate(date:timePeriod.dateTo, formatName:'webViewDateFormat')
%>
<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
<head>
    <title>${msg.weekReportsPageTitle}</title>

    <link rel="import" href="${config.resourceURL}/juicy-jsoneditor/src/juicy-jsoneditor.html">
    <!-- josdejong/jsoneditor#104 workaround-->
    <link rel="stylesheet" type="text/css" href="${config.resourceURL}/jsoneditor/jsoneditor.min.css">
</head>
<body>
    <vs-innerpage-signal caption="${msg.reportsPageTitle}"></vs-innerpage-signal>
    <div class="pageContentDiv" style="max-width:1000px; margin: 20px auto 0px auto;">
        <div class="pageHeader" style="margin:0px auto; text-align: center;">
            ${msg.periodLbl}: ${dateFrom} - ${dateTo}
        </div>
        <juicy-jsoneditor json="${reportsFile}" id="jsoneditor" modes="['code', 'form', 'text', 'tree', 'view']"
                      style="width:100%; height:1000px;"></juicy-jsoneditor>
    </div>
</body>
</html>
<script>
    var options = { mode: 'code', modes: ['code', 'form', 'text', 'tree', 'view'], // allowed modes
            error: function (err) { alert(err.toString());}
        };

    document.addEventListener('innerPageSignal', function() {
        document.querySelector('#jsoneditor').editor.expandAll();
    });
</script>
