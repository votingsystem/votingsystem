package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.DateUtils;

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.util.Set;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignersInfoDialogFragment extends DialogFragment {

    public static final String TAG = SignersInfoDialogFragment.class.getSimpleName();

    public static SignersInfoDialogFragment newInstance(byte[] smimeMessage){
        SignersInfoDialogFragment frag = new SignersInfoDialogFragment();
        Bundle args = new Bundle();
        args.putByteArray(ContextVS.MESSAGE_KEY, smimeMessage);
        frag.setArguments(args);
        return frag;
    }


    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View mainLayout = inflater.inflate(
                R.layout.signers_info_dialog, null);
        LinearLayout containerView = (LinearLayout) mainLayout.findViewById(R.id.signers_container);
        byte[] smimeMessageBytes = getArguments().getByteArray(ContextVS.MESSAGE_KEY);
        SMIMEMessage smimeMessage = null;
        try {
            smimeMessage = new SMIMEMessage(null,
                    new ByteArrayInputStream(smimeMessageBytes), null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        Set<UserVS> signers = smimeMessage.getSigners();
        for(UserVS signer : signers) {
            X509Certificate certificate = signer.getCertificate();
            String userCertInfo = getActivity().getString(R.string.cert_info_formated_msg,
                    certificate.getSubjectDN().toString(),
                    certificate.getIssuerDN().toString(),
                    certificate.getSerialNumber().toString(),
                    DateUtils.getDate_Es(
                            certificate.getNotBefore()),
                    DateUtils.getDate_Es(
                            certificate.getNotAfter()));
            View signerView = inflater.inflate(R.layout.signer, null);
            TextView signerInfo = (TextView) signerView.findViewById(R.id.signer_info);
            signerInfo.setText(Html.fromHtml(userCertInfo));
            containerView.addView(signerView);
        }
        AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity()).setTitle(
                getString(R.string.show_signers_info_lbl));
        AlertDialog dialog = builder.create();
        dialog.setView(mainLayout);
        //to avoid avoid dissapear on screen orientation change
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        return dialog;
    }

}
