<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
        <link rel="stylesheet" href="${resource(dir:'css',file:'clean.css')}" />                                   
  		<title>${message(code: 'nombreServidorLabel', null)}</title>
		<script src="${resource(dir:'js',file:'deployJava.js')}"></script>

  <!--                                           -->
  <!-- This script loads your compiled module.   -->
  <!-- If you add any GWT meta tags, they must   -->
  <!-- be added before this line.                -->
  <!--                                           -->
  <script type="text/javascript" src="${resource(dir: 'gwt/org.centrocontrol.clientegwt.PuntoEntradaPrincipal', file: 'org.centrocontrol.clientegwt.PuntoEntradaPrincipal.nocache.js')}"></script>                                           
</head>

<!--                                           -->
<!-- The body can have arbitrary html, or      -->
<!-- you can leave the body empty if you want  -->
<!-- to create a completely dynamic ui         -->
<!--                                           -->
<body id="uiBody">
  <!-- OPTIONAL: include this if you want history support -->
  	<iframe id="__gwt_historyFrame" style="width:0;height:0;border:0"></iframe>
   	<div class="ui" id="ui"></div>
</body>
</html>
