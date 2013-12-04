<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main"/>
			<r:require modules="application"/>
		<title><g:message code="simulationWebAppCaption"/></title>
		<style type="text/css" media="screen"></style>
	</head>
	<body>
		<div class="pageContent" style="position:relative; height:700px;">
			<div style="width: 50%;height: 50%;overflow: auto;margin: auto;top: 0; left: 0; bottom: 0; right: 0;">

                <votingSystem:simpleButton id="initElectionProtocolSimulationButton"
                                style="margin:15px 20px 0px 0px;padding:2px 5px 2px 0px; height:30px;width:400px;">
                    <g:message code="initElectionProtocolSimulationButton"/>
                </votingSystem:simpleButton>

                <votingSystem:simpleButton id="initManifestProtocolSimulationButton"
					            style="margin:15px 20px 0px 0px;padding:2px 5px 2px 0px; height:30px;width:400px;">
						<g:message code="initManifestProtocolSimulationButton"/>
				</votingSystem:simpleButton>
				
				<votingSystem:simpleButton id="initClaimProtocolSimulationButton"
					            style="margin:15px 20px 0px 0px;padding:2px 5px 2px 0px; height:30px;width:400px;">
						<g:message code="initClaimProtocolSimulationButton"/>
				</votingSystem:simpleButton>

                <votingSystem:simpleButton id="initTimeStampProtocolSimulationButton"
                               style="margin:15px 20px 0px 0px;padding:2px 5px 2px 0px; height:30px;width:400px;">
                    <g:message code="initTimeStampProtocolSimulationButton"/>
                </votingSystem:simpleButton>

                <votingSystem:simpleButton id="initMultiSignProtocolSimulationButton"
                               style="margin:15px 20px 0px 0px;padding:2px 5px 2px 0px; height:30px;width:400px;">
                    <g:message code="initMultiSignProtocolSimulationButton"/>
                </votingSystem:simpleButton>

                <votingSystem:simpleButton id="initEncryptionProtocolSimulationButton"
                               style="margin:15px 20px 0px 0px;padding:2px 5px 2px 0px; height:30px;width:400px;">
                    <g:message code="initEncryptionProtocolSimulationButton"/>
                </votingSystem:simpleButton>




				<votingSystem:simpleButton id="simulationRunningButton"
					            style="margin:15px 20px 0px 0px;padding:2px 5px 2px 0px; height:30px;width:400px;">
						Show simulation running window
				</votingSystem:simpleButton>
				
			</div>
		</div>
	<div style="display:none;">
		<g:include view="/include/simulationRunningDialog.gsp" style="display:none;"/>
        <g:include view="/include/dialog/addClaimFieldDialog.gsp"/>
	</div>
	</body>
	<r:script>


		$(function() {

		    $("#initElectionProtocolSimulationButton").click(function() {
				console.log("initElectionProtocolSimulationButton.click")
				var targetURL = "${createLink(controller: 'electionProtocolSimulation', action:'inputData', absolute:true)}"
				openWindow(targetURL)
			 })

			$("#initManifestProtocolSimulationButton").click(function() {	
				console.log("initManifestProtocolSimulationButton.click")
				var targetURL = "${createLink(controller: 'manifestProtocolSimulation',  action:'inputData', absolute:true)}"
 				openWindow(targetURL)
			 })
			 
 			$("#initClaimProtocolSimulationButton").click(function() {	
				console.log("initClaimProtocolSimulationButton.click")
				var targetURL = "${createLink(controller: 'claimProtocolSimulation', action:'inputData', absolute:true)}"
				openWindow(targetURL)
			 })


            $("#initTimeStampProtocolSimulationButton").click(function() {
				console.log("initTimeStampProtocolSimulationButton.click")
				var targetURL = "${createLink(controller: 'timeStampSimulation', action:'inputData', absolute:true)}"
				openWindow(targetURL)
			 })


            $("#initMultiSignProtocolSimulationButton").click(function() {
				console.log("initTimeStampProtocolSimulationButton.click")
				var targetURL = "${createLink(controller: 'multiSignSimulation', action:'inputData', absolute:true)}"
				openWindow(targetURL)
			 })

            $("#initEncryptionProtocolSimulationButton").click(function() {
				console.log("initEncryptionProtocolSimulationButton.click")
				var targetURL = "${createLink(controller: 'encryptionSimulation', action:'inputData', absolute:true)}"
				openWindow(targetURL)
			 })




            function openWindow(targetURL) {
                var width = 1000
  				var height = 800
				var left = (screen.width/2) - (width/2);
  				var top = (screen.height/2) - (height/2);
  				var title = ''

  				var newWindow =  window.open(targetURL, title, 'toolbar=no, scrollbars=yes, resizable=yes, '  +
  					'width='+ width +
  					', height='+ height  +', top='+ top +', left='+ left + '');
            }

 			$("#simulationRunningButton").click(function() {	
				console.log("simulationRunningButton")	 	
				showSimulationRunningDialog("Mensaje de la página principal");
			 })
			 
		});
	</r:script>
</html>
