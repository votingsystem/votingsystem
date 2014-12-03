package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.util.QRMessageVS;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.ResponseVS;

import java.util.ArrayList;
import java.util.List;
import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRGeneratorFormFragment extends Fragment {

    public static final String TAG = QRGeneratorFormFragment.class.getSimpleName();

    private AppContextVS contextVS;
    private String broadCastId = QRGeneratorFormFragment.class.getSimpleName();
    private View rootView;
    private LinearLayout currency_amount_selector;
    private QRMessageVS qrMessageVS;
    private Spinner operationSpinner;
    private Spinner currencySpinner;
    private EditText amount_text;
    private static final TypeVS[] OPERATION_ARRAY =
            new TypeVS[]{TypeVS.COOIN_TICKET_REQUEST, TypeVS.TRANSACTIONVS};

    @Override public View onCreateView(LayoutInflater inflater,
               ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        rootView = inflater.inflate(R.layout.qr_generator_form_fragment, container, false);
        ((Button) rootView.findViewById(R.id.request_button)).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        generateQR();
                    }
                });
        currency_amount_selector = (LinearLayout) rootView.findViewById(R.id.currency_amount_selector);
        operationSpinner = (Spinner) rootView.findViewById(R.id.operation_spinner);
        amount_text = (EditText) rootView.findViewById(R.id.amount_text);
        List<String> list = new ArrayList<String>();//indices must correspond to OPERATION_ARRAY
        list.add(getString(R.string.cooin_ticket_request_operation));
        list.add(getString(R.string.transactionvs_operation));
        ArrayAdapter<String> operationAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, list);
        operationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        operationSpinner.setAdapter(operationAdapter);
        currencySpinner = (Spinner) rootView.findViewById(R.id.currency_spinner);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.menu_qr));


        if(contextVS.getWebSocketSession() == null) {
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.qr_code_lbl), getString(R.string.qr_connection_required_msg),
                    getActivity()).setPositiveButton(getString(R.string.accept_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            getActivity().finish();
                        }
                    });
            UIUtils.showMessageDialog(builder);
        }
        return rootView;
    }

    private void generateQR() {
        LOGD(TAG + ".generateQR", "generateQR");
        Integer selectedAmount = Integer.valueOf(amount_text.getText().toString());
        if(selectedAmount <= 0) {
            MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                    getString(R.string.min_withdrawal_msg), getFragmentManager());
            return;
        }
        Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
        String qrMessage = String.format("operation=%s;;;amount=%s_%s_WILDTAG;",
                OPERATION_ARRAY[operationSpinner.getSelectedItemPosition()].toString() ,
                amount_text.getText(), currencySpinner.getSelectedItem());
        LOGD(TAG + ".generateQR", "qrMessage: " + qrMessage);
        intent.putExtra(ContextVS.MESSAGE_KEY, qrMessage);
        intent.putExtra(ContextVS.FRAGMENT_KEY, QRGeneratorFragment.class.getName());
        startActivity(intent);
    }


    public class OperationSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
            TypeVS selectedOperation = OPERATION_ARRAY[pos];
            switch (selectedOperation) {
                case COOIN_TICKET_REQUEST:

                    break;
                case TRANSACTIONVS:
                    break;
            }

        }

        @Override public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }
    }

}
