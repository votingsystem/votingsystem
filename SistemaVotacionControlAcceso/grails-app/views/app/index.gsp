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
 		<script src="${resource(dir:'js',file:'deployJava.js')}"></script>
 		<script>
		//http://www.mkyong.com/javascript/how-to-detect-ie-version-using-javascript/
		function getInternetExplorerVersion() {
		// Returns the version of Windows Internet Explorer or a -1
		// (indicating the use of another browser).
		   var rv = -1; // Return value assumes failure.
		   if (navigator.appName == 'Microsoft Internet Explorer')
		   {
		      var ua = navigator.userAgent;
		      var re  = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
		      if (re.exec(ua) != null)
		         rv = parseFloat( RegExp.$1 );
		   }
		   return rv;
		}
		
		function checkIEVersion() {
		   var ver = getInternetExplorerVersion();
		   if ( ver> -1 ) {
		      if ( ver<= 8.0 ) {
		    	  alert("Navegador no soportado, actualizate")
			  }
		   }
		}

		function loadjsfile(filename){
			var fileref=document.createElement('script')
			fileref.setAttribute("type","text/javascript")
		 	fileref.setAttribute("src", filename)
		 }

		function setAppMessage(appMessage) {
			androidClient.setAppMessage(appMessage);
		}
		
 		</script>
  		<script type="text/javascript" 
  				src="${resource(dir: 'gwt/org.controlacceso.clientegwt.PuntoEntrada', 
				file: 'org.controlacceso.clientegwt.PuntoEntrada.nocache.js')}">


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
