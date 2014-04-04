--- Por favor, no hagas planes contando con este proyecto porque la información puede cambiar sin previo aviso. ---

El código de este directorio corresponde al Applet empleado para poder votar desde plataformas PC 
a través del navegador

Para generar el Applet firmado hay que ejecutar desde la línea de comandos:
	
	$gradle install

* Al tener que acceder a recursos restringidos del sistema el Applet debe ir firmado.

--------------------------------------------------------------------------------------------------------------------
A partir Java 7 Update 51 para poder ejecutar el applet autofirmado hay que:
* Modificar el nivel de seguridad permitido en el panel de control
* Añadir el servidor en la lista de excepciones de sitios

http://www.java.com/en/download/help/java_blocked.xml

