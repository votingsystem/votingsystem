package org.votingsystem.web.accesscontrol.ejb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.votingsystem.json.EventVSMetaInf;
import org.votingsystem.json.RepresentativesAccreditations;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.TimeStampBean;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.mail.Header;
import javax.persistence.Query;
import java.io.File;
import java.io.FileOutputStream;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;


@Stateless
public class EventVSElectionBean {

    private static final Logger log = Logger.getLogger(EventVSElectionBean.class.getSimpleName());

    @Inject ControlCenterBean controlCenterBean;
    @Inject EventVSBean eventVSBean;
    @Inject ConfigVS config;
    @Inject TagVSBean tagVSBean;
    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject RepresentativeBean representativeBean;
    @Inject TimeStampBean timeStampBean;
    @Inject MessagesBean messages;

    public MessageSMIME saveEvent(MessageSMIME messageSMIME) throws Exception {
        UserVS userSigner = messageSMIME.getUserVS();
        Map<String, Object> dataMap = messageSMIME.getSignedContentMap();
        Date requestDate = DateUtils.getDateFromString((String) dataMap.get("dateBegin"));
        Date dateBegin = DateUtils.resetDay(requestDate).getTime();
        Date dateFinish = DateUtils.resetDay(DateUtils.addDays(requestDate, 1).getTime()).getTime();
        ControlCenterVS controlCenterVS = controlCenterBean.getControlCenter();
        EventVSElection eventVS = new EventVSElection((String)dataMap.get("subject"), (String)dataMap.get("content"),
                EventVS.Cardinality.EXCLUSIVE, userSigner, controlCenterVS, dateBegin, dateFinish);
        eventVSBean.setEventDatesState(eventVS);
        if(EventVS.State.TERMINATED ==  eventVS.getState()) throw new ValidationExceptionVS(
                "ERROR - eventFinishedErrorMsg dateFinish: " + dateFinish);
        dataMap.put("dateFinish", DateUtils.getDateStr(dateFinish));
        dataMap.put("controlCenterURL", controlCenterVS.getServerURL());
        Map accessControlMap = new HashMap<>();
        accessControlMap.put("serverURL", config.getContextURL());
        accessControlMap.put("name", config.getServerName());
        if (dataMap.containsKey("tags")) {
            Set<TagVS> tagSet = tagVSBean.save((List<String>) dataMap.get("tags"));
            if(!tagSet.isEmpty()) eventVS.setTagVSSet(tagSet);
        }
        dao.persist(eventVS);
        Set<FieldEventVS> electionOptions = saveElectionOptions(eventVS, (List<String>) dataMap.get("fieldsEventVS"));
        List<Map> optionsMap = new ArrayList<>();
        for(FieldEventVS option : electionOptions) {
            optionsMap.add(option.toMap());
        }
        dataMap.put("fieldsEventVS", optionsMap);
        dataMap.put("id", eventVS.getId());
        dataMap.put("URL",config.getRestURL() + "/eventVSElection/id/" + eventVS.getId());
        dataMap.put("dateCreated", eventVS.getDateCreated());
        dataMap.put("type", TypeVS.VOTING_EVENT);

        KeyStoreVS keyStoreVS = signatureBean.generateElectionKeysStore(eventVS);
        dataMap.put("certCAVotacion", new String(CertUtils.getPEMEncoded (keyStoreVS.getCertificateVS().getX509Cert())));
        dataMap.put("certChain", CertUtils.getPEMEncoded (signatureBean.getCertChain()));
        X509Certificate certUsuX509 = userSigner.getCertificate();
        dataMap.put("userVS", new String(CertUtils.getPEMEncoded(certUsuX509)));
        Header header = new Header ("serverURL", config.getContextURL());
        String fromUser = config.getServerName();
        String toUser = controlCenterVS.getName();
        String subject = messages.get("mime.subject.votingEventValidated");
        SMIMEMessage smime = signatureBean.getSMIMETimeStamped(fromUser, toUser, dataMap.toString(), subject, header);
        ResponseVS responseVS = HttpHelper.getInstance().sendData(smime.getBytes(),
                ContentTypeVS.JSON_SIGNED, controlCenterVS.getServerURL() + "/rest/eventVSElection");
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
            throw new ExceptionVS(messages.get("controlCenterCommunicationErrorMsg"));
        }
        Query query = dao.getEM().createQuery("select c from CertificateVS c where c.type =:type " +
                "and c.actorVS =:actorVS and c.state =:state").setParameter("type", CertificateVS.Type.ACTOR_VS)
                .setParameter("actorVS", controlCenterVS).setParameter("state", CertificateVS.State.OK);
        CertificateVS controlCenterCert = dao.getSingleResult(CertificateVS.class, query);
        X509Certificate controlCenterX509Cert = controlCenterCert.getX509Cert();
        CertificateVS eventVSControlCenterCertificate = dao.persist(
                new CertificateVS(controlCenterVS, eventVS, controlCenterX509Cert));
        CertificateVS eventVSAccessControlCertificate = dao.persist(
                new CertificateVS(null, eventVS, signatureBean.getServerCert()));
        MessageSMIME messageSMIMEResult = new MessageSMIME(smime, TypeVS.RECEIPT, messageSMIME);
        messageSMIMEResult.setEventVS(eventVS);
        dao.persist(messageSMIMEResult);
        dao.merge(eventVS.setState(EventVS.State.ACTIVE));
        return messageSMIMEResult;
        /*return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS, type:TypeVS.VOTING_EVENT,
                messageSMIME: messageSMIME, contentType: ContentTypeVS.JSON_SIGNED)*/
    }

    public Set<FieldEventVS> saveElectionOptions(EventVSElection eventVS, List<String> optionList) throws ExceptionVS {
        if(optionList.size() < 2) throw new ExceptionVS("elections must have at least two options");
        Set<FieldEventVS> result = new HashSet<>();
        for(String option : optionList) {
            FieldEventVS fieldEventVS = dao.persist(new FieldEventVS(eventVS, option));
            result.add(fieldEventVS);
        }
        return result;
    }

    public synchronized void generateBackups () throws Exception {
        Date checkDate = DateUtils.addDays(new Date(), -1).getTime();
        Query query = dao.getEM().createQuery("select e from EventVSElection e where e.dateFinish <:checkDate " +
                "and e.backupAvailable =:backupAvailable").setParameter("checkDate", checkDate)
                .setParameter("backupAvailable", Boolean.FALSE);
        List<EventVSElection> terminatedPolls = query.getResultList();
        for(EventVSElection eventVS : terminatedPolls) {
            generateBackup(eventVS);
        }
    }

    @Asynchronous
    public synchronized void generateBackup (EventVSElection eventVS) throws Exception {
        log.info("generateBackup - eventVS:" + eventVS.getId());
        /*if (eventVS.isActive(Calendar.getInstance().getTime())) {
            throw new ExceptionVS(messageSource.getMessage('eventActiveErrorMsg', [eventVS.id].toArray(), locale))
        }*/
        BakupFiles backupFiles = new BakupFiles(eventVS, TypeVS.VOTING_EVENT, config.getProperty("vs.errorsBasePath"),
                config.getProperty("vs.backupBasePath"));
        File filesDir = backupFiles.getFilesDir();
        String datePathPart = DateUtils.getDateStr(eventVS.getDateFinish(),"yyyy_MM_dd");
        String downloadURL = format("/backup/{0}/{1}_${2}.zip", datePathPart, TypeVS.VOTING_EVENT, eventVS.getId());
        //String webResourcePath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${downloadURL}"
        if(backupFiles.getZipResult().exists()) {
            log.info("generateBackup - backup file already exists");
            //return new ObjectMapper().readValue(backupFiles.getMetaInfFile(), new TypeReference<EventVSMetaInf>() {});
            return;
        }
        RepresentativesAccreditations representativesAccreditations =
                representativeBean.getAccreditationsBackupForEvent(eventVS);
        Set<X509Certificate> eventTrustedCerts = signatureBean.getEventTrustedCerts(eventVS);
        Set<X509Certificate> systemTrustedCerts = signatureBean.getTrustedCerts();
        File systemTrustedCertsFile = new File(format("{0}/systemTrustedCerts.pem", filesDir.getAbsolutePath()));
        IOUtils.write(CertUtils.getPEMEncoded(systemTrustedCerts), new FileOutputStream(systemTrustedCertsFile));

        File eventTrustedCertsFile = new File(format("{0}/eventTrustedCerts.pem", filesDir.getAbsolutePath()));
        IOUtils.write(CertUtils.getPEMEncoded(eventTrustedCerts), new FileOutputStream(eventTrustedCertsFile));

        File timeStampCertFile = new File(format("{0}/timeStampCert.pem", filesDir.getAbsolutePath()));
        IOUtils.write(CertUtils.getPEMEncoded(timeStampBean.getSigningCertPEMBytes()), new FileOutputStream(timeStampCertFile));

        Query query = dao.getEM().createQuery("select count(v) from VoteVS v where v.state =:state and v.eventVS=:eventVS")
                .setParameter("state", VoteVS.State.OK).setParameter("eventVS", eventVS);
        Long numTotalVotes = (long) query.getSingleResult();
        query = dao.getEM().createQuery("select count(a) from AccessRequestVS a where a.state =:state " +
                "and a.eventVS =:eventVS").setParameter("state", AccessRequestVS.State.OK).setParameter("eventVS", eventVS);
        Long numTotalAccessRequests = (long) query.getSingleResult();
        EventVSMetaInf eventMetaInf = new EventVSMetaInf(eventVS, config.getContextURL(), downloadURL);
        eventMetaInf.setBackupData(numTotalVotes, numTotalAccessRequests);
        new ObjectMapper().writeValue(new FileOutputStream(backupFiles.getMetaInfFile()), eventMetaInf);
        DecimalFormat formatted = new DecimalFormat("00000000");
        Long votesBatch = 0L;
        String votesBaseDir= format("{0}/votes/batch_{1}", filesDir.getAbsolutePath(), formatted.format(++votesBatch));
        new File(votesBaseDir).mkdirs();
        Long accessRequestBatch = 0L;
        String accessRequestBaseDir= format("{0}/accessRequest/batch_{1}", filesDir.getAbsolutePath(),
                formatted.format(++accessRequestBatch));
        new File(accessRequestBaseDir).mkdirs();
        long begin = System.currentTimeMillis();
        //TODO
        query = dao.getEM().createQuery("select v from VoteVS v where v.state =:state and v.eventVS =:eventVS")
                .setParameter("state", VoteVS.State.OK).setParameter("eventVS", eventVS);
        List<VoteVS> votes = query.getResultList();
        for (VoteVS voteVS : votes) {
            UserVS representative = voteVS.getCertificateVS().getUserVS();
            File smimeFile = null;
            if(representative != null) {//not anonymous, representative vote
                smimeFile = new File(format("{0}/representativeVote_{1}.p7m", votesBaseDir, representative.getNif()));
            } else {//anonymous, user vote
                smimeFile = new File(format("{0}/vote_{1}.p7m", votesBaseDir, formatted.format(voteVS.getId())));
            }
            IOUtils.write(voteVS.getMessageSMIME().getContent(), new FileOutputStream(smimeFile));
            /*if(((votes.getRowNumber() + 1) % 2000) == 0) {
                votesBaseDir="${filesDir.absolutePath}/votes/batch_${formatted.format(++votesBatch)}"
                new File(votesBaseDir).mkdirs()
            }*/
        }
        begin = System.currentTimeMillis();
        query = dao.getEM().createQuery("select a from AccessRequestVS a where a.state =:state and a.eventVS =:eventVS")
                .setParameter("state", AccessRequestVS.State.OK).setParameter("eventVS", eventVS);
        List<AccessRequestVS> accessRequestList = query.getResultList();
        for(AccessRequestVS accessRequest : accessRequestList) {
            File smimeFile = new File(format("{0}/accessRequest_{1}.p7m", accessRequestBaseDir, accessRequest.getUserVS().getNif()));
            IOUtils.write(accessRequest.getMessageSMIME().getContent(), new FileOutputStream(smimeFile));
            /*if((accessRequests.getRowNumber() % 100) == 0) {
                String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillis(
                        System.currentTimeMillis() - begin)
                log.debug(" - accessRequest ${accessRequests.getRowNumber()} of ${numTotalAccessRequests} - ${elapsedTimeStr}");
                sessionFactory.currentSession.flush()
                sessionFactory.currentSession.clear()
            }
            if(((accessRequests.getRowNumber() + 1) % 2000) == 0) {
                accessRequestBaseDir="${filesDir.absolutePath}/accessRequest/batch_${formatted.format(++accessRequestBatch)}"
                new File(accessRequestBaseDir).mkdirs()
            }*/
        }
        new ZipUtils(backupFiles.getBaseDir()).zipIt(backupFiles.getZipResult());
        //TODO copy zip result to serve as static resource -> downloadURL
        //if (!eventVS.isAttached()) { eventVS.attach() }
        eventVS.setMetaInf(new ObjectMapper().writeValueAsString(eventMetaInf));
        dao.merge(eventVS);
        log.info("ZipResult absolutePath: " + backupFiles.getZipResult().getAbsolutePath());
    }


    public Map getStatsMap (EventVSElection eventVS) {
        Map result = new HashMap();
        result.put("id", eventVS.getId());
        result.put("subject", eventVS.getSubject());
        Query query = dao.getEM().createQuery("select count (a) from AccessRequestVS a where a.eventVS =:eventVS")
                .setParameter("eventVS", eventVS);
        result.put("numAccessRequests", (long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(a) from AccessRequestVS a where a.eventVS =:eventVS " +
                "and a.state =:state").setParameter("eventVS", eventVS).setParameter("state", AccessRequestVS.State.OK);
        result.put("numAccessRequestsOK", (long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(a) from AccessRequestVS a where a.eventVS =:eventVS " +
                "and a.state =:state").setParameter("eventVS", eventVS).setParameter("state", AccessRequestVS.State.CANCELED);
        result.put("numAccessRequestsCancelled", (long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(v) from VoteVS v where v.eventVS =:eventVS").setParameter("eventVS", eventVS);
        result.put("numVotesVS", (long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(v) from VoteVS v where v.eventVS =:eventVS and v.state =:state")
                .setParameter("eventVS", eventVS).setParameter("state", VoteVS.State.OK);
        result.put("numVotesVSOK", (long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(v) from VoteVS v where v.eventVS =:eventVS and v.state =:state")
                .setParameter("eventVS", eventVS).setParameter("state", VoteVS.State.CANCELED);
        result.put("numVotesVSVotesVSCANCELED", (long) query.getSingleResult());
        List<Map> optionList = new ArrayList<>();
        for(FieldEventVS option : eventVS.getFieldsEventVS()) {
            query = dao.getEM().createQuery("select count(v) from VoteVS v where v.optionSelected =:option " +
                    "and v.state =:state").setParameter("option", option).setParameter("state", VoteVS.State.OK);
            optionList.add(option.toMap((long) query.getSingleResult()));
        }
        result.put("fieldsEventVS", optionList);
        return result;
    }

}