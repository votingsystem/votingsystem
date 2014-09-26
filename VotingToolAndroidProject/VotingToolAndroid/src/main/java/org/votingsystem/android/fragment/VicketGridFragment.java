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
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.ActivityVS;
import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.activity.VicketPagerActivity;
import org.votingsystem.android.contentprovider.VicketContentProvider;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.Vicket;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ObjectUtils;

import java.text.Collator;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

public class VicketGridFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener {

    public static final String TAG = VicketGridFragment.class.getSimpleName();

    private View rootView;
    private GridView gridView;
    private VicketListAdapter adapter = null;
    private String queryStr = null;
    private AppContextVS contextVS = null;
    private Long offset = new Long(0);
    private Integer firstVisiblePosition = null;
    private String broadCastId = VicketGridFragment.class.getSimpleName();
    private int loaderId = -1;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver.onReceive(...)", "extras(): " + intent.getExtras());
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
            ((ActivityVS)getActivity()).showProgress(false, false);
        }
        }
    };

    private void launchUpdateUserInfoService() {

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
        Log.d(TAG +  ".onCreate(...)", "args: " + getArguments() + " - loaderId: " + loaderId);
        setHasOptionsMenu(true);
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        Log.d(TAG +  ".onCreateView(..)", "savedInstanceState: " + savedInstanceState);
        ((FragmentContainerActivity)getActivity()).setTitle(getString(R.string.vicket_lbl), null,
                R.drawable.fa_money_32);
        rootView = inflater.inflate(R.layout.generic_grid_fragment, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        adapter = new VicketListAdapter(getActivity().getApplicationContext(), null,false);
        gridView.setAdapter(adapter);
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v, pos, id);
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick(parent, v, position, id);
            }
        });
        gridView.setOnScrollListener(this);
        ((ActivityVS)getActivity()).showProgress(true, true);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG +  ".onActivityCreated(...)", "savedInstanceState: " + savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        //Prepare the loader. Either re-connect with an existing one or start a new one.
        getLoaderManager().initLoader(loaderId, null, this);
        if(savedInstanceState != null) {
            Parcelable gridState = savedInstanceState.getParcelable(ContextVS.LIST_STATE_KEY);
            gridView.onRestoreInstanceState(gridState);
            offset = savedInstanceState.getLong(ContextVS.OFFSET_KEY);
            if(savedInstanceState.getBoolean(ContextVS.LOADING_KEY, false))
                ((ActivityVS)getActivity()).showProgress(true, true);
        }
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        Log.d(TAG + ".onLongListItemClick(...)", "id: " + id);
        return true;
    }

    @Override public void onScrollStateChanged(AbsListView absListView, int i) { }

    @Override public void onScroll(AbsListView view, int firstVisibleItem,
               int visibleItemCount, int totalItemCount) { }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.vicket_user_info, menu);
        menu.setGroupVisible(R.id.general_items, false);
        menu.removeItem(R.id.search_item);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG +  ".onOptionsItemSelected(..)", "Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.reload:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        Log.d(TAG +  ".onListItemClick(...)", "Clicked item - position:" + position +" -id: " + id);
        Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        Intent intent = new Intent(getActivity().getApplicationContext(),VicketPagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        startActivity(intent);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(TAG + ".onCreateLoader(...)", "");
        String weekLapse = DateUtils.getDirPath(DateUtils.getMonday(Calendar.getInstance()).getTime());
        String selection = VicketContentProvider.WEEK_LAPSE_COL + " =? ";
        CursorLoader loader = new CursorLoader(this.getActivity(), VicketContentProvider.CONTENT_URI,
                null, selection, new String[]{weekLapse}, null);
        return loader;
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG + ".onLoadFinished(...)", " - cursor.getCount(): " + cursor.getCount() +
                " - firstVisiblePosition: " + firstVisiblePosition);
        ((ActivityVS)getActivity()).showProgress(false, true);
        if(firstVisiblePosition != null) cursor.moveToPosition(firstVisiblePosition);
        firstVisiblePosition = null;
        ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
        if(cursor.getCount() == 0) {
            rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        } else rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
    }

    @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Log.d(TAG + ".onLoaderReset(...)", "");
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
            Log.d(TAG + ".onAttach()", "activity: " + activity.getClass().getName() +
                    " - query: " + query + " - activity: ");
        }
    }

    public class VicketListAdapter extends CursorAdapter {

        private LayoutInflater inflater = null;

        public VicketListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            inflater = LayoutInflater.from(context);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return inflater.inflate(R.layout.row_vicket, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            if(cursor != null) {
                byte[] serializedVicket = cursor.getBlob(cursor.getColumnIndex(
                        VicketContentProvider.SERIALIZED_OBJECT_COL));
                Long vicketId = cursor.getLong(cursor.getColumnIndex(
                        VicketContentProvider.ID_COL));

                Vicket vicket = (Vicket) ObjectUtils.
                        deSerializeObject(serializedVicket);
                String weekLapseStr = cursor.getString(cursor.getColumnIndex(
                        VicketContentProvider.WEEK_LAPSE_COL));
                Date weekLapse = DateUtils.getDateFromDirPath(weekLapseStr);
                Calendar weekLapseCalendar = Calendar.getInstance();
                weekLapseCalendar.setTime(weekLapse);
                LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                linearLayout.setBackgroundColor(Color.WHITE);
                TextView date_data = (TextView)view.findViewById(R.id.date_data);
                String dateData = getString(R.string.vicket_data_info_lbl,
                        DateUtils.getLongDate_Es(vicket.getValidFrom()),
                        DateUtils.getLongDate_Es(vicket.getValidTo()));
                date_data.setText(DateUtils.getLongDate_Es(vicket.getValidFrom()));

                TextView vicket_state = (TextView) view.findViewById(R.id.vicket_state);
                vicket_state.setText(vicket.getState().toString() + " - ID: " + vicketId);
                TextView week_lapse = (TextView) view.findViewById(R.id.week_lapse);
                week_lapse.setText(weekLapseStr);

                TextView amount = (TextView) view.findViewById(R.id.amount);
                amount.setText(vicket.getAmount().toPlainString());
                TextView currency = (TextView) view.findViewById(R.id.currencyCode);
                currency.setText(vicket.getCurrencyCode().toString());
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ContextVS.OFFSET_KEY, offset);
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
        Log.d(TAG +  ".onSaveInstanceState(...)", "outState: " + outState);
    }

    @Override public void onResume() {
        Log.d(TAG + ".onResume() ", "");
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }
}