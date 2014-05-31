package org.votingsystem.vicket.service

import grails.converters.JSON
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubscriptionVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.vicket.MetaInfMsg
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.IbanVSUtil
import org.votingsystem.util.NifUtils

import java.security.cert.X509Certificate

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class UserVSService {
	
	static transactional = false

    def signatureVSService
	def grailsApplication
    def grailsLinkGenerator
    def messageSource
    private UserVS systemUser

    public synchronized Map init() throws Exception {
        log.debug("init")
        systemUser = UserVS.findWhere(type:UserVS.Type.SYSTEM)
        if(!systemUser) {
            systemUser = new UserVS(nif:grailsApplication.config.VotingSystem.systemNIF, type:UserVS.Type.SYSTEM,
                    name:grailsApplication.config.VotingSystem.serverName).save()
            systemUser.setIBAN(IbanVSUtil.getInstance().getIBAN(systemUser.id))
            systemUser.save()
        }
        return [systemUser:systemUser]
    }

    public UserVS getSystemUser() {
        if(!systemUser) systemUser = init().systemUser
        return systemUser;
    }

    public ResponseVS saveVicketSource(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("saveVicketSource - methodName: ${methodName}")
        UserVS vicketSource = null
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("saveVicketSource - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.info || !messageJSON.certChainPEM ||
                (TypeVS.VICKET_SOURCE_NEW != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName} - PARAMS ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_ERROR, message:msg,
                    metaInf:MetaInfMsg.getErrorParamsMsg(methodName), statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        Collection<X509Certificate> certChain = CertUtil.fromPEMToX509CertCollection(messageJSON.certChainPEM.getBytes());
        responseVS = signatureVSService.validateCertificates(new ArrayList(certChain))

        X509Certificate x509Certificate = certChain.iterator().next();
        for(Iterator iterator = certChain.iterator();iterator.hasNext(); ) {
            log.debug ("====== ${((X509Certificate)iterator.next()).getSubjectDN()}")
        }
        //{info:getEditor_editorDivData(),certChainPEM:$("#pemCert").val(), operation:Operation.VICKET_SOURCE_NEW}


        /*

        if (!messageJSON.groupvsName || !messageJSON.groupvsInfo ||
                (TypeVS.VICKET_GROUP_NEW != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "saveGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    metaInf:MetaInfMsg.saveVicketGroup_ERROR_params, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }

        groupVS = GroupVS.findWhere(name:messageJSON.groupvsName.trim())
        if(groupVS) {
            msg = messageSource.getMessage('nameGroupRepeatedMsg', [messageJSON.groupvsName].toArray(), locale)
            log.error "saveGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.saveVicketGroup_ERROR_nameGroupRepeatedMsg)
        }

        groupVS = new GroupVS(name:messageJSON.groupvsName.trim(), state:UserVS.State.ACTIVE, groupRepresentative:userSigner,
                description:messageJSON.groupvsInfo).save()
        groupVS.setIBAN(IbanVSUtil.getInstance().getIBAN(groupVS.id))
        String metaInf =  MetaInfMsg.saveVicketGroup_OK + groupVS.id

        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = userSigner.getNif()
        String subject = messageSource.getMessage('newGroupVSReceiptSubject', null, locale)
        byte[] smimeMessageRespBytes = signatureVSService.getSignedMimeMessage(fromUser, toUser, documentStr, subject, null)

        MessageSMIME.withTransaction { new MessageSMIME(type:TypeVS.RECEIPT, metaInf:metaInf,
                smimeParent:messageSMIMEReq, content:smimeMessageRespBytes).save() }
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_GROUP_NEW, data:groupVS)*/
    }

	public Map getUserVS(Date fromDate){
		def usersVS
		UserVS.withTransaction {
			usersVS = UserVS.createCriteria().list(offset: 0) {
				gt("dateCreated", fromDate)
			}
		}
		return [totalNumUsu:usersVS?usersVS.getTotalCount():0]
	}

    public Map getUserVSDataMap(UserVS userVS){
        return [id:userVS?.id, nif:userVS?.nif, firstName: userVS.firstName, lastName: userVS.lastName, name:userVS.name,
            IBAN:userVS.IBAN, state:userVS.state.toString(), type:userVS.type.toString()]
    }

    public Map getSubscriptionVSDataMap(SubscriptionVS subscriptionVS){
        Map resultMap = [id:subscriptionVS.id, dateActivated:subscriptionVS.dateActivated,
             dateCancelled:subscriptionVS.dateCancelled, lastUpdated:subscriptionVS.lastUpdated,
             uservs:[id:subscriptionVS.userVS.id, NIF:subscriptionVS.userVS.nif,
                   name:"${subscriptionVS.userVS.firstName} ${subscriptionVS.userVS.lastName}"],
             groupvs:[name:subscriptionVS.groupVS.name, id:subscriptionVS.groupVS.id],
                state:subscriptionVS.state.toString(), dateCreated:subscriptionVS.dateCreated]
        return resultMap
    }

    public Map getSubscriptionVSDetailedDataMap(SubscriptionVS subscriptionVS){
        String subscriptionMessageURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${subscriptionVS.subscriptionSMIME.id}"
        def adminMessages = []
        SubscriptionVS.withTransaction {
            subscriptionVS.adminMessageSMIMESet.each {adminMessage ->
                adminMessages.add("${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${adminMessage.id}")
            }
        }

        Map resultMap = [id:subscriptionVS.id, dateActivated:subscriptionVS.dateActivated,
                dateCancelled:subscriptionVS.dateCancelled, lastUpdated:subscriptionVS.lastUpdated,
                messageURL:subscriptionMessageURL,adminMessages:adminMessages,
                uservs:[id:subscriptionVS.userVS.id, NIF:subscriptionVS.userVS.nif,
                      name:"${subscriptionVS.userVS.firstName} ${subscriptionVS.userVS.lastName}"],
                groupvs:[name:subscriptionVS.groupVS.name, id:subscriptionVS.groupVS.id],
                state:subscriptionVS.state.toString(), dateCreated:subscriptionVS.dateCreated]
        return resultMap
    }

	boolean isUserAdmin(String nif) {
        nif = NifUtils.validate(nif);
        boolean result = grailsApplication.config.VotingSystem.adminsDNI.contains(nif)
        if(result) log.debug("isUserAdmin - nif: ${nif}")
		return result
	}

}

