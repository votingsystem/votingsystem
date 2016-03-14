package org.votingsystem.web.accesscontrol.ejb;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.voting.*;
import org.votingsystem.model.BackupRequest;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.ImageVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.ws.rs.NotFoundException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class RepresentativeBean {

    private static final Logger log = Logger.getLogger(RepresentativeBean.class.getName());

    enum State {WITHOUT_ACCESS_REQUEST, WITH_ACCESS_REQUEST, WITH_VOTE}

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject MailBean mailBean;
    @Inject CMSBean cmsBean;
    @Inject RepresentativeDelegationBean representativeDelegationBean;

    public RepresentativeDocument saveRepresentative(CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        UserVS signer = cmsMessage.getUserVS();
        AnonymousDelegation anonymousDelegation = representativeDelegationBean.getAnonymousDelegation(signer);
        if(anonymousDelegation != null) throw new ValidationExceptionVS(messages.get(
                "representativeRequestWithActiveAnonymousDelegation"));
        UserVSDto request = cmsMessage.getSignedContent(UserVSDto.class);
        String msg = null;
        signer.setDescription(request.getDescription());
        if(UserVS.Type.REPRESENTATIVE != signer.getType()) {
            representativeDelegationBean.cancelRepresentationDocument(cmsMessage);
            msg = messages.get("representativeDataCreatedOKMsg", signer.getFirstName(), signer.getLastName());
        } else {
            msg = messages.get("representativeDataUpdatedMsg", signer.getFirstName(), signer.getLastName());
        }
        dao.merge(signer.setType(UserVS.Type.REPRESENTATIVE).setRepresentative(null));
        Query query = dao.getEM().createQuery("select i from ImageVS i where i.userVS =:userVS and i.type =:type")
                .setParameter("userVS", signer).setParameter("type", ImageVS.Type.REPRESENTATIVE);
        List<ImageVS> images = query.getResultList();
        for(ImageVS imageVS : images) {
            dao.merge(imageVS.setType(ImageVS.Type.REPRESENTATIVE_CANCELED));
        }
        byte[] imageBytes = Base64.getDecoder().decode(request.getBase64Image().getBytes());
        dao.persist(new ImageVS(signer, cmsMessage, ImageVS.Type.REPRESENTATIVE, imageBytes));
        query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.userVS =:userVS " +
                "and r.state =:state").setParameter("userVS", signer).setParameter("state", RepresentativeDocument.State.OK);
        RepresentativeDocument representativeDocument = dao.getSingleResult(RepresentativeDocument.class, query);
        if(representativeDocument != null) {
            representativeDocument.setState(RepresentativeDocument.State.RENEWED).setCancellationCMS(cmsMessage);
            dao.merge(representativeDocument);

        }
        cmsMessage.setCMS(cmsBean.addSignature(cmsMessage.getCMS()));
        dao.merge(cmsMessage);
        RepresentativeDocument repDocument = dao.persist(new RepresentativeDocument(signer, cmsMessage,
                request.getDescription()));
        log.info ("saveRepresentative - user id: " + signer.getId());
        return repDocument;
    }


    public CMSMessage processRevoke(CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        CMSSignedMessage cmsSignedMessage = cmsMessage.getCMS();
        UserVS signer = cmsMessage.getUserVS();
        UserVS representative = null;
        Query query = null;
        UserVSDto request = cmsMessage.getSignedContent(UserVSDto.class);
        if(TypeVS.REPRESENTATIVE_REVOKE != request.getOperation()) throw new ValidationExceptionVS(
                "ERROR - operation missmatch - expected: 'TypeVS.REPRESENTATIVE_REVOKE' - found:" + request.getOperation());
        String representativeNIF = NifUtils.validate(request.getNIF());
        if(!cmsBean.isAdmin(signer.getNif()) && !signer.getNif().equals(representativeNIF)) {
            throw new ValidationExceptionVS("user without privileges");
        }
        if(signer.getNif().equals(signer.getNif())) representative = signer;
        else {
            query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", representativeNIF);
            representative = dao.getSingleResult(UserVS.class, query);
        }
        query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.userVS =:userVS and " +
                "r.state =:state").setParameter("userVS", representative).setParameter("state", RepresentativeDocument.State.OK);
        RepresentativeDocument representativeDocument = dao.getSingleResult(RepresentativeDocument.class, query);
        if(representativeDocument == null) throw new ValidationExceptionVS(
                messages.get("unsubscribeRepresentativeUserErrorMsg", representative.getNif()));
        log.info("processRevoke - user: " + representative.getId());
        query = dao.getEM().createQuery("select u from UserVS u where u.representative =:userVS")
                .setParameter("userVS", representative);
        List<UserVS> representedList = query.getResultList();
        for(UserVS represented : representedList) {
            query = dao.getEM().createQuery("select r from RepresentationDocument r where r.userVS =:represented " +
                    "and r.representative =:representative and r.state =:state").setParameter("represented", represented)
                    .setParameter("representative", representative).setParameter("state", RepresentationDocument.State.OK);
            RepresentationDocument representationDocument = dao.getSingleResult(RepresentationDocument.class, query);
            representationDocument.setState(RepresentationDocument.State.CANCELED_BY_REPRESENTATIVE).setCancellationCMS(
                    cmsMessage).setDateCanceled(represented.getTimeStampToken().getTimeStampInfo().getGenTime());
            dao.merge(representationDocument);
            dao.merge(represented.setRepresentative(null));
        }
        dao.merge(representative.setType(UserVS.Type.USER));
        CMSSignedMessage cmsMessageResp = cmsBean.addSignature(cmsSignedMessage);
        dao.merge(cmsMessage.setCMS(cmsMessageResp));
        dao.merge(representativeDocument.setState(RepresentativeDocument.State.CANCELED)
                .setCancellationCMS(cmsMessage).setDateCanceled(new Date()));
        return cmsMessage;
    }

    public RepresentationStateDto checkRepresentationState(String nifToCheck) throws ExceptionVS {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        nifToCheck = NifUtils.validate(nifToCheck);
        Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif").setParameter("nif", nifToCheck);
        UserVS userVS  = dao.getSingleResult(UserVS.class, query);
        if(userVS == null) throw new ValidationExceptionVS(messages.get("userVSNotFoundByNIF", nifToCheck));
        RepresentationStateDto result = new RepresentationStateDto();
        result.setLastCheckedDate(new Date());
        if(UserVS.Type.REPRESENTATIVE == userVS.getType()) {
            result.setState(RepresentationState.REPRESENTATIVE);
            result.setRepresentative(org.votingsystem.dto.UserVSDto.BASIC(userVS));
            query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.userVS =:userVS and " +
                    "r.state =:state").setParameter("userVS", userVS).setParameter("state", RepresentativeDocument.State.OK);
            RepresentativeDocument representativeDocument = dao.getSingleResult(RepresentativeDocument.class, query);
            result.setBase64ContentDigest(representativeDocument.getActivationCMS().getBase64ContentDigest());
            return result;
        }
        AnonymousDelegation anonymousDelegation = representativeDelegationBean.getAnonymousDelegation(userVS);
        if(anonymousDelegation != null) {
            result.setState(RepresentationState.WITH_ANONYMOUS_REPRESENTATION);
            result.setBase64ContentDigest(anonymousDelegation.getDelegationCMS().getBase64ContentDigest());
            result.setDateFrom(anonymousDelegation.getDateFrom());
            result.setDateTo(anonymousDelegation.getDateTo());
            return result;
        }
        result.setState(RepresentationState.WITHOUT_REPRESENTATION);
        return result;
    }

    public RepresentativesAccreditations getAccreditationsBackupForEvent (EventElection eventVS)
            throws ExceptionVS, IOException {
		/*if(event.isActive(Calendar.getInstance().getTime())) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:messageSource.getMessage('eventActiveErrorMsg',
                    [event.id].toArray(), locale))
		}*/
        BakupFiles bakupFiles = new BakupFiles(eventVS, TypeVS.REPRESENTATIVE_DATA, config.getServerDir().getAbsolutePath());
        File zipResult   = bakupFiles.getZipResult();
        File filesDir    = bakupFiles.getFilesDir();
        File metaInfFile = bakupFiles.getMetaInfFile();
        log.info("event: " + eventVS.getId() + " - dir: " + filesDir.getAbsolutePath());
        String backupFileName = format("{0}_EventVS_{1}.zip", TypeVS.REPRESENTATIVE_DATA, eventVS.getId());
        if(zipResult.exists()) {
            log.info("existing backup file:" + backupFileName);
            RepresentativesAccreditations repAccreditations = JSON.getMapper().readValue(metaInfFile,
                    new TypeReference<RepresentativesAccreditations>() {});
            return repAccreditations;
        }
        Map<Long, ElectionOptionDto> optionsMap = new HashMap<>();
        Query query = null;
        for(FieldEventVS option : eventVS.getFieldsEventVS()) {
            query = dao.getEM().createQuery("select count(v) from Vote v where v.optionSelected =:option " +
                    "and v.state =:state").setParameter("option", option).setParameter("state", Vote.State.OK);
            Long numVoteRequests = (long) query.getSingleResult();
            query = dao.getEM().createQuery("select count(v) from Vote v where v.optionSelected =:option " +
                    "and v.state =:state and v.certificateVS.userVS is null").setParameter("option", option)
                    .setParameter("state", Vote.State.OK);
            Long numUsersWithVote = (long) query.getSingleResult();
            Long numRepresentativesWithVote = numVoteRequests - numUsersWithVote;
            ElectionOptionDto electionOptionDto = new ElectionOptionDto(option.getContent(), numVoteRequests, numUsersWithVote,
                    numRepresentativesWithVote, null);
            optionsMap.put(option.getId(), electionOptionDto);
        }
        log.info("this is for TEST - CHANGE dateCreated to dateBegin !!!");
        query = dao.getEM().createQuery("select count(r) from RepresentativeDocument r where r.dateCreated <:dateBegin " +
                "and r.state in :states and (r.dateCanceled is null or r.dateCanceled >:dateFinish)")
                .setParameter("dateBegin", eventVS.getDateBegin()).setParameter("dateFinish", eventVS.getDateFinish())
                .setParameter("states", Arrays.asList(RepresentativeDocument.State.OK, RepresentativeDocument.State.RENEWED));
        Long numRepresentatives = (long) query.getSingleResult();
        Long numRepresentativesWithAccessRequest = 0L;
        Long numRepresentativesWithVote = 0L;
        Long numTotalRepresented = 0L;
        Long numTotalRepresentedWithAccessRequest = 0L;
        Long numVotesRepresentedByRepresentatives = 0L;
        long beginCalc = System.currentTimeMillis();

        int offset = 0;
        int pageSize = 100;
        query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.dateCreated <:dateBegin " +
                "and r.state in :states and (r.dateCanceled is null or r.dateCanceled >:dateFinish)")
                .setParameter("dateBegin", eventVS.getDateBegin()).setParameter("dateFinish", eventVS.getDateFinish())
                .setParameter("states", Arrays.asList(RepresentativeDocument.State.OK, RepresentativeDocument.State.RENEWED))
                .setMaxResults(pageSize);
        List<RepresentativeDocument> representativeDocList = null;
        Map<String, RepresentativeVoteDto> representativesMap = new HashMap<>();
        DecimalFormat batchFormat = new DecimalFormat("00000000");
        int batch = 0;
        while ((representativeDocList = query.setFirstResult(offset).getResultList()).size() > 0) {
            for (RepresentativeDocument representativeDoc : representativeDocList) {
                UserVS representative = representativeDoc.getUserVS();
                String representativeBaseDir = format("{0}/representative_{1}/batch_{2}",
                        filesDir.getAbsolutePath(), representative.getNif(), batchFormat.format(++batch));
                new File(representativeBaseDir).mkdirs();
                Long numRepresented = 1L; //The representative itself
                Long numRepresentedWithAccessRequest = 0L;
                Query representationQuery = dao.getEM().createQuery("select r from RepresentationDocument r where " +
                        "r.representative =:representative and r.dateCreated <:dateBegin and r.state =:state and " +
                        "(r.dateCanceled is null or r.dateCanceled >:dateFinish)").setParameter("representative", representative)
                        .setParameter("dateBegin", eventVS.getDateBegin()).setParameter("dateFinish", eventVS.getDateFinish())
                        .setParameter("state", RepresentationDocument.State.OK).setMaxResults(pageSize);
                int representationOffset = 0;
                List<RepresentationDocument> representationList = null;
                while ((representationList = representationQuery.setFirstResult(representationOffset)
                        .getResultList()).size() > 0) {
                    for(RepresentationDocument representationDoc : representationList) {
                        UserVS represented = representationDoc.getUserVS();
                        ++numRepresented;
                        Query representationDocQuery = dao.getEM().createQuery("select a from AccessRequest a where " +
                                "a.state =:state and a.userVS =:userVS and a.eventVS =:eventVS").setParameter("state", AccessRequest.State.OK)
                                .setParameter("userVS", represented).setParameter("eventVS", eventVS);
                        AccessRequest representedAccessRequest = dao.getSingleResult(AccessRequest.class, query);
                        String repDocFileName = null;
                        if(representedAccessRequest != null) {
                            numRepresentedWithAccessRequest++;
                            repDocFileName = format("{0}/{1}_delegation_with_vote.p7s", representativeBaseDir, represented.getNif());
                        } else repDocFileName = format("{0}/{1}_delegation.p7s", representativeBaseDir, represented.getNif());
                        File representationDocFile = new File(repDocFileName);
                        IOUtils.write(representationDoc.getActivationCMS().getContentPEM(), new FileOutputStream(representationDocFile));
                        if((numRepresented  % 100) == 0) {
                            dao.getEM().flush();
                            dao.getEM().clear();
                            log.info(format("Representative {0} - processed {1} representations", representative.getNif(), numRepresented));
                        }
                        if(numRepresented % 2000 == 0) {
                            representativeBaseDir = format("{0}/representative_{1}/batch_{2}",
                                    filesDir.getAbsolutePath(), representative.getNif(), batchFormat.format(++batch));
                            new File(representativeBaseDir).mkdirs();
                        }
                    }
                    representationOffset += pageSize;
                    representationQuery.setFirstResult(representationOffset);
                    String elapsedTime = DateUtils.getElapsedTimeHoursMinutesMillis(System.currentTimeMillis() - beginCalc);
                    log.info("processed " + representationOffset + " representatives - elapsedTime: " + elapsedTime);
                }
                numTotalRepresented += numRepresented;
                numTotalRepresentedWithAccessRequest += numRepresentedWithAccessRequest;
                State state = State.WITHOUT_ACCESS_REQUEST;
                Query representativeQuery = dao.getEM().createQuery("select a from AccessRequest a where " +
                        "a.eventVS =:eventVS and a.userVS =:userVS and a.state =:state")
                        .setParameter("eventVS", eventVS).setParameter("userVS", representative)
                        .setParameter("state", AccessRequest.State.OK);
                AccessRequest accessRequest = dao.getSingleResult(AccessRequest.class, representativeQuery);
                Vote representativeVote = null;
                if(accessRequest != null) {//Representative has access request
                    numRepresentativesWithAccessRequest++;
                    state = State.WITH_ACCESS_REQUEST;
                    representativeQuery = dao.getEM().createQuery("select v from Vote v where v.certificateVS.userVS =:userVS and " +
                            "v.eventVS =:eventVS and v.state =:state").setParameter("userVS", representative)
                            .setParameter("eventVS", eventVS).setParameter("state", Vote.State.OK);
                    representativeVote = dao.getSingleResult(Vote.class, representativeQuery);
                }
                Long numVotesRepresentedByRepresentative = 0L;
                if(representativeVote != null) {
                    state = State.WITH_VOTE;
                    ++numRepresentativesWithVote;
                    numVotesRepresentedByRepresentative = numRepresented  - numRepresentedWithAccessRequest;
                    numVotesRepresentedByRepresentatives += numVotesRepresentedByRepresentative;
                    optionsMap.get(representativeVote.getOptionSelected().getId()).addNumVotesResult(
                            numVotesRepresentedByRepresentative - 1);
                }
                RepresentativeVoteDto representativeVoteDto = new RepresentativeVoteDto(representative.getId(),
                        representativeVote.getOptionSelected().getId(), numRepresented, numRepresentedWithAccessRequest,
                        numVotesRepresentedByRepresentative);
                representativesMap.put(representative.getNif(), representativeVoteDto);

                String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillis(System.currentTimeMillis() - beginCalc);
                /*String csvLine = "${representative.nif}, numRepresented:${formatted.format(numRepresented)}, " +
                    "numRepresentedWithAccessRequest:${formatted.format(numRepresentedWithAccessRequest)}, ${state.toString()}\n"
                reportFile.append(csvLine)*/
            }
            dao.getEM().flush();
            dao.getEM().clear();
            offset += pageSize;
            query.setFirstResult(offset);
            String elapsedTime = DateUtils.getElapsedTimeHoursMinutesMillis(System.currentTimeMillis() - beginCalc);
            log.info("processed " + offset + " of " + numRepresentatives + " representatives - elapsedTime: " + elapsedTime);
        }
        RepresentativesAccreditations representativesAccreditations = new RepresentativesAccreditations(numRepresentatives,
                numRepresentativesWithAccessRequest, numRepresentativesWithVote, numTotalRepresentedWithAccessRequest,
                numTotalRepresented, numVotesRepresentedByRepresentatives, optionsMap, representativesMap);
        JSON.getMapper().writeValue(metaInfFile, representativesAccreditations);
        return representativesAccreditations;
    }


    private synchronized RepresentativeAccreditationsMetaInf getAccreditationsBackup (
            UserVS representative, Date selectedDate) throws IOException {
        log.info(format("representative: {0} - selectedDate: {1}", representative.getNif(), selectedDate));
        int pageSize = 100;
        int offset = 0;
        Query query = dao.getEM().createQuery("select r from RepresentationDocument r where r.representative =:representative " +
                "and r.dateCreated <:selectedDate and r.state in :inList and (r.dateCanceled is null or r.dateCanceled >:selectedDate)")
                .setParameter("representative", representative).setParameter("selectedDate", selectedDate)
                .setParameter("inList", Arrays.asList(RepresentationDocument.State.CANCELED, RepresentationDocument.State.OK))
                .setMaxResults(pageSize);

        String selectedDatePath = DateUtils.getDateStr(selectedDate, "yyyy/MM/dd");
        String accreditationsPath =  format("/backup/AccreditationsBackup/{0}/representative{1}", selectedDatePath,
                representative.getNif());
        String downloadURL = config.getStaticResURL() + accreditationsPath + ".zip";
        String representativeURL = format("{0}/rest/representative/id/{1}", config.getContextURL(), representative.getId());
        String basedir = config.getServerDir().getAbsolutePath() + accreditationsPath;
        new File(basedir).mkdirs();
        File zipResult = new File(basedir + ".zip");
        File metaInfFile;
        if(zipResult.exists()) {
            metaInfFile = new File(basedir + "/meta.inf");
            if(metaInfFile.exists()) {
                RepresentativeAccreditationsMetaInf metaInf = JSON.getMapper().readValue(
                        metaInfFile, new TypeReference<RepresentativeAccreditationsMetaInf>() {});
                return metaInf;
            }
        }
        Long numAccreditations = 0L;
        List<RepresentationDocument> representationDocuments = null;
        while ((representationDocuments = query.setFirstResult(offset).getResultList()).size() > 0) {
            for(RepresentationDocument representationDocument : representationDocuments) {
                ++numAccreditations;
                CMSMessage cmsMessage = representationDocument.getActivationCMS();
                File cmsFile = new File(format("{0}/accreditation_{1}", basedir, representationDocument.getId()));
                IOUtils.write(cmsMessage.getContentPEM(), new FileOutputStream(cmsFile));
                if((numAccreditations % 100) == 0) {
                    dao.getEM().flush();
                    dao.getEM().clear();
                    log.info("getAccreditationsBackup - processed representations: " + numAccreditations);
                }
            }
            offset = offset + pageSize;
        }
        RepresentativeAccreditationsMetaInf metaInf = new RepresentativeAccreditationsMetaInf(numAccreditations,
                selectedDate, representativeURL, accreditationsPath + ".zip", downloadURL);
        metaInfFile = new File(basedir + "/meta.inf");
        JSON.getMapper().writeValue(new FileOutputStream(metaInfFile), metaInf);
        new ZipUtils(basedir).zipIt(zipResult);
        log.info("getAccreditationsBackup - zipResult: " + zipResult.getAbsolutePath());
        return metaInf;
    }

    @Asynchronous
    public void processVotingHistoryRequest(CMSMessage cmsMessage, String messageTemplate) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        try {
            CMSSignedMessage cmsSignedMessage = cmsMessage.getCMS();
            UserVS userVS = cmsMessage.getUserVS();
            RepresentativeVotingHistoryDto request = cmsMessage.getSignedContent(RepresentativeVotingHistoryDto.class);
            request.validate();
            Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif and u.type =:type")
                    .setParameter("nif", request.getRepresentativeNif()).setParameter("type", UserVS.Type.REPRESENTATIVE);
            UserVS representative = dao.getSingleResult(UserVS.class, query);
            if(representative == null) throw new ValidationExceptionVS("ERROR - user is not representative - nif: " +
                    request.getRepresentativeNif());
            RepresentativeVotingHistoryMetaInf metaInf =
                    getVotingHistoryBackup(representative, request.getDateFrom(), request.getDateTo());
            BackupRequest backupRequest = dao.persist(new BackupRequest(metaInf.getDownloadURL(),
                    TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST,
                    representative, cmsMessage, request.getEmail()));
            String downloadURL = config.getContextURL() + "/rest/backup/request/id/" + backupRequest.getId() + "/download";
            String requestURL = config.getContextURL() + "/rest/backup/request/id/" + backupRequest.getId();
            String subject = messages.get("representativeAccreditationsMailSubject", backupRequest.getRepresentative().getName());
            String content = MessageFormat.format(messageTemplate, userVS.getName(), requestURL, representative.getName(),
                    DateUtils.getDayWeekDateStr(request.getDateFrom(), "HH:mm"), DateUtils.getDayWeekDateStr(request.getDateTo(), "HH:mm"),
                    downloadURL);
            mailBean.send(request.getEmail(), subject, content);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Asynchronous
    public void processAccreditationsRequest(CMSMessage cmsMessage, String messageTemplate) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        try {
            UserVS userVS = cmsMessage.getUserVS();
            RepresentativeAccreditationsDto request = cmsMessage.getSignedContent(RepresentativeAccreditationsDto.class);
            request.validate();
            Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif and u.type =:type")
                    .setParameter("nif", request.getRepresentativeNif()).setParameter("type", UserVS.Type.REPRESENTATIVE);
            UserVS representative = dao.getSingleResult(UserVS.class, query);
            if(representative == null) throw new ValidationExceptionVS("ERROR - representativeNifErrorMsg - nif: " +
                    request.getRepresentativeNif());
            RepresentativeAccreditationsMetaInf metaInf = getAccreditationsBackup(representative, request.getSelectedDate());
            BackupRequest backupRequest = dao.persist(new BackupRequest(metaInf.getFilePath(),
                    TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST, representative, cmsMessage, request.getEmail()));
            String downloadURL = config.getContextURL() + "/rest/backup/request/id/" + backupRequest.getId() + "/download";
            String requestURL = config.getContextURL() + "/rest/backup/request/id/" + backupRequest.getId();
            String subject = messages.get("representativeAccreditationsMailSubject", backupRequest.getRepresentative().getName());
            String content = MessageFormat.format(messageTemplate, userVS.getName(), requestURL, representative.getName(),
                    DateUtils.getDayWeekDateStr(request.getSelectedDate(), "HH:mm"), downloadURL);
            mailBean.send(request.getEmail(), subject, content);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private RepresentativeVotingHistoryMetaInf getVotingHistoryBackup (
            UserVS representative, Date dateFrom, Date dateTo) throws IOException {
        log.info(format("getVotingHistoryBackup - representative: {0} - dateFrom: {1} - dateTo: {2}", representative.getNif(),
                dateFrom, dateTo));
        String dateFromPath = DateUtils.getDateStr(dateFrom, "yyyy/MM/dd");
        String dateToPath = DateUtils.getDateStr(dateTo,"yyyy/MM/dd");
        String votingHistoryPath = format("/backup/RepresentativeHistoryVoting/{0}_{1}/representative_{2}",
                dateFromPath, dateToPath, representative.getNif());
        String basedir = config.getServerDir().getAbsolutePath() + votingHistoryPath;
        new File(basedir).mkdirs();
        log.info("getVotingHistoryBackup - basedir: " + basedir);
        File zipResult = new File(basedir + ".zip");
        String downloadURL = config.getStaticResURL() + votingHistoryPath + ".zip";
        File metaInfFile;
        if(zipResult.exists()) {
            metaInfFile = new File(basedir + "/meta.inf");
            if(metaInfFile.exists()) {
                RepresentativeVotingHistoryMetaInf metaInf = JSON.getMapper().readValue(metaInfFile,
                        new TypeReference<RepresentativeVotingHistoryMetaInf>() {});
                return metaInf;
            }
        }
        Query query = dao.getEM().createQuery("select v from Vote v where v.certificateVS.userVS =:userVS " +
                "and v.state =:state  and v.dateCreated between :dateFrom and :dateTo").setParameter("userVS", representative)
                .setParameter("state", Vote.State.OK).setParameter("dateFrom", dateFrom).setParameter("dateTo", dateTo);
        List<Vote> representativeVotes = query.getResultList();
        long numVotes = representativeVotes.size();
        for (Vote vote : representativeVotes) {
            String voteId = String.format("%08d", vote.getId());
            File cmsFile = new File(format("{0}/vote_{1}.p7s", basedir, voteId));
            IOUtils.write(vote.getCMSMessage().getContentPEM(), new FileOutputStream(cmsFile));
        }
        log.info(format("representative: {0} - numVotes: {1}", representative.getNif(), numVotes));
        String representativeURL = format("{0}/rest/representative/id/{1}", config.getContextURL(), representative.getId());
        RepresentativeVotingHistoryMetaInf metaInf = new RepresentativeVotingHistoryMetaInf(numVotes, dateFrom,
                dateTo, representativeURL, votingHistoryPath + ".zip" , downloadURL);
        metaInfFile = new File(basedir + "/meta.inf");
        JSON.getMapper().writeValue(new FileOutputStream(metaInfFile), metaInf);
        new ZipUtils(basedir).zipIt(zipResult);
        return metaInf;
    }

    public UserVSDto getRepresentativeDto(UserVS representative) {
        Query query = dao.getEM().createQuery("select count(d) from RepresentationDocument d where " +
                "d.representative =:representative and d.state =:state").setParameter("representative", representative)
                .setParameter("state", RepresentationDocument.State.OK);
        long numRepresentations = (long) query.getSingleResult();
        query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.userVS =:representative " +
                "and r.state =:state").setParameter("representative", representative)
                .setParameter("state", RepresentativeDocument.State.OK);
        RepresentativeDocument representativeDocument = dao.getSingleResult(RepresentativeDocument.class, query);
        if (representativeDocument == null) throw new NotFoundException(
                "ERROR - RepresentativeDocument not found - representativeId: " + representative.getId());
        return UserVSDto.REPRESENTATIVE(representative,
                representativeDocument.getActivationCMS().getId(), numRepresentations, config.getContextURL());
    }
}