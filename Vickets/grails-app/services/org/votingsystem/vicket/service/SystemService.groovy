package org.votingsystem.vicket.service

import grails.transaction.Transactional
import net.sf.json.JSONArray
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VicketTagVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.NifUtils
import org.votingsystem.vicket.model.UserVSAccount
import org.votingsystem.vicket.util.IbanVSUtil

import java.security.cert.X509Certificate

@Transactional
class SystemService {

    private static final CLASS_NAME = SystemService.class.getSimpleName()

    private UserVS systemUser
    private VicketTagVS wildTag
    private Locale defaultLocale
    def grailsApplication
    def subscriptionVSService
    def signatureVSService

    @Transactional
    public synchronized Map init() throws Exception {
        log.debug("init")
        systemUser = UserVS.findWhere(type:UserVS.Type.SYSTEM)
        if(!systemUser) {
            systemUser = new UserVS(nif:grailsApplication.config.VotingSystem.systemNIF, type:UserVS.Type.SYSTEM,
                    name:grailsApplication.config.VotingSystem.serverName).save()
            systemUser.setIBAN(IbanVSUtil.getInstance().getIBAN(systemUser.id))
            systemUser.save()
            wildTag = new VicketTagVS(name:VicketTagVS.WILDTAG).save()
            new UserVSAccount(currencyCode: Currency.getInstance('EUR').getCurrencyCode(), userVS:systemUser,
                    balance:BigDecimal.ZERO, IBAN:systemUser.getIBAN(), tag:wildTag).save()
        }
        updateAdmins()
        return [systemUser:systemUser]
    }

    @Transactional
    public JSONArray updateAdmins() {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        File directory = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certAuthoritiesDirPath).getFile()
        File[] adminCerts = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String fileName) {
                return fileName.startsWith("ADMIN_") && fileName.endsWith(".pem");
            }
        });
        JSONArray adminsArray = new JSONArray()
        for(File adminCert:adminCerts) {
            log.debug("$methodName - checking admin cert '${adminCert.absolutePath}'")
            X509Certificate x509AdminCert = CertUtil.fromPEMToX509Cert(FileUtils.getBytesFromFile(adminCert))
            UserVS userVS = UserVS.getUserVS(x509AdminCert)
            ResponseVS responseVS = signatureVSService.verifyUserCertificate(userVS)
            if(ResponseVS.SC_OK != responseVS.statusCode) throw new ExceptionVS("$updateAdmins - Problems verifying " +
                    "admin certificate - '${responseVS.getMessage()}'")
            responseVS = subscriptionVSService.checkUser(userVS, getDefaultLocale())
            if(ResponseVS.SC_OK != responseVS.statusCode) throw new ExceptionVS("Problems updating admin cert " +
                    "'${adminCert.absolutePath}'")
            userVS = responseVS.userVS
            if(!userVS.IBAN) subscriptionVSService.checkUserVSAccount(userVS)
            adminsArray.add(userVS.getNif())
        }
        getSystemUser().getMetaInfJSON().put("adminsDNI", adminsArray)
        getSystemUser().setMetaInf(getSystemUser().getMetaInfJSON().toString())
        getSystemUser().save()
        log.debug("$methodName - adminsDNI: '${getSystemUser().getMetaInfJSON().adminsDNI}'")
        return systemUser.getMetaInfJSON().adminsDNI
    }

    boolean isUserAdmin(String nif) {
        nif = NifUtils.validate(nif);
        return getSystemUser().getMetaInfJSON()?.adminsDNI?.contains(nif)
    }

    public void updateTagBalance(BigDecimal amount, String currencyCode, VicketTagVS tag) {
        UserVSAccount tagAccount = UserVSAccount.findWhere(userVS:getSystemUser(), tag:tag, currencyCode:currencyCode,
                state: UserVSAccount.State.ACTIVE)
        if(!tagAccount) throw new Exception("THERE'S NOT ACTIVE SYSTEM ACCOUNT FOR TAG '${tag.name}' and currency '${currencyCode}'")
        tagAccount.balance = tagAccount.balance.add(amount)
        tagAccount.save()
    }

    public Locale getDefaultLocale() {
        if(defaultLocale == null) defaultLocale = new Locale(grailsApplication.config.VotingSystem.defaultLocale)
        return defaultLocale
    }

    public UserVS getSystemUser() {
        if(!systemUser) systemUser = init().systemUser
        return systemUser;
    }

    public VicketTagVS getWildTag() {
        if(!wildTag) wildTag = VicketTagVS.findWhere(name:VicketTagVS.WILDTAG)
        return wildTag
    }
}
