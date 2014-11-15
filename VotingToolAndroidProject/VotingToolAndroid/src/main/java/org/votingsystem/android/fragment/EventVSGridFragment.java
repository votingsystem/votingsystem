package org.votingsystem.android.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.EventVSPagerActivity;
import org.votingsystem.android.contentprovider.EventVSContentProvider;
import org.votingsystem.android.service.EventVSService;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.ChildPosition;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.GroupPosition;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
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

    private ModalProgressDialogFragment progressDialog;
    private View rootView;
    //private TextView searchTextView;
    private GridView gridView;
    //private ListView gridView;
    private EventListAdapter mAdapter = null;
    private EventVS.State eventState = null;
    private GroupPosition groupPosition = GroupPosition.VOTING;
    private ChildPosition childPosition = null;
    private String queryStr = null;
    private AppContextVS contextVS = null;
    private Long offset = new Long(0);
    private Integer firstVisiblePosition = null;
    private int loaderId = -1;
    private String broadCastId = null;
    private AtomicBoolean isProgressDialogVisible = new AtomicBoolean(false);

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(ResponseVS.SC_CONNECTION_TIMEOUT == responseVS.getStatusCode())  showHTTPError();
            MessageDialogFragment.showDialog(responseVS, getFragmentManager());
        }
    };

    public EventVS.State getState() {
        return eventState;
    }

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<EventVS> ALPHA_COMPARATOR = new Comparator<EventVS>() {
        private final Collator sCollator = Collator.getInstance();
        @Override public int compare(EventVS object1, EventVS object2) {
            return sCollator.compare(object1.getSubject(), object2.getSubject());
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        eventState = (EventVS.State) getArguments().getSerializable(ContextVS.EVENT_STATE_KEY);
        groupPosition = (GroupPosition) getArguments().getSerializable(ContextVS.TYPEVS_KEY);
        childPosition = (ChildPosition)getArguments().getSerializable(ContextVS.CHILD_POSITION_KEY);
        queryStr = getArguments().getString(SearchManager.QUERY);
        loaderId = groupPosition.getLoaderId(childPosition.getPosition());
        PrefUtils.registerPreferenceChangeListener(contextVS, this);
        LOGD(TAG +  ".onCreate", "args: " + getArguments() + " - loaderId: " + loaderId);
        setHasOptionsMenu(true);
    };

    /*
    if(queryString != null) {
                    String url = contextVS.getAccessControl().getSearchServiceURL(
                            0, contextVS.EVENTS_PAGE_SIZE);
                    QueryData queryData = new QueryData(
                            groupPosition.getSubsystem(), eventState, queryString);
                    responseVS = HttpHelper.sendData(
                            queryData.toJSON().toString().getBytes(), null, url);
                    searchTextView.setText(Html.fromHtml(getContext().getString(
                            R.string.search_query_info_msg, queryString)));
                    searchTextView.setVisibility(View.VISIBLE);
                }
     */

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
             Bundle savedInstanceState) {
        LOGD(TAG +  ".onCreateView", "savedInstanceState: " + savedInstanceState);
        rootView = inflater.inflate(R.layout.eventvs_grid, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        //gridView = (ListView) rootView.findViewById(android.R.id.list);
        mAdapter = new EventListAdapter(getActivity().getApplicationContext(), null,false);
        gridView.setAdapter(mAdapter);
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v,pos,id);
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick(parent, v, position, id);
            }
        });
        gridView.setOnScrollListener(this);
        broadCastId = EventVSGridFragment.class.getSimpleName() + "_" + groupPosition.toString() +
                "_" + eventState.toString();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        LOGD(TAG +  ".onActivityCreated", "savedInstanceState: " + savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(loaderId, null, this);
        if(savedInstanceState != null) {
            Parcelable gridState = savedInstanceState.getParcelable(ContextVS.LIST_STATE_KEY);
            gridView.onRestoreInstanceState(gridState);
            offset = savedInstanceState.getLong(ContextVS.OFFSET_KEY);
            if(savedInstanceState.getBoolean(ContextVS.LOADING_KEY, false))
                setProgressDialogVisible(true);
        } else setProgressDialogVisible(true);
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        LOGD(TAG, ".onLongListItemClick - id: " + id);
        return true;
    }

    private void setProgressDialogVisible(boolean isVisible) {
        isProgressDialogVisible.set(isVisible);
        if(isVisible){
            if(progressDialog != null && progressDialog.isVisible()) return;
            getActivity().runOnUiThread(new Runnable() {
                @Override public void run() {
                    progressDialog = ModalProgressDialogFragment.showDialog(
                            getString(R.string.loading_data_msg),
                            getString(R.string.loading_info_msg),
                            getFragmentManager());
                }
            });
        } else if(progressDialog != null) {
            //bug, without Handler triggers 'Can not perform this action inside of onLoadFinished'
            new Handler(){
                @Override public void handleMessage(Message msg) {progressDialog.dismiss();}
            }.sendEmptyMessage(UIUtils.EMPTY_MESSAGE);
        }
    }

    @Override public void onScrollStateChanged(AbsListView absListView, int i) { }

    @Override public void onScroll(AbsListView view,  int firstVisibleItem,
                                   int visibleItemCount, int totalItemCount) {
        /*LOGD(TAG + ".onScroll", "firstVisibleItem: " + firstVisibleItem +
                " - visibleItemCount: " + visibleItemCount + " - visibleItemCount: " +
                visibleItemCount + " - groupPosition: " + groupPosition + " - eventState: " +
                eventState);*/
        if(contextVS.getAccessControl() == null) {
            LOGD(TAG +  ".onScroll", "Missing Access Control. Waiting for data");
            return;
        }
        if (totalItemCount == 0 || firstVisibleItem == 0) return ;
        /* maybe add a padding */
        boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount;
        Long numTotalEvents = EventVSContentProvider.getNumTotal(groupPosition.getTypeVS(),
                eventState);
        if(numTotalEvents == null) fetchItems(offset);
        else {
            int cursorCount = ((CursorAdapter)gridView.getAdapter()).getCursor().getCount();
            if(loadMore && !  (isProgressDialogVisible.get() && offset < numTotalEvents &&
                    cursorCount < numTotalEvents)) {
                LOGD(TAG +  ".onScroll", "loadMore - firstVisibleItem: " + firstVisibleItem +
                        " - visibleItemCount: " + visibleItemCount + " - totalItemCount: " +
                        totalItemCount + " - numTotalEvents: " + numTotalEvents +
                        " - cursorCount: " + cursorCount + " - groupPosition: " + groupPosition +
                        " - eventState: " + eventState);
                firstVisiblePosition = firstVisibleItem;
                fetchItems(new Long(totalItemCount));
            }
        }
    }

    public Long getOffset() {
        return this.offset;
    }

    private void showHTTPError() {
        setProgressDialogVisible(false);
        if(gridView.getAdapter().getCount() == 0)
            rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
    }

    public void fetchItems(Long offset) {
        LOGD(TAG +  ".fetchItems", "offset: " + offset + " - progressVisible: " +
                isProgressDialogVisible.get() +
                " - groupPosition: " + groupPosition + " - eventState: " + eventState);
        if(isProgressDialogVisible.get()) return;
        Intent startIntent = new Intent(getActivity().getApplicationContext(),
                EventVSService.class);
        startIntent.putExtra(ContextVS.STATE_KEY, eventState);
        startIntent.putExtra(ContextVS.OFFSET_KEY, offset);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, groupPosition.getTypeVS());
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        getActivity().startService(startIntent);
    }

    public void fetchItems(EventVS.State eventState, GroupPosition groupPosition) {
        this.offset = 0L;
        this.eventState = eventState;
        this.groupPosition = groupPosition;
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
                " - ItemId: " + item.getItemId() + " - groupPosition: " + groupPosition +
                " - eventState: " + eventState);
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
        LOGD(TAG + ".onListItemClick", "Clicked item - position:" + position +
                " -id: " + id);
        //Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        Intent intent = new Intent(getActivity().getApplicationContext(), EventVSPagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        intent.putExtra(ContextVS.TYPEVS_KEY, groupPosition.getTypeVS().toString());
        intent.putExtra(ContextVS.EVENT_STATE_KEY, eventState.toString());
        startActivity(intent);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        LOGD(TAG + ".onCreateLoader", "groupPosition: " + groupPosition + " - eventState: " +
            eventState);
        String selection = EventVSContentProvider.TYPE_COL + "=? AND " +
                EventVSContentProvider.STATE_COL + "= ? ";
        CursorLoader loader = new CursorLoader(this.getActivity().getApplicationContext(),
                EventVSContentProvider.CONTENT_URI, null, selection,
                new String[]{groupPosition.getTypeVS().toString(), eventState.toString()}, null);
        return loader;
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        LOGD(TAG + ".onLoadFinished", "groupPosition: " + groupPosition +
                " - eventState: " + eventState + " - numTotal: " + EventVSContentProvider.
                getNumTotal(groupPosition.getTypeVS(), eventState) +
                " - cursor.getCount(): " + cursor.getCount() +
                " - firstVisiblePosition: " + firstVisiblePosition);
        if(EventVSContentProvider.getNumTotal(groupPosition.getTypeVS(), eventState) == null &&
                contextVS.getAccessControl() != null)
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

    @Override public void onDestroy() {
        super.onDestroy();
        PrefUtils.unregisterPreferenceChangeListener(contextVS, this);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LOGD(TAG, "onSharedPreferenceChanged - key: " + key);
        if(ContextVS.ACCESS_CONTROL_URL_KEY.equals(key)) {
            Long numTotalEvents = EventVSContentProvider.getNumTotal(groupPosition.getTypeVS(),
                    eventState);
            if(numTotalEvents == null) fetchItems(eventState, groupPosition);
        }
    }

    public class EventListAdapter  extends CursorAdapter {

        public EventListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return LayoutInflater.from(context).inflate(R.layout.election_card, viewGroup, false);
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
