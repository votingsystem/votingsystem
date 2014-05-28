<html>
    <head>
        <title>${message(code: 'serverNameLbl', null)}</title>
        <asset:stylesheet src="charts.css"/>
        <asset:javascript src="jsapi.js"/>
</head>
    <body>    
        <div id="subject" class='statisticData'>${statisticsMap.subject}</div>
		<div id="numSignatures" class='statisticData'>${message(code: 'numSignaturesLabel', null)}: ${statisticsMap.numSignatures}</div>
    </body>
</html>