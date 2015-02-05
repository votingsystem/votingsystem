package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.callable.AnonymousSMIMESender;
import org.votingsystem.android.callable.SignedMapSender;
import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.AnonymousDelegation;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Representation;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.UserVSRepresentativesInfo;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ArgVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeService extends IntentService {

    public static final String TAG = RepresentativeService.class.getSimpleName();

    private AppContextVS contextVS;

    public RepresentativeService() { super(TAG); }

    @Override protected void onHandleIntent(Intent intent) {
        contextVS = (AppContextVS) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        LOGD(TAG + ".onHandleIntent", "operation: " + operation + " - serviceCaller: " + serviceCaller);
        ResponseVS responseVS = UIUtils.checkIfAvailable(contextVS.getAccessControlURL(), contextVS);
        if(responseVS != null) {
            contextVS.broadcastResponse(responseVS.setServiceCaller(
                    serviceCaller).setTypeVS(operation));
            return;
        }
        switch(operation) {
            case ITEMS_REQUEST:
                requestRepresentatives(arguments.getString(ContextVS.URL_KEY), serviceCaller);
                break;
            case ITEM_REQUEST:
                requestRepresentative(arguments.getLong(ContextVS.ITEM_ID_KEY), serviceCaller);
                break;
            case NIF_REQUEST:
                String nif = arguments.getString(ContextVS.NIF_KEY);
                requestRepresentativeByNif(nif, serviceCaller);
                break;
            case NEW_REPRESENTATIVE:
                newRepresentative(intent.getExtras(), serviceCaller, operation);
                break;
            case ANONYMOUS_REPRESENTATIVE_SELECTION:
                anonymousDelegation(intent.getExtras(), serviceCaller);
                break;
            case REPRESENTATIVE_SELECTION:
                publicDelegation(intent.getExtras(), serviceCaller);
                break;
            case REPRESENTATIVE_REVOKE:
                revokeRepresentative(serviceCaller);
                break;
            case STATE:
                checkRepresentationState(serviceCaller);
                break;
            case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED:
                cancelAnonymousDelegation(intent.getExtras(), serviceCaller);
                break;
            default: LOGD(TAG + ".onHandleIntent", "unhandled operation: " + operation.toString());
        }
    }

    private void checkRepresentationState(String serviceCaller) {
        ResponseVS responseVS = updateRepresentationState();
        responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.STATE);
        contextVS.broadcastResponse(responseVS);
    }

    private ResponseVS updateRepresentationState() {
        String serviceURL = contextVS.getAccessControl().getRepresentationStateServiceURL(
                contextVS.getUserVS().getNif());
        ResponseVS responseVS = null;
        try {
            responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                Representation representation = new Representation(Calendar.getInstance().getTime(),
                        Representation.State.valueOf(
                                responseVS.getMessageJSON().getString("state")), null, null);
                switch (representation.getState()) {
                    case REPRESENTATIVE:
                    case WITH_PUBLIC_REPRESENTATION:
                        representation.setRepresentative(UserVS.parse(responseVS.getMessageJSON().
                                getJSONObject("representative")));
                        ResponseVS representativeImageReponse = HttpHelper.getData(
                                contextVS.getAccessControl().getRepresentativeImageURL(
                                        representation.getRepresentative().getId()), null);
                        if (ResponseVS.SC_OK == representativeImageReponse.getStatusCode()) {
                            representation.getRepresentative().setImageBytes(
                                    representativeImageReponse.getMessageBytes());
                        }
                        break;
                    case WITH_ANONYMOUS_REPRESENTATION:
                        representation.setDateTo(DateUtils.getDateFromString(
                                responseVS.getMessageJSON().getString("dateTo")));
                        break;
                    case WITHOUT_REPRESENTATION:
                        break;
                }
                PrefUtils.putRepresentationState(representation, this);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            return responseVS;
        }
    }

    private void revokeRepresentative(String serviceCaller) {
        Map contentToSignMap = new HashMap();
        contentToSignMap.put("operation", TypeVS.REPRESENTATIVE_REVOKE.toString());
        contentToSignMap.put("UUID", UUID.randomUUID().toString());
        ResponseVS responseVS = contextVS.signMessage(contextVS.getAccessControl().getNameNormalized(),
                new JSONObject(contentToSignMap).toString(),
                getString(R.string.revoke_representative_msg_subject));
        try {
            responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(), ContentTypeVS.JSON_SIGNED,
                    contextVS.getAccessControl().getRepresentativeRevokeServiceURL());
            String selection = UserContentProvider.NIF_COL + " = ?";
            String[] selectionArgs = { contextVS.getUserVS().getNif() };
            getContentResolver().delete(UserContentProvider.CONTENT_URI, selection, selectionArgs);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) updateRepresentationState();
        } catch (Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        }
        responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.REPRESENTATIVE_REVOKE);
        contextVS.broadcastResponse(responseVS);
    }

    private void requestRepresentatives(String serviceURL, String serviceCaller) {
        ResponseVS responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            try {
                JSONObject requestJSON = new JSONObject(responseVS.getMessage());
                UserVSRepresentativesInfo response = UserVSRepresentativesInfo.parse(requestJSON);
                UserContentProvider.setNumTotalRepresentatives(
                        response.getNumTotalRepresentatives());
                List<ContentValues> contentValuesList = new ArrayList<ContentValues>();
                for(UserVS representative : response.getUsers()) {
                    contentValuesList.add(UserContentProvider.getContentValues(representative));
                }
                if(!contentValuesList.isEmpty()) {
                    int numRowsCreated = getContentResolver().bulkInsert(
                            UserContentProvider.CONTENT_URI,contentValuesList.toArray(
                            new ContentValues[contentValuesList.size()]));
                    LOGD(TAG + ".requestRepresentatives", "inserted: " + numRowsCreated + " rows");
                } else { //To notify ContentProvider Listeners
                    getContentResolver().insert(UserContentProvider.CONTENT_URI, null);
                }
                responseVS = new ResponseVS(ResponseVS.SC_OK);
            } catch (Exception ex) {
                ex.printStackTrace();
                responseVS = ResponseVS.getExceptionResponse(ex, this);
            }
        }
        responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.ITEMS_REQUEST);
        contextVS.broadcastResponse(responseVS);
    }


    private void requestRepresentativeByNif(String nif, String serviceCaller) {
        String serviceURL = contextVS.getAccessControl().getRepresentativeURLByNif(nif);
        ResponseVS responseVS = HttpHelper.getData(serviceURL, null);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
            responseVS.setCaption(getString(R.string.operation_error_msg));
        } else {
            try {
                JSONObject jsonResponse = new JSONObject(responseVS.getMessage());
                Long representativeId = jsonResponse.getLong("representativeId");
                requestRepresentative(representativeId, serviceCaller);
            } catch(Exception ex) {
                ex.printStackTrace();
                responseVS = ResponseVS.getExceptionResponse(ex, this);
            }
        }
        responseVS.setTypeVS(TypeVS.ITEM_REQUEST).setServiceCaller(serviceCaller);
        contextVS.broadcastResponse(responseVS);
    }

    private void requestRepresentative(Long representativeId, String serviceCaller) {
        String serviceURL = contextVS.getAccessControl().getRepresentativeURL(representativeId);
        String imageServiceURL = contextVS.getAccessControl().
                getRepresentativeImageURL(representativeId);
        byte[] representativeImageBytes = null;
        ResponseVS responseVS = null;
        List<ArgVS> argVSList = new ArrayList<ArgVS>();
        try {
            responseVS = HttpHelper.getData(imageServiceURL, null);
            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                representativeImageBytes = responseVS.getMessageBytes();
            responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONObject requestJSON = new JSONObject(responseVS.getMessage());
                UserVS representative = UserVS.parse(requestJSON);
                representative.setImageBytes(representativeImageBytes);
                Uri representativeURI = UserContentProvider.getUserVSURI(
                        representative.getId());
                getContentResolver().insert(UserContentProvider.CONTENT_URI,
                        UserContentProvider.getContentValues(representative));
                responseVS.setUri(representativeURI);
                argVSList.add(new ArgVS(ContextVS.USER_KEY, representative));
            } else responseVS.setCaption(getString(R.string.operation_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        }
        responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.ITEM_REQUEST);
        contextVS.broadcastResponse(responseVS, argVSList.toArray(new ArgVS[argVSList.size()]));
    }

    private void publicDelegation(Bundle arguments, String serviceCaller) {
        ResponseVS responseVS = null;
        UserVS representative = (UserVS) arguments.getSerializable(ContextVS.USER_KEY);
        String serviceURL = contextVS.getAccessControl().getRepresentativeDelegationServiceURL();
        String messageSubject = getString(R.string.representative_delegation_lbl);
        try {
            Map signatureDataMap = new HashMap();
            signatureDataMap.put("operation", TypeVS.REPRESENTATIVE_SELECTION.toString());
            signatureDataMap.put("UUID", UUID.randomUUID().toString());
            signatureDataMap.put("accessControlURL", contextVS.getAccessControl().getServerURL());
            signatureDataMap.put("representativeNif", representative.getNif());
            signatureDataMap.put("representativeName", representative.getName());
            JSONObject signatureContent = new JSONObject(signatureDataMap);
            responseVS = contextVS.signMessage(contextVS.getAccessControl().getNameNormalized(),
                    signatureContent.toString(), messageSubject);
            responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                    ContentTypeVS.JSON_SIGNED, serviceURL);
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setCaption(getString(R.string.error_lbl));
                if(ContentTypeVS.JSON == responseVS.getContentType()) {
                    JSONObject responseJSON = responseVS.getMessageJSON();
                    responseVS.setNotificationMessage(responseJSON.getString("message"));
                    responseVS.setData(responseJSON.getString("URL"));
                } else responseVS.setNotificationMessage(responseVS.getMessage());
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.REPRESENTATIVE_SELECTION);
            contextVS.broadcastResponse(responseVS);
        }
    }

    private void cancelAnonymousDelegation(Bundle arguments, String serviceCaller) {
        LOGD(TAG + ".cancelAnonymousDelegation", "cancelAnonymousDelegation");
        ResponseVS responseVS = null;
        try {
            AnonymousDelegation anonymousDelegation = PrefUtils.getAnonymousDelegation(this);
            if(anonymousDelegation == null) {
                responseVS = new ResponseVS(ResponseVS.SC_ERROR,
                        getString(R.string.missing_anonymous_delegation_cancellation_data));
            } else {
                responseVS = cancelAnonymousDelegation(anonymousDelegation);
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    PrefUtils.putAnonymousDelegation(null, this);
                    responseVS.setCaption(getString(R.string.cancel_anonymouys_representation_lbl)).
                            setNotificationMessage(getString(R.string.cancel_anonymous_representation_ok_msg));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            responseVS.setServiceCaller(serviceCaller).setTypeVS(
                    TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED);
            contextVS.broadcastResponse(responseVS);
        }
    }

    private ResponseVS cancelAnonymousDelegation(
            AnonymousDelegation anonymousDelegation) throws Exception {
        LOGD(TAG + ".cancelAnonymousDelegation", "cancelAnonymousDelegation");
        JSONObject requestJSON = anonymousDelegation.getCancellationRequest();
        ResponseVS responseVS = contextVS.signMessage(contextVS.getAccessControl().getNameNormalized(),
                requestJSON.toString(), getString(R.string.anonymous_delegation_cancellation_lbl));
        responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(), ContentTypeVS.JSON_SIGNED,
                contextVS.getAccessControl().getCancelAnonymousDelegationServiceURL());
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            SMIMEMessage delegationReceipt = new SMIMEMessage(new ByteArrayInputStream(
                    responseVS.getMessageBytes()));
            Collection matches = delegationReceipt.checkSignerCert(
                    contextVS.getAccessControl().getCertificate());
            if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");
            responseVS.setSMIME(delegationReceipt);
        }
        return responseVS;
    }

    private void anonymousDelegation(Bundle arguments, String serviceCaller) {
        Integer weeksOperationActive = Integer.valueOf(arguments.getString(ContextVS.TIME_KEY));
        Date dateFrom = DateUtils.getMonday(DateUtils.addDays(7)).getTime();//Next week Monday
        Integer weeksDelegation = Integer.valueOf(weeksOperationActive);
        Date dateTo = DateUtils.addDays(dateFrom, weeksDelegation * 7).getTime();
        UserVS representative = (UserVS) arguments.getSerializable(ContextVS.USER_KEY);
        ResponseVS responseVS = null;
        try {
            String messageSubject = getString(R.string.representative_delegation_lbl);
            AnonymousDelegation anonymousDelegation = new AnonymousDelegation(weeksOperationActive,
                    dateFrom, dateTo, contextVS.getAccessControl().getServerURL());
            String fromUser = contextVS.getUserVS().getNif();
            String representativeDataFileName = ContextVS.REPRESENTATIVE_DATA_FILE_NAME + ":" +
                    ContentTypeVS.JSON_SIGNED.getName();
            String csrFileName = ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.TEXT.getName();
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, anonymousDelegation.
                    getCertificationRequest().getCsrPEM());
            //request signed with user certificate (data signed without representative data)
            SignedMapSender signedMapSender = new SignedMapSender(fromUser,
                    contextVS.getAccessControl().getNameNormalized(),
                    anonymousDelegation.getRequest().toString(), mapToSend, messageSubject, null,
                    contextVS.getAccessControl().getAnonymousDelegationRequestServiceURL(),
                    representativeDataFileName, (AppContextVS)getApplicationContext());
            responseVS = signedMapSender.call();
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                anonymousDelegation.getCertificationRequest().initSigner(responseVS.getMessageBytes());
                responseVS.setData(anonymousDelegation.getCertificationRequest());
                String fromAnonymousUser = anonymousDelegation.getHashCertVS();
                String toUser = contextVS.getAccessControl().getNameNormalized();
                //delegation signed with anonymous certificate (with delegation data)
                AnonymousSMIMESender anonymousSender = new AnonymousSMIMESender(fromAnonymousUser,
                        toUser, anonymousDelegation.getDelegation(representative.getNif(),
                        representative.getName()).toString(), messageSubject, null,
                        contextVS.getAccessControl().getAnonymousDelegationServiceURL(), null,
                        anonymousDelegation.getCertificationRequest(),
                        (AppContextVS)getApplicationContext());
                responseVS = anonymousSender.call();
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    SMIMEMessage delegationReceipt = new SMIMEMessage(new ByteArrayInputStream(
                            responseVS.getMessageBytes()));
                    Collection matches = delegationReceipt.checkSignerCert(
                            contextVS.getAccessControl().getCertificate());
                    if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");
                    anonymousDelegation.setRepresentative(representative);
                    PrefUtils.putAnonymousDelegation(anonymousDelegation, this);
                    responseVS.setCaption(getString(R.string.anonymous_delegation_caption)).setNotificationMessage(
                            getString(R.string.anonymous_delegation_msg,
                                    representative.getName(), weeksOperationActive));
                } else cancelAnonymousDelegation(anonymousDelegation);
            } else {
                responseVS.setCaption(getString(R.string.error_lbl));
                if(ContentTypeVS.JSON == responseVS.getContentType()) {
                    JSONObject responseJSON = responseVS.getMessageJSON();
                    responseVS.setNotificationMessage(responseJSON.getString("message"));
                    responseVS.setData(responseJSON.getString("URL"));
                } else responseVS.setNotificationMessage(responseVS.getMessage());
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            responseVS.setServiceCaller(serviceCaller).setTypeVS(
                    TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION);
            contextVS.broadcastResponse(responseVS);
        }
    }

    private void newRepresentative(Bundle arguments, String serviceCaller, TypeVS operationType) {
        ResponseVS responseVS = null;
        try {
            String serviceURL = contextVS.getAccessControl().getRepresentativeServiceURL();
            String editorContent = arguments.getString(ContextVS.MESSAGE_KEY);
            String messageSubject = arguments.getString(ContextVS.MESSAGE_SUBJECT_KEY);
            byte[] imageBytes = arguments.getByteArray(ContextVS.IMAGE_KEY);
            MessageDigest messageDigest = MessageDigest.getInstance(
                    ContextVS.VOTING_DATA_DIGEST);
            byte[] resultDigest =  messageDigest.digest(imageBytes);
            String base64ResultDigest = new String(Base64.encode(resultDigest));
            Map contentToSignMap = new HashMap();
            contentToSignMap.put("operation", TypeVS.NEW_REPRESENTATIVE.toString());
            contentToSignMap.put("base64ImageHash", base64ResultDigest);
            contentToSignMap.put("representativeInfo", editorContent);
            contentToSignMap.put("UUID", UUID.randomUUID().toString());
            responseVS = contextVS.signMessage(contextVS.getAccessControl().getNameNormalized(),
                    new JSONObject(contentToSignMap).toString(), messageSubject);
            Map<String, Object> fileMap = new HashMap<String, Object>();
            String representativeDataFileName = ContextVS.REPRESENTATIVE_DATA_FILE_NAME + ":" +
                    ContentTypeVS.JSON_SIGNED.getName();
            fileMap.put(representativeDataFileName, responseVS.getSMIME().getBytes());
            fileMap.put(ContextVS.IMAGE_FILE_NAME, imageBytes);
            responseVS = HttpHelper.sendObjectMap(fileMap, serviceURL);
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setCaption(getString(R.string.new_representative_error_caption));
            } else {
                responseVS.setCaption(getString(R.string.operation_ok_msg));
                responseVS.setNotificationMessage(getString(R.string.new_representative_ok_notification_msg));
                new Thread(
                    new Runnable() { @Override public void run() {updateRepresentationState();}
                }).start();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        }
        responseVS.setTypeVS(TypeVS.NEW_REPRESENTATIVE).setServiceCaller(serviceCaller);
        contextVS.broadcastResponse(responseVS);
    }

    private byte[] reduceImageFileSize(Uri imageUri) {
        byte[] imageBytes = null;
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(imageUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            int compressFactor = 80;
            //Gallery images form phones like Nexus4 can be greater than 3 MB
            //InputStream inputStream = getContentResolver().openInputStream(imageUri);
            //representativeImageBytes = FileUtils.getBytesFromInputStream(inputStream);
            //Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            do {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                //0 meaning compress for small size, 100 meaning compress for max quality.
                // Some formats, like PNG which is lossless, will ignore the quality setting
                bitmap.compress(Bitmap.CompressFormat.JPEG, compressFactor, out);
                imageBytes = out.toByteArray();
                compressFactor = compressFactor - 10;
                LOGD(TAG + ".reduceImageFileSize", "compressFactor: " + compressFactor +
                        " - imageBytes: " + imageBytes.length);
            } while(imageBytes.length > ContextVS.MAX_REPRESENTATIVE_IMAGE_FILE_SIZE);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return imageBytes;
    }

}