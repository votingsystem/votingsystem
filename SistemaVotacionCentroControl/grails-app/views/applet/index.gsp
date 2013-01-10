<u><h3>Controlador servidor de Applets</h3></u>
<div>
  	<p>Servicio que lanza los <a href="http://es.wikipedia.org/wiki/Java_applet">Applets Java</a> que utiliza la aplicación para firmar documentos.</p>
  	<h3>Métodos soportados</h3>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/applet/cliente">/applet/cliente</a><br/>
	    <b>Respuesta:</b><br/>
	    Devuelve el Applet que utiliza la aplicación para hacer firmar en navegadores con máquina virtual Java.
    </p>     	
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/applet/jnlp">/applet/jnlp</a><br/>
      <b>Parámetros:</b><br/>
         <b>Respuesta:</b><br/>
         El archivo JNLP que utiliza el Applet para descargar los archivos que necesita.
    </p>
</div>

