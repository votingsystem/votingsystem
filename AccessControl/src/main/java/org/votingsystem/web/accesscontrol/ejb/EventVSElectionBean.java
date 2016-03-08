package org.votingsystem.web.accesscontrol.ejb;

import org.apache.commons.io.IOUtils;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.*;
import org.votingsystem.model.*;
import org.votingsystem.model.voting.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.TimeStampBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.io.File;
import java.io.FileOutputStream;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

@Stateless
public class EventVSElectionBean {

    private static final Logger log = Logger.getLogger(EventVSElectionBean.class.getName());

    @Inject EventVSBean eventVSBean;
    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject RepresentativeBean representativeBean;
    @Inject TimeStampBean timeStampBean;

    public EventVSElection saveEvent(MessageCMS messageCMS) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        UserVS userSigner = messageCMS.getUserVS();
        EventVSDto request  = messageCMS.getSignedContent(EventVSDto.class);
        request.setDateFinish(DateUtils.resetDay(DateUtils.addDays(request.getDateBegin(), 1).getTime()).getTime());
        ControlCenterVS controlCenterVS = config.getControlCenter();
        Query query = dao.getEM().createQuery("select a from ActorVS a where a.serverURL =:serverURL")
                .setParameter("serverURL", config.getContextURL());
        AccessControlVS accessControlVS = dao.getSingleResult(AccessControlVS.class, query);
        EventVSElection eventVS = request.getEventVSElection();
        eventVS.setUserVS(userSigner);
        eventVS.setControlCenterVS(controlCenterVS);
        eventVS.setAccessControlVS(accessControlVS);
        eventVSBean.setEventDatesState(eventVS);
        if(EventVS.State.TERMINATED ==  eventVS.getState()) throw new ValidationExceptionVS(
                "ERROR - eventFinishedErrorMsg dateFinish: " + request.getDateFinish());
        request.setControlCenterURL(controlCenterVS.getServerURL());
        dao.persist(eventVS);
        eventVS.setAccessControlEventVSId(eventVS.getId());
        request.setFieldsEventVS(eventVS.getFieldsEventVS());
        request.setId(eventVS.getId());
        request.setAccessControlEventVSId(eventVS.getId());
        request.setAccessControlURL(config.getContextURL());
        request.setURL(config.getContextURL() + "/rest/eventVSElection/id/" + eventVS.getId());
        request.setDateCreated(eventVS.getDateCreated());
        request.setType(EventVS.Type.ELECTION);

        KeyStoreDto keyStoreDto = signatureBean.generateElectionKeysStore(eventVS);
        CertificateVS certificateVS = dao.persist(CertificateVS.ELECTION((X509Certificate) keyStoreDto.getX509Cert()));
        dao.merge(eventVS.setCertificateVS(certificateVS));
        keyStoreDto.getKeyStoreVS().setCertificateVS(certificateVS);
        KeyStoreVS keyStoreVS = dao.persist(keyStoreDto.getKeyStoreVS());
        eventVS.setKeyStoreVS(keyStoreVS);

        request.setCertCAVotacion( new String(PEMUtils.getPEMEncoded (keyStoreVS.getCertificateVS().getX509Cert())));
        request.setCertChain(new String(PEMUtils.getPEMEncoded (signatureBean.getCertChain())));
        X509Certificate certUsuX509 = userSigner.getCertificate();
        request.setUserVS(new String(PEMUtils.getPEMEncoded(certUsuX509)));
        CMSSignedMessage cms = signatureBean.signDataWithTimeStamp(JSON.getMapper().writeValueAsString(request));
        ResponseVS responseVS = HttpHelper.getInstance().sendData(cms.toPEM(),
                ContentTypeVS.JSON_SIGNED, controlCenterVS.getServerURL() + "/rest/eventVSElection");
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
            throw new ExceptionVS(messages.get("controlCenterCommunicationErrorMsg", controlCenterVS.getServerURL()));
        }
        query = dao.getEM().createQuery("select c from CertificateVS c where c.type =:type " +
                "and c.actorVS =:actorVS and c.state =:state").setParameter("type", CertificateVS.Type.ACTOR_VS)
                .setParameter("actorVS", controlCenterVS).setParameter("state", CertificateVS.State.OK);
        CertificateVS controlCenterCert = dao.getSingleResult(CertificateVS.class, query);
        CertificateVS controlCenterCertEventVS = dao.persist(
                CertificateVS.ACTORVS(controlCenterVS, controlCenterCert.getX509Cert()));
        CertificateVS accessControlCertEventVS = dao.persist(
                CertificateVS.ACTORVS(null, signatureBean.getServerCert()));
        dao.merge(messageCMS.setType(TypeVS.VOTING_EVENT).setCMS(cms));
        dao.merge(eventVS.setControlCenterCert(controlCenterCertEventVS).setAccessControlCert(accessControlCertEventVS)
                .setState(EventVS.State.ACTIVE).setPublishRequestCMS(messageCMS));
        return eventVS;
    }

    public void generateBackups () throws Exception {
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
    public void generateBackup (EventVSElection eventVS) throws Exception {
        log.info("generateBackup - eventVS:" + eventVS.getId());
        /*if (eventVS.isActive(Calendar.getInstance().getTime())) {
            throw new ExceptionVS(messageSource.getMessage('eventActiveErrorMsg', [eventVS.id].toArray(), locale))
        }*/
        BakupFiles backupFiles = new BakupFiles(eventVS, TypeVS.VOTING_EVENT, config.getServerDir().getAbsolutePath());
        File filesDir = backupFiles.getFilesDir();
        String datePathPart = DateUtils.getDateStr(eventVS.getDateFinish(),"yyyy_MM_dd");
        String downloadURL = config.getStaticResURL() + format("/backup/{0}/{1}_${2}.zip", datePathPart,
                TypeVS.VOTING_EVENT, eventVS.getId());
        if(backupFiles.getZipResult().exists()) {
            log.info("generateBackup - backup file already exists");
            //return JSON.getMapper().readValue(backupFiles.getMetaInfFile(), new TypeReference<EventVSMetaInf>() {});
            return;
        }
        RepresentativesAccreditations representativesAccreditations =
                representativeBean.getAccreditationsBackupForEvent(eventVS);
        Set<X509Certificate> eventTrustedCerts = eventVS.getTrustedCerts();
        Set<X509Certificate> systemTrustedCerts = signatureBean.getTrustedCerts();
        File systemTrustedCertsFile = new File(format("{0}/systemTrustedCerts.pem", filesDir.getAbsolutePath()));
        IOUtils.write(PEMUtils.getPEMEncoded(systemTrustedCerts), new FileOutputStream(systemTrustedCertsFile));

        File eventTrustedCertsFile = new File(format("{0}/eventTrustedCerts.pem", filesDir.getAbsolutePath()));
        IOUtils.write(PEMUtils.getPEMEncoded(eventTrustedCerts), new FileOutputStream(eventTrustedCertsFile));

        File timeStampCertFile = new File(format("{0}/timeStampCert.pem", filesDir.getAbsolutePath()));
        IOUtils.write(PEMUtils.getPEMEncoded(timeStampBean.getSigningCertPEMBytes()), new FileOutputStream(timeStampCertFile));

        Query query = dao.getEM().createQuery("select count(v) from VoteVS v where v.state =:state and v.eventVS=:eventVS")
                .setParameter("state", VoteVS.State.OK).setParameter("eventVS", eventVS);
        Long numTotalVotes = (long) query.getSingleResult();
        query = dao.getEM().createQuery("select count(a) from AccessRequestVS a where a.state =:state " +
                "and a.eventVS =:eventVS").setParameter("state", AccessRequestVS.State.OK).setParameter("eventVS", eventVS);
        Long numTotalAccessRequests = (long) query.getSingleResult();
        EventVSMetaInf eventMetaInf = new EventVSMetaInf(eventVS, config.getContextURL(), downloadURL);
        eventMetaInf.setBackupData(numTotalVotes, numTotalAccessRequests);
        JSON.getMapper().writeValue(new FileOutputStream(backupFiles.getMetaInfFile()), eventMetaInf);
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
            File cmsFile = null;
            if(representative != null) {//not anonymous, representative vote
                cmsFile = new File(format("{0}/representativeVote_{1}.p7m", votesBaseDir, representative.getNif()));
            } else {//anonymous, user vote
                cmsFile = new File(format("{0}/vote_{1}.p7m", votesBaseDir, formatted.format(voteVS.getId())));
            }
            IOUtils.write(voteVS.getCMSMessage().getCMS().toPEM(), new FileOutputStream(cmsFile));
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
            File cmsFile = new File(format("{0}/accessRequest_{1}.p7m", accessRequestBaseDir, accessRequest.getUserVS().getNif()));
            IOUtils.write(accessRequest.getMessageCMS().getContent(), new FileOutputStream(cmsFile));
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
        eventVS.setMetaInf(JSON.getMapper().writeValueAsString(eventMetaInf));
        dao.merge(eventVS);
        log.info("ZipResult absolutePath: " + backupFiles.getZipResult().getAbsolutePath());
    }

    public EventVSStatsDto getStats (EventVSElection eventVS) {
        EventVSStatsDto statsDto = new EventVSStatsDto();
        statsDto.setId(eventVS.getId());
        statsDto.setSubject(eventVS.getSubject() + " - 'this is inside simple quotes' - ");
        Query query = dao.getEM().createQuery("select count (a) from AccessRequestVS a where a.eventVS =:eventVS")
                .setParameter("eventVS", eventVS);
        statsDto.setNumAccessRequests((long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(a) from AccessRequestVS a where a.eventVS =:eventVS " +
                "and a.state =:state").setParameter("eventVS", eventVS).setParameter("state", AccessRequestVS.State.OK);
        statsDto.setNumAccessRequestsOK((long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(a) from AccessRequestVS a where a.eventVS =:eventVS " +
                "and a.state =:state").setParameter("eventVS", eventVS).setParameter("state", AccessRequestVS.State.CANCELED);
        statsDto.setNumAccessRequestsCancelled((long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(v) from VoteVS v where v.eventVS =:eventVS").setParameter("eventVS", eventVS);
        statsDto.setNumVotesVS((long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(v) from VoteVS v where v.eventVS =:eventVS and v.state =:state")
                .setParameter("eventVS", eventVS).setParameter("state", VoteVS.State.OK);
        statsDto.setNumVotesVSOK((long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(v) from VoteVS v where v.eventVS =:eventVS and v.state =:state")
                .setParameter("eventVS", eventVS).setParameter("state", VoteVS.State.CANCELED);
        statsDto.setNumVotesVSVotesVSCANCELED((long) query.getSingleResult());
        for(FieldEventVS option : eventVS.getFieldsEventVS()) {
            query = dao.getEM().createQuery("select count(v) from VoteVS v where v.optionSelected =:option " +
                    "and v.state =:state").setParameter("option", option).setParameter("state", VoteVS.State.OK);
            option.setNumVotesVS((long) query.getSingleResult());
        }
        statsDto.setFieldsEventVS(eventVS.getFieldsEventVS());
        return statsDto;
    }

}