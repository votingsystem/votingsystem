package org.sistemavotacion.controlacceso

import org.apache.log4j.*
import org.sistemavotacion.controlacceso.modelo.*;
import grails.test.mixin.*
import org.junit.*
import grails.test.*
/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(SolicitudCopiaController)
class SolicitudCopiaControllerTests extends GroovyTestCase {
	

	protected void setUp() { super.setUp()}
	
	protected void tearDown() {
		super.tearDown()
	}
	
    void testSomething() {
		log.debug("hola")
		assert Respuesta.SC_OK == 200
    }
}
