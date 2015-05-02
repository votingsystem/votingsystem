[**Generación y configuración del almacén de claves de la aplicación**](https://github.com/votingsystem/votingsystem/wiki/Almacenes-de-claves).

#### Server certificates

**DO NOT USE 'ControlCenter.jks' CERTIFICATES IN PRODUCTION**. You should get a real certificate signed by a trusted certificate authority.

#### Authority certificates

To add a new trusted authority copy its cert (PEM files) in the dir with the 'AC_' prefix in the file name

- [AC_RAIZ_DNIE_SHA1.pem' certificado raíz del DNI electrónico emitido en España]
(http://www.dnielectronico.es/seccion_integradores/certs.html)
- [AC_RAIZ_DNIE_SHA2.pem' certificado raíz del DNI electrónico emitido en España]
(http://www.dnielectronico.es/seccion_integradores/certs.html)
- [AC_DNIE_001_SHA1.pem' certificado intermedio del DNI electrónico emitido en España]
(http://www.dnielectronico.es/seccion_integradores/certs.html)
- [AC_DNIE_001_SHA1.pem' certificado intermedio del DNI electrónico emitido en España]
(http://www.dnielectronico.es/seccion_integradores/certs.html)
- [AC_DNIE_002_SHA1.pem' certificado intermedio del DNI electrónico emitido en España]
(http://www.dnielectronico.es/seccion_integradores/certs.html)
- [AC_DNIE_002_SHA2.pem' certificado intermedio del DNI electrónico emitido en España]
(http://www.dnielectronico.es/seccion_integradores/certs.html)
- [AC_DNIE_003_SHA1.pem' certificado intermedio del DNI electrónico emitido en España]
(http://www.dnielectronico.es/seccion_integradores/certs.html)
- [AC_DNIE_003_SHA2.pem' certificado intermedio del DNI electrónico emitido en España]
(http://www.dnielectronico.es/seccion_integradores/certs.html)

#### Admin certificates
To add a new admin copy its cert (PEM files) in the dir with the 'ADMIN_' prefix in the file name

