package org.votingsystem.web.accesscontrol.ejb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.votingsystem.dto.*;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.ws.rs.NotFoundException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class RepresentativeBean {

    private static final Logger log = Logger.getLogger(RepresentativeBean.class.getSimpleName());

    enum State {WITHOUT_ACCESS_REQUEST, WITH_ACCESS_REQUEST, WITH_VOTE}

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject MailBean mailBean;
    @Inject RepresentativeDelegationBean representativeDelegationBean;
    @Inject MessagesBean messages;

    public ResponseVS<RepresentativeDocument> saveRepresentativeData(MessageSMIME messageSMIME, byte[] imageBytes) throws Exception {
        UserVS signer = messageSMIME.getUserVS();
        AnonymousDelegation anonymousDelegation = representativeDelegationBean.getAnonymousDelegation(signer);
        if(anonymousDelegation != null) throw new ValidationExceptionVS(messages.get(
                "representativeRequestWithActiveAnonymousDelegation"));
        Map requestMap = messageSMIME.getSignedContentMap();
        String base64ImageHash = (String) requestMap.get("base64ImageHash");
        MessageDigest messageDigest = MessageDigest.getInstance(ContextVS.VOTING_DATA_DIGEST);
        byte[] resultDigest =  messageDigest.digest(imageBytes);
        String base64ResultDigest = Base64.getEncoder().encodeToString(resultDigest);
        if(!base64ResultDigest.equals(base64ImageHash)) throw new ValidationExceptionVS(messages.get("imageHashErrorMsg"));
        //String base64EncodedImage = requestMap.base64RepresentativeEncodedImage
        //BASE64Decoder decoder = new BASE64Decoder();
        //byte[] imageFileBytes = decoder.decodeBuffer(base64EncodedImage);
        String msg = null;
        if(UserVS.Type.REPRESENTATIVE != signer.getType()) {
            dao.merge(signer.setType(UserVS.Type.REPRESENTATIVE).setRepresentative(null));
            representativeDelegationBean.cancelRepresentationDocument(messageSMIME);
            msg = messages.get("representativeDataCreatedOKMsg", signer.getFirstName(), signer.getLastName());
        } else {
            msg = messages.get("representativeDataUpdatedMsg", signer.getFirstName(), signer.getLastName());
        }
        Query query = dao.getEM().createQuery("select i from ImageVS i where i.userVS =:userVS and i.type =:type")
                .setParameter("userVS", signer).setParameter("type", ImageVS.Type.REPRESENTATIVE);
        List<ImageVS> images = query.getResultList();
        for(ImageVS imageVS : images) {
            dao.merge(imageVS.setType(ImageVS.Type.REPRESENTATIVE_CANCELED));
        }
        ImageVS newImage = dao.persist(new ImageVS(signer,messageSMIME, ImageVS.Type.REPRESENTATIVE, imageBytes));
        query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.userVS =:userVS " +
                "and r.state =:state").setParameter("userVS", signer).setParameter("state", RepresentativeDocument.State.OK);
        RepresentativeDocument representativeDocument = dao.getSingleResult(RepresentativeDocument.class, query);
        if(representativeDocument != null) {
            representativeDocument.setState(RepresentativeDocument.State.RENEWED).setCancellationSMIME(messageSMIME);
            dao.merge(representativeDocument);

        }
        RepresentativeDocument repDocument = dao.persist(new RepresentativeDocument(signer, messageSMIME,
                (String) requestMap.get("representativeInfo")));
        log.info ("saveRepresentativeData - user id: " + signer.getId());
        //return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg,  type:TypeVS.REPRESENTATIVE_DATA)
        return new ResponseVS<RepresentativeDocument>(ResponseVS.SC_OK, msg, repDocument);
    }

    public Map checkRepresentationState(String nifToCheck) throws ExceptionVS {
        nifToCheck = NifUtils.validate(nifToCheck);
        Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif").setParameter("nif", nifToCheck);
        UserVS userVS  = dao.getSingleResult(UserVS.class, query);
        if(userVS == null) throw new ValidationExceptionVS(messages.get("userVSNotFoundByNIF", nifToCheck));
        Map result = new HashMap<>();
        result.put("lastCheckedDate", new Date());
        if(userVS.getRepresentative() != null) {
            query = dao.getEM().createQuery("select r from RepresentationDocument r where r.userVS =:userVS and " +
                    "r.state =:state").setParameter("userVS", userVS).setParameter("state", RepresentationDocument.State.OK);
            RepresentationDocument representationDocument = dao.getSingleResult(RepresentationDocument.class, query);
            result.put("state", RepresentationState.WITH_PUBLIC_REPRESENTATION);
            result.put("base64ContentDigest", representationDocument.getActivationSMIME().getBase64ContentDigest());
            result.put("representative", representativeDelegationBean.getRepresentativeJSON(userVS.getRepresentative()));
            return result;
        }
        if(UserVS.Type.REPRESENTATIVE == userVS.getType()) {
            result.put("state", RepresentationState.REPRESENTATIVE);
            result.put("representative", representativeDelegationBean.getRepresentativeJSON(userVS));
            return result;
        }
        AnonymousDelegation anonymousDelegation = representativeDelegationBean.getAnonymousDelegation(userVS);
        if(anonymousDelegation != null) {
            result.put("state", RepresentationState.WITH_ANONYMOUS_REPRESENTATION);
            result.put("base64ContentDigest", anonymousDelegation.getDelegationSMIME().getBase64ContentDigest());
            result.put("dateFrom", anonymousDelegation.getDateFrom());
            result.put("dateTo", anonymousDelegation.getDateTo());
            return result;
        }
        result.put("state", RepresentationState.WITHOUT_REPRESENTATION);
        return result;
    }

    public synchronized RepresentativesAccreditations getAccreditationsBackupForEvent (EventVSElection eventVS)
            throws ExceptionVS, IOException {
		/*if(event.isActive(Calendar.getInstance().getTime())) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:messageSource.getMessage('eventActiveErrorMsg',
                    [event.id].toArray(), locale))
		}*/
        BakupFiles bakupFiles = new BakupFiles(eventVS, TypeVS.REPRESENTATIVE_DATA, config.getProperty("vs.errorsBasePath"),
                config.getProperty("vs.backupBasePath"));
        File zipResult   = bakupFiles.getZipResult();
        File filesDir    = bakupFiles.getFilesDir();
        File metaInfFile = bakupFiles.getMetaInfFile();
        log.info("event: " + eventVS.getId() + " - dir: " + filesDir.getAbsolutePath());
        String backupFileName = format("{0}_EventVS_{1}.zip", TypeVS.REPRESENTATIVE_DATA, eventVS.getId());
        if(zipResult.exists()) {
            log.info("existing backup file:" + backupFileName);
            RepresentativesAccreditations repAccreditations = new ObjectMapper().readValue(metaInfFile,
                    new TypeReference<RepresentativesAccreditations>() {});
            return repAccreditations;
        }
        Map<Long, OptionInfo> optionsMap = new HashMap<>();
        Query query = null;
        for(FieldEventVS option : eventVS.getFieldsEventVS()) {
            query = dao.getEM().createQuery("select count(v) from VoteVS v where v.optionSelected =:option " +
                    "and v.state =:state").setParameter("option", option).setParameter("state", VoteVS.State.OK);
            Long numVoteRequests = (long) query.getSingleResult();
            query = dao.getEM().createQuery("select count(v) from VoteVS v where v.optionSelected =:option " +
                    "and v.state =:state and v.certificateVS.userVS is null").setParameter("option", option)
                    .setParameter("state", VoteVS.State.OK);
            Long numUsersWithVote = (long) query.getSingleResult();
            Long numRepresentativesWithVote = numVoteRequests - numUsersWithVote;
            OptionInfo optionInfo = new OptionInfo(option.getContent(), numVoteRequests, numUsersWithVote,
                    numRepresentativesWithVote, null);
            optionsMap.put(option.getId(), optionInfo);
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
        Map<String, RepresentativeVoteInfo> representativesMap = new HashMap<>();
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
                        Query representationDocQuery = dao.getEM().createQuery("select a from AccessRequestVS a where " +
                                "a.state =:state and a.userVS =:userVS and a.eventVS =:eventVS").setParameter("state", AccessRequestVS.State.OK)
                                .setParameter("userVS", represented).setParameter("eventVS", eventVS);
                        AccessRequestVS representedAccessRequest = dao.getSingleResult(AccessRequestVS.class, query);
                        String repDocFileName = null;
                        if(representedAccessRequest != null) {
                            numRepresentedWithAccessRequest++;
                            repDocFileName = format("{0}/{1}_delegation_with_vote.p7m", representativeBaseDir, represented.getNif());
                        } else repDocFileName = format("{0}/{1}_delegation.p7m", representativeBaseDir, represented.getNif());
                        File representationDocFile = new File(repDocFileName);
                        IOUtils.write(representationDoc.getActivationSMIME().getContent(), new FileOutputStream(representationDocFile));
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
                Query representativeQuery = dao.getEM().createQuery("select a from AccessRequestVS a where " +
                        "a.eventVS =:eventVS and a.userVS =:userVS and a.state =:state")
                        .setParameter("eventVS", eventVS).setParameter("userVS", representative)
                        .setParameter("state", AccessRequestVS.State.OK);
                AccessRequestVS accessRequestVS = dao.getSingleResult(AccessRequestVS.class, representativeQuery);
                VoteVS representativeVote = null;
                if(accessRequestVS != null) {//Representative has access request
                    numRepresentativesWithAccessRequest++;
                    state = State.WITH_ACCESS_REQUEST;
                    representativeQuery = dao.getEM().createQuery("select v from VoteVS v where v.certificateVS.userVS =:userVS and " +
                            "v.eventVS =:eventVS and v.state =:state").setParameter("userVS", representative)
                            .setParameter("eventVS", eventVS).setParameter("state", VoteVS.State.OK);
                    representativeVote = dao.getSingleResult(VoteVS.class, representativeQuery);
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
                RepresentativeVoteInfo representativeVoteInfo = new RepresentativeVoteInfo(representative.getId(),
                        representativeVote.getOptionSelected().getId(), numRepresented, numRepresentedWithAccessRequest,
                        numVotesRepresentedByRepresentative);
                representativesMap.put(representative.getNif(), representativeVoteInfo);

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
        new ObjectMapper().writeValue(metaInfFile, representativesAccreditations);
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

        String selectedDateStr = DateUtils.getDateStr(selectedDate, "yyyy/MM/dd");
        String basedir = format("{0}/AccreditationsBackup/{1}/representative{2}", config.getProperty("vs.backupBasePath"),
                selectedDateStr, representative.getNif());
        new File(basedir).mkdirs();
        File zipResult = new File(basedir + ".zip");
        File metaInfFile;
        if(zipResult.exists()) {
            metaInfFile = new File(basedir + "/meta.inf");
            if(metaInfFile.exists()) {
                RepresentativeAccreditationsMetaInf metaInf = new ObjectMapper().readValue(
                        metaInfFile, new TypeReference<RepresentativeAccreditationsMetaInf>() {});
                return metaInf;
            }
        }
        Long numAccreditations = 0L;
        List<RepresentationDocument> representationDocuments = null;
        while ((representationDocuments = query.setFirstResult(offset).getResultList()).size() > 0) {
            for(RepresentationDocument representationDocument : representationDocuments) {
                ++numAccreditations;
                MessageSMIME messageSMIME = representationDocument.getActivationSMIME();
                File smimeFile = new File(format("{0}/accreditation_{1}", basedir, representationDocument.getId()));
                IOUtils.write(messageSMIME.getContent(), new FileOutputStream(smimeFile));
                if((numAccreditations % 100) == 0) {
                    dao.getEM().flush();
                    dao.getEM().clear();
                    log.info("getAccreditationsBackup - processed representations: " + numAccreditations);
                }
            }
            offset = offset + pageSize;
        }
        RepresentativeAccreditationsMetaInf metaInf = new RepresentativeAccreditationsMetaInf(numAccreditations,
                selectedDate, format("{0}/representative/id/{1}", config.getRestURL(), representative.getId()),
                zipResult.getAbsolutePath());
        metaInfFile = new File(basedir + "/meta.inf");
        new ObjectMapper().writeValue(new FileOutputStream(metaInfFile), metaInf);
        new ZipUtils(basedir).zipIt(zipResult);
        return metaInf;
    }

    @Asynchronous
    public void processVotingHistoryRequest(MessageSMIME messageSMIME, String messageTemplate) throws Exception {
        SMIMEMessage smimeMessage = messageSMIME.getSMIME();
        UserVS userVS = messageSMIME.getUserVS();
        RepresentativeVotingHistoryRequest request = messageSMIME.getSignedContent(RepresentativeVotingHistoryRequest.class);
        request.validate();
        Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif and u.type =:type")
                .setParameter("nif", request.getRepresentativeNif()).setParameter("type", UserVS.Type.REPRESENTATIVE);
        UserVS representative = dao.getSingleResult(UserVS.class, query);
        if(representative == null) throw new ValidationExceptionVS("ERROR - user is not representative - nif: " +
                request.getRepresentativeNif());
        RepresentativeVotingHistoryMetaInf metaInf =
                getVotingHistoryBackup(representative, request.getDateFrom(),  request.getDateTo());

        BackupRequestVS backupRequest = dao.persist(new BackupRequestVS(metaInf.getDownloadURL(),
                TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST,
                representative, messageSMIME, request.getEmail()));
        mailBean.sendRepresentativeVotingHistory(backupRequest, messageTemplate, request.getDateFrom(), request.getDateTo());
    }

    @Asynchronous
    public void processAccreditationsRequest(MessageSMIME messageSMIME, String messageTemplate) throws Exception {
        SMIMEMessage smimeMessage = messageSMIME.getSMIME();
        UserVS userVS = messageSMIME.getUserVS();
        RepresentativeAccreditationsRequest request = messageSMIME.getSignedContent(RepresentativeAccreditationsRequest.class);
        request.validate();
        Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif and u.type =:type")
                .setParameter("nif", request.getRepresentativeNif()).setParameter("type", UserVS.Type.REPRESENTATIVE);
        UserVS representative = dao.getSingleResult(UserVS.class, query);
        if(representative == null) throw new ValidationExceptionVS("ERROR - representativeNifErrorMsg - nif: " +
                request.getRepresentativeNif());
        RepresentativeAccreditationsMetaInf metaInf = getAccreditationsBackup( representative, request.getSelectedDate());
        BackupRequestVS backupRequest = dao.persist(new BackupRequestVS(metaInf.getDownloadURL(),
                TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST, representative, messageSMIME, request.getEmail()));
        mailBean.sendRepresentativeAccreditations(backupRequest, messageTemplate ,request.getSelectedDate());
    }

    private RepresentativeVotingHistoryMetaInf getVotingHistoryBackup (
            UserVS representative, Date dateFrom, Date dateTo) throws IOException {
        log.info(format("getVotingHistoryBackup - representative: {0} - dateFrom: {1} - dateTo: {2}", representative.getNif(),
                dateFrom, dateTo));
        String dateFromStr = DateUtils.getDateStr(dateFrom, "yyyy/MM/dd");
        String dateToStr = DateUtils.getDateStr(dateTo,"yyyy/MM/dd");
        String datePathPart = DateUtils.getDateStr(Calendar.getInstance().getTime(), "yyyy/MM/dd");
        String basedir = format("{0}/RepresentativeHistoryVoting/{1}_{2}/representative_{3}",
                config.getProperty("vs.backupBasePath"), dateFromStr, dateToStr, representative.getNif());
        new File(basedir).mkdirs();
        log.info("getVotingHistoryBackup - basedir: " + basedir);
        File zipResult = new File(basedir + ".zip");
        String downloadURL = format("/backup/{0}/representative_{1}.zip", datePathPart, representative.getNif());
        String webappBackupPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${backupURL}";//TODO
        File metaInfFile;
        if(zipResult.exists()) {
            metaInfFile = new File(basedir + "/meta.inf");
            if(metaInfFile.exists()) {
                RepresentativeVotingHistoryMetaInf metaInf = new ObjectMapper().readValue(metaInfFile,
                        new TypeReference<RepresentativeVotingHistoryMetaInf>() {});
                return metaInf;
            }
        }
        Query query = dao.getEM().createQuery("select v from VoteVS v where v.certificateVS.userVS =:userVS " +
                "and v.state =:state  and v.dateCreated between :dateFrom and :dateTo").setParameter("userVS", representative)
                .setParameter("state", VoteVS.State.OK).setParameter("dateFrom", dateFrom).setParameter("dateTo", dateTo);
        List<VoteVS> representativeVotes = query.getResultList();
        long numVotes = representativeVotes.size();
        for (VoteVS voteVS : representativeVotes) {
            String voteId = String.format("%08d", voteVS.getId());
            File smimeFile = new File(format("{0}/vote_{1}.p7m", basedir, voteId));
            IOUtils.write(voteVS.getMessageSMIME().getContent(), new FileOutputStream(smimeFile));
        }
        log.info(format("representative: {0} - numVotes: {1}", representative.getNif(), numVotes));
        String representativeURL = format("{0}/representative/id/{1}", config.getRestURL(), representative.getId());
        RepresentativeVotingHistoryMetaInf metaInf = new RepresentativeVotingHistoryMetaInf(numVotes, dateFrom,
                dateTo, representativeURL, downloadURL);
        metaInfFile = new File(basedir + "/meta.inf");
        new ObjectMapper().writeValue(new FileOutputStream(metaInfFile), metaInf);
        new ZipUtils(basedir).zipIt(zipResult);
        return metaInf;
    }

    public RepresentativeDto geRepresentativeJSON(UserVS representative) {
        Query query = dao.getEM().createQuery("select count(d) from RepresentationDocument d where " +
                "d.representative =:representative and d.state =:state").setParameter("representative", representative)
                .setParameter("state", RepresentativeDocument.State.OK);
        long numRepresentations = (long) query.getSingleResult();
        query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.userVS =:representative " +
                "and r.state =:state").setParameter("representative", representative)
                .setParameter("state", RepresentativeDocument.State.OK);
        RepresentativeDocument representativeDocument = dao.getSingleResult(RepresentativeDocument.class, query);
        if(representativeDocument == null) throw new NotFoundException(
                "ERROR - RepresentativeDocument not found - representativeId: " + representative.getId());
        return new RepresentativeDto(representative,
                representativeDocument.getActivationSMIME().getId(), numRepresentations, config.getRestURL());
    }
}