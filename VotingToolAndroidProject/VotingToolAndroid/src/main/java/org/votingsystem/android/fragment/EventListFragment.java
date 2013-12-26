package org.votingsystem.android.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.util.Log;
import android.view.*;
import android.view.animation.AnimationUtils;
import android.widget.*;

import org.votingsystem.android.activity.EventPagerActivity;
import org.votingsystem.android.activity.MainActivity;
import org.votingsystem.android.R;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVSResponse;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.QueryData;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.GroupPosition;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<EventVS>> {

    public static final String TAG = "EventListFragment";

    public static final String EVENT_STATE_KEY = "eventState";
    public static final String EVENT_TYPE_KEY  = "eventType";
    private static final int EVENT_LIST_LOADER_ID = 0;


    private static String errorLoadingEventsMsg = null;
    private static TextView searchTextView;
    private static TextView emptyResultsView;
    private static ListView listView;
    private boolean mListShown;
    private View mProgressContainer;
    private View mListContainer;
    private EventListAdapter mAdapter = null;
    private EventVS.State eventState = null;
    private GroupPosition groupPosition = GroupPosition.VOTING;
    private String queryStr = null;
    private int offset = 0;
    private static ContextVS contextVS = null;

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
        contextVS = contextVS.getInstance(getActivity().getBaseContext());
        if(contextVS.getAccessControl() == null) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
        Bundle args = getArguments();
        String eventStateStr = null;
        String subSystemStr = "";
        if (args != null) {
            eventStateStr = args.getString(EVENT_STATE_KEY);
            if(eventStateStr != null) eventState = EventVS.State.valueOf(eventStateStr);
            subSystemStr = args.getString(EVENT_TYPE_KEY);
            if(subSystemStr != null) groupPosition = GroupPosition.valueOf(subSystemStr);
            queryStr = args.getString(SearchManager.QUERY);
            offset = args.getInt(ContextVS.OFFSET_KEY);
        }
        Log.d(TAG +  ".onCreate(..)", "args: " + args);
        setRetainInstance(true);
    };

    public NavigatorDrawerOptionsAdapter.ChildPosition childPosition = null;

    public void setChildPosition(NavigatorDrawerOptionsAdapter.ChildPosition childPosition) {
        this.childPosition = childPosition;
    }

    public NavigatorDrawerOptionsAdapter.ChildPosition getChildPosition() {
        return childPosition;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG +  ".onCreateView(...)", "savedInstanceState: " + savedInstanceState);
        View rootView = inflater.inflate(R.layout.event_list_fragment, container, false);
        searchTextView = (TextView) rootView.findViewById(R.id.search_query);
        listView = (ListView) rootView.findViewById(android.R.id.list);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v,pos,id);
            }
        });
        emptyResultsView = (TextView) rootView.findViewById(android.R.id.empty);
        searchTextView.setVisibility(View.GONE);
        mProgressContainer = rootView.findViewById(R.id.progressContainer);
        mListContainer =  rootView.findViewById(R.id.listContainer);
        mListShown = true;
        setHasOptionsMenu(true);
        return rootView;
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {//context menu
        Log.i(TAG, ".onLongListItemClick - id: " + id);
        return true;
    }

    public void setListShown(boolean shown, boolean animate){
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.INVISIBLE);
        }
    }
    public void setListShown(boolean shown){
        setListShown(shown, true);
    }
    public void setListShownNoAnimation(boolean shown) {
        setListShown(shown, false);
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG +  ".onActivityCreated(...)", "onActivityCreated - savedInstanceState: " +
                savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        getView().setBackgroundColor(Color.WHITE);
        //if(savedInstanceState != null) return;
        mAdapter = new EventListAdapter(getActivity());
        setListAdapter(mAdapter);
        // Start out with a progress indicator.
        setListShown(false);
        // Prepare the loader.  Either re-connect with an existing one or start a new one.
        getLoaderManager().initLoader(EVENT_LIST_LOADER_ID, null, this);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
                getLoaderManager().restartLoader(0, null, this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(TAG +  ".onListItemClick", "Item clicked: " + id);
        EventVS event = ((EventVS) getListAdapter().getItem(position));
        contextVS.setEvent(event);
        contextVS.setEventList(mAdapter.getEvents());
        Intent intent = new Intent(getActivity(), EventPagerActivity.class);
        startActivity(intent);
    }

    @Override public Loader<List<EventVS>> onCreateLoader(int id, Bundle args) {
        Log.d(TAG +  ".onCreateLoader(..)", " - eventState: " + eventState + " - groupPosition: " + groupPosition);
        return new EventListLoader(getActivity(), groupPosition, eventState, queryStr, offset);
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

    @Override public void onLoadFinished(Loader<List<EventVS>> loader, List<EventVS> data) {
        Log.i(TAG +  ".onLoadFinished", " - onLoadFinished - data: " +
                ((data == null) ? "NULL":data.size()));
        if(errorLoadingEventsMsg == null) {

            ((EventListAdapter)getListAdapter()).setData(data);
        } else {
            //setEmptyText(getString(R.string.connection_error_msg));
            //emptyResultsView.setText(getString(R.string.connection_error_msg));
            errorLoadingEventsMsg = null;
        }
        if (isResumed()) {
            setListShown(true);//Can't be used with a custom content view
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override public void onLoaderReset(Loader<List<EventVS>> loader) {
        Log.i(TAG +  ".onLoaderReset", " - onLoaderReset ");
        mAdapter.setData(null);
    }


    public class EventListAdapter extends ArrayAdapter<EventVS> {

        private final LayoutInflater mInflater;

        List<EventVS> events;

        public EventListAdapter(Context context) {
            super(context, R.layout.row_evento);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<EventVS> data) {
            clear();
            if (data != null) {
                if(events == null) {
                    events = new ArrayList<EventVS>();
                }
                events.addAll(data);
                for(EventVS event : data) {
                    add(event);
                }
            }
        }

        public List<EventVS> getEvents() {
            return events;
        }

        /**
         * Populate new items in the list.
         */
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.row_evento, parent, false);
            } else {
                view = convertView;
            }
            EventVS eventVS = (EventVS) getItem(position);
            if (eventVS != null) {
                LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                linearLayout.setBackgroundColor(Color.WHITE);
                TextView subject = (TextView) view.findViewById(R.id.event_subject);
                TextView dateInfo = (TextView) view.findViewById(R.id.event_date_info);
                TextView author = (TextView) view.findViewById(R.id.event_author);
                subject.setText(eventVS.getSubject());
                String dateInfoStr = null;
                ImageView imgView = (ImageView)view.findViewById(R.id.event_icon);
                switch(eventVS.getStateEnumValue()) {
                    case ACTIVE:
                        imgView.setImageResource(R.drawable.open);
                        dateInfoStr = "<b>" + getContext().getString(R.string.remain_lbl, DateUtils.
                                getElpasedTimeHoursFromNow(eventVS.getDateFinish()))  +"</b>";
                        break;
                    case AWAITING:
                        imgView.setImageResource(R.drawable.pending);
                        dateInfoStr = "<b>" + getContext().getString(R.string.inicio_lbl) + "</b>: " +
                                DateUtils.getShortSpanishStringFromDate(eventVS.getDateBegin()) + " - " +
                                "<b>" + getContext().getString(R.string.fin_lbl) + "</b>: " +
                                DateUtils.getShortSpanishStringFromDate(eventVS.getDateFinish());
                        break;
                    case CANCELLED:
                    case TERMINATED:
                        imgView.setImageResource(R.drawable.closed);
                        dateInfoStr = "<b>" + getContext().getString(R.string.inicio_lbl) + "</b>: " +
                                DateUtils.getShortSpanishStringFromDate(eventVS.getDateBegin()) + " - " +
                                "<b>" + getContext().getString(R.string.fin_lbl) + "</b>: " +
                                DateUtils.getShortSpanishStringFromDate(eventVS.getDateFinish());
                        break;
                    default:
                        Log.d(TAG +  ".getView", "event state: " + eventVS.getStateEnumValue());
                }

                if(dateInfoStr != null) dateInfo.setText(Html.fromHtml(dateInfoStr));
                else dateInfo.setVisibility(View.GONE);
                if(eventVS.getUserVS() != null && !"".equals( eventVS.getUserVS().getFullName())) {
                    String authorStr =  "<b>" + getContext().getString(R.string.author_lbl) +
                            "</b>: " + eventVS.getUserVS().getFullName();
                    author.setText(Html.fromHtml(authorStr));
                } else author.setVisibility(View.GONE);
            }
            return view;
        }
    }


    /**
     * A custom Loader that loads events
     */
    public static class EventListLoader extends AsyncTaskLoader<List<EventVS>> {

        List<EventVS> events;
        GroupPosition groupPosition;
        EventVS.State eventState;
        String queryString;
        int offset = 0;

        public EventListLoader(Context context) {
            super(context);
        }

        public EventListLoader(Context context, GroupPosition groupPosition, EventVS.State eventState,
                               String queryString, int offset) {
            super(context);
            this.groupPosition = groupPosition;
            this.eventState = eventState;
            this.queryString = queryString;
            this.offset = offset;
        }

        /**
         * This is where the bulk of our work is done.  This function is
         * called in a background thread and should generate a new set of
         * data to be published by the loader.
         */
        @Override public List<EventVS> loadInBackground() {
            Log.d(TAG + ".EventListLoader.loadInBackground()", " - groupPosition: " + groupPosition
                    + " - eventState: " + eventState + " - queryString: " + queryString);

            List<EventVS> eventList = null;
            try {
                ResponseVS responseVS = null;
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
                } else {
                    String url = contextVS.getAccessControl().getEventVSURL(eventState,
                            groupPosition.getURLPart(), contextVS.EVENTS_PAGE_SIZE, offset);
                    responseVS = HttpHelper.getData(url, null);
                    searchTextView.setVisibility(View.GONE);
                }
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    EventVSResponse response = EventVSResponse.parse(responseVS.getMessage());
                    eventList = response.getEvents();
                } else errorLoadingEventsMsg = responseVS.getMessage();
            } catch (Exception ex) {
                Log.e(TAG + ".doInBackground", ex.getMessage(), ex);
                errorLoadingEventsMsg = getContext().getString(R.string.connection_error_msg);
            }
            return eventList;
        }

        /**
         * Called when there is new data to deliver to the client.  The
         * super class will take care of delivering it; the implementation
         * here just adds a little more logic.
         */
        @Override public void deliverResult(List<EventVS> events) {
            if (isReset()) {
                // An async query came in while the loader is stopped.  We
                // don't need the result.
                if (events != null) {
                    onReleaseResources(events);
                }
            }
            this.events = events;

            Log.d(TAG + ".deliverResult", " - deliverResult - events.size(): " +
                    (events == null? " NULL " : events.size()));
            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(events);
            }
            // At this point we can release the resources associated with
            // 'oldApps' if needed; now that the new result is delivered we
            // know that it is no longer in use.
            if (events != null) {
                onReleaseResources(events);
            }
        }

        /**
         * Handles a request to start the Loader.
         */
        @Override protected void onStartLoading() {
            Log.d(TAG + ".onStartLoading()", " - onStartLoading");
            if (events != null) {
                // If we currently have a result available, deliver it immediately.
                deliverResult(events);
            }
            forceLoad();
        }

        /**
         * Handles a request to stop the Loader.
         */
        @Override protected void onStopLoading() {
            Log.d(TAG + ".onStopLoading()", " - onStopLoading");
            cancelLoad();
        }

        /**
         * Handles a request to cancel a load.
         */
        @Override public void onCanceled(List<EventVS> apps) {
            super.onCanceled(apps);
            onReleaseResources(apps);
        }

        /**
         * Handles a request to completely reset the Loader.
         */
        @Override protected void onReset() {
            super.onReset();
            onStopLoading();
            events = null;
        }

        /**
         * Helper function to take care of releasing resources associated
         * with an actively loaded data set.
         */
        protected void onReleaseResources(List<EventVS> apps) {
            // For a simple List<> there is nothing to do.  For something
            // like a Cursor, we would close it here.
        }
    }

}
