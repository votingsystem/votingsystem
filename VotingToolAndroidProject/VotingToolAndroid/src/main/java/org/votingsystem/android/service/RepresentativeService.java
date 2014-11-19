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
import org.votingsystem.android.contentprovider.ReceiptContentProvider;
import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.model.AnonymousDelegationVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.UserVSRepresentativesInfo;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.ResponseVS;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.votingsystem.android.util.LogUtils.LOGD;

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
        LOGD(TAG + ".onHandleIntent", "operation: " + operation);
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        if(operation == TypeVS.ITEMS_REQUEST) {
            requestRepresentatives(arguments.getString(ContextVS.URL_KEY), serviceCaller);
        } else if (operation == TypeVS.ITEM_REQUEST) {
            requestRepresentative(arguments.getLong(ContextVS.ITEM_ID_KEY), serviceCaller);
        } else if (operation == TypeVS.NIF_REQUEST) {
            String nif = arguments.getString(ContextVS.NIF_KEY);
            requestRepresentativeByNif(nif, serviceCaller);
        } else if(operation == TypeVS.NEW_REPRESENTATIVE) {
            newRepresentative(intent.getExtras(), serviceCaller, operation);
        } else if(operation == TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION) {
            anonymousDelegation(intent.getExtras(), serviceCaller, operation);
        }
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
                    ContentValues values = new ContentValues();
                    values.put(UserContentProvider.SQL_INSERT_OR_REPLACE, true );
                    values.put(UserContentProvider.ID_COL, representative.getId());
                    values.put(UserContentProvider.URL_COL, representative.getURL());
                    values.put(UserContentProvider.TYPE_COL, UserVS.Type.REPRESENTATIVE.toString());
                    values.put(UserContentProvider.FULL_NAME_COL, representative.getFullName());
                    values.put(UserContentProvider.SERIALIZED_OBJECT_COL,
                            ObjectUtils.serializeObject(representative));
                    values.put(UserContentProvider.NIF_COL, representative.getNif());
                    values.put(UserContentProvider.NUM_REPRESENTATIONS_COL,
                            representative.getNumRepresentations());
                    contentValuesList.add(values);
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
        try {
            responseVS = HttpHelper.getData(imageServiceURL, null);
            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                representativeImageBytes = responseVS.getMessageBytes();
            responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONObject requestJSON = new JSONObject(responseVS.getMessage());
                UserVS representative = UserVS.parse(requestJSON);
                representative.setImageBytes(representativeImageBytes);
                Uri representativeURI = UserContentProvider.getRepresentativeURI(
                        representative.getId());
                ContentValues values = new ContentValues();
                values.put(UserContentProvider.SQL_INSERT_OR_REPLACE, true);
                values.put(UserContentProvider.ID_COL, representative.getId());
                values.put(UserContentProvider.URL_COL, representative.getURL());
                values.put(UserContentProvider.TYPE_COL, UserVS.Type.REPRESENTATIVE.toString());
                values.put(UserContentProvider.FULL_NAME_COL, representative.getFullName());
                values.put(UserContentProvider.SERIALIZED_OBJECT_COL,
                        ObjectUtils.serializeObject(representative));
                values.put(UserContentProvider.NIF_COL, representative.getNif());
                values.put(UserContentProvider.NUM_REPRESENTATIONS_COL,
                        representative.getNumRepresentations());
                getContentResolver().insert(UserContentProvider.CONTENT_URI, values);
                responseVS.setUri(representativeURI);
            } else responseVS.setCaption(getString(R.string.operation_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        }
        responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.ITEM_REQUEST);
        contextVS.broadcastResponse(responseVS);
    }

    private void anonymousDelegation(Bundle arguments, String serviceCaller, TypeVS operationType) {
        X509Certificate destinationCert = contextVS.getAccessControl().getCertificate();
        String weeksOperationActive = arguments.getString(ContextVS.TIME_KEY);
        Calendar calendar = DateUtils.getMonday(Calendar.getInstance());
        Date anonymousDelegationFromDate = calendar.getTime();
        Integer weeksDelegation = Integer.valueOf(weeksOperationActive);
        calendar.add(Calendar.DAY_OF_YEAR, weeksDelegation*7);
        Date anonymousDelegationToDate = calendar.getTime();
        UserVS representative = (UserVS) arguments.getSerializable(ContextVS.USER_KEY);
        ResponseVS responseVS = null;
        try {
            String messageSubject = arguments.getString(ContextVS.MESSAGE_SUBJECT_KEY);
            AnonymousDelegationVS anonymousDelegation = new AnonymousDelegationVS(
                    weeksOperationActive, contextVS.getAccessControl().getServerURL());
            String fromUser = contextVS.getUserVS().getNif();
            Map smimeContentMap = new HashMap();
            smimeContentMap.put("weeksOperationActive", weeksOperationActive);
            smimeContentMap.put("dateFrom", DateUtils.getDayWeekDateStr(anonymousDelegationFromDate));
            smimeContentMap.put("dateTo", DateUtils.getDayWeekDateStr(anonymousDelegationToDate));
            smimeContentMap.put("UUID", UUID.randomUUID().toString());
            smimeContentMap.put("accessControlURL", contextVS.getAccessControl().getServerURL());
            smimeContentMap.put("operation", TypeVS.ANONYMOUS_REPRESENTATIVE_REQUEST.toString());
            JSONObject requestJSON = new JSONObject(smimeContentMap);
            String representativeDataFileName = ContextVS.REPRESENTATIVE_DATA_FILE_NAME + ":" +
                    ContentTypeVS.JSON_SIGNED.getName();
            String csrFileName = ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.TEXT.getName();
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, anonymousDelegation.
                    getCertificationRequest().getCsrPEM());
            //request signed with user certificate (data signed without representative data)
            SignedMapSender signedMapSender = new SignedMapSender(fromUser,
                    contextVS.getAccessControl().getNameNormalized(),
                    requestJSON.toString(), mapToSend, messageSubject, null,
                    contextVS.getAccessControl().getAnonymousDelegationRequestServiceURL(),
                    representativeDataFileName,
                    (AppContextVS)getApplicationContext());
            responseVS = signedMapSender.call();
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                anonymousDelegation.getCertificationRequest().initSigner(responseVS.getMessageBytes());
                X509Certificate anonymousCert = anonymousDelegation.getCertificationRequest().
                        getCertificate();
                anonymousDelegation.setValidFrom(anonymousCert.getNotBefore());
                anonymousDelegation.setValidTo(anonymousCert.getNotAfter());
                responseVS.setData(anonymousDelegation.getCertificationRequest());
                String fromAnonymousUser = anonymousDelegation.getHashCertVS();
                String toUser = contextVS.getAccessControl().getNameNormalized();
                //delegation signed with anonymous certificate (with representative data)
                smimeContentMap.put("representativeNif", representative.getNif());
                smimeContentMap.put("representativeName", representative.getFullName());
                smimeContentMap.put("operation", TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION.toString());
                AnonymousSMIMESender anonymousSender = new AnonymousSMIMESender(fromAnonymousUser,
                        toUser, new JSONObject(smimeContentMap).toString(), messageSubject, null,
                        contextVS.getAccessControl().getAnonymousDelegationServiceURL(),
                        contextVS.getAccessControl().getCertificate(),
                        ContentTypeVS.JSON_SIGNED,
                        anonymousDelegation.getCertificationRequest(),
                        (AppContextVS)getApplicationContext());
                responseVS = anonymousSender.call();
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    SMIMEMessage delegationReceipt = Encryptor.decryptSMIME(
                            responseVS.getMessageBytes(),
                            anonymousDelegation.getCertificationRequest().getKeyPair().getPrivate());

                    anonymousDelegation.setDelegationReceipt(delegationReceipt);
                    ContentValues values = new ContentValues();
                    values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL, ObjectUtils.serializeObject(anonymousDelegation));
                    values.put(ReceiptContentProvider.URL_COL, anonymousDelegation.getMessageId());
                    values.put(ReceiptContentProvider.TYPE_COL, anonymousDelegation.getTypeVS().toString());
                    values.put(ReceiptContentProvider.STATE_COL, ReceiptContainer.State.ACTIVE.toString());
                    Uri uri = getContentResolver().insert(ReceiptContentProvider.CONTENT_URI, values);
                    responseVS.setUri(uri);
                    responseVS.setCaption(getString(R.string.anonymous_delegation_caption));
                    responseVS.setNotificationMessage(getString(R.string.anonymous_delegation_msg,
                            representative.getFullName(), weeksOperationActive));
                } else {
                    LOGD(TAG + ".anonymousDelegation", " _ TODO _ cancel anonymous delegation");
                }
            } else {
                responseVS.setCaption(getString(R.string.error_lbl));
                if(ContentTypeVS.JSON == responseVS.getContentType()) {
                    try {
                        JSONObject responseJSON = new JSONObject(responseVS.getNotificationMessage());
                        responseVS.setNotificationMessage(responseJSON.getString("message"));
                        responseVS.setData(responseJSON.getString("URL"));
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                } else responseVS.setNotificationMessage(responseVS.getMessage());
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            responseVS.setServiceCaller(serviceCaller).setTypeVS(operationType);
            contextVS.broadcastResponse(responseVS);
        }
    }

    private void newRepresentative(Bundle arguments, String serviceCaller, TypeVS operationType) {
        String caption = null;
        String message = null;
        ResponseVS responseVS = null;
        try {
            String serviceURL = arguments.getString(ContextVS.URL_KEY);
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
            responseVS = contextVS.signMessage(
                    contextVS.getAccessControl().getNameNormalized(),
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
                UserVS representativeData = contextVS.getUserVS();
                representativeData.setDescription(editorContent);
                representativeData.setImageBytes(imageBytes);
                PrefUtils.putRepresentative(this, representativeData);
            }
            responseVS.setNotificationMessage(getString(R.string.new_representative_ok_notification_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        }
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