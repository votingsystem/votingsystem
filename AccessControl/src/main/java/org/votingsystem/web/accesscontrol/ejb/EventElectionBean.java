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
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
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
public class EventElectionBean {

    private static final Logger log = Logger.getLogger(EventElectionBean.class.getName());

    @Inject EventVSBean eventVSBean;
    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject CMSBean cmsBean;
    @Inject RepresentativeBean representativeBean;
    @Inject TimeStampBean timeStampBean;

    public EventElection saveEvent(CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        User userSigner = cmsMessage.getUser();
        EventVSDto request  = cmsMessage.getSignedContent(EventVSDto.class);
        request.setDateFinish(DateUtils.resetDay(DateUtils.addDays(request.getDateBegin(), 1).getTime()).getTime());
        ControlCenter controlCenter = config.getControlCenter();
        Query query = dao.getEM().createQuery("select a from Actor a where a.serverURL =:serverURL")
                .setParameter("serverURL", config.getContextURL());
        AccessControl accessControl = dao.getSingleResult(AccessControl.class, query);
        EventElection eventVS = request.getEventElection();
        eventVS.setUser(userSigner);
        eventVS.setControlCenter(controlCenter);
        eventVS.setAccessControl(accessControl);
        eventVSBean.setEventDatesState(eventVS);
        if(EventVS.State.TERMINATED ==  eventVS.getState()) throw new ValidationExceptionVS(
                "ERROR - eventFinishedErrorMsg dateFinish: " + request.getDateFinish());
        request.setControlCenterURL(controlCenter.getServerURL());
        dao.persist(eventVS);
        eventVS.setAccessControlEventId(eventVS.getId());
        request.setFieldsEventVS(eventVS.getFieldsEventVS());
        request.setId(eventVS.getId());
        request.setAccessControlEventId(eventVS.getId());
        request.setAccessControlURL(config.getContextURL());
        request.setURL(config.getContextURL() + "/rest/eventElection/id/" + eventVS.getId());
        request.setDateCreated(eventVS.getDateCreated());
        request.setType(EventVS.Type.ELECTION);

        KeyStoreDto keyStoreDto = cmsBean.generateElectionKeysStore(eventVS);
        CertificateVS certificateVS = dao.persist(CertificateVS.ELECTION((X509Certificate) keyStoreDto.getX509Cert()));
        dao.merge(eventVS.setCertificateVS(certificateVS));
        keyStoreDto.getKeyStoreVS().setCertificateVS(certificateVS);
        KeyStoreVS keyStoreVS = dao.persist(keyStoreDto.getKeyStoreVS());
        eventVS.setKeyStoreVS(keyStoreVS);

        request.setCertCAVotacion( new String(PEMUtils.getPEMEncoded (keyStoreVS.getCertificateVS().getX509Cert())));
        request.setCertChain(new String(PEMUtils.getPEMEncoded (cmsBean.getCertChain())));
        X509Certificate certUsuX509 = userSigner.getCertificate();
        request.setUser(new String(PEMUtils.getPEMEncoded(certUsuX509)));
        CMSSignedMessage cms = cmsBean.signDataWithTimeStamp(JSON.getMapper().writeValueAsBytes(request));
        ResponseVS responseVS = HttpHelper.getInstance().sendData(cms.toPEM(),
                ContentTypeVS.JSON_SIGNED, controlCenter.getServerURL() + "/rest/eventElection");
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
            throw new ExceptionVS(messages.get("controlCenterCommunicationErrorMsg", controlCenter.getServerURL()));
        }
        query = dao.getEM().createQuery("select c from CertificateVS c where c.type =:type " +
                "and c.actor =:actor and c.state =:state").setParameter("type", CertificateVS.Type.ACTOR_VS)
                .setParameter("actor", controlCenter).setParameter("state", CertificateVS.State.OK);
        CertificateVS controlCenterCert = dao.getSingleResult(CertificateVS.class, query);
        CertificateVS controlCenterCertEventVS = dao.persist(
                CertificateVS.ACTOR(controlCenter, controlCenterCert.getX509Cert()));
        CertificateVS accessControlCertEventVS = dao.persist(
                CertificateVS.ACTOR(null, cmsBean.getServerCert()));
        dao.merge(cmsMessage.setType(TypeVS.VOTING_EVENT).setCMS(cms));
        dao.merge(eventVS.setControlCenterCert(controlCenterCertEventVS).setAccessControlCert(accessControlCertEventVS)
                .setState(EventVS.State.ACTIVE).setCmsMessage(cmsMessage));
        return eventVS;
    }

    public void generateBackups () throws Exception {
        Date checkDate = DateUtils.addDays(new Date(), -1).getTime();
        Query query = dao.getEM().createQuery("select e from EventElection e where e.dateFinish <:checkDate " +
                "and e.backupAvailable =:backupAvailable").setParameter("checkDate", checkDate)
                .setParameter("backupAvailable", Boolean.FALSE);
        List<EventElection> terminatedPolls = query.getResultList();
        for(EventElection eventVS : terminatedPolls) {
            generateBackup(eventVS);
        }
    }

    @Asynchronous
    public void generateBackup (EventElection eventVS) throws Exception {
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
        Set<X509Certificate> systemTrustedCerts = cmsBean.getTrustedCerts();
        File systemTrustedCertsFile = new File(format("{0}/systemTrustedCerts.pem", filesDir.getAbsolutePath()));
        IOUtils.write(PEMUtils.getPEMEncoded(systemTrustedCerts), new FileOutputStream(systemTrustedCertsFile));

        File eventTrustedCertsFile = new File(format("{0}/eventTrustedCerts.pem", filesDir.getAbsolutePath()));
        IOUtils.write(PEMUtils.getPEMEncoded(eventTrustedCerts), new FileOutputStream(eventTrustedCertsFile));

        File timeStampCertFile = new File(format("{0}/timeStampCert.pem", filesDir.getAbsolutePath()));
        IOUtils.write(PEMUtils.getPEMEncoded(timeStampBean.getSigningCertPEMBytes()), new FileOutputStream(timeStampCertFile));

        Query query = dao.getEM().createQuery("select count(v) from Vote v where v.state =:state and v.eventVS=:eventVS")
                .setParameter("state", Vote.State.OK).setParameter("eventVS", eventVS);
        Long numTotalVotes = (long) query.getSingleResult();
        query = dao.getEM().createQuery("select count(a) from AccessRequest a where a.state =:state " +
                "and a.eventVS =:eventVS").setParameter("state", AccessRequest.State.OK).setParameter("eventVS", eventVS);
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
        query = dao.getEM().createQuery("select v from Vote v where v.state =:state and v.eventVS =:eventVS")
                .setParameter("state", Vote.State.OK).setParameter("eventVS", eventVS);
        List<Vote> votes = query.getResultList();
        for (Vote vote : votes) {
            User representative = vote.getCertificateVS().getUser();
            File cmsFile = null;
            if(representative != null) {//not anonymous, representative vote
                cmsFile = new File(format("{0}/representativeVote_{1}.p7s", votesBaseDir, representative.getNif()));
            } else {//anonymous, user vote
                cmsFile = new File(format("{0}/vote_{1}.p7s", votesBaseDir, formatted.format(vote.getId())));
            }
            IOUtils.write(vote.getCMSMessage().getCMS().toPEM(), new FileOutputStream(cmsFile));
            /*if(((votes.getRowNumber() + 1) % 2000) == 0) {
                votesBaseDir="${filesDir.absolutePath}/votes/batch_${formatted.format(++votesBatch)}"
                new File(votesBaseDir).mkdirs()
            }*/
        }
        begin = System.currentTimeMillis();
        query = dao.getEM().createQuery("select a from AccessRequest a where a.state =:state and a.eventVS =:eventVS")
                .setParameter("state", AccessRequest.State.OK).setParameter("eventVS", eventVS);
        List<AccessRequest> accessRequestList = query.getResultList();
        for(AccessRequest accessRequest : accessRequestList) {
            File cmsFile = new File(format("{0}/accessRequest_{1}.p7s", accessRequestBaseDir, accessRequest.getUser().getNif()));
            IOUtils.write(accessRequest.getCmsMessage().getContentPEM(), new FileOutputStream(cmsFile));
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

    public EventVSStatsDto getStats (EventElection eventVS) {
        EventVSStatsDto statsDto = new EventVSStatsDto();
        statsDto.setId(eventVS.getId());
        statsDto.setSubject(eventVS.getSubject() + " - 'this is inside simple quotes' - ");
        Query query = dao.getEM().createQuery("select count (a) from AccessRequest a where a.eventVS =:eventVS")
                .setParameter("eventVS", eventVS);
        statsDto.setNumAccessRequests((long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(a) from AccessRequest a where a.eventVS =:eventVS " +
                "and a.state =:state").setParameter("eventVS", eventVS).setParameter("state", AccessRequest.State.OK);
        statsDto.setNumAccessRequestsOK((long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(a) from AccessRequest a where a.eventVS =:eventVS " +
                "and a.state =:state").setParameter("eventVS", eventVS).setParameter("state", AccessRequest.State.CANCELED);
        statsDto.setNumAccessRequestsCancelled((long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(v) from Vote v where v.eventVS =:eventVS").setParameter("eventVS", eventVS);
        statsDto.setNumVotesVS((long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(v) from Vote v where v.eventVS =:eventVS and v.state =:state")
                .setParameter("eventVS", eventVS).setParameter("state", Vote.State.OK);
        statsDto.setNumVotesVSOK((long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(v) from Vote v where v.eventVS =:eventVS and v.state =:state")
                .setParameter("eventVS", eventVS).setParameter("state", Vote.State.CANCELED);
        statsDto.setNumVotesVSVotesVSCANCELED((long) query.getSingleResult());
        for(FieldEvent option : eventVS.getFieldsEventVS()) {
            query = dao.getEM().createQuery("select count(v) from Vote v where v.optionSelected =:option " +
                    "and v.state =:state").setParameter("option", option).setParameter("state", Vote.State.OK);
            option.setNumVotesVS((long) query.getSingleResult());
        }
        statsDto.setFieldsEventVS(eventVS.getFieldsEventVS());
        return statsDto;
    }

}