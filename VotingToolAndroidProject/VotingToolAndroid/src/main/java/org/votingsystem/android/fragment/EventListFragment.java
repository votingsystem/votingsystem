package org.votingsystem.android.fragment;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.text.Html;
import android.util.Log;
import android.view.*;
import android.view.animation.AnimationUtils;
import android.widget.*;

import org.json.JSONObject;
import org.votingsystem.android.activity.EventPagerActivity;
import org.votingsystem.android.activity.MainActivity;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.EventVSContentProvider;
import org.votingsystem.android.service.EventVSService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.QueryData;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.GroupPosition;
import java.text.Collator;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener {

    public static final String TAG = "EventListFragment";

    private AtomicBoolean progressVisible = null;

    private static TextView searchTextView;
    private static TextView emptyResultsView;
    private static GridView gridView;
    private View mProgressContainer;
    private View mListContainer;
    private EventListAdapter mAdapter = null;
    private EventVS.State eventState = null;
    private GroupPosition groupPosition = GroupPosition.VOTING;
    private String queryStr = null;
    private static ContextVS contextVS = null;

    private Long offset = new Long(0);
    private Integer firstVisiblePosition = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equalsIgnoreCase(ContextVS.HTTP_DATA_INITIALIZED_ACTION_ID)){
                int responseStatus = intent.getIntExtra(ContextVS.HTTP_RESPONSE_STATUS_KEY, -1);
                String intentEventStateStr = intent.getStringExtra(ContextVS.STATE_KEY);
                String intentEventTypeStr = intent.getStringExtra(ContextVS.EVENT_TYPE_KEY);
                EventVS.State intentEventState = EventVS.State.valueOf(intentEventStateStr);
                TypeVS intentEventType = TypeVS.valueOf(intentEventTypeStr);
                if(intentEventStateStr == null || intentEventTypeStr == null) return;
                if(groupPosition.getTypeVS() != intentEventType || eventState != intentEventState)
                    return;
                Log.d(TAG + ".broadcastReceiver.onReceive(...)", "extras: " + intent.getExtras());
                getLoaderManager().restartLoader(ContextVS.EVENT_LIST_LOADER_ID, null,
                        EventListFragment.this);
                /* if(ResponseVS.SC_OK == responseStatus) {
                    offset = intent.getLongExtra(ContextVS.OFFSET_KEY, 0L);
                    EventVSContentProvider.setNumTotal(groupPosition.getTypeVS(),
                            intent.getLongExtra(ContextVS.NUM_TOTAL_KEY, 0L));
                } else showMessage(contextVS.getMessage("connErrorCaption"),
                        intent.getStringExtra(ContextVS.HTTP_RESPONSE_DATA_KEY));*/
            }
        }
    };

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
        contextVS = ContextVS.getInstance(getActivity().getApplicationContext());
        if(contextVS.getAccessControl() == null) {
            Intent intent = new Intent(getActivity().getApplicationContext(), MainActivity.class);
            startActivity(intent);
        }
        if (getArguments() != null) {
            String eventStateStr = getArguments().getString(ContextVS.EVENT_STATE_KEY);
            if(eventStateStr != null) eventState = EventVS.State.valueOf(eventStateStr);
            String subSystemStr = getArguments().getString(ContextVS.EVENT_TYPE_KEY);
            if(subSystemStr != null) groupPosition = GroupPosition.valueOf(subSystemStr);
            queryStr = getArguments().getString(SearchManager.QUERY);

        }
        Log.d(TAG +  ".onCreate(...)", "args: " + getArguments());
        setHasOptionsMenu(true);
        progressVisible = new AtomicBoolean(false);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(ContextVS.HTTP_DATA_INITIALIZED_ACTION_ID));
        //Prepare the loader. Either re-connect with an existing one or start a new one.
        getLoaderManager().initLoader(ContextVS.EVENT_LIST_LOADER_ID, null, this);
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

    private void showMessage(String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "caption: " + caption + "  - showMessage: " + message);
        AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
        builder.setTitle(caption).setMessage(message).show();
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
             Bundle savedInstanceState) {
        Log.d(TAG +  ".onCreateView(...)", "savedInstanceState: " + savedInstanceState);
        View rootView = inflater.inflate(R.layout.event_list_fragment, container, false);
        searchTextView = (TextView) rootView.findViewById(R.id.search_query);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
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
        emptyResultsView = (TextView) rootView.findViewById(android.R.id.empty);
        searchTextView.setVisibility(View.GONE);
        mProgressContainer = rootView.findViewById(R.id.progressContainer);
        mListContainer =  rootView.findViewById(R.id.listContainer);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG +  ".onActivityCreated(...)", "savedInstanceState: " + savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null) {
            Parcelable gridState = savedInstanceState.getParcelable(ContextVS.LIST_STATE_KEY);
            gridView.onRestoreInstanceState(gridState);
            offset = savedInstanceState.getLong(ContextVS.OFFSET_KEY);
        }
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        Log.i(TAG, ".onLongListItemClick - id: " + id);
        return true;
    }

    public void showProgressIndicator(boolean showProgress, boolean animate){
        if (progressVisible.get() == showProgress) return;
        else progressVisible.set(showProgress);
        if (progressVisible.get()) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_out));
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.INVISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_in));
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override public void onScrollStateChanged(AbsListView absListView, int i) { }

    @Override public void onScroll(AbsListView view, int firstVisibleItem,
           int visibleItemCount, int totalItemCount) {
        if (totalItemCount == 0) return ;
        /* maybe add a padding */
        boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount;
        Long numTotalEvents = EventVSContentProvider.getNumTotal(groupPosition.getTypeVS(),eventState);
        if(loadMore && !progressVisible.get() && offset < numTotalEvents &&
                totalItemCount < numTotalEvents) {
            Log.d(TAG +  ".onScroll(...)", "loadMore - firstVisibleItem: " + firstVisibleItem +
                    " - visibleItemCount:" + visibleItemCount + " - totalItemCount:" +
                    totalItemCount + " - numTotalEvents: " + numTotalEvents);
            firstVisiblePosition = firstVisibleItem;
            loadHttpItems(new Long(totalItemCount));
        }
    }

    private void loadHttpItems(Long offset) {
        showProgressIndicator(true, true);
        String serviceURL = contextVS.getAccessControl().getEventVSURL(eventState,
                groupPosition.getURLPart(), ContextVS.EVENTS_PAGE_SIZE, offset);
        Intent startIntent = new Intent(getActivity().getApplicationContext(),
                EventVSService.class);
        startIntent.putExtra(ContextVS.STATE_KEY, eventState.toString());
        startIntent.putExtra(ContextVS.OFFSET_KEY, offset);
        startIntent.putExtra(ContextVS.EVENT_TYPE_KEY, groupPosition.getTypeVS().toString());
        getActivity().startService(startIntent);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ContextVS.OFFSET_KEY, offset);
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
        Log.d(TAG +  ".onSaveInstanceState(...)", "outState: " + outState);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(TAG +  ".onCreateOptionsMenu(...)", "onCreateOptionsMenu - onCreateOptionsMenu");
        if(menu.findItem(R.id.reload) == null) inflater.inflate(R.menu.event_list_fragment, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG +  ".onOptionsItemSelected(..)", " - Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.reload:
                Log.d(TAG +  ".onOptionsItemSelected(..)", " ===== Reloading EventListFragment ==== ");
                getLoaderManager().restartLoader(ContextVS.EVENT_LIST_LOADER_ID, null, this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        Log.d(TAG +  ".onListItemClick(...)", "Clicked item - position:" + position +
                " -id: " + id);
        Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        /*Long eventId = cursor.getLong(cursor.getColumnIndex(
                RepresentativeContentProvider.ID_COL));*/
        Intent intent = new Intent(getActivity().getApplicationContext(), EventPagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        startActivity(intent);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(TAG + ".onCreateLoader(...)", "groupPosition: " + groupPosition + " - eventState: " +
            eventState);
        String selection = EventVSContentProvider.TYPE_COL + "=? AND " +
                EventVSContentProvider.STATE_COL + "= ? ";
        CursorLoader loader = new CursorLoader(this.getActivity(),
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
        if(EventVSContentProvider.getNumTotal(groupPosition.getTypeVS(), eventState) == null)
            loadHttpItems(offset);
        else {
            showProgressIndicator(false, true);
            if(firstVisiblePosition != null) cursor.moveToPosition(firstVisiblePosition);
            firstVisiblePosition = null;
            ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
        }
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

    @Override public void onStop() {
        super.onStop();
        Log.d(TAG +  ".onStop()", " - onStop - ");
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", "onDestroy");
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    public class EventListAdapter  extends CursorAdapter {

        private LayoutInflater inflater = null;

        public EventListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            inflater = LayoutInflater.from(context);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return inflater.inflate(R.layout.row_evento, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            try {
                String eventJSONData = cursor.getString(cursor.getColumnIndex(
                        EventVSContentProvider.JSON_DATA_COL));
                Log.d(TAG + ".bindView(...)", "eventJSONData: " + eventJSONData);
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
                    switch(eventVS.getState()) {
                        case ACTIVE:
                            imgView.setImageResource(R.drawable.open);
                            dateInfoStr = "<b>" + getActivity().getApplicationContext().
                                    getString(R.string.remain_lbl, DateUtils.
                                            getElpasedTimeHoursFromNow(eventVS.getDateFinish()))  +"</b>";
                            break;
                        case AWAITING:
                            imgView.setImageResource(R.drawable.pending);
                            dateInfoStr = "<b>" + getActivity().getApplicationContext().getString(
                                    R.string.inicio_lbl) + "</b>: " +
                                    DateUtils.getShortSpanishStringFromDate(eventVS.getDateBegin()) + " - " +
                                    "<b>" + getActivity().getApplicationContext().getString(
                                    R.string.fin_lbl) + "</b>: " +
                                    DateUtils.getShortSpanishStringFromDate(eventVS.getDateFinish());
                            break;
                        case CANCELLED:
                        case TERMINATED:
                            imgView.setImageResource(R.drawable.closed);
                            dateInfoStr = "<b>" + getActivity().getApplicationContext().getString(
                                    R.string.inicio_lbl) + "</b>: " +
                                    DateUtils.getShortSpanishStringFromDate(eventVS.getDateBegin()) + " - " +
                                    "<b>" + getActivity().getApplicationContext().getString(
                                    R.string.fin_lbl) + "</b>: " +
                                    DateUtils.getShortSpanishStringFromDate(eventVS.getDateFinish());
                            break;
                        default:
                            Log.d(TAG +  ".getView", "event state: " + eventVS.getState());
                    }

                    if(dateInfoStr != null) dateInfo.setText(Html.fromHtml(dateInfoStr));
                    else dateInfo.setVisibility(View.GONE);
                    if(eventVS.getUserVS() != null && !"".equals( eventVS.getUserVS().getFullName())) {
                        String authorStr =  "<b>" + getActivity().getApplicationContext().getString(
                                R.string.author_lbl) + "</b>: " + eventVS.getUserVS().getFullName();
                        author.setText(Html.fromHtml(authorStr));
                    } else author.setVisibility(View.GONE);
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }

    }

}
