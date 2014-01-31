package org.votingsystem.android;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;

import com.itextpdf.text.Context_iTextVS;

import org.votingsystem.android.activity.MessageActivity;
import org.votingsystem.android.callable.MessageTimeStamper;
import org.votingsystem.android.contentprovider.TicketContentProvider;
import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.model.AccessControlVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ControlCenterVS;
import org.votingsystem.model.CurrencyData;
import org.votingsystem.model.CurrencyVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TicketAccount;
import org.votingsystem.model.TicketServer;
import org.votingsystem.model.TicketVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.signature.util.VotingSystemKeyGenerator;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.ObjectUtils;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.votingsystem.model.ContextVS.ALGORITHM_RNG;
import static org.votingsystem.model.ContextVS.KEY_SIZE;
import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;
import static org.votingsystem.model.ContextVS.NIF_KEY;
import static org.votingsystem.model.ContextVS.PROVIDER;
import static org.votingsystem.model.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.model.ContextVS.SIG_NAME;
import static org.votingsystem.model.ContextVS.STATE_KEY;
import static org.votingsystem.model.ContextVS.State;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;
import static org.votingsystem.model.ContextVS.USER_DATA_FILE_NAME;
import static org.votingsystem.model.ContextVS.VOTING_SYSTEM_PRIVATE_PREFS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class AppContextVS extends Application {

    public static final String TAG = "AppContextVS";

    private State state = State.WITHOUT_CSR;
    private String ticketServerURL;
    private AccessControlVS accessControl;
    private ControlCenterVS controlCenter;
    private TicketServer ticketServer;
    private UserVS userVS;
    private Map<String, X509Certificate> certsMap = new HashMap<String, X509Certificate>();
    private OperationVS operationVS = null;


	@Override public void onCreate() {
        //System.setProperty("android.os.Build.ID", android.os.Build.ID);
        Log.d(TAG + ".onCreate()", "");
        try {
            Context_iTextVS.init(getApplicationContext());
            VotingSystemKeyGenerator.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
            Properties props = new Properties();
            props.load(getAssets().open("VotingSystem.properties"));
            ticketServerURL = props.getProperty(ContextVS.TICKET_SERVER_URL);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
	}

    public String getTicketServerURL() {
        return ticketServerURL;
    }

    public String getHostID() {
        return android.os.Build.ID;
    }

    public OperationVS getOperationVS() {
        return operationVS;
    }

    public void setOperationVS(OperationVS operationVS) {
        if(operationVS == null) Log.d(TAG + ".setOperationVS(...)", "- removing pending operationVS");
        else Log.d(TAG + ".setOperationVS(...)", "- operationVS: " + operationVS.getTypeVS());
        this.operationVS = operationVS;
    }

    public void setState(State state, String nif) {
        Log.d(TAG + ".setState(...)", STATE_KEY + "_" + accessControl.getServerURL() +
                " - state: " + state.toString());
        SharedPreferences settings = getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(STATE_KEY + "_" + accessControl.getServerURL() , state.toString());
        if(nif != null) editor.putString(NIF_KEY, nif);
        if(State.WITH_CERTIFICATE == state) loadUser();
        editor.commit();
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public X509Certificate getCert(String serverURL) {
        Log.d(TAG + ".getCert(...)", " - getCert - serverURL: " + serverURL);
        if(serverURL == null) return null;
        return certsMap.get(serverURL);
    }

    public void putCert(String serverURL, X509Certificate cert) {
        Log.d(TAG + ".putCert(...)", " serverURL: " + serverURL);
        certsMap.put(serverURL, cert);
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public AccessControlVS getAccessControl() {
        return accessControl;
    }

    public void setAccessControlVS(AccessControlVS accessControl) {
        Log.d(TAG + ".setAccessControlURL() ", " - setAccessControlURL: " +
                accessControl.getServerURL());
        SharedPreferences settings = getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String stateStr = settings.getString(
                STATE_KEY + "_" + accessControl.getServerURL(), State.WITHOUT_CSR.toString());
        state = State.valueOf(stateStr);
        this.accessControl = accessControl;
        loadUser();
    }

    public void loadUser() {
        try {
            File representativeDataFile = new File(getFilesDir(), USER_DATA_FILE_NAME);
            if(representativeDataFile.exists()) {
                byte[] serializedUserData = FileUtils.getBytesFromFile(
                        representativeDataFile);
                userVS = (UserVS) ObjectUtils.deSerializeObject(serializedUserData);
            } else Log.d(TAG + ".setAccessControlVS(...)", "user data not found");
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getLapseWeekLbl(Calendar calendar) {
        Calendar thisWeekMonday = DateUtils.getMonday(calendar);
        calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return getString(R.string.week_lapse_lbl, DateUtils.getDate_Es(
                thisWeekMonday.getTime()), DateUtils.getDate_Es(calendar.getTime()));
    }

    public void updateTicketAccountLastChecked() {
        SharedPreferences settings = getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(ContextVS.TICKET_ACCOUNT_LAST_CHECKED_KEY,
        Calendar.getInstance().getTimeInMillis());
        editor.commit();
    }

    public Date getTicketAccountLastChecked() {
        SharedPreferences pref = getSharedPreferences(ContextVS.VOTING_SYSTEM_PRIVATE_PREFS,
                Context.MODE_PRIVATE);
        GregorianCalendar lastCheckedTime = new GregorianCalendar();
        lastCheckedTime.setTimeInMillis(pref.getLong(ContextVS.TICKET_ACCOUNT_LAST_CHECKED_KEY, 0));

        Calendar currentLapseCalendar = DateUtils.getMonday(Calendar.getInstance());

        if(lastCheckedTime.getTime().after(currentLapseCalendar.getTime())) {
            return lastCheckedTime.getTime();
        } else return null;
    }

    public void setTicketAccount(TicketAccount updatedTicketAccount) {
        Set<CurrencyVS> keySet = updatedTicketAccount.getCurrencyMap().keySet();
        for(CurrencyVS currencyVS : keySet) {
            CurrencyData currencyData = updatedTicketAccount.getCurrencyMap().get(currencyVS);
            for(TransactionVS transactionVS : currencyData.getTransactionList()) {
                addTransaction(transactionVS,
                        DateUtils.getDirPath(updatedTicketAccount.getWeekLapse()));
            }
            currencyData.setTransactionList(null);
        }
        updateTicketAccountLastChecked();
    }

    public Uri addTransaction(TransactionVS transactionVS, String weekLapse) {
        String weekLapseStr = (weekLapse == null) ? getCurrentWeekLapseId():weekLapse;
        ContentValues values = new ContentValues();
        values.put(TransactionVSContentProvider.SQL_INSERT_OR_REPLACE, true);
        values.put(TransactionVSContentProvider.ID_COL, transactionVS.getId());
        values.put(TransactionVSContentProvider.URL_COL, transactionVS.getMessageSMIMEURL());
        values.put(TransactionVSContentProvider.FROM_USER_COL,
                transactionVS.getFromUserVS().getNif());
        values.put(TransactionVSContentProvider.TO_USER_COL,
                transactionVS.getToUserVS().getNif());
        values.put(TransactionVSContentProvider.SUBJECT_COL, transactionVS.getSubject());
        values.put(TransactionVSContentProvider.AMOUNT_COL, transactionVS.getAmount().toPlainString());
        values.put(TransactionVSContentProvider.CURRENCY_COL, transactionVS.getCurrencyVS().toString());
        values.put(TransactionVSContentProvider.TYPE_COL, transactionVS.getType().toString());
        values.put(TransactionVSContentProvider.SERIALIZED_OBJECT_COL,
                ObjectUtils.serializeObject(transactionVS));
        values.put(TransactionVSContentProvider.WEEK_LAPSE_COL, weekLapseStr);
        values.put(TransactionVSContentProvider.TIMESTAMP_TRANSACTION_COL,
                transactionVS.getDateCreated().getTime());
        return getContentResolver().insert(TransactionVSContentProvider.CONTENT_URI, values);
    }

    public CurrencyData getCurrencyData(CurrencyVS currency) {
        String selection = TicketContentProvider.WEEK_LAPSE_COL + " =? AND " +
                TicketContentProvider.STATE_COL + " =? AND " +
                TicketContentProvider.CURRENCY_COL + "= ? ";
        String weekLapseId = getCurrentWeekLapseId();
        Cursor cursor = getContentResolver().query(TicketContentProvider.CONTENT_URI,null, selection,
                new String[]{weekLapseId, TicketVS.State.OK.toString(), currency.toString()}, null);
        Log.d(TAG + ".getCurrencyData(...)", "TicketContentProvider - cursor.getCount(): " + cursor.getCount());
        List<TicketVS> ticketList = new ArrayList<TicketVS>();
        while(cursor.moveToNext()) {
            TicketVS ticketVS = (TicketVS) ObjectUtils.deSerializeObject(cursor.getBlob(
                    cursor.getColumnIndex(TicketContentProvider.SERIALIZED_OBJECT_COL)));
            Long ticketId = cursor.getLong(cursor.getColumnIndex(TicketContentProvider.ID_COL));
            ticketVS.setLocalId(ticketId);
            ticketList.add(ticketVS);
        }
        selection = TransactionVSContentProvider.WEEK_LAPSE_COL + " =? AND " +
                TransactionVSContentProvider.CURRENCY_COL + "= ? ";
        cursor = getContentResolver().query(TransactionVSContentProvider.CONTENT_URI,null, selection,
                new String[]{weekLapseId, currency.toString()}, null);
        List<TransactionVS> transactionList = new ArrayList<TransactionVS>();
        while(cursor.moveToNext()) {
            TransactionVS transactionVS = (TransactionVS) ObjectUtils.deSerializeObject(cursor.getBlob(
                    cursor.getColumnIndex(TransactionVSContentProvider.SERIALIZED_OBJECT_COL)));
            transactionList.add(transactionVS);
        }
        CurrencyData currencyData = null;
        try {
            currencyData = new CurrencyData(transactionList);
            currencyData.setTicketList(ticketList);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return currencyData;
    }

    public String getCurrentWeekLapseId() {
        Calendar currentLapseCalendar = DateUtils.getMonday(Calendar.getInstance());
        return DateUtils.getDirPath(currentLapseCalendar.getTime());
    }

    public void insertTickets(Collection<TicketVS> tickets) {
        for(TicketVS ticketVS : tickets) {
            getContentResolver().insert(TicketContentProvider.CONTENT_URI,
                    populateTicketContentValues(ticketVS));
        }
    }

    public ContentValues populateTicketContentValues(TicketVS ticketVS) {
        ContentValues values = new ContentValues();
        values.put(TicketContentProvider.AMOUNT_COL, ticketVS.getAmount().toPlainString());
        values.put(TicketContentProvider.CURRENCY_COL, ticketVS.getCurrency().toString());
        values.put(TicketContentProvider.STATE_COL, ticketVS.getState().toString());
        values.put(TicketContentProvider.SERIALIZED_OBJECT_COL,
                ObjectUtils.serializeObject(ticketVS));
        values.put(TransactionVSContentProvider.WEEK_LAPSE_COL, getCurrentWeekLapseId());
        return values;
    }

    public int updateTicket(TicketVS ticket) {
        Log.d(TAG + ".updateTicket(...) ", "ticket id: " + ticket.getLocalId() +
                " - state: " + ticket.getState());
        ContentValues values = populateTicketContentValues(ticket);
        return getContentResolver().update(TicketContentProvider.getTicketURI(ticket.getLocalId()),
                values, null, null);
    }

    public ResponseVS signMessage(String toUser, String textToSign, String subject, String pin) {
        ResponseVS responseVS = null;
        try {
            FileInputStream fis = openFileInput(KEY_STORE_FILE);
            byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
            String userVS = getUserVS().getNif();
            Log.d(TAG + ".signMessage(...) ", "subject: " + subject);
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    keyStoreBytes, USER_CERT_ALIAS, pin.toCharArray(), SIGNATURE_ALGORITHM);
            SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(userVS, toUser,
                    textToSign, subject);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage,
                    (AppContextVS)getApplicationContext());
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setCaption(getString(R.string.timestamp_service_error_caption));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            String message = ex.getMessage();
            if(message == null || message.isEmpty()) message = getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(getString(R.string.exception_lbl),message);
        } finally {
            return responseVS;
        }
    }

    public boolean checkPin(String pin) {
        try {
            FileInputStream fis = openFileInput(KEY_STORE_FILE);
            byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
            KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
                    keyStoreBytes, pin.toCharArray());
            return true;
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public void setControlCenter(ControlCenterVS controlCenter) {
        this.controlCenter = controlCenter;
    }

    public ControlCenterVS getControlCenter() {
        return controlCenter;
    }

    public TicketServer getTicketServer() {
        return ticketServer;
    }

    public void setTicketServer(TicketServer ticketServer) {
        this.ticketServer = ticketServer;
    }


    public void showNotification(ResponseVS responseVS){
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        Intent clickIntent = new Intent(this, MessageActivity.class);
        clickIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, ContextVS.
                TICKET_SERVICE_NOTIFICATION_ID, clickIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(responseVS.getCaption()).setContentText(Html.fromHtml(
                        responseVS.getNotificationMessage())).setSmallIcon(responseVS.getIconId())
                .setContentIntent(pendingIntent);
        Notification note = builder.build();
        // hide the notification after its selected
        note.flags |= Notification.FLAG_AUTO_CANCEL;
        //Identifies our service icon in the icon tray.
        notificationManager.notify(ContextVS.REPRESENTATIVE_SERVICE_NOTIFICATION_ID, note);
    }

    public void sendBroadcast(ResponseVS responseVS) {
        Log.d(TAG + ".sendMessage(...) ", "statusCode: " + responseVS.getStatusCode() +
                " - type: " + responseVS.getTypeVS() + " - serviceCaller: " +
                responseVS.getServiceCaller());
        Intent intent = new Intent(responseVS.getServiceCaller());
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
