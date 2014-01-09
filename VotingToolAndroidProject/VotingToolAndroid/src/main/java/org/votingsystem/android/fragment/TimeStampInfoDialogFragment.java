package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle2.cms.SignerId;
import org.bouncycastle2.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle2.util.CollectionStore;
import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.DateUtils;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TimeStampInfoDialogFragment extends DialogFragment {

    public static final String TAG = "TimeStampInfoDialogFragment";

    public static TimeStampInfoDialogFragment newInstance(TimeStampToken timeStampToken,
            Context context){
        TimeStampTokenInfo tsInfo= timeStampToken.getTimeStampInfo();
        String certificateInfo = null;
        SignerId signerId = timeStampToken.getSID();
        BigInteger cert_serial_number = signerId.getSerialNumber();
        String dateInfoStr = DateUtils.getSpanishStringFromDate(tsInfo.getGenTime());
        CollectionStore store = (CollectionStore) timeStampToken.getCertificates();
        Collection<X509CertificateHolder> matches = store.getMatches(null);

        if(matches.size() > 0) {
            boolean validationOk = false;
            for(X509CertificateHolder certificateHolder : matches) {
                boolean isSigner = false;
                Log.d(TAG + ".newInstance(...)", "cert_serial_number: '" + cert_serial_number +
                        "' - serial number: '" + certificateHolder.getSerialNumber() + "'");
                if(certificateHolder.getSerialNumber().compareTo(cert_serial_number) == 0) {
                    try {
                        Log.d(TAG + ".newInstance(...)", "certificateHolder.getSubject(): "
                                + certificateHolder.getSubject() +
                                " - serial number" + certificateHolder.getSerialNumber());
                        timeStampToken.validate(new JcaSimpleSignerInfoVerifierBuilder().
                                setProvider(ContextVS.PROVIDER).build(certificateHolder));
                        Log.d(TAG + ".newInstance(...)", "Validation OK");
                        validationOk = true;
                        isSigner = true;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                try {
                    X509Certificate certificate = new JcaX509CertificateConverter().
                            getCertificate(certificateHolder);
                    certificateInfo = context.getString(R.string.cert_info_formated_msg,
                            certificate.getSubjectDN().toString(),
                            certificate.getIssuerDN().toString(),
                            certificate.getSerialNumber().toString(),
                            DateUtils.getSpanishStringFromDate(
                                    certificate.getNotBefore()),
                            DateUtils.getSpanishStringFromDate(
                                    certificate.getNotAfter()));
                } catch (CertificateException ex) {
                    ex.printStackTrace();
                }
                if(!validationOk) Log.d(TAG + ".newInstance(...)", "Validation ERROR");
            }
        }
        String htmlInfo = context.getString(R.string.timestamp_info_formated_msg, dateInfoStr,
                tsInfo.getSerialNumber().toString(),
                timeStampToken.getSID().getSerialNumber().toString(),
                certificateInfo);
        TimeStampInfoDialogFragment frag = new TimeStampInfoDialogFragment();
        Bundle args = new Bundle();
        args.putString(ContextVS.MESSAGE_KEY, htmlInfo);
        frag.setArguments(args);
        return frag;
    }


    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.message_dialog_fragment, null);
        AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity()).setTitle(
                getString(R.string.show_timestamp_info_lbl));
        TextView messageTextView = (TextView)view.findViewById(R.id.message);
        messageTextView.setText(Html.fromHtml(getArguments().getString(ContextVS.MESSAGE_KEY)));
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        builder.setIcon(R.drawable.signature_ok_32);
        AlertDialog dialog = builder.create();
        dialog.setView(view);
        //to avoid avoid dissapear on screen orientation change
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        return dialog;
    }


}