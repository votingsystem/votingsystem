package org.votingsystem.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.List;

public class MailUtils {

    public static final String TAG = "MailUtils";

    protected void sendEmail(Context context, List<String> recipients,
                             String toUser, String content) {

        //String[] recipients = {recipient.getText().toString()};
        Intent email = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
        // prompts email clients only
        email.setType("message/rfc822");

        email.putExtra(Intent.EXTRA_EMAIL, recipients.toArray());
        email.putExtra(Intent.EXTRA_SUBJECT, toUser);
        email.putExtra(Intent.EXTRA_TEXT, content);

        try {
            // the user can choose the email client
            context.startActivity(Intent.createChooser(email, "Choose an email client from..."));
        } catch (android.content.ActivityNotFoundException ex) {
            //Toast.makeText(context, "No email client installed.", Toast.LENGTH_LONG).show();
        }
    }

}
