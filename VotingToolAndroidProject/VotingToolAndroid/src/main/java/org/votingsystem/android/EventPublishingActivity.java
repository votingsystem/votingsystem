package org.votingsystem.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import org.votingsystem.android.callable.PDFPublisher;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.android.fragment.EventPublishingFragment;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.SubSystemVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.android.ui.CertNotFoundDialog;
import org.votingsystem.android.ui.CertPinDialog;
import org.votingsystem.android.ui.CertPinDialogListener;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.FileUtils;

import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.UUID;

import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;
import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventPublishingActivity extends ActionBarActivity {

    public static final String TAG = "EventPublishingActivity";

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.user_cert_request_activity);
        // if we're being restored from a previous state should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }
        EventPublishingFragment publishFragment = new EventPublishingFragment();
        publishFragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,
                publishFragment, EventPublishingFragment.TAG).commit();
    }

    public void processOperation(OperationVS operationVS) {
        EventPublishingFragment fragment = (EventPublishingFragment)getSupportFragmentManager().
                findFragmentByTag(EventPublishingFragment.TAG);
        if (fragment != null) fragment.processOperation(operationVS);

    }

    /*@Override public void onBackPressed() {
        EventPublishingFragment fragment = (EventPublishingFragment)getSupportFragmentManager().
                findFragmentByTag(EventPublishingFragment.TAG);
        if (fragment != null) fragment.onBackPressed();
        else  super.onBackPressed();
    }*/

    //@Override public boolean onKeyDown(int keyCode, KeyEvent event) {}

}