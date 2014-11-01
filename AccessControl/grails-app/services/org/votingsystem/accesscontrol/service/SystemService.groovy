package org.votingsystem.accesscontrol.service

import grails.transaction.Transactional
import net.sf.json.JSONArray
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ControlCenterVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.NifUtils
import java.security.cert.X509Certificate

@Transactional
class SystemService {

    private UserVS systemUser
    def grailsApplication
    private ControlCenterVS controlCenter

    @Transactional public synchronized Map init() throws Exception {
        log.debug("init")
        systemUser = UserVS.findWhere(type:UserVS.Type.SYSTEM)
        if(!systemUser) {
            systemUser = new UserVS(nif:grailsApplication.config.vs.systemNIF, type:UserVS.Type.SYSTEM,
                    name:grailsApplication.config.vs.serverName).save()
        }
        updateAdmins()
        controlCenter = getControlCenter()
        return [systemUser:systemUser]
    }

    public ControlCenterVS getControlCenter() {
        if(controlCenter == null) controlCenter = loadControlCenter()
        return controlCenter
    }

    @Transactional public JSONArray updateAdmins() {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        File directory = grailsApplication.mainContext.getResource(
                grailsApplication.config.vs.certAuthoritiesDirPath).getFile()
        File[] adminCerts = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String fileName) {
                return fileName.startsWith("ADMIN_") && fileName.endsWith(".pem");
            }
        });
        JSONArray adminsArray = new JSONArray()
        for(File adminCert:adminCerts) {
            log.debug("$methodName - checking admin cert '${adminCert.absolutePath}'")
            X509Certificate x509AdminCert = CertUtils.fromPEMToX509Cert(FileUtils.getBytesFromFile(adminCert))
            UserVS userVS = UserVS.getUserVS(x509AdminCert)
            //to avoid circular references
            ((SignatureVSService)grailsApplication.mainContext.getBean("signatureVSService")).verifyUserCertificate(userVS)
            ResponseVS responseVS = ((SubscriptionVSService)grailsApplication.mainContext.getBean(
                    "subscriptionVSService")).checkUser(userVS)
            if(ResponseVS.SC_OK != responseVS.statusCode) throw new ExceptionVS("Problems updating admin cert " +
                    "'${adminCert.absolutePath}'")
            userVS = responseVS.userVS
            adminsArray.add(userVS.getNif())
        }
        getSystemUser().getMetaInfJSON().put("adminsDNI", adminsArray)
        getSystemUser().setMetaInf(getSystemUser().getMetaInfJSON().toString())
        getSystemUser().save()
        log.debug("$methodName - admins list: '${getSystemUser().getMetaInfJSON().adminsDNI}'")
        return systemUser.getMetaInfJSON().adminsDNI
    }

    boolean isUserAdmin(String nif) {
        nif = NifUtils.validate(nif);
        return getSystemUser().getMetaInfJSON()?.adminsDNI?.contains(nif)
    }

    private ControlCenterVS loadControlCenter() throws Exception {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        List<ControlCenterVS> controlCenters = ControlCenterVS.createCriteria().list (offset: 0, order:'desc') {
            eq("state", ActorVS.State.OK)
        }
        if(!controlCenters.isEmpty()) {
            ControlCenterVS controlCenterVS = controlCenters.iterator().next()
            ResponseVS responseVS = ((SubscriptionVSService)grailsApplication.mainContext.getBean(
                    "subscriptionVSService")).checkControlCenter(controlCenterVS.serverURL)
            if(ResponseVS.SC_OK == responseVS.statusCode) {
                log.debug("$methodName - There are '${controlCenters.size()}' with state 'OK' - fetching Control Center " +
                        "with url: '${controlCenterVS.serverURL}' ")
                return responseVS.data.controlCenterVS
            } else throw new ExceptionVS(responseVS.getMessage())
        }
        log.error("Missing Control Center!!!!")
        String targetURL = null
        if(grails.util.Metadata.current.isWarDeployed()) {
            targetURL = "${grailsApplication.config.vs.prod.controlCenterURL}"
        } else  targetURL = "${grailsApplication.config.vs.dev.controlCenterURL}"
        ResponseVS responseVS = ((SubscriptionVSService)grailsApplication.mainContext.getBean(
                "subscriptionVSService")).checkControlCenter(targetURL)
        if(ResponseVS.SC_OK == responseVS.statusCode) return responseVS.data.controlCenterVS
        return null;
    }

    public UserVS getSystemUser() {
        if(!systemUser) systemUser = init().systemUser
        return systemUser;
    }

}