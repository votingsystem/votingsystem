package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.votingsystem.android.activity.NavigationDrawer;
import org.votingsystem.android.R;
import org.votingsystem.android.ui.CertPinDialog;
import org.votingsystem.android.ui.CertPinDialogListener;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class RepresentativeOperationsFragment extends Fragment implements CertPinDialogListener {

	public static final String TAG = "RepresentativeOperationsFragment";

    private ContextVS contextVS;
    private TextView progressMessage;
    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean isProgressShown;
    private SendDataTask sendDataTask;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        Log.d(TAG + ".onCreateView(...)", "onCreateView");
        super.onCreate(savedInstanceState);
        contextVS = ContextVS.getInstance(getActivity());
        View rootView = inflater.inflate(R.layout.representative_operations_fragment, container, false);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        getActivity().setTitle(ContextVS.getMessage("representativeOperationsDescription"));
        Button checkRepresentativeButton = (Button) rootView.findViewById(R.id.check_representative_button);
        Button newRepresentativeButton = (Button) rootView.findViewById(R.id.new_representative_button);
        Button cancelRepresentativeButton = (Button) rootView.findViewById(R.id.cancel_representative_button);
        Button editRepresentativeButton = (Button) rootView.findViewById(R.id.edit_representative_button);
        editRepresentativeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { }
        });
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        progressMessage = (TextView)rootView.findViewById(R.id.progressMessage);
        progressMessage.setText(R.string.loading_data_msg);
        mainLayout.getForeground().setAlpha(0);
        isProgressShown = false;
        // if set to true savedInstanceState will be allways null
        setRetainInstance(true);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void onStart() {
    	Log.d(TAG + ".onStart(...) ", "onStart");
    	super.onStart();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", "onDestroy");
        setRetainInstance(true);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override public void onStop() {
        super.onStop();
        Log.d(TAG + ".onStop()", "onStop");
    }


    @Override public void onResume() {
        super.onResume();
        Log.d(TAG + ".onResume() ", "onResume");
    }

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
		switch (item.getItemId()) {
	    	case android.R.id.home:
	    		Log.d(TAG + ".onOptionsItemSelected(...) ", "home");
	    		Intent intent = new Intent(getActivity(), NavigationDrawer.class);
	    		startActivity(intent);
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}



	private void showMessage(String caption, String message) {
		Log.d(TAG + ".showMessage(...) ", "caption: " + caption + "  - showMessage: " + message);
		AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
		builder.setTitle(caption).setMessage(message).show();
	}

    private void showPinScreen(String message) {
        CertPinDialog pinDialog = CertPinDialog.newInstance(message, this, false);
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(CertPinDialog.TAG);
        if (prev != null) ft.remove(prev);
        ft.addToBackStack(null);
        pinDialog.show(ft, CertPinDialog.TAG);
    }

    @Override public void setPin(final String pin) {
        Log.d(TAG + ".setPin()", "setPin");
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(CertPinDialog.TAG);
        if (prev != null) ft.remove(prev);
        ft.commit();
        if(pin == null) return;
        if(sendDataTask != null) sendDataTask.cancel(true);
        sendDataTask = new SendDataTask(pin);
        sendDataTask.execute(contextVS.getAccessControl().getUserCSRServiceURL());
    }

    public void showProgress(boolean shown, boolean animate) {
        if (isProgressShown == shown) return;
        isProgressShown = shown;
        if (!shown) {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                        android.R.anim.fade_out));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_in));
            }
            progressContainer.setVisibility(View.GONE);
            //eventContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha( 0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) {return false;}
            });
        } else {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_out));
            }
            progressContainer.setVisibility(View.VISIBLE);
            //eventContainer.setVisibility(View.INVISIBLE);
            mainLayout.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) { return true; }
            });
        }
    }

    public class SendDataTask extends AsyncTask<String, Void, ResponseVS> {

        private String password;

        public SendDataTask(String password) {
            this.password = password;
        }

        protected void onPreExecute() {
            Log.d(TAG + ".SendDataTask.onPreExecute(...)", "onPreExecute");
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            showProgress(true, true);
        }


        @Override protected ResponseVS doInBackground(String... urls) {
            Log.d(TAG + ".SendDataTask.doInBackground(...)", "url:" + urls[0]);
            byte[] csrBytes = null;
            try {
                return new ResponseVS(ResponseVS.SC_OK);
            } catch (Exception ex) {
                ex.printStackTrace();
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            Log.d(TAG + ".SendDataTask.onPostExecute", "statusCode: " + responseVS.getStatusCode());
            //showProgress(false, true);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {

            } else {
                AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.alert_exception_caption).setMessage(
                        responseVS.getMessage()).setPositiveButton("OK", null).show();
            }
        }
    }

}