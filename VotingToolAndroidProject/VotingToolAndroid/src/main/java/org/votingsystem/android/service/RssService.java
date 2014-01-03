package org.votingsystem.android.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import org.votingsystem.android.R;
import org.votingsystem.android.activity.NavigationDrawer;
import org.votingsystem.android.contentprovider.RssContentProvider;
import org.votingsystem.model.ContextVS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * from https://android.googlesource.com/platform/development/+/master/samples/RSSReader/
 */
public class RssService extends Service  implements Runnable {

    public static final String TAG = "RssService";

    public static final String REQUERY_KEY = "REQUERY_KEY"; // Sent to tell us force a requery.
    public static final String RSS_URL = "RSS_URL"; // Sent to tell us to requery a specific item.

    private Handler handler;
    private Cursor cursor;                        // RSS content provider cursor.
    private GregorianCalendar lastCheckedTime; // Time we last checked our feeds.

    private final String LAST_CHECKED_KEY = "LAST_CHECKED_KEY";
    static final int UPDATE_FREQUENCY_IN_MINUTES = 60;

    @Override public void onCreate(){
        // Display an icon to show that the service is running.
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        Intent clickIntent = new Intent(Intent.ACTION_MAIN);
        clickIntent.setClassName(this, NavigationDrawer.class.getName());
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, clickIntent, 0);
        Notification note = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.rss_service_lbl))
                .setContentIntent(pIntent)
                .setSmallIcon(R.drawable.feed_16)
                .build();

        //Identifies our service icon in the icon tray.
        notificationManager.notify(ContextVS.RSS_SERVICE_NOTIFICATION_ID, note);
        handler = new Handler();
        // Load last updated value.
        // We store last updated value in preferences.
        SharedPreferences pref = getSharedPreferences(
                ContextVS.VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        lastCheckedTime = new GregorianCalendar();
        lastCheckedTime.setTimeInMillis(pref.getLong(LAST_CHECKED_KEY, 0));

        /*Services run in the main thread of their hosting process. This means that, if
        * it's going to do any CPU intensive (such as networking) operations, it should
        * spawn its own thread in which to do that work.*/
        Thread thr = new Thread(null, this, "rss_service_thread");
        thr.start();
        Log.i(TAG + ".onCreate(...) ", "RssService created");
    }

    // When the service is destroyed, get rid of our persistent icon.
    @Override public void onDestroy(){
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(
                ContextVS.RSS_SERVICE_NOTIFICATION_ID);
    }

    // Determines whether the next scheduled check time has passed.
    // Loads this value from a stored preference. If it has (or if no
    // previous value has been stored), it will requery all RSS feeds;
    // otherwise, it will post a delayed reminder to check again after
    // now - next_check_time milliseconds.
    public void queryIfPeriodicRefreshRequired() {
        GregorianCalendar nextCheckTime = new GregorianCalendar();
        nextCheckTime = (GregorianCalendar) lastCheckedTime.clone();
        nextCheckTime.add(GregorianCalendar.MINUTE, UPDATE_FREQUENCY_IN_MINUTES);
        Log.d(TAG + ".queryIfPeriodicRefreshRequired(...) ", "last checked time:" +
                lastCheckedTime.toString() + " - Next checked time: " + nextCheckTime.toString());

        if(lastCheckedTime.before(nextCheckTime)) queryRssItems();
        else {
            // Post a message to query again when we get to the next check time.
            long timeTillNextUpdate = lastCheckedTime.getTimeInMillis() -
                    GregorianCalendar.getInstance().getTimeInMillis();
            handler.postDelayed(this, 1000 * 60 * UPDATE_FREQUENCY_IN_MINUTES);
        }

    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Bundle arguments = intent.getExtras();
        if(arguments != null) {
            if(arguments.containsKey(REQUERY_KEY)) {
                queryRssItems();
            }
            if(arguments.containsKey(RSS_URL)) {
                // Typically called after adding a new RSS feed to the list.
                queryItem(arguments.getString(RSS_URL));
            }
        }
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    // Query all feeds. If the new feed has a newer pubDate than the previous,
    // then update it.
    void queryRssItems(){
        Log.d(TAG + ".queryRssItems() ", "");
        cursor = getContentResolver().query(RssContentProvider.CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext()){
            // Get the URL for the feed from the cursor.
            int urlColumnIndex = cursor.getColumnIndex(RssContentProvider.URL_COL);
            String url = cursor.getString(urlColumnIndex);
            queryItem(url);
        }
        // Reset the global "last checked" time
        lastCheckedTime.setTimeInMillis(System.currentTimeMillis());

        // Post a message to query again in [update_frequency] minutes
        handler.postDelayed(this, 1000 * 60 * UPDATE_FREQUENCY_IN_MINUTES);
    }

    // Query an individual RSS feed. Returns true if successful, false otherwise.
    private boolean queryItem(String url) {
        try {
            URL wrappedUrl = new URL(url);
            String rssFeed = readRss(wrappedUrl);
            Log.d(TAG + ".queryItem(...)", "RSS Feed " + url + ":\n " + rssFeed);
            if(TextUtils.isEmpty(rssFeed)) {
                return false;
            }
            // Parse out the feed update date, and compare to the current version.
            // If feed update time is newer, or zero (if never updated, for new
            // items), then update the content, date, and hasBeenRead fields.
            // lastUpdated = <rss><channel><pubDate>value</pubDate></channel></rss>.
            // If that value doesn't exist, the current date is used.
            GregorianCalendar feedPubDate = parseRssDocPubDate(rssFeed);
            GregorianCalendar lastUpdated = new GregorianCalendar();
            int lastUpdatedColumnIndex = cursor.getColumnIndex(RssContentProvider.LAST_UPDATED_COL);
            lastUpdated.setTimeInMillis(cursor.getLong(lastUpdatedColumnIndex));
            if(lastUpdated.getTimeInMillis() == 0 ||
                    lastUpdated.before(feedPubDate) && !TextUtils.isEmpty(rssFeed)) {
                ContentValues values = new ContentValues();
                values.put(RssContentProvider.CONTENT_COL, rssFeed);
                values.put(RssContentProvider.HAS_BEEN_READ_COL, 0);
                values.put(RssContentProvider.LAST_UPDATED_COL, feedPubDate.getTimeInMillis());
                Uri cursorUri = ContentUris.withAppendedId(RssContentProvider.CONTENT_URI,
                        cursor.getInt(cursor.getColumnIndex(RssContentProvider.ID_COL)));
                getContentResolver().update(cursorUri, values, null, null);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private GregorianCalendar parseRssDocPubDate(String xml){
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(0);
        String patt ="<[\\s]*pubDate[\\s]*>(.+?)</pubDate[\\s]*>";
        Pattern p = Pattern.compile(patt);
        Matcher m = p.matcher(xml);
        try {
            if(m.find()) {
                SimpleDateFormat pubDate = new SimpleDateFormat();
                cal.setTime(pubDate.parse(m.group(1)));
            }
        } catch(ParseException ex) {
            ex.printStackTrace();
        }
        return cal;
    }

    private String readRss(URL url){
        String html = "<html><body><h2>No data</h2></body></html>";
        try {
            Log.d(TAG + ".readRss(...)", "url:" + url.toString());
            BufferedReader inStream = new BufferedReader(new InputStreamReader(url.openStream()),
                            1024);
            String line;
            StringBuilder rssFeed = new StringBuilder();
            while ((line = inStream.readLine()) != null){
                rssFeed.append(line);
            }
            html = rssFeed.toString();
        } catch(IOException ex) {
            ex.printStackTrace();
        }
        return html;
    }

    @Override public IBinder onBind(Intent intent){
        return mBinder;
    }

    private final IBinder mBinder = new Binder()  {

        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
        return super.onTransact(code, data, reply, flags);
        }
    };

    @Override public void run() {
        queryIfPeriodicRefreshRequired();
    }

}