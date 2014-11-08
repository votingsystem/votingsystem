Certs are PEM files.
To add a new trusted authority copy its cert chain within the dir  './WEB_INF/votingsystem/certs' in a file with
the 'AC_' prefix in the file name

- 'AC_RAIZ_DNIE_SHA1.pem' certificateVS raíz del DNI electrónico emitido en España
(http://www.dnielectronico.es/seccion_integradores/certs.html)
- 'AC_RAIZ_DNIE_SHA2.pem' certificateVS raíz del DNI electrónico emitido en España
(http://www.dnielectronico.es/seccion_integradores/certs.html)
- 'AC_DNIE_001_SHA1.pem' certificateVS intermedio del DNI electrónico emitido en España
(http://www.dnielectronico.es/seccion_integradores/certs.html)
- 'AC_DNIE_001_SHA1.pem' certificateVS intermedio del DNI electrónico emitido en España
(http://www.dnielectronico.es/seccion_integradores/certs.html)
- 'AC_DNIE_002_SHA1.pem' certificateVS intermedio del DNI electrónico emitido en España
(http://www.dnielectronico.es/seccion_integradores/certs.html)
- 'AC_DNIE_002_SHA2.pem' certificateVS intermedio del DNI electrónico emitido en España
(http://www.dnielectronico.es/seccion_integradores/certs.html)
- 'AC_DNIE_003_SHA1.pem' certificateVS intermedio del DNI electrónico emitido en España
(http://www.dnielectronico.es/seccion_integradores/certs.html)
- 'AC_DNIE_003_SHA2.pem' certificateVS intermedio del DNI electrónico emitido en España
(http://www.dnielectronico.es/seccion_integradores/certs.html)

Generación y configuración del almacén de claves de la aplicación:
https://github.com/votingsystem/votingsystem/wiki/Almacenes-de-claves

--------------------------------------------------------------------------------------------
- Para renovar los certificados del servidor renombrar el archivo AccessControl.jks a AccessControl.jks_cancelled
y copiar los nuevos certificados en un almacén de claves JKS con el name AccessControl.jks.

- Para dar de baja en el sistema los certificados asociados a uno de los archivos del directorio, 
renombrar el archivo correspondiente añadiendo al final del name la cadena "_cancelled"

