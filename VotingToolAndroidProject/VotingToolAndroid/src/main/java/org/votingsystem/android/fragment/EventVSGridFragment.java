package org.votingsystem.android.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.EventVSPagerActivity;
import org.votingsystem.android.contentprovider.EventVSContentProvider;
import org.votingsystem.android.service.EventVSService;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ResponseVS;

import java.text.Collator;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSGridFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        AbsListView.OnScrollListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = EventVSGridFragment.class.getSimpleName();

    private View rootView;
    private GridView gridView;
    private EventListAdapter mAdapter = null;
    private EventVS.State eventState = null;
    private AppContextVS contextVS = null;
    private Long offset = new Long(0);
    private Integer firstVisiblePosition = null;
    private static final int loaderId = 0;
    private String broadCastId = null;
    private AtomicBoolean isProgressDialogVisible = new AtomicBoolean(false);

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(ResponseVS.SC_CONNECTION_TIMEOUT == responseVS.getStatusCode())  showHTTPError();
            if(ResponseVS.SC_OK != responseVS.getStatusCode())
                MessageDialogFragment.showDialog(responseVS, getFragmentManager());
        }
    };

    /**
     *
     *
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<EventVS> ALPHA_COMPARATOR = new Comparator<EventVS>() {
        private final Collator sCollator = Collator.getInstance();
        @Override public int compare(EventVS object1, EventVS object2) {
            return sCollator.compare(object1.getSubject(), object2.getSubject());
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
             Bundle savedInstanceState) {
        LOGD(TAG +  ".onCreateView", "savedInstanceState: " + savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        eventState = (EventVS.State) getArguments().getSerializable(ContextVS.EVENT_STATE_KEY);
        PrefUtils.registerPreferenceChangeListener(contextVS, this);
        rootView = inflater.inflate(R.layout.eventvs_grid, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        //gridView = (ListView) rootView.findViewById(android.R.id.list);
        mAdapter = new EventListAdapter(getActivity(), null,false);
        gridView.setAdapter(mAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick(parent, v, position, id);
            }
        });
        gridView.setOnScrollListener(this);
        setHasOptionsMenu(true);
        broadCastId = EventVSGridFragment.class.getSimpleName() + "_" +  "_" + eventState.toString();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        getLoaderManager().initLoader(loaderId, null, this);
        if(savedInstanceState != null) {
            Parcelable gridState = savedInstanceState.getParcelable(ContextVS.LIST_STATE_KEY);
            gridView.onRestoreInstanceState(gridState);
            offset = savedInstanceState.getLong(ContextVS.OFFSET_KEY);
        }
        return rootView;
    }

    private void setProgressDialogVisible(final boolean isVisible) {
        isProgressDialogVisible.set(isVisible);
        //bug, without Handler triggers 'Can not perform this action inside of onLoadFinished'
        new Handler(){
            @Override public void handleMessage(Message msg) {
                if (isVisible) {
                    ProgressDialogFragment.showDialog(getString(R.string.loading_info_msg),
                            getString(R.string.loading_data_msg), getFragmentManager());
                } else ProgressDialogFragment.hide(getFragmentManager());
            }
        }.sendEmptyMessage(UIUtils.EMPTY_MESSAGE);
    }

    @Override public void onScrollStateChanged(AbsListView absListView, int i) { }

    @Override public void onScroll(AbsListView view,  int firstVisibleItem,
                                   int visibleItemCount, int totalItemCount) {
        if(contextVS.getAccessControl() == null) {
            LOGD(TAG +  ".onScroll", "Missing Access Control. Waiting for data");
            Toast.makeText(getActivity(), getString(R.string.waiting_for_access_control_connection),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (totalItemCount == 0 || firstVisibleItem == 0) return ;
        boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount;
        Long numTotalEvents = EventVSContentProvider.getNumTotal(eventState);
        if(numTotalEvents == null) fetchItems(offset);
        else {
            int cursorCount = ((CursorAdapter)gridView.getAdapter()).getCursor().getCount();
            if(loadMore && !isProgressDialogVisible.get() && (cursorCount < numTotalEvents)) {
                LOGD(TAG +  ".onScroll", "loadMore - firstVisibleItem: " + firstVisibleItem +
                        " - visibleItemCount: " + visibleItemCount + " - totalItemCount: " +
                        totalItemCount + " - numTotalEvents: " + numTotalEvents +
                        " - cursorCount: " + cursorCount + " - eventState: " + eventState);
                firstVisiblePosition = firstVisibleItem;
                fetchItems(new Long(totalItemCount));
            }
        }
    }

    public Long getOffset() {
        return this.offset;
    }

    public EventVS.State getState() {
        return eventState;
    }

    private void showHTTPError() {
        setProgressDialogVisible(false);
        if(gridView.getAdapter().getCount() == 0)
            rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
    }

    public void fetchItems(Long offset) {
        LOGD(TAG +  ".fetchItems", "offset: " + offset + " - progressVisible: " +
                isProgressDialogVisible.get()  + " - eventState: " + eventState);
        if(isProgressDialogVisible.get()) return;
        setProgressDialogVisible(true);
        Intent startIntent = new Intent(getActivity(), EventVSService.class);
        startIntent.putExtra(ContextVS.STATE_KEY, eventState);
        startIntent.putExtra(ContextVS.OFFSET_KEY, offset);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        getActivity().startService(startIntent);
    }

    public void fetchItems(EventVS.State eventState) {
        this.offset = 0L;
        this.eventState = eventState;
        getLoaderManager().restartLoader(loaderId, null, this);
        ((CursorAdapter)gridView.getAdapter()).notifyDataSetChanged();
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ContextVS.OFFSET_KEY, offset);
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
        LOGD(TAG +  ".onSaveInstanceState", "outState: " + outState);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG +  ".onOptionsItemSelected(..)", " - Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId() + " - eventState: " + eventState);
        switch (item.getItemId()) {
            /*case R.id.reload:
                fetchItems(offset);
                //rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
                //gridView.invalidateViews();
                //getLoaderManager().restartLoader(loaderId, null, this);
                //((CursorAdapter)gridView.getAdapter()).notifyDataSetChanged();
                return true;*/
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        LOGD(TAG + ".onListItemClick", "position:" + position + " - id: " + id);
        //Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        Intent intent = new Intent(getActivity(), EventVSPagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        intent.putExtra(ContextVS.EVENT_STATE_KEY, eventState.toString());
        startActivity(intent);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        LOGD(TAG + ".onCreateLoader", "eventState: " + eventState);
        String selection = EventVSContentProvider.TYPE_COL + "=? AND " +
                EventVSContentProvider.STATE_COL + "= ? ";
        CursorLoader loader = new CursorLoader(this.getActivity(),
                EventVSContentProvider.CONTENT_URI, null, selection,
                new String[]{TypeVS.VOTING_EVENT.toString(), eventState.toString()}, null);
        return loader;
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        LOGD(TAG + ".onLoadFinished", "eventState: " + eventState +
                " - numTotal: " + EventVSContentProvider.getNumTotal(eventState) +
                " - cursor.getCount(): " + cursor.getCount() +
                " - firstVisiblePosition: " + firstVisiblePosition);
        if((EventVSContentProvider.getNumTotal(eventState) == null)
                && contextVS.getAccessControl() != null)
            fetchItems(offset);
        else {
            //bug, without thread triggers 'Can not perform this action inside of onLoadFinished'
            setProgressDialogVisible(false);
            if(firstVisiblePosition != null) cursor.moveToPosition(firstVisiblePosition);
            else cursor.moveToFirst();
            firstVisiblePosition = null;
            ((CursorAdapter)gridView.getAdapter()).changeCursor(cursor);
            if(cursor.getCount() == 0) {
                rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
            } else rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
        }
    }

    @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        LOGD(TAG + ".onLoaderReset", "onLoaderReset");
        ((CursorAdapter)gridView.getAdapter()).swapCursor(null);
    }

    @Override public void onPause() {
        super.onPause();
        PrefUtils.unregisterPreferenceChangeListener(this, contextVS);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(ContextVS.ACCESS_CONTROL_URL_KEY.equals(key)) {
            LOGD(TAG + ".onSharedPreferenceChanged", "key: " + key);
            Long numTotalEvents = EventVSContentProvider.getNumTotal(eventState);
            if(numTotalEvents == null) fetchItems(eventState);
        }
    }

    public class EventListAdapter  extends CursorAdapter {

        public EventListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return LayoutInflater.from(context).inflate(R.layout.eventvs_card, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            try {
                String eventJSONData = cursor.getString(cursor.getColumnIndex(
                        EventVSContentProvider.JSON_DATA_COL));
                EventVS eventVS = EventVS.parse(new JSONObject(eventJSONData));
                int state_color = R.color.frg_vs;
                String tameInfoMsg = null;
                switch(eventVS.getState()) {
                    case ACTIVE:
                        state_color = R.color.active_vs;
                        tameInfoMsg = getString(R.string.remaining_lbl, DateUtils.
                                getElapsedTimeStr(eventVS.getDateFinish()));
                        break;
                    case CANCELLED:
                    case TERMINATED:
                        state_color = R.color.terminated_vs;
                        tameInfoMsg = getString(R.string.voting_closed_lbl);
                        break;
                    case PENDING:
                        state_color = R.color.pending_vs;
                        tameInfoMsg = getString(R.string.pending_lbl, DateUtils.
                                getElapsedTimeStr(eventVS.getDateBegin()));
                        break;
                }
                if(eventVS.getUserVS() != null && !eventVS.getUserVS().getFullName().isEmpty()) {
                    ((TextView)view.findViewById(R.id.publisher)).setText(eventVS.getUserVS().getFullName());
                }
                ((LinearLayout)view.findViewById(R.id.subject_layout)).setBackgroundColor(
                        getResources().getColor(state_color));
                ((TextView)view.findViewById(R.id.subject)).setText(eventVS.getSubject());
                TextView time_info = ((TextView)view.findViewById(R.id.time_info));
                time_info.setText(tameInfoMsg);
                time_info.setTextColor(getResources().getColor(state_color));
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}