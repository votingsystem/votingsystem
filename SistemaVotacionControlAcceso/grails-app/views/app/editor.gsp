<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
		<% request.setAttribute("org.grails.rendering.view", Boolean.TRUE) %>
        <link rel="stylesheet" href="${resource(dir:'css',file:'clean.css')}" />   
        <link rel="stylesheet" media="handheld, only screen and (max-device-width: 320px)" 
        	href="${resource(dir:'css',file:'mobile.css')}">                               
  		<title>${message(code: 'nombreServidorLabel', null)}</title>
 		<meta http-equiv="content-type" content="text/html; charset=UTF-8">
 		<meta name="viewport" content="width=device-width" />
 		<meta name="HandheldFriendly" content="true" />
  		<script type="text/javascript" 
  				src="${resource(dir: 'gwt/org.controlacceso.clientegwt.PuntoEntradaEditor', 
				file: 'org.controlacceso.clientegwt.PuntoEntradaEditor.nocache.js')}">
  		</script>
  		<script type="text/javascript">

				function setVotingWebAppMessage(appMessage) {
					androidClient.setVotingWebAppMessage(appMessage);
				}

				function setEditorData(appMessage) {
					androidClient.setEditorData(appMessage);
				}

				function setVotingWebAppMessage(appMessage) {
					androidClient.setVotingWebAppMessage(appMessage);
				}

				function setMessage(appMessage) {
					androidClient.setMessage(appMessage);
				}
				
				function showProgressDialog(appMessage) {
					androidClient.showProgressDialog(appMessage);
				}
				
  		</script>
</head>

<!--                                           -->
<!-- The body can have arbitrary html, or      -->
<!-- you can leave the body empty if you want  -->
<!-- to create a completely dynamic ui         -->
<!--                                           -->
<body id="uiBody" onload="checkIEVersion()">
  <!-- OPTIONAL: include this if you want history support -->
    <iframe src="javascript:''" id="__gwt_historyFrame" tabIndex='-1' style="position:absolute;width:0;height:0;border:0"></iframe>
   	<div class="ui" id="ui"></div>
</body>
</html>
