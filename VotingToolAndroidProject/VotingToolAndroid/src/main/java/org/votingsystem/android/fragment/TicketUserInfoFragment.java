package org.votingsystem.android.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.service.SignAndSendService;
import org.votingsystem.android.service.TicketService;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TicketAccount;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.ObjectUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TicketUserInfoFragment extends Fragment {

	public static final String TAG = "TicketUserInfoFragment";

    private static final int REPRESENTATIVE_DELEGATION   = 1;

    private View rootView;
    private String broadCastId = null;
    private AppContextVS contextVS;
    private Button transactionButton;
    private View progressContainer;
    private TextView ticket_account_info;
    private TextView ticket_cash_info;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver.onReceive(...)", "extras: " + intent.getExtras());
        String pin = intent.getStringExtra(ContextVS.PIN_KEY);
        TypeVS typeVS = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
        if(pin != null) launchUpdateUserInfoService(pin);
        else {
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                loadUserInfo();
            }
            showProgress(false, false);
        }
        }
    };


    public static Fragment newInstance(Long representativeId) {
        TicketUserInfoFragment fragment = new TicketUserInfoFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.USER_KEY, representativeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        contextVS = (AppContextVS) getActivity().getApplicationContext();

        rootView = inflater.inflate(R.layout.ticket_user_info, container, false);
        ticket_account_info = (TextView)rootView.findViewById(R.id.ticket_account_info);
        ticket_cash_info = (TextView)rootView.findViewById(R.id.ticket_cash_info);
        transactionButton = (Button) rootView.findViewById(R.id.transaction_button);
        transactionButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

            }
        });
        transactionButton.setVisibility(View.GONE);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        setHasOptionsMenu(true);
        broadCastId = this.getClass().getSimpleName();
        loadUserInfo();
        return rootView;
    }

    private void loadUserInfo() {
        try {
            File ticketUserInfoDataFile = new File(getActivity().getApplicationContext().getFilesDir(),
                    ContextVS.TICKET_USER_INFO_DATA_FILE_NAME);
            if(ticketUserInfoDataFile.exists()) {
                byte[] serializedTicketUserInfo = FileUtils.getBytesFromFile(ticketUserInfoDataFile);
                TicketAccount ticketUserInfo = (TicketAccount) ObjectUtils.deSerializeObject(
                        serializedTicketUserInfo);
                ticket_account_info.setText(Html.fromHtml(getString(R.string.ticket_account_amount_info_lbl,
                        ticketUserInfo.getAccountBalance())));
                ticket_cash_info.setText(Html.fromHtml(getString(R.string.ticket_cash_amount_info_lbl,
                        ticketUserInfo.getCashBalance())));
            } else {
                Log.d(TAG + ".onCreateView(...)", "ticketUserInfoDataFile doesn't exist");
                showMessage(ResponseVS.SC_ERROR, getString(R.string.empty_ticket_user_info_caption),
                        getString(R.string.empty_ticket_user_info));
            }
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG + ".onActivityResult(...)", "requestCode: " + requestCode + " - resultCode: " +
                resultCode);
        int statusCode = -1;
        String caption = null;
        String message = null;
        if(data != null) message = data.getStringExtra(ContextVS.MESSAGE_KEY);
        if(Activity.RESULT_OK == requestCode) {
            statusCode = ResponseVS.SC_OK;
            caption = getString(R.string.operation_ok_msg);
            showMessage(statusCode, caption, message);
        } else if(message != null) {
            statusCode = ResponseVS.SC_ERROR;
            caption = getString(R.string.operation_error_msg);
            showMessage(statusCode, caption, message);
        }
    }

    private void printRepresentativeData(UserVS representative) {
        if(representative.getImageBytes() != null) {
            final Bitmap bmp = BitmapFactory.decodeByteArray(representative.getImageBytes(), 0,
                    representative.getImageBytes().length);
            ImageView image = (ImageView) rootView.findViewById(R.id.representative_image);
            image.setImageBitmap(bmp);
            image.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    ImageView imageView = new ImageView(getActivity());
                    imageView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    imageView.setImageBitmap(bmp);
                    new AlertDialog.Builder(getActivity()).setView(imageView).show();
                }
            });
        }
        if(representative.getDescription() != null) {
            String representativeDescription =
                    "<html style='background-color:#eeeeee;margin: 5px 10px 10px 10px;'>" +
                    representative.getDescription() + "</html>";
            ((WebView)rootView.findViewById(R.id.representative_description)).loadData(
                    representativeDescription, "text/html", "utf-8");
        }
        transactionButton.setVisibility(View.VISIBLE);
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.ticket_user_info, menu);
        menu.setGroupVisible(R.id.general_items, false);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.update_signers_info:
                PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                        getString(R.string.update_user_info_pin_msg), false, TypeVS.TICKET_USER_INFO);
                return true;
            case R.id.send_message:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void launchUpdateUserInfoService(String pin) {
        Log.d(TAG + ".launchUpdateUserInfoService(...) ", "");
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    TicketService.class);
            startIntent.putExtra(ContextVS.PIN_KEY, pin);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.TICKET_USER_INFO);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            showProgress(true, true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_in));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_out));
            }
            progressContainer.setVisibility(View.VISIBLE);
            //eventContainer.setVisibility(View.INVISIBLE);
            mainLayout.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } else {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_out));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_in));
            }
            progressContainer.setVisibility(View.GONE);
            //eventContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha(0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }
    }

    @Override public void onResume() {
        Log.d(TAG + ".onResume() ", "");
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
    }

}