package org.votingsystem.android.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.ActivityVS;
import org.votingsystem.android.activity.TransactionVSPagerActivity;
import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.android.service.VicketService;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.ResponseVS;

import java.text.Collator;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static org.votingsystem.android.util.LogUtils.LOGD;

public class TransactionVSGridFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener {

    public static final String TAG = TransactionVSGridFragment.class.getSimpleName();

    private View rootView;
    private GridView gridView;
    private TransactionVSListAdapter adapter = null;
    private String queryStr = null;
    private AppContextVS contextVS = null;
    private Long offset = new Long(0);
    private Integer firstVisiblePosition = null;
    private String broadCastId = TransactionVSGridFragment.class.getName();
    private int loaderId = -1;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            switch(responseVS.getTypeVS()) {
                case VICKET_USER_INFO:
                    launchUpdateUserInfoService();
                    break;
            }
        } else {
            switch(responseVS.getTypeVS()) {
                case VICKET_USER_INFO:
                    break;
            }
            ((ActivityVS)getActivity()).refreshingStateChanged(false);
        }
        }
    };

    public static Fragment newInstance(Bundle args) {
        TransactionVSGridFragment fragment = new TransactionVSGridFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void launchUpdateUserInfoService() {
        LOGD(TAG + ".launchUpdateUserInfoService", "");
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    VicketService.class);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.VICKET_USER_INFO);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            ((ActivityVS)getActivity()).refreshingStateChanged(true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<UserVS> ALPHA_COMPARATOR = new Comparator<UserVS>() {
        private final Collator sCollator = Collator.getInstance();
        @Override public int compare(UserVS object1, UserVS object2) {
            return sCollator.compare(object1.getName(), object2.getName());
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        loaderId = NavigatorDrawerOptionsAdapter.GroupPosition.VICKETS.getLoaderId(0);
        queryStr = getArguments().getString(SearchManager.QUERY);
        LOGD(TAG +  ".onCreate(...)", "args: " + getArguments() + " - loaderId: " + loaderId);
        setHasOptionsMenu(true);
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG +  ".onCreateView", "savedInstanceState: " + savedInstanceState);
        rootView = inflater.inflate(R.layout.generic_grid, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        adapter = new TransactionVSListAdapter(getActivity().getApplicationContext(), null,false);
        gridView.setAdapter(adapter);
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v, pos, id);
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick(parent, v, position, id);
            }
        });
        gridView.setOnScrollListener(this);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        LOGD(TAG +  ".onActivityCreated(...)", "savedInstanceState: " + savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        //Prepare the loader. Either re-connect with an existing one or start a new one.
        getLoaderManager().initLoader(loaderId, null, this);
        if(savedInstanceState != null) {
            Parcelable gridState = savedInstanceState.getParcelable(ContextVS.LIST_STATE_KEY);
            gridView.onRestoreInstanceState(gridState);
            offset = savedInstanceState.getLong(ContextVS.OFFSET_KEY);
            if(savedInstanceState.getBoolean(ContextVS.LOADING_KEY, false))
                ((ActivityVS)getActivity()).refreshingStateChanged(true);
        }
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        LOGD(TAG + ".onLongListItemClick(...)", "id: " + id);
        return true;
    }

    @Override public void onScrollStateChanged(AbsListView absListView, int i) { }

    @Override public void onScroll(AbsListView view, int firstVisibleItem,
               int visibleItemCount, int totalItemCount) { }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.vicket_user_info, menu);

        menu.setGroupVisible(R.id.general_items, false);
        menu.removeItem(R.id.search_item);
        List<String> transactionWeekList =TransactionVSContentProvider.getTransactionWeekList (
                (AppContextVS)getActivity().getApplicationContext());
        for(final String weekLbl: transactionWeekList) {
            MenuItem item = menu.add (weekLbl);
            item.setOnMenuItemClickListener (new MenuItem.OnMenuItemClickListener(){
                @Override public boolean onMenuItemClick (MenuItem item){
                    LOGD(TAG +  ".onMenuItemClick(..)", "click on weekLbl: " + weekLbl);
                    return true;
                }
            });
        }

    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG +  ".onOptionsItemSelected(..)", "Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.update_signers_info:
                PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                        getString(R.string.update_user_info_pin_msg), false, TypeVS.VICKET_USER_INFO);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        LOGD(TAG +  ".onListItemClick", "Clicked item - position:" + position +" -id: " + id);
        Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        Intent intent = new Intent(getActivity().getApplicationContext(),
                TransactionVSPagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        startActivity(intent);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        LOGD(TAG + ".onCreateLoader", "");
        String selection = TransactionVSContentProvider.WEEK_LAPSE_COL + " =? ";
        CursorLoader loader = new CursorLoader(this.getActivity(),
                TransactionVSContentProvider.CONTENT_URI, null, selection,
                new String[]{contextVS.getCurrentWeekLapseId()}, null);
        return loader;
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        LOGD(TAG + ".onLoadFinished", " - cursor.getCount(): " + cursor.getCount() +
                " - firstVisiblePosition: " + firstVisiblePosition);
        ((ActivityVS)getActivity()).refreshingStateChanged(false);
        if(firstVisiblePosition != null) cursor.moveToPosition(firstVisiblePosition);
        firstVisiblePosition = null;
        ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
        if(cursor.getCount() == 0) {
            rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        } else rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
    }

    @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        LOGD(TAG + ".onLoaderReset", "");
        ((CursorAdapter)gridView.getAdapter()).swapCursor(null);
    }

    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        Intent intent = activity.getIntent();
        if(intent != null) {
            String query = null;
            if (Intent.ACTION_SEARCH.equals(intent)) {
                query = intent.getStringExtra(SearchManager.QUERY);
            }
            LOGD(TAG + ".onAttach()", "activity: " + activity.getClass().getName() +
                    " - query: " + query + " - activity: ");
        }
    }

    public class TransactionVSListAdapter  extends CursorAdapter {

        private LayoutInflater inflater = null;

        public TransactionVSListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            inflater = LayoutInflater.from(context);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return inflater.inflate(R.layout.row_transactionvs, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            if(cursor != null) {
                byte[] serializedTransactionVS = cursor.getBlob(cursor.getColumnIndex(
                        TransactionVSContentProvider.SERIALIZED_OBJECT_COL));
                if(serializedTransactionVS != null) {
                    TransactionVS transactionVS = (TransactionVS) ObjectUtils.
                            deSerializeObject(serializedTransactionVS);
                    String weekLapseStr = cursor.getString(cursor.getColumnIndex(
                            TransactionVSContentProvider.WEEK_LAPSE_COL));
                    Date weekLapse = DateUtils.getDateFromPath(weekLapseStr);
                    Calendar weekLapseCalendar = Calendar.getInstance();
                    weekLapseCalendar.setTime(weekLapse);
                    LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                    linearLayout.setBackgroundColor(Color.WHITE);
                    TextView transaction_type = (TextView) view.findViewById(R.id.transaction_type);
                    transaction_type.setText(transactionVS.getDescription(getActivity().getApplicationContext()));
                    TextView week_lapse = (TextView) view.findViewById(R.id.week_lapse);
                    week_lapse.setText(DateUtils.getDayWeekDateStr(transactionVS.getDateCreated()));

                    TextView amount = (TextView) view.findViewById(R.id.amount);
                    amount.setText(transactionVS.getAmount().toPlainString());
                    TextView currency = (TextView) view.findViewById(R.id.currencyCode);
                    currency.setText(transactionVS.getCurrencyCode());

                    ((ImageView)view.findViewById(R.id.transaction_icon)).setImageResource(
                            transactionVS.getIconId(getActivity().getApplicationContext()));
                }
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ContextVS.OFFSET_KEY, offset);
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
        LOGD(TAG +  ".onSaveInstanceState(...)", "outState: " + outState);
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

}