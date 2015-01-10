package org.votingsystem.android.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TransactionRequest;
import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionRequestFragment extends Fragment {

	public static final String TAG = TransactionRequestFragment.class.getSimpleName();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        // if set to true savedInstanceState will be allways null
        setHasOptionsMenu(true);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "progressVisible: ");
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.transaction_request_fragment, container, false);
        getActivity().setTitle(getString(R.string.payment_lbl));
        try {
            JSONObject jsonObject = new JSONObject(getArguments().getString(ContextVS.JSON_DATA_KEY));
            TransactionRequest transactionRequest = TransactionRequest.parse(jsonObject);
        } catch (Exception ex) { ex.printStackTrace(); }
        Button cancelButton = (Button) rootView.findViewById(R.id.cancel_lbl);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        Button save_button = (Button) rootView.findViewById(R.id.save_button);
        save_button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                submitForm();
            }
        });
        return rootView;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
		LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
		switch (item.getItemId()) {
	    	case android.R.id.home:
                getActivity().onBackPressed();
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}

    private void submitForm() { }

    private void showMessage(Integer statusCode, String caption, String message) {
        LOGD(TAG + ".showMessage", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }

}