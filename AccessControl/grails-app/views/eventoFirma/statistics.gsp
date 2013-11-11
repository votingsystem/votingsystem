<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
       	<r:require module="charts"/>
</head>
    <body>    
        <div id="subject" class='statisticData'>${statisticsMap.asunto}</div>
		<div id="numSignatures" class='statisticData'>${message(code: 'numSignaturesLabel', null)}: ${statisticsMap.numeroFirmas}</div>
    </body>
</html>