--- Por favor, no hagas planes contando con este proyecto porque la información puede cambiar sin previo aviso. ---

El código de este repositorio corresponde a la aplicación ejecutada por la aplicación de voto y recogida de firmas:
https://github.com/jgzornoza/SistemaVotacionControlAcceso/wiki/Control-de-Acceso

Para compilar la aplicación es necesario tener en instalado en maven la dependencia plugin.jar.	
(El Applet se comunica con la parte Javascript de la aplicación utilizando la clase
JSObject de la librería plugin.jar). El archivo plugin.jar se puede encontrar en 
el directorio jre/lib de las máquinas virtuales de Oracle.


Una vez disponible el archivo plugin.jar se puede instalar en el repositorio 
local de Maven con el comando:
    mvn install:install-file -DgroupId=sun.plugin \
    -DartifactId=plugin \
    -Dversion=1.3 \
    -Dfile=plugin.jar \
    -Dpackaging=jar \
    -DgeneratePom=true


Para generar el Applet ejecutar:
        mvn assembly:assembly
        Fimar el archivo target/SistemaVotacion-jar-with-dependencies.jar
	

* Al tener que acceder a recursos restringidos del sistema el Applet debe ir firmado.

-------------------------------------------------------------------------------
Partido Político del Programa -> 
https://github.com/jgzornoza/SistemaVotacionControlAcceso/wiki/Partido-Pol%C3%ADtico-del-Programa
