/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.votingsystem.android.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle2.cms.SignerId;
import org.bouncycastle2.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle2.util.CollectionStore;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.activity.MessageActivity;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.FieldEventVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ResponseVS;

import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.android.util.LogUtils.makeLogTag;

/**
 * An assortment of UI helpers.
 */
public class UIUtils  {

    private static final String TAG = makeLogTag(UIUtils.class);

    public static final int EMPTY_MESSAGE = 1;
    /**
     * Regex to search for HTML escape sequences.
     *
     * <p></p>Searches for any continuous string of characters starting with an ampersand and ending with a
     * semicolon. (Example: &amp;amp;)
     */
    public static final String TARGET_FORM_FACTOR_HANDSET = "handset";
    public static final String TARGET_FORM_FACTOR_TABLET = "tablet";
    private static final Pattern REGEX_HTML_ESCAPE = Pattern.compile(".*&\\S;.*");
    public static final int ANIMATION_FADE_IN_TIME = 250;
    public static final String TRACK_ICONS_TAG = "tracks";
    private static SimpleDateFormat sDayOfWeekFormat = new SimpleDateFormat("E");
    private static DateFormat sShortTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);


    public static void launchMessageActivity(Context context, ResponseVS responseVS) {
        Intent intent = new Intent(context, MessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        context.startActivity(intent);
    }

    public static void launchMessageActivity(Integer statusCode, String message, String caption,
             Context context) {
        ResponseVS responseVS = new ResponseVS(statusCode);
        responseVS.setCaption(caption).setNotificationMessage(message);
        Intent intent = new Intent(context, MessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        context.startActivity(intent);
    }

    public static void showSignersInfoDialog(Set<UserVS> signers, FragmentManager fragmentManager,
             Context context) {
        StringBuilder signersInfo = new StringBuilder(context.getString(R.string.num_signers_lbl,
                signers.size()) + "<br/><br/>");
        for(UserVS signer : signers) {
            X509Certificate certificate = signer.getCertificate();
            signersInfo.append(context.getString(R.string.cert_info_formated_msg,
                    certificate.getSubjectDN().toString(),
                    certificate.getIssuerDN().toString(),
                    certificate.getSerialNumber().toString(),
                    DateUtils.getDayWeekDateStr(certificate.getNotBefore()),
                    DateUtils.getDayWeekDateStr(certificate.getNotAfter())) + "<br/>");
        }
        MessageDialogFragment.showDialog(ResponseVS.SC_OK, context.getString(
                R.string.signers_info_lbl), signersInfo.toString(), fragmentManager);
    }

    public static Drawable getEmptyLogo(Context context) {
        Drawable drawable = new ColorDrawable(context.getResources().getColor(android.R.color.transparent));
        return drawable;
    }

    public static void showTimeStampInfoDialog(TimeStampToken timeStampToken,
           X509Certificate timeStampServerCert, FragmentManager fragmentManager, Context context) {
        try {
            TimeStampTokenInfo tsInfo= timeStampToken.getTimeStampInfo();
            String certificateInfo = null;
            SignerId signerId = timeStampToken.getSID();
            String dateInfoStr = DateUtils.getDayWeekDateStr(tsInfo.getGenTime());
            CollectionStore store = (CollectionStore) timeStampToken.getCertificates();
            Collection<X509CertificateHolder> matches = store.getMatches(signerId);
            X509CertificateHolder certificateHolder = null;
            if(matches.size() == 0) {
                LOGD(TAG + ".showTimeStampInfoDialog",
                        "no cert matches found, validating with timestamp server cert");
                certificateHolder = new X509CertificateHolder(timeStampServerCert.getEncoded());
                timeStampToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                        ContextVS.PROVIDER).build(certificateHolder));
            } else certificateHolder = matches.iterator().next();
            LOGD(TAG + ".showTimeStampInfoDialog", "serial number: '" +
                    certificateHolder.getSerialNumber() + "'");
            X509Certificate certificate = new JcaX509CertificateConverter().
                    getCertificate(certificateHolder);
            certificateInfo = context.getString(R.string.timestamp_info_formated_msg, dateInfoStr,
                    tsInfo.getSerialNumber().toString(),
                    certificate.getSubjectDN(),
                    timeStampToken.getSID().getSerialNumber().toString());
            MessageDialogFragment.showDialog(ResponseVS.SC_OK, context.getString(
                    R.string.timestamp_info_lbl), certificateInfo, fragmentManager);
        } catch (Exception ex) {
            ex.printStackTrace();
            MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, context.getString(
                    R.string.error_lbl), context.getString(R.string.timestamp_error_lbl),
                    fragmentManager);
        }
    }

    public static boolean isSameDayDisplay(long time1, long time2, Context context) {
        TimeZone displayTimeZone = PrefUtils.getDisplayTimeZone(context);
        Calendar cal1 = Calendar.getInstance(displayTimeZone);
        Calendar cal2 = Calendar.getInstance(displayTimeZone);
        cal1.setTimeInMillis(time1);
        cal2.setTimeInMillis(time2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    //http://stackoverflow.com/questions/15055458/detect-7-inch-and-10-inch-tablet-programmatically
    public static double getDiagonalInches(Display display) {
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        //float scaleFactor = metrics.density;
        //float widthDp = widthPixels / scaleFactor;
        //float heightDp = heightPixels / scaleFactor;
        float widthDpi = metrics.xdpi;
        float heightDpi = metrics.ydpi;
        float widthInches = widthPixels / widthDpi;
        float heightInches = heightPixels / heightDpi;
        double diagonalInches = Math.sqrt((widthInches * widthInches) +
                (heightInches * heightInches));
        return diagonalInches;
    }

    /**
     * Populate the given {@link android.widget.TextView} with the requested text, formatting
     * through {@link android.text.Html#fromHtml(String)} when applicable. Also sets
     * {@link android.widget.TextView#setMovementMethod} so inline links are handled.
     */
    public static void setTextMaybeHtml(TextView view, String text) {
        if (TextUtils.isEmpty(text)) {
            view.setText("");
            return;
        }
        if ((text.contains("<") && text.contains(">")) || REGEX_HTML_ESCAPE.matcher(text).find()) {
            view.setText(Html.fromHtml(text));
            view.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            view.setText(text);
        }
    }

    public static String getPollText(final Context context, long start, long end) {
        long now = Calendar.getInstance().getTimeInMillis();
        if (now < start) {
            return "";
        } else if (start <= now && now <= end) {
            return "";
        } else {
            return "";
        }
    }

    /**
     * Given a snippet string with matching segments surrounded by curly
     * braces, turn those areas into bold spans, removing the curly braces.
     */
    public static Spannable buildStyledSnippet(String snippet) {
        final SpannableStringBuilder builder = new SpannableStringBuilder(snippet);
        // Walk through string, inserting bold snippet spans
        int startIndex, endIndex = -1, delta = 0;
        while ((startIndex = snippet.indexOf('{', endIndex)) != -1) {
            endIndex = snippet.indexOf('}', startIndex);
            // Remove braces from both sides
            builder.delete(startIndex - delta, startIndex - delta + 1);
            builder.delete(endIndex - delta - 1, endIndex - delta);
            // Insert bold style
            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    startIndex - delta, endIndex - delta - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //builder.setSpan(new ForegroundColorSpan(0xff111111),
            //        startIndex - delta, endIndex - delta - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            delta += 2;
        }
        return builder;
    }

    public static void preferPackageForIntent(Context context, Intent intent, String packageName) {
        PackageManager pm = context.getPackageManager();
        for (ResolveInfo resolveInfo : pm.queryIntentActivities(intent, 0)) {
            if (resolveInfo.activityInfo.packageName.equals(packageName)) {
                intent.setPackage(packageName);
                break;
            }
        }
    }

    public static Drawable getLogoIcon(Context context, int iconId) {
        Drawable logoIcon = context.getResources().getDrawable(iconId);
        logoIcon.setBounds(0, 0, 32, 32);
        //logoIcon.setColorFilter(R.color.navdrawer_background, PorterDuff.Mode.MULTIPLY);
        return logoIcon;
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static void changeSearchIcon(Menu menu, Context context) {
        MenuItem searchViewMenuItem = menu.findItem(R.id.search_item);
        SearchView mSearchView = (SearchView) searchViewMenuItem.getActionView();
        int searchImgId = context.getResources().getIdentifier("android:id/search_button", null, null);
        ImageView v = (ImageView) mSearchView.findViewById(searchImgId);
        v.setImageResource(R.drawable.action_search);
    }

    public static EditText addFormField(String label, Integer type, LinearLayout mFormView, int id,
                Context context) {
        TextView textView = new TextView(context);
        textView.setTextSize(context.getResources().getDimension(R.dimen.claim_field_text_size));
        textView.setText(label);
        EditText fieldText = new EditText(context.getApplicationContext());
        fieldText.setLayoutParams(UIUtils.getFormItemParams(false));
        fieldText.setTextColor(Color.BLACK);
        // setting an unique id is important in order to save the state
        // (content) of this view across screen configuration changes
        fieldText.setId(id);
        fieldText.setInputType(type);
        mFormView.addView(textView);
        mFormView.addView(fieldText);
        return fieldText;
    }

    public static LinearLayout.LayoutParams getFormItemParams(boolean isLabel) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (isLabel) {
            params.bottomMargin = 5;
            params.topMargin = 10;
        }
        params.leftMargin = 20;
        params.rightMargin = 20;
        return params;
    }

    // Whether a feedback notification was fired for a particular session. In the event that a
    // feedback notification has not been fired yet, return false and set the bit.
    public static boolean isFeedbackNotificationFiredForSession(Context context, String sessionId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = String.format("feedback_notification_fired_%s", sessionId);
        boolean fired = sp.getBoolean(key, false);
        sp.edit().putBoolean(key, true).commit();
        return fired;
    }

    // Clear the flag that says a notification was fired for the given session.
    // Typically used to debug notifications.
    public static void unmarkFeedbackNotificationFiredForSession(Context context, String sessionId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = String.format("feedback_notification_fired_%s", sessionId);
        sp.edit().putBoolean(key, false).commit();
    }
    // Shows whether a notification was fired for a particular session time block. In the
    // event that notification has not been fired yet, return false and set the bit.
    public static boolean isNotificationFiredForBlock(Context context, String blockId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = String.format("notification_fired_%s", blockId);
        boolean fired = sp.getBoolean(key, false);
        sp.edit().putBoolean(key, true).commit();
        return fired;
    }

    private static final int[] RES_IDS_ACTION_BAR_SIZE = { android.R.attr.actionBarSize };

    /** Calculates the Action Bar height in pixels. */
    public static int calculateActionBarSize(Context context) {
        if (context == null) {
            return 0;
        }

        Resources.Theme curTheme = context.getTheme();
        if (curTheme == null) {
            return 0;
        }

        TypedArray att = curTheme.obtainStyledAttributes(RES_IDS_ACTION_BAR_SIZE);
        if (att == null) {
            return 0;
        }

        float size = att.getDimension(0, 0);
        att.recycle();
        return (int) size;
    }


    public static Map<Integer, EditText> showClaimFieldsDialog(final Set<FieldEventVS> fields,
               final View.OnClickListener listener, Activity activity) {
        AlertDialog.Builder builder= new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        ScrollView mScrollView = (ScrollView) inflater.inflate(R.layout.claim_dinamic_form,
                (ViewGroup) activity.getCurrentFocus());
        LinearLayout mFormView = (LinearLayout) mScrollView.findViewById(R.id.form);
        final TextView errorMsgTextView = (TextView) mScrollView.findViewById(R.id.errorMsg);
        errorMsgTextView.setVisibility(View.GONE);
        final Map<Integer, EditText> fieldsMap = new HashMap<Integer, EditText>();
        for (FieldEventVS field : fields) {
            fieldsMap.put(field.getId().intValue(), UIUtils.addFormField(field.getContent(),
                    InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
                    mFormView, field.getId().intValue(), activity));
        }
        builder.setTitle(R.string.eventfields_dialog_caption).setView(mScrollView).
                setPositiveButton(activity.getString(R.string.accept_lbl), null).
                setNegativeButton(R.string.cancel_lbl, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) { }
                });
        final AlertDialog dialog = builder.create();
        dialog.show();//to get positiveButton this must be called first
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View onClick) {
                for (FieldEventVS field : fields) {
                    EditText editText = fieldsMap.get(field.getId().intValue());
                    String fieldValue = editText.getText().toString();
                    if (fieldValue.isEmpty()) {
                        errorMsgTextView.setVisibility(View.VISIBLE);
                        return;
                    } else field.setValue(fieldValue);
                    LOGD(TAG + ".showClaimFieldsDialog", "field id: " + field.getId() +
                            " - text: " + fieldValue);
                }
                //listener.onClick();
                dialog.dismiss();
            }
        });
        //to avoid avoid dissapear on screen orientation change
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        return fieldsMap;
    }

    public static int setColorAlpha(int color, float alpha) {
        int alpha_int = Math.min(Math.max((int)(alpha * 255.0f), 0), 255);
        return Color.argb(alpha_int, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int scaleColor(int color, float factor, boolean scaleAlpha) {
        return Color.argb(scaleAlpha ? (Math.round(Color.alpha(color) * factor)) : Color.alpha(color),
                Math.round(Color.red(color) * factor), Math.round(Color.green(color) * factor),
                Math.round(Color.blue(color) * factor));
    }

    public static boolean hasActionBar(Activity activity) {
        return (((ActionBarActivity)activity).getSupportActionBar() != null);
    }

    public static void setStartPadding(final Context context, View view, int padding) {
        if (isRtl(context)) {
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), padding, view.getPaddingBottom());
        } else {
            view.setPadding(padding, view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isRtl(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return false;
        } else {
            return context.getResources().getConfiguration().getLayoutDirection()
                    == View.LAYOUT_DIRECTION_RTL;
        }
    }

    public static void setAccessibilityIgnore(View view) {
        view.setClickable(false);
        view.setFocusable(false);
        view.setContentDescription("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    public static void launchEmbeddedFragment(String className, Context context) {
        Intent intent = new Intent(context, FragmentContainerActivity.class);
        intent.putExtra(ContextVS.FRAGMENT_KEY, className);
        context.startActivity(intent);
    }

    public static void killApp(boolean killSafely) {
        if (killSafely) {
            /*
             * Notify the system to finalize and collect all objects of the app
             * on exit so that the virtual machine running the app can be killed
             * by the system without causing issues. NOTE: If this is set to
             * true then the virtual machine will not be killed until all of its
             * threads have closed.
             */
            System.runFinalizersOnExit(true);

            /*
             * Force the system to close the app down completely instead of
             * retaining it in the background. The virtual machine that runs the
             * app will be killed. The app will be completely created as a new
             * app in a new virtual machine running in a new process if the user
             * starts the app again.
             */
            System.exit(0);
        } else {
            /*
             * Alternatively the process that runs the virtual machine could be
             * abruptly killed. This is the quickest way to remove the app from
             * the device but it could cause problems since resources will not
             * be finalized first. For example, all threads running under the
             * process will be abruptly killed when the process is abruptly
             * killed. If one of those threads was making multiple related
             * changes to the database, then it may have committed some of those
             * changes but not all of those changes when it was abruptly killed.
             */
            android.os.Process.killProcess(android.os.Process.myPid());
        }

    }
}
