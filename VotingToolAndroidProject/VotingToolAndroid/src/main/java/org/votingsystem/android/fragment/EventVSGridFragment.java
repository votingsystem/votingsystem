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
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
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
import org.votingsystem.android.activity.ActivityVS;
import org.votingsystem.android.activity.EventVSPagerActivity;
import org.votingsystem.android.contentprovider.EventVSContentProvider;
import org.votingsystem.android.service.EventVSService;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.ChildPosition;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.GroupPosition;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.ResponseVS;

import java.text.Collator;
import java.util.Comparator;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSGridFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        AbsListView.OnScrollListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = EventVSGridFragment.class.getSimpleName();

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

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(ResponseVS.SC_CONNECTION_TIMEOUT == responseVS.getStatusCode())  showHTTPError();
            ((ActivityVS)getActivity()).showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                    responseVS.getNotificationMessage());
        }
    };

    public GroupPosition getGroupPosition() {
        return groupPosition;
    }

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
        PrefUtils.registerOnSharedPreferenceChangeListener(contextVS, this);
        Log.d(TAG +  ".onCreate(...)", "args: " + getArguments() + " - loaderId: " + loaderId);
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
        Log.d(TAG +  ".onCreateView(...)", "savedInstanceState: " + savedInstanceState);
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
        Log.d(TAG +  ".onActivityCreated(...)", "savedInstanceState: " + savedInstanceState);
        super.onActivityCreated(savedInstanceState);
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
        Log.i(TAG, ".onLongListItemClick - id: " + id);
        return true;
    }


    @Override public void onScrollStateChanged(AbsListView absListView, int i) { }

    @Override public void onScroll(AbsListView view,  int firstVisibleItem,
                                   int visibleItemCount, int totalItemCount) {
        LOGD(TAG + ".onScroll(...)", "firstVisibleItem: " + firstVisibleItem +
                " - visibleItemCount: " + visibleItemCount + " - visibleItemCount: " +
                visibleItemCount + " - groupPosition: " + groupPosition + " - eventState: " +
                eventState);
        if(contextVS.getAccessControl() == null) {
            LOGD(TAG +  ".onScroll(...)", "Missing Access Control. Waiting for data");
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
            if(loadMore && !  ((ActivityVS)getActivity()).isRefreshing() && offset < numTotalEvents &&
                    cursorCount < numTotalEvents) {
                Log.d(TAG +  ".onScroll(...)", "loadMore - firstVisibleItem: " + firstVisibleItem +
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
        ((ActivityVS)getActivity()).refreshingStateChanged(false);
        if(gridView.getAdapter().getCount() == 0)
            rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
    }

    public void fetchItems(Long offset) {
        Log.d(TAG +  ".fetchItems(...)", "offset: " + offset + " - progressVisible: " +
                ((ActivityVS)getActivity()).isRefreshing() +
                " - groupPosition: " + groupPosition + " - eventState: " + eventState);
        if(((ActivityVS)getActivity()).isRefreshing()) return;
        ((ActivityVS)getActivity()).refreshingStateChanged(true);
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
        Log.d(TAG +  ".onSaveInstanceState(...)", "outState: " + outState);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG +  ".onOptionsItemSelected(..)", " - Title: " + item.getTitle() +
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
        Log.d(TAG + ".onListItemClick(...)", "Clicked item - position:" + position +
                " -id: " + id);
        //Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        Intent intent = new Intent(getActivity().getApplicationContext(), EventVSPagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        intent.putExtra(ContextVS.TYPEVS_KEY, groupPosition.getTypeVS().toString());
        intent.putExtra(ContextVS.EVENT_STATE_KEY, eventState.toString());
        startActivity(intent);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(TAG + ".onCreateLoader(...)", "groupPosition: " + groupPosition + " - eventState: " +
            eventState);
        String selection = EventVSContentProvider.TYPE_COL + "=? AND " +
                EventVSContentProvider.STATE_COL + "= ? ";
        CursorLoader loader = new CursorLoader(this.getActivity().getApplicationContext(),
                EventVSContentProvider.CONTENT_URI, null, selection,
                new String[]{groupPosition.getTypeVS().toString(), eventState.toString()}, null);
        return loader;
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG + ".onLoadFinished(...)", "groupPosition: " + groupPosition +
                " - eventState: " + eventState + " - numTotal: " + EventVSContentProvider.
                getNumTotal(groupPosition.getTypeVS(), eventState) +
                " - cursor.getCount(): " + cursor.getCount() +
                " - firstVisiblePosition: " + firstVisiblePosition);
        if(EventVSContentProvider.getNumTotal(groupPosition.getTypeVS(), eventState) == null &&
                contextVS.getAccessControl() != null)
            fetchItems(offset);
        else {
            ((ActivityVS)getActivity()).refreshingStateChanged(false);
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
        Log.d(TAG + ".onLoaderReset(...)", "onLoaderReset");
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

    @Override public void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", "onDestroy");
        PrefUtils.unregisterOnSharedPreferenceChangeListener(contextVS, this);
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

        private LayoutInflater inflater = null;

        public EventListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            inflater = LayoutInflater.from(context);
        }

        /*@Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return inflater.inflate(R.layout.row_eventvs, viewGroup, false);
        }*/

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return inflater.inflate(R.layout.row_representative, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            try {
                String eventJSONData = cursor.getString(cursor.getColumnIndex(
                        EventVSContentProvider.JSON_DATA_COL));
                EventVS eventVS = EventVS.parse(new JSONObject(eventJSONData));
                LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                linearLayout.setBackgroundColor(Color.WHITE);
                TextView representativeName = (TextView)view.findViewById(R.id.representative_name);
                TextView delegationInfo = (TextView) view.findViewById(
                        R.id.representative_delegations);
                representativeName.setText("eventId: " + eventVS.getId().toString());
                delegationInfo.setText(eventVS.getTypeVS() + " - " + eventVS.getState());
                //ImageView imgView = (ImageView)view.findViewById(R.id.representative_icon);
                //imgView.setImageDrawable();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }

        /*@Override public void bindView(View view, Context context, Cursor cursor) {
            try {
                String eventJSONData = cursor.getString(cursor.getColumnIndex(
                        EventVSContentProvider.JSON_DATA_COL));
                EventVS eventVS = EventVS.parse(new JSONObject(eventJSONData));
                if (eventVS != null) {
                    LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                    linearLayout.setBackgroundColor(Color.WHITE);
                    TextView subject = (TextView) view.findViewById(R.id.event_subject);
                    TextView dateInfo = (TextView) view.findViewById(R.id.event_date_info);
                    TextView author = (TextView) view.findViewById(R.id.event_author);
                    subject.setText(eventVS.getSubject());
                    String dateInfoStr = null;
                    ImageView imgView = (ImageView)view.findViewById(R.id.event_icon);
                    Log.d(TAG + ".bindView(...)", "cursor.getPosition(): "  + cursor.getPosition() +
                        " - eventState:"+ eventVS.getState()+ " - eventJSONData: " + eventJSONData);
                    switch(eventVS.getState()) {
                        case ACTIVE:
                            imgView.setImageResource(R.drawable.open);
                            dateInfoStr = "<b>" + getActivity().getApplicationContext().
                                    getString(R.string.remain_lbl, DateUtils.
                                            getElapsedTimeStr(eventVS.getDateFinish()))  +"</b>";
                            break;
                        case PENDING:
                            imgView.setImageResource(R.drawable.pending);
                            dateInfoStr = "<b>" + getActivity().getApplicationContext().getString(
                                    R.string.init_lbl) + "</b>: " +
                                    DateUtils.getDateWithDayWeek(eventVS.getDateBegin()) + " - " +
                                    "<b>" + getActivity().getApplicationContext().getString(
                                    R.string.finish_lbl) + "</b>: " +
                                    DateUtils.getDateWithDayWeek(eventVS.getDateFinish());
                            break;
                        case CANCELLED:
                        case TERMINATED:
                            imgView.setImageResource(R.drawable.closed);
                            dateInfoStr = "<b>" + getActivity().getApplicationContext().getString(
                                    R.string.init_lbl) + "</b>: " +
                                    DateUtils.getDateWithDayWeek(eventVS.getDateBegin()) + " - " +
                                    "<b>" + getActivity().getApplicationContext().getString(
                                    R.string.finish_lbl) + "</b>: " +
                                    DateUtils.getDateWithDayWeek(eventVS.getDateFinish());
                            break;
                        default:
                            Log.d(TAG +  ".bindView(...)", "unknown event state: " +
                                    eventVS.getState());
                    }
                    if(dateInfoStr != null) dateInfo.setText(Html.fromHtml(dateInfoStr));
                    else dateInfo.setVisibility(View.GONE);
                    if(eventVS.getUserVS() != null && !eventVS.getUserVS().getFullName().isEmpty()) {
                        String authorStr =  "<b>" + getActivity().getApplicationContext().getString(
                                R.string.author_lbl) + "</b>: " + eventVS.getUserVS().getFullName();
                        author.setText(Html.fromHtml(authorStr));
                    }
                } else Log.d(TAG +  ".bindView(...)", "Event null");
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }*/
    }

}
