package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.WriterException;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.util.QRMessageVS;
import org.votingsystem.android.util.QRUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.ExceptionVS;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRGeneratorFragment extends Fragment {

    public static final String TAG = QRGeneratorFragment.class.getSimpleName();

    private AppContextVS contextVS;
    private String broadCastId = QRGeneratorFragment.class.getSimpleName();
    private View rootView;
    private QRMessageVS qrMessageVS;

    @Override public View onCreateView(LayoutInflater inflater,
               ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        rootView = inflater.inflate(R.layout.qr_generator_fragment, container, false);
        Intent intent = getActivity().getIntent();
        String qrMessage = intent.getStringExtra(ContextVS.MESSAGE_KEY);
        try {
            qrMessageVS = new QRMessageVS(qrMessage);
        } catch (ExceptionVS ex) {
            ex.printStackTrace();
        }
        LOGD(TAG + ".onAttach", "qrMessage: " + qrMessage);
        Bitmap bitmap = null;
        try {
            bitmap = QRUtils.encodeAsBitmap(qrMessage, getActivity());
        } catch (WriterException ex) {
            ex.printStackTrace();
        }
        ImageView view = (ImageView) rootView.findViewById(R.id.image_view);
        view.setImageBitmap(bitmap);
        TextView contents = (TextView) rootView.findViewById(R.id.contents_text_view);
        contents.setText(qrMessage);
        setHasOptionsMenu(true);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.qr_code_lbl));
        ((ActionBarActivity)getActivity()).getSupportActionBar().setSubtitle(
                getOperationMessage(qrMessageVS.getOperation()));
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

    private String getOperationMessage(TypeVS operation) {
        switch(operation) {
            case COOIN_TICKET_REQUEST:
                return getString(R.string.cooin_ticket_request_qr_msg);
            case TRANSACTIONVS:
                return getString(R.string.transactionvs_qr_msg);
            default: return operation.toString();
        }
    }

}
