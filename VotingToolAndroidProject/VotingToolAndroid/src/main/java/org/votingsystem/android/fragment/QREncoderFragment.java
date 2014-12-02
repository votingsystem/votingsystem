package org.votingsystem.android.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.WriterException;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.util.QRUtils;
import org.votingsystem.model.ContextVS;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QREncoderFragment extends Fragment {

    public static final String TAG = QREncoderFragment.class.getSimpleName();

    private AppContextVS contextVS;
    private String broadCastId = QREncoderFragment.class.getSimpleName();
    private View rootView;

    @Override public View onCreateView(LayoutInflater inflater,
               ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        rootView = inflater.inflate(R.layout.qrencoder_fragment, container, false);
        Intent intent = getActivity().getIntent();
        String qrMessage = intent.getStringExtra(ContextVS.MESSAGE_KEY);
        LOGD(TAG + ".onAttach", "qrMessage: " + qrMessage);
        Bitmap bitmap = null;
        try {
            bitmap = QRUtils.encodeAsBitmap("TestQREncoderFragment;;;;12345", getActivity());
        } catch (WriterException ex) {
            ex.printStackTrace();
        }
        ImageView view = (ImageView) rootView.findViewById(R.id.image_view);
        view.setImageBitmap(bitmap);
        TextView contents = (TextView) rootView.findViewById(R.id.contents_text_view);
        contents.setText(qrMessage);
        setHasOptionsMenu(true);
        return rootView;
    }


}
