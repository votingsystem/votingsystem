<html>
    <head>
        <title>${message(code: 'serverNameLabel', null)}</title>
       	<r:require module="charts"/> 
</head>
    <body>    
        <div id="subject" class='statisticData'>${statisticsMap.subject}</div>
		<div id="numSignatures" class='statisticData'>${message(code: 'numSignaturesLabel', null)}: ${statisticsMap.numSignatures}</div>
    </body>
</html>