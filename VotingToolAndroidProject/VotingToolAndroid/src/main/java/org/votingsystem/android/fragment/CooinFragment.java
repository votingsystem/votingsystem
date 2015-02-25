package org.votingsystem.android.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.util.MsgUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Cooin;
import org.votingsystem.util.DateUtils;

import static org.votingsystem.util.LogUtils.LOGD;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinFragment extends Fragment {

    public static final String TAG = CooinFragment.class.getSimpleName();

    private AppContextVS contextVS;
    private Cooin cooin;
    private TextView cooin_amount, cooin_state, cooin_currency, date_info, hash_cert;


    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        View rootView = inflater.inflate(R.layout.cooin, container, false);
        cooin_amount = (TextView)rootView.findViewById(R.id.cooin_amount);
        cooin_state = (TextView)rootView.findViewById(R.id.cooin_state);
        cooin_currency = (TextView)rootView.findViewById(R.id.cooin_currency);
        date_info = (TextView)rootView.findViewById(R.id.date_info);
        hash_cert = (TextView)rootView.findViewById(R.id.hash_cert);
        if(getArguments() != null && getArguments().containsKey(ContextVS.COOIN_KEY)) {
            cooin = (Cooin) getArguments().getSerializable(ContextVS.COOIN_KEY);
            initCooinScreen(cooin);
        }
        return rootView;
    }

    public void initCooinScreen(Cooin cooin) {
        try {
            hash_cert.setText(cooin.getHashCertVS());
            cooin_amount.setText(cooin.getAmount().toPlainString());
            cooin_currency.setText(cooin.getCurrencyCode());
            getActivity().setTitle(MsgUtils.getCooinDescriptionMessage(cooin, getActivity()));
            date_info.setText(getString(R.string.cooin_date_info,
                    DateUtils.getDateStr(cooin.getDateFrom(), "dd MMM yyyy' 'HH:mm"),
                    DateUtils.getDateStr(cooin.getDateTo(), "dd MMM yyyy' 'HH:mm")));
            if(cooin.getState() != null && Cooin.State.OK != cooin.getState()) {
                cooin_state.setText(MsgUtils.getCooinStateMessage(cooin, getActivity()));
                cooin_state.setVisibility(View.VISIBLE);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

}