//grails run-script src/scripts/genKeyStore.groovy --stacktrace
import grails.util.Metadata
import org.votingsystem.accesscontrol.service.KeyStoreService
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.util.FileUtils

println  "genKeyStore - Evironment: '${grails.util.Environment.current}' - isWarDeployed: ${Metadata.current.isWarDeployed()}"
ContextVS.init()
KeyStoreService keyStoreService = ctx.getBean('keyStoreService')

String password = "ABCDE"
String givenName = "00111222GivenName"
String surname = "00111222Surname"
String nif = "00111222V"
ResponseVS responseVS = keyStoreService.generateUserTestKeysStore(givenName, surname, nif, password)
if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
    byte[] resultBytes = KeyStoreUtil.getBytes(responseVS.data, password.toCharArray())
    File destFile = new File("${System.getProperty('user.home')}/${givenName}_${nif}.jks")
    destFile.createNewFile()
    println("keyStore file: $destFile.absolutePath")
    FileUtils.copyStreamToFile(new ByteArrayInputStream(resultBytes), destFile);
}

println("result - statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage())