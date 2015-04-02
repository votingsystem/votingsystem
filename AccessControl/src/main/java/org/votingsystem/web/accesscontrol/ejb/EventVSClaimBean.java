package org.votingsystem.web.accesscontrol.ejb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.votingsystem.json.EventVSMetaInf;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.BakupFiles;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.ZipUtils;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.TimeStampBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;


@Stateless
public class EventVSClaimBean {

    private static final Logger log = Logger.getLogger(EventVSClaimBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject SignatureBean signatureBean;
    @Inject TagVSBean tagVSBean;
    @Inject EventVSBean eventVSBean;
    @Inject TimeStampBean timeStampBean;
    @Inject MessagesBean messages;


    public MessageSMIME saveEvent(MessageSMIME messageSMIME) throws Exception {
        EventVSClaim eventVS;
        UserVS signerVS = messageSMIME.getUserVS();
        log.info("saveEvent - signerVS: " + signerVS.getNif());
        PublishClaimRequest request = messageSMIME.getSignedContent(PublishClaimRequest.class);
        request.validate();
        eventVS = new EventVSClaim(signerVS, request.subject, request.content, request.backupAvailable,
                request.cardinality, request.dateFinish);
        eventVSBean.setEventDatesState(eventVS);
        dao.persist(eventVS);
        if (request.tags != null && !request.tags.isEmpty()) {
            Set<TagVS> tagSet = tagVSBean.save(request.tags);
            eventVS.setTagVSSet(tagSet);
        }
        request.id = eventVS.getId();
        request.dateCreated = eventVS.getDateCreated();
        request.type = TypeVS.CLAIM_EVENT;
        if(request.fieldsEventVS != null && !request.fieldsEventVS.isEmpty()) {
            for(Map fieldMap : request.fieldsEventVS) {
                FieldEventVS fieldEventVS = dao.persist(new FieldEventVS(eventVS, (String) fieldMap.get("content")));

            }
        }
        String fromUser = config.getServerName();
        String toUser = signerVS.getNif();
        String subject = messages.get("mime.subject.claimEventValidated");
        SMIMEMessage smimeMessage = signatureBean.getSMIMEMultiSigned(fromUser, toUser, messageSMIME.getSMIME(), subject);
        messageSMIME.setSMIME(smimeMessage).setEventVS(eventVS);
        dao.merge(messageSMIME);
        return messageSMIME;
    }


    public synchronized EventVSMetaInf generateBackup (EventVSClaim eventVS) throws ExceptionVS, IOException {
        log.info("generateBackup - eventId: " + eventVS.getId());
        BakupFiles backupFiles = new BakupFiles(eventVS, TypeVS.CLAIM_EVENT, config.getProperty("vs.errorsBasePath"),
                config.getProperty("vs.backupBasePath"));
        String datePathPart = DateUtils.getDateStr(eventVS.getDateFinish(),"yyyy/MM/dd");
        String downloadURL = format("/backup/{0}/eventvs_claim_{1}.zip", datePathPart, eventVS.getId()); 
        //String downloadURLPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${downloadURL}"
        if(backupFiles.getZipResult().exists()) {
            log.info("generateBackup - backup file already exists");
            log.info("generateBackup - backup file already exists");
            return new ObjectMapper().readValue(backupFiles.getMetaInfFile(), new TypeReference<EventVSMetaInf>() {});
        }
        Query query = dao.getEM().createQuery("select count(m) from MessageSMIME m where m.eventVS =:eventVS and " +
                "m.type =:type").setParameter("eventVS", eventVS).setParameter("type", TypeVS.CLAIM_EVENT_SIGN);
        DecimalFormat formatted = new DecimalFormat("00000000");
        int claimsBatch = 0;
        String baseDir= format("{0}/batch_{1}", backupFiles.getFilesDir().getAbsolutePath(), formatted.format(++claimsBatch));
        new File(baseDir).mkdirs();
        long begin = System.currentTimeMillis();
        query = dao.getEM().createQuery("select m from MessageSMIME m where m.eventVS =:eventVS and m.type =:type")
                .setParameter("eventVS", eventVS).setParameter("type", TypeVS.CLAIM_EVENT_SIGN);
        List<MessageSMIME> eventSignatures = query.getResultList();
        int counter = 0;
        for(MessageSMIME messageSMIME:eventSignatures) {
            File smimeFile = new File(format("{0}/claim_{1}.p7m", baseDir, formatted.format(++counter)));
            IOUtils.write(messageSMIME.getContent(), new FileOutputStream(smimeFile));
            /*if((eventSigantures.getRowNumber() % 100) == 0) {
                String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillis(System.currentTimeMillis() - begin)
                log.debug(" - accessRequest ${eventSigantures.getRowNumber()} of ${numSignatures} - ${elapsedTimeStr}");
                sessionFactory.currentSession.flush()
                sessionFactory.currentSession.clear()
            }
            if(((eventSigantures.getRowNumber() + 1) % 2000) == 0) {
                baseDir="${filesDir.absolutePath}/batch_${formatted.format(++claimsBatch)}"
                new File(baseDir).mkdirs()
            }*/
        }
        Set<X509Certificate> systemTrustedCerts = signatureBean.getTrustedCerts();
        byte[] systemTrustedCertsPEMBytes = CertUtils.getPEMEncoded(systemTrustedCerts);
        File systemTrustedCertsFile = new File(format("{0}/systemTrustedCerts.pem",
                backupFiles.getFilesDir().getAbsolutePath()));
        IOUtils.write(systemTrustedCertsPEMBytes, new FileOutputStream(systemTrustedCertsFile));

        byte[] timeStampCertPEMBytes = timeStampBean.getSigningCertPEMBytes();
        File timeStampCertFile = new File("{0}/timeStampCert.pem", backupFiles.getFilesDir().getAbsolutePath());
        IOUtils.write(timeStampCertPEMBytes, new FileOutputStream(timeStampCertFile));
        new ZipUtils(backupFiles.getBaseDir()).zipIt(backupFiles.getZipResult());
        Long numSignatures = (long)query.getSingleResult();
        EventVSMetaInf eventMetaInf = new EventVSMetaInf(eventVS, config.getContextURL(), downloadURL);
        eventMetaInf.setClaimBackupData(numSignatures);
        eventVS.setMetaInf(new ObjectMapper().writeValueAsString(eventMetaInf));
        dao.merge(eventVS);
        return eventMetaInf;
    }


    public class PublishClaimRequest {
        Long id;
        Date dateFinish;
        Date dateCreated;
        String subject, content;
        Boolean backupAvailable;
        EventVS.Cardinality cardinality;
        List<String> tags;
        List<Map> fieldsEventVS;
        TypeVS type;


        public PublishClaimRequest() { }

        public void validate() throws ValidationExceptionVS {
            if(dateFinish.before(new Date())) {
                throw new ValidationExceptionVS(messages.get("publishDocumentDateErrorMsg",
                        DateUtils.getDayWeekDateStr(dateFinish)));
            }
            if(cardinality == null) cardinality = EventVS.Cardinality.EXCLUSIVE;
        }

    }
}
