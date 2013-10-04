<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
        <link rel="stylesheet" href="${resource(dir:'css',file:'charts.css')}" />   
  <script type="text/javascript"></script>
</head>
    <body>    
        <div id="subject" class='statisticData'>${statisticsMap.asunto}</div>
		<div id="numSignatures" class='statisticData'>${message(code: 'numSignaturesLabel', null)}: ${statisticsMap.numeroFirmas}</div>
    </body>
</html>