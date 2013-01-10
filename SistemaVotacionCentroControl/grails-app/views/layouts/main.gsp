<!DOCTYPE html>
<html>
    <head>
        <title><g:layoutTitle default="Control de Acceso" /></title>
        <link rel="stylesheet" href="${resource(dir:'css',file:'main.css')}" />
        <link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
      <style type="text/css" media="screen">
        #encabezado {background: #eaf4ff; color: #333333;}
        .tituloEncabezado {
          background-repeat:no-repeat;
          height:41px;
          padding-left:60px;
          color: #FFCC00; 
          position:relative; left:20px; top:5px;
        }
        #tituloEncabezado a {
	      color: #3E125F;
          cursor: hand; 
          text-decoration:none;
          font-size: 26px; font-weight:bold;
          font-family: "Verdana", Arial, sans-serif;
          
          /*text-shadow: -1px 0pt black, 0pt 1px black, 1px 0pt black, 0pt -1px black;*/
        }
        .pieDePagina {
          color: #000;
          clear: both;
          font-size: 0.8em;
          margin-top: 1.5em;
          margin-left: 32em;
          padding: 1em;
          min-height: 1em;
        }
      </style>
        <g:layoutHead />
        <g:javascript library="application" />
    </head>
    <body>
                <div id="encabezado">
                  <div id="tituloEncabezado" class="tituloEncabezado" role="banner"><a href="${grailsApplication.config.grails.serverURL}">Centro de Control</a></div>
                </div>
		<g:layoutBody/>
		<div class="pieDePagina" role="contentinfo">
                  <a href="mailto:jgzornoza@gmail.com">Correo</a>
                </div>
		<div id="spinner" class="spinner" style="display:none;"><g:message code="spinner.alt" default="Loading&hellip;"/></div>
		<g:javascript library="application"/>
    </body>
</html>