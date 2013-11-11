<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
       	<r:require module="charts"/>
        <r:layoutResources />
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
        
       for (var key in jsonData.opciones) {
            var option = jsonData.opciones[key];
            data.addRow([option.contenido, option.numeroVotos])
        }

        var chart = new google.visualization.PieChart(document.getElementById('chart_div'));

        var options = {
          title: jsonData.asunto,
          sliceVisibilityThreshold:0,
          is3D: true
        };
        chart.draw(data, options);
    }
</g:applyCodec>    
</r:script>
<r:layoutResources />