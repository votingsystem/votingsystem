<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main"/>
			<r:require modules="application"/>
		<title><g:message code="manifestProtocolSimulationCaption"/></title>
		<style type="text/css" media="screen"></style>
	</head>
	<body>
		<div class="pageContent" style="position:relative; height:700px;">
			<div style="width: 50%;height: 50%;overflow: auto;margin: auto;top: 0; left: 0; bottom: 0; right: 0;">
				<votingSystem:simpleButton id="initManifestProtocolSimulationButton" isButton='true'  
					style="margin:15px 20px 0px 0px;padding:2px 5px 2px 0px; height:30px;">
						<g:message code="initManifestProtocolSimulationButton"/>
				</votingSystem:simpleButton>
				
				<votingSystem:simpleButton id="initClaimProtocolSimulationButton" isButton='true'  
					style="margin:15px 20px 0px 0px;padding:2px 5px 2px 0px; height:30px;">
						<g:message code="initClaimProtocolSimulationButton"/>
				</votingSystem:simpleButton>
				
				<votingSystem:simpleButton id="simulationRunningButton" isButton='true'  
					style="margin:15px 20px 0px 0px;padding:2px 5px 2px 0px; height:30px;">
						Show simulation running window
				</votingSystem:simpleButton>
				
			</div>
		</div>
	<div style="display:none;">
		<g:include view="/manifestProtocolSimulation/inputSimulationDataDialog.gsp" style="display:none;"/>
		<g:include view="/include/simulationRunningDialog.gsp" style="display:none;"/>
		<g:include view="/claimProtocolSimulation/inputSimulationDataDialog.gsp" style="display:none;"/>
	</div>
	</body>
	<r:script>
		$(function() {
			$("#initManifestProtocolSimulationButton").click(function() {	
				console.log("initManifestProtocolSimulationButton.click")	
				
				var targetURL = "${createLink(controller: 'manifestProtocolSimulation', 
					action:'inputData', absolute:true)}"
  				var width = 750
  				var height = 650
				var left = (screen.width/2) - (width/2);
  				var top = (screen.height/2) - (height/2);
  				var title = ''
  				var newWindow =  window.open(targetURL, title, 'toolbar=no, location=no, directories=no, '  + 
  					' scrollbars = yes, resizable = yes, width='+ width +
  					', height='+ height  +', top='+ top +', left='+ left + '');
	

				//showManifestProtocolSinulationDataDialog();
			 })
			 
 			$("#initClaimProtocolSimulationButton").click(function() {	
				console.log("initClaimProtocolSimulationButton.click")	 	
				showClaimProtocolSinulationDataDialog();
			 })
			 
			 
 			$("#simulationRunningButton").click(function() {	
				console.log("simulationRunningButton")	 	
				showSimulationRunningDialog("brun brumm brum");
			 })
			 
		});
	</r:script>
</html>
