<u><h3>Anulación de votos</h3></u>
<div>
  	<p>Lógica necesaria para anular votos.</p>
  	<h3>Métodos soportados</h3>
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/anuladorVoto/guardar">/anuladorVoto/guardar</a><br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: El <a href="https://github.com/jgzornoza/SistemaVotacionClientePublicacion/wiki/Anulador-de-voto">anulador de voto</a>.<br/>
         <b>Respuesta:</b><br/>
         Si todo es correcto anulará el voto asociado y devolverá una respuesta HTTP con código de estado 200.
    </p>
	<HR>
</div>
