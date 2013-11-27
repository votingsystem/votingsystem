<!DOCTYPE html>
<html>
<head>
	<r:require module="charts"/>
</head>
    <body>    
        <div id="chart_div">
            <div class='loading'>
                <img src="${resource(dir:'images',file:'Indeterminate.gif')}"/>
            </div>        
        </div>
    </body>
</html>
<r:script>
<g:applyCodec encodeAs="none">
    var jsonData = ${statisticsJSON}
    google.load('visualization', '1', {'packages':['corechart']});
    google.setOnLoadCallback(drawChart);
    
    function drawChart() {
        var data = new google.visualization.DataTable();
        data.addColumn('string', '${message(code: 'optionLabel', null)}');
        data.addColumn('number', '${message(code: 'numVotesLabel', null)}');
        
       for (var key in jsonData.fieldsEventVS) {
            var option = jsonData.fieldsEventVS[key];
            data.addRow([option.content, option.numVotesVS])
        }

        var chart = new google.visualization.PieChart(document.getElementById('chart_div'));

        var options = {
          title: jsonData.subject,
          sliceVisibilityThreshold:0,
          is3D: true
        };
        chart.draw(data, options);
    }
</g:applyCodec>
</r:script>