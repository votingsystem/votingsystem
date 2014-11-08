<html>
    <head>
        <title>${message(code: 'serverNameLbl', null)}</title>
        <asset:stylesheet src="charts.css"/>
        <asset:javascript src="jsapi.js"/>
</head>
    <body>    
        <div id="subject" class='statisticData'>${statsMap.subject}</div>
		<div id="numSignatures" class='statisticData'>${message(code: 'numSignaturesLabel', null)}: ${statsMap.numSignatures}</div>
    </body>
</html>