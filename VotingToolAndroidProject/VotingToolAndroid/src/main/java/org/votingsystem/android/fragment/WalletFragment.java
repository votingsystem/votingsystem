package org.votingsystem.android.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.util.MsgUtils;
import org.votingsystem.android.util.Utils;
import org.votingsystem.android.util.Wallet;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Cooin;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ResponseVS;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.model.ContextVS.FRAGMENT_KEY;

public class WalletFragment extends Fragment {

    public static final String TAG = WalletFragment.class.getSimpleName();
    public static final String AUTHENTICATED_KEY = "AUTHENTICATED_KEY";

    private View rootView;
    private GridView gridView;
    private CooinListAdapter adapter = null;
    private List<Cooin> cooinList;
    private String broadCastId = WalletFragment.class.getSimpleName();
    private Menu menu;
    private boolean walletLoaded = false;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
                switch(responseVS.getTypeVS()) {
                    case COOIN:
                        try {
                            cooinList = Wallet.getCooinList((String) responseVS.getData(),
                                    (AppContextVS) getActivity().getApplicationContext());
                            Utils.launchCooinStatusCheck(broadCastId, getActivity());
                            printSummary();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                                    getString(R.string.error_lbl), ex.getMessage(), getFragmentManager());
                        }
                        break;
                }
            } else {
                switch(responseVS.getTypeVS()) {
                    case COOIN_CHECK:
                        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                            cooinList = Wallet.getCooinList();
                            printSummary();
                            MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                                    getString(R.string.error_lbl), responseVS.getMessage(),
                                    getFragmentManager());
                        }
                        break;
                    case COOIN_ACCOUNTS_INFO:
                        break;
                }
            }
        }
    };

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<UserVS> ALPHA_COMPARATOR = new Comparator<UserVS>() {
        private final Collator sCollator = Collator.getInstance();
        @Override public int compare(UserVS object1, UserVS object2) {
            return sCollator.compare(object1.getName(), object2.getName());
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ((ActionBarActivity)getActivity()).setTitle(getString(R.string.wallet_lbl));
        rootView = inflater.inflate(R.layout.wallet_fragment, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v, pos, id);
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                intent.putExtra(FRAGMENT_KEY, CooinFragment.class.getName());
                intent.putExtra(ContextVS.COOIN_KEY, cooinList.get(position));
                startActivity(intent);
            }
        });
        cooinList = Wallet.getCooinList();
        adapter = new CooinListAdapter(cooinList, getActivity());
        gridView.setAdapter(adapter);
        if(cooinList == null) {
            PinDialogFragment.showWalletScreen(getFragmentManager(), broadCastId,
                    getString(R.string.enter_wallet_pin_msg), false, TypeVS.COOIN);
            cooinList = new ArrayList<Cooin>();
        } else {
            printSummary();
            walletLoaded = true;
        }
        setHasOptionsMenu(true);
        ResponseVS responseVS = (getArguments() != null)? (ResponseVS) getArguments().getParcelable(
                ContextVS.RESPONSEVS_KEY) :null;
        if(responseVS != null) {
            MessageDialogFragment.showDialog(responseVS.getStatusCode(),
                    responseVS.getCaption(), responseVS.getMessage(),
                    getFragmentManager());
        }
        return rootView;
    }

    private void printSummary() {
        adapter.setItemList(cooinList);
        adapter.notifyDataSetChanged();
        if(menu != null) menu.removeItem(R.id.open_wallet);
        LinearLayout summary = (LinearLayout) rootView.findViewById(R.id.summary);
        Map<String, Map<String, Map>> currencyMap = Wallet.getCurrencyMap();
        for(String currency : currencyMap.keySet()) {
            LinearLayout currencyData = (LinearLayout) getActivity().getLayoutInflater().inflate(
                    R.layout.wallet_currency_summary, null);
            ((LinearLayout)rootView.findViewById(R.id.summary)).addView(currencyData);
            Map<String, Map> tagInfoMap = currencyMap.get(currency);
            for(String tag : tagInfoMap.keySet()) {
                LinearLayout tagData = (LinearLayout) getActivity().getLayoutInflater().inflate(
                        R.layout.wallet_tag_summary, null);
                String contentFormatted = getString(R.string.tag_info,
                        ((BigDecimal)tagInfoMap.get(tag).get("total")).toPlainString(), currency,
                        MsgUtils.getTagVSMessage(tag, getActivity()));
                if(((BigDecimal)tagInfoMap.get(tag).get("timeLimited")).compareTo(BigDecimal.ZERO) > 0) {
                    contentFormatted = contentFormatted + " " + getString(R.string.tag_info_time_limited,
                            ((BigDecimal)tagInfoMap.get(tag).get("timeLimited")).toPlainString(),
                            currency);
                }
                contentFormatted = "<html><body style='background-color:#eeeeee;margin:0 auto;'>" +
                        "<p style='text-align:center; margin:0px;font-size:0.9em;'>" +
                        contentFormatted + "</p></body></html>";
                ((WebView)tagData.findViewById(R.id.tag_info)).loadData(
                        contentFormatted, "text/html; charset=UTF-8", null);
                ((LinearLayout)currencyData.findViewById(R.id.tag_info)).addView(tagData);
            }
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        if(!walletLoaded) menuInflater.inflate(R.menu.wallet, menu);
        this.menu = menu;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.open_wallet:
                PinDialogFragment.showWalletScreen(getFragmentManager(), broadCastId,
                        getString(R.string.enter_wallet_pin_msg), false, TypeVS.COOIN);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void isProgressDialogVisible(boolean isVisible) {
        if(isVisible) ProgressDialogFragment.showDialog(
                getString(R.string.unlocking_wallet_msg), getString(R.string.wait_msg), getFragmentManager());
        else ProgressDialogFragment.hide(getFragmentManager());
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        LOGD(TAG + ".onLongListItemClick", "id: " + id);
        return true;
    }

    public class CooinListAdapter  extends ArrayAdapter<Cooin> {

        private List<Cooin> itemList;
        private Context context;

        public CooinListAdapter(List<Cooin> itemList, Context ctx) {
            super(ctx, R.layout.cooin_card, itemList);
            this.itemList = itemList;
            this.context = ctx;
        }

        public int getCount() {
            if (itemList != null) return itemList.size();
            return 0;
        }

        public Cooin getItem(int position) {
            if (itemList != null) return itemList.get(position);
            return null;
        }

        @Override public View getView(int position, View view, ViewGroup parent) {
            Cooin cooin = itemList.get(position);
            if (view == null) {
                LayoutInflater inflater =
                        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.cooin_card, null);
            }
            //Date weekLapse = DateUtils.getDateFromPath(weekLapseStr);
            //Calendar weekLapseCalendar = Calendar.getInstance();
            //weekLapseCalendar.setTime(weekLapse);
            LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
            linearLayout.setBackgroundColor(Color.WHITE);
            TextView date_data = (TextView)view.findViewById(R.id.date_data);
            date_data.setText(DateUtils.getDayWeekDateStr(cooin.getDateFrom()));

            TextView cooin_state = (TextView) view.findViewById(R.id.cooin_state);
            cooin_state.setText(cooin.getStateMsg(getActivity()));
            cooin_state.setTextColor(cooin.getStateColor(getActivity()));

            TextView amount = (TextView) view.findViewById(R.id.amount);
            amount.setText(cooin.getAmount().toPlainString());
            amount.setTextColor(cooin.getStateColor(getActivity()));
            TextView currency = (TextView) view.findViewById(R.id.currencyCode);
            currency.setText(cooin.getCurrencyCode().toString());
            currency.setTextColor(cooin.getStateColor(getActivity()));


            if(DateUtils.getCurrentWeekPeriod().inRange(cooin.getDateTo())) {
                TextView time_limited_msg = (TextView) view.findViewById(R.id.time_limited_msg);
                time_limited_msg.setText(getString(R.string.lapse_lbl,
                        DateUtils.getDayWeekDateStr(cooin.getDateTo())));
            }
            ((TextView) view.findViewById(R.id.tag_data)).setText(MsgUtils.getTagVSMessage(
                    cooin.getSignedTagVS(), getActivity()));
            return view;
        }

        public List<Cooin> getItemList() {
            return itemList;
        }

        public void setItemList(List<Cooin> itemList) {
            this.itemList = itemList;
        }
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

}