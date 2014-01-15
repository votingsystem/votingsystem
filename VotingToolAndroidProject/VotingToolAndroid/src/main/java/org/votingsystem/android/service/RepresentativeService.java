package org.votingsystem.android.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.MessageActivity;
import org.votingsystem.android.callable.MessageTimeStamper;
import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.UserVSResponse;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;
import static org.votingsystem.model.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class RepresentativeService extends IntentService {

    public static final String TAG = "RepresentativeService";

    public RepresentativeService() { super(TAG); }

    private ContextVS contextVS;

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        Log.d(TAG + ".onHandleIntent(...) ", "operation: " + operation);
        contextVS = ContextVS.getInstance(getApplicationContext());
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        if(operation == TypeVS.ITEMS_REQUEST) {
            requestRepresentatives(arguments.getString(ContextVS.URL_KEY), serviceCaller);
        } else if (operation == TypeVS.ITEM_REQUEST) {
            requestRepresentative(arguments.getLong(ContextVS.ITEM_ID_KEY), serviceCaller);
        } else if (operation == TypeVS.NIF_REQUEST) {
            String nif = arguments.getString(ContextVS.NIF_KEY);
            requestRepresentativeByNif(nif, serviceCaller);
        } else if(operation == TypeVS.NEW_REPRESENTATIVE) {
            newRepresentative(intent.getExtras());
        }
    }

    private void requestRepresentatives(String serviceURL, String serviceCaller) {
        ResponseVS responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            try {
                JSONObject requestJSON = new JSONObject(responseVS.getMessage());
                UserVSResponse response = UserVSResponse.populate(requestJSON);
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
                    values.put(UserContentProvider.TIMESTAMP_CREATED_COL,
                            System.currentTimeMillis());
                    values.put(UserContentProvider.TIMESTAMP_UPDATED_COL,
                            System.currentTimeMillis());
                    contentValuesList.add(values);
                }
                if(!contentValuesList.isEmpty()) {
                    int numRowsCreated = getContentResolver().bulkInsert(
                            UserContentProvider.CONTENT_URI,contentValuesList.toArray(
                            new ContentValues[contentValuesList.size()]));
                    Log.d(TAG + ".onHandleIntent(...)", "inserted: " + numRowsCreated +" rows");
                } else { //To notify ContentProvider Listeners
                    getContentResolver().insert(UserContentProvider.CONTENT_URI, null);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                sendMessage(ResponseVS.SC_ERROR, getString(R.string.alert_exception_caption),
                        ex.getMessage(), TypeVS.ITEMS_REQUEST, serviceCaller, null);

            }
        } else sendMessage(responseVS.getStatusCode(), getString(R.string.operation_error_msg),
                responseVS.getMessage(),TypeVS.ITEMS_REQUEST, serviceCaller, null);
    }


    private void requestRepresentativeByNif(String nif, String serviceCaller) {
        String serviceURL = ContextVS.getInstance(this).getAccessControl().
                getRepresentativeURLByNif(nif);
        ResponseVS responseVS = HttpHelper.getData(serviceURL, null);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
            sendMessage(responseVS.getStatusCode(), getString(R.string.operation_error_msg),
                    responseVS.getMessage(), TypeVS.ITEM_REQUEST, serviceCaller, null);
        } else {
            try {
                JSONObject jsonResponse = new JSONObject(responseVS.getMessage());
                Long representativeId = jsonResponse.getLong("representativeId");
                requestRepresentative(representativeId, serviceCaller);
            } catch(Exception ex) {
                sendMessage(ResponseVS.SC_ERROR, getString(R.string.operation_error_msg),
                        ex.getMessage(), TypeVS.ITEM_REQUEST, serviceCaller, null);
            }
        }
    }

    private void requestRepresentative(Long representativeId, String serviceCaller) {
        String serviceURL = ContextVS.getInstance(this).getAccessControl().
                getRepresentativeURL(representativeId);
        String imageServiceURL = ContextVS.getInstance(this).getAccessControl().
                getRepresentativeImageURL(representativeId);
        byte[] representativeImageBytes = null;
        try {
            ResponseVS responseVS = HttpHelper.getData(imageServiceURL, null);
            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                representativeImageBytes = responseVS.getMessageBytes();
            responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONObject requestJSON = new JSONObject(responseVS.getMessage());
                UserVS representative = UserVS.populate(requestJSON);
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
                values.put(UserContentProvider.TIMESTAMP_CREATED_COL, System.currentTimeMillis());
                values.put(UserContentProvider.TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
                getContentResolver().insert(UserContentProvider.CONTENT_URI, values);
                sendMessage(responseVS.getStatusCode(), null, null, TypeVS.ITEM_REQUEST,
                        serviceCaller, representativeURI);
            } else sendMessage(responseVS.getStatusCode(), getString(R.string.operation_error_msg),
                    responseVS.getMessage(), TypeVS.ITEM_REQUEST, serviceCaller, null);
        } catch(Exception ex) {
            ex.printStackTrace();
            sendMessage(ResponseVS.SC_ERROR, getString(R.string.operation_error_msg),
                    ex.getMessage(), TypeVS.ITEM_REQUEST, serviceCaller, null);
        }
    }

    private void newRepresentative(Bundle arguments) {
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        TypeVS operationType = (TypeVS) arguments.getSerializable(ContextVS.TYPEVS_KEY);
        String caption = null;
        String message = null;
        try {
            String pin = arguments.getString(ContextVS.PIN_KEY);
            String serviceURL = arguments.getString(ContextVS.URL_KEY);
            String editorContent = arguments.getString(ContextVS.MESSAGE_KEY);
            String messageSubject = arguments.getString(ContextVS.MESSAGE_SUBJECT_KEY);
            //Uri imageUri = (Uri) arguments.getParcelable(ContextVS.URI_KEY);
            //reduceImageFileSize(imageUri);
            byte[] imageBytes = null;
            try {
                File representativeDataFile = new File(getApplicationContext().getFilesDir(),
                        ContextVS.REPRESENTATIVE_DATA_FILE_NAME);
                byte[] serializedRepresentative = FileUtils.getBytesFromFile(
                        representativeDataFile);
                UserVS representativeData = (UserVS) ObjectUtils.deSerializeObject(
                        serializedRepresentative);
                imageBytes = representativeData.getImageBytes();
            }catch(Exception ex) {
                ex.printStackTrace();
            }
            MessageDigest messageDigest = MessageDigest.getInstance(
                    ContextVS.VOTING_DATA_DIGEST);
            byte[] resultDigest =  messageDigest.digest(imageBytes);
            String base64ResultDigest = new String(Base64.encode(resultDigest));
            Map contentToSignMap = new HashMap();
            contentToSignMap.put("operation", TypeVS.NEW_REPRESENTATIVE.toString());
            contentToSignMap.put("base64ImageHash", base64ResultDigest);
            contentToSignMap.put("representativeInfo", editorContent);
            contentToSignMap.put("UUID", UUID.randomUUID().toString());
            String contentToSign = new JSONObject(contentToSignMap).toString();

            byte[] keyStoreBytes = null;
            FileInputStream fis = openFileInput(KEY_STORE_FILE);
            keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
            String userVS = null;
            if (ContextVS.getInstance(getApplicationContext()).getUserVS() != null) userVS =
                    ContextVS.getInstance(getApplicationContext()).getUserVS().getNif();

            String notificationMessage = null;
            Log.d(TAG + ".onHandleIntent(...) ", "signing message: " + messageSubject);
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    keyStoreBytes, USER_CERT_ALIAS, pin.toCharArray(), SIGNATURE_ALGORITHM);
            SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(userVS,
                    ContextVS.getInstance(this).getAccessControl().getNameNormalized(),
                    contentToSign, messageSubject);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, getApplicationContext());
            ResponseVS responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                caption = getString(R.string.timestamp_service_error_caption);
                message = responseVS.getMessage();
            } else {
                smimeMessage = timeStamper.getSmimeMessage();
                byte[] representativeEncryptedDataBytes = Encryptor.encryptSMIME(
                        smimeMessage, contextVS.getAccessControl().getCertificate());
                Map<String, Object> fileMap = new HashMap<String, Object>();
                String representativeDataFileName = ContextVS.REPRESENTATIVE_DATA_FILE_NAME + ":" +
                        ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED.getName();
                fileMap.put(representativeDataFileName, representativeEncryptedDataBytes);
                fileMap.put(ContextVS.IMAGE_FILE_NAME, imageBytes);
                responseVS = HttpHelper.sendObjectMap(fileMap, serviceURL);
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                    caption = getString(R.string.new_representative_error_caption);
                } else {
                    caption = getString(R.string.operation_ok_msg);
                    UserVS representativeData = new UserVS();
                    representativeData.setDescription(editorContent);
                    representativeData.setImageBytes(imageBytes);
                    byte[] representativeDataBytes = ObjectUtils.serializeObject(representativeData);
                    FileOutputStream outputStream;
                    try {
                        outputStream = openFileOutput(ContextVS.REPRESENTATIVE_DATA_FILE_NAME,
                                Context.MODE_PRIVATE);
                        outputStream.write(representativeDataBytes);
                        outputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                message = responseVS.getMessage();
                showNotification(responseVS, operationType, serviceCaller);
            }
            sendMessage(responseVS.getStatusCode(), caption, message, operationType,
                    serviceCaller, null);
        } catch(Exception ex) {
            ex.printStackTrace();
            sendMessage(ResponseVS.SC_ERROR, getString(R.string.operation_error_msg),
                    ex.getMessage(), operationType, serviceCaller, null);
        }
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
                Log.d(TAG + ".reduceImageFileSize(...)", "compressFactor: " + compressFactor +
                        " - imageBytes: " + imageBytes.length);
            } while(imageBytes.length > ContextVS.MAX_REPRESENTATIVE_IMAGE_FILE_SIZE);
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        return imageBytes;
    }

    private void showNotification(ResponseVS responseVS, TypeVS typeVS, String serviceCaller){
        String title = null;
        String message = responseVS.getMessage();
        int resultIcon = R.drawable.cancel_22;
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            title = getString(R.string.new_representative_ok_notification_msg);
            resultIcon = R.drawable.system_users_22;
        }
        else title = getString(R.string.signature_error_notification_msg);
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);

        Intent clickIntent = new Intent(this, MessageActivity.class);
        clickIntent.putExtra(ContextVS.RESPONSE_STATUS_KEY, responseVS.getStatusCode());
        clickIntent.putExtra(ContextVS.ICON_KEY, resultIcon);
        clickIntent.putExtra(ContextVS.TYPEVS_KEY, typeVS);
        clickIntent.putExtra(ContextVS.CAPTION_KEY, title);
        clickIntent.putExtra(ContextVS.MESSAGE_KEY, message);
        clickIntent.putExtra(ContextVS.CALLER_KEY, serviceCaller);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, ContextVS.
                SIGN_AND_SEND_SERVICE_NOTIFICATION_ID, clickIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(title).setContentText(message).setSmallIcon(resultIcon)
                .setContentIntent(pendingIntent);
        Notification note = builder.build();
        // hide the notification after its selected
        note.flags |= Notification.FLAG_AUTO_CANCEL;
        //Identifies our service icon in the icon tray.
        notificationManager.notify(ContextVS.SIGN_AND_SEND_SERVICE_NOTIFICATION_ID, note);
    }

    private void sendMessage(Integer statusCode, String caption, String message, TypeVS typeVS,
             String serviceCaller, Uri representativeUri) {
        Log.d(TAG + ".sendMessage(...) ", "statusCode: " + statusCode + " - caption: " +
                caption  + " - message: " + message + " - serviceCaller: " + serviceCaller);
        Intent intent = new Intent(serviceCaller);
        intent.putExtra(ContextVS.TYPEVS_KEY, typeVS);
        if(statusCode != null) {
            intent.putExtra(ContextVS.RESPONSE_STATUS_KEY, statusCode.intValue());
            if(ResponseVS.SC_CONNECTION_TIMEOUT == statusCode)
                message = getString(R.string.conn_timeout_msg);
        }
        if(caption != null) intent.putExtra(ContextVS.CAPTION_KEY, caption);
        if(message != null) intent.putExtra(ContextVS.MESSAGE_KEY, message);
        if(representativeUri != null) intent.putExtra(ContextVS.URI_KEY, representativeUri);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

}