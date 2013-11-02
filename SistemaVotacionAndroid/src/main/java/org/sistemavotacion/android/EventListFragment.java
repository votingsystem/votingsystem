package org.sistemavotacion.android;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Consulta;
import org.sistemavotacion.modelo.DatosBusqueda;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.EventState;
import org.sistemavotacion.util.HttpHelper;
import org.sistemavotacion.util.ServerPaths;
import org.sistemavotacion.util.SubSystem;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EventListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<List<Evento>> {

    public static final String TAG = "EventListFragment";

    private static String errorLoadingEventsMsg = null;


    private static TextView searchTextView;
    private static TextView emptyResultsView;
    private static ListView listView;

    private boolean mListShown;
    private View mProgressContainer;
    private View mListContainer;

    private EventListAdapter mAdapter = null;
    private EventState eventState = null;
    private SubSystem subSystem = SubSystem.VOTING;
    private String queryStr = null;
    private int offset = 0;
    private static AppData appData = null;

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<Evento> ALPHA_COMPARATOR = new Comparator<Evento>() {
        private final Collator sCollator = Collator.getInstance();
        @Override public int compare(Evento object1, Evento object2) {
            return sCollator.compare(object1.getAsunto(), object2.getAsunto());
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appData = appData.getInstance(getActivity().getBaseContext());
        if(appData.getAccessControlURL() == null) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
        Bundle args = getArguments();
        String eventStateStr = null;
        String subSystemStr = "";
        if (args != null) {
            eventStateStr = args.getString("eventState");
            if(eventStateStr != null) eventState = EventState.valueOf(eventStateStr);
            subSystemStr = args.getString("subSystem");
            if(subSystemStr != null) subSystem = SubSystem.valueOf(subSystemStr);
            queryStr = args.getString(SearchManager.QUERY);
            offset = args.getInt("offset");
        }
        Log.d(TAG +  ".onCreate(..)", " - eventState: " + eventState +
                " - subSystem: " + subSystem + " - queryStr: " + queryStr);
    };


    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
        Log.i(TAG, ".onLongListItemClick - id: =========== " + id);
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
        super.onActivityCreated(savedInstanceState);
        getView().setBackgroundColor(Color.WHITE);
  //      setEmptyText(getActivity().getBaseContext().
  //              getString(R.string.empty_search_lbl));

        mAdapter = new EventListAdapter(getActivity());
        setListAdapter(mAdapter);
        // Start out with a progress indicator.
        setListShown(false);
        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(TAG +  "onCreateOptionsMenu(..)", " - onCreateOptionsMenu - onCreateOptionsMenu");
        inflater.inflate(R.menu.event_list_fragment, menu);
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
        Evento event = ((Evento) getListAdapter().getItem(position));
        appData.setEvent(event);
        appData.setEventList(mAdapter.getEvents());
        Intent intent = new Intent(getActivity(), EventPagerActivity.class);
        startActivity(intent);
    }

    @Override public Loader<List<Evento>> onCreateLoader(int id, Bundle args) {
        Log.d(TAG +  ".onCreateLoader(..)", " - eventState: " + eventState + " - subSystem: " + subSystem);
        return new EventListLoader(getActivity(), subSystem, eventState, queryStr, offset);
    }

    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        Intent intent = activity.getIntent();
        if(intent != null) {
            String query = null;
            if (Intent.ACTION_SEARCH.equals(intent)) {
                query = intent.getStringExtra(SearchManager.QUERY);
            }
            Log.d(TAG + ".onAttach()", " - activity: " + activity.getClass().getName() +
                    " - query: " + query + " - activity: ");
        }
    }

    @Override public void onStop() {
        super.onStop();
        Log.d(TAG +  ".onStop()", " - onStop - ");
    }

    @Override public void onLoadFinished(Loader<List<Evento>> loader, List<Evento> data) {
        Log.i(TAG +  ".onLoadFinished", " - onLoadFinished - data: " +
                ((data == null) ? "NULL":data.size()));
        if(errorLoadingEventsMsg == null) {
            mAdapter.setData(data);
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

    @Override public void onLoaderReset(Loader<List<Evento>> loader) {
        Log.i(TAG +  ".onLoaderReset", " - onLoaderReset ");
        mAdapter.setData(null);
    }


    public class EventListAdapter extends ArrayAdapter<Evento> {

        private final LayoutInflater mInflater;

        List<Evento> events;

        public EventListAdapter(Context context) {
            super(context, R.layout.row_evento);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<Evento> data) {
            clear();
            if (data != null) {
                if(events == null) {
                    events = new ArrayList<Evento>();
                }
                events.addAll(data);
                for(Evento event : data) {
                    add(event);
                }
            }
        }

        public List<Evento> getEvents() {
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
            Evento evento = getItem(position);
            if (evento != null) {
                LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                linearLayout.setBackgroundColor(Color.WHITE);
                TextView subject = (TextView) view.findViewById(R.id.event_subject);
                TextView dateInfo = (TextView) view.findViewById(R.id.event_date_info);
                TextView author = (TextView) view.findViewById(R.id.event_author);
                subject.setText(evento.getAsunto());
                String dateInfoStr = null;
                ImageView imgView = (ImageView)view.findViewById(R.id.event_icon);
                switch(evento.getEstadoEnumValue()) {
                    case ACTIVO:
                        imgView.setImageResource(R.drawable.open);
                        dateInfoStr = "<b>" + getContext().getString(R.string.remain_lbl,
                                DateUtils.getElpasedTimeHoursFromNow(evento.getFechaFin()))  +"</b>";
                        break;
                    case PENDIENTE_COMIENZO:
                        imgView.setImageResource(R.drawable.pending);
                        dateInfoStr = "<b>" + getContext().getString(R.string.inicio_lbl) + "</b>: " +
                                DateUtils.getShortSpanishStringFromDate(evento.getFechaInicio()) + " - " +
                                "<b>" + getContext().getString(R.string.fin_lbl) + "</b>: " +
                                DateUtils.getShortSpanishStringFromDate(evento.getFechaFin());
                        break;
                    case CANCELADO:
                    case FINALIZADO:
                        imgView.setImageResource(R.drawable.closed);
                        dateInfoStr = "<b>" + getContext().getString(R.string.inicio_lbl) + "</b>: " +
                                DateUtils.getShortSpanishStringFromDate(evento.getFechaInicio()) + " - " +
                                "<b>" + getContext().getString(R.string.fin_lbl) + "</b>: " +
                                DateUtils.getShortSpanishStringFromDate(evento.getFechaFin());
                        break;
                    default:
                        Log.d(TAG +  ".getView", " - event state: " + evento.getEstadoEnumValue());
                }

                if(dateInfoStr != null) dateInfo.setText(Html.fromHtml(dateInfoStr));
                else dateInfo.setVisibility(View.GONE);
                if(evento.getUsuario() != null && !"".equals(
                        evento.getUsuario().getNombreCompleto())) {
                    String authorStr =  "<b>" + getContext().getString(R.string.author_lbl) + "</b>: " +
                            evento.getUsuario().getNombreCompleto();
                    author.setText(Html.fromHtml(authorStr));
                } else author.setVisibility(View.GONE);
            }
            return view;
        }
    }


    /**
     * A custom Loader that loads events
     */
    public static class EventListLoader extends AsyncTaskLoader<List<Evento>> {

        List<Evento> events;
        SubSystem subSystem;
        EventState eventState;
        String queryString;
        int offset = 0;

        public EventListLoader(Context context) {
            super(context);
        }

        public EventListLoader(Context context, SubSystem subSystem,
                               EventState eventState, String queryString, int offset) {
            super(context);
            this.subSystem = subSystem;
            this.eventState = eventState;
            this.queryString = queryString;
            this.offset = offset;
        }

        /**
         * This is where the bulk of our work is done.  This function is
         * called in a background thread and should generate a new set of
         * data to be published by the loader.
         */
        @Override public List<Evento> loadInBackground() {
            Log.d(TAG + ".EventListLoader.loadInBackground()", " - subSystem: " + subSystem
                    + " - eventState: " + eventState + " - queryString: " + queryString);

            List<Evento> eventList = null;
            try {
                HttpResponse response = null;
                if(queryString != null) {
                    String url = ServerPaths.getURLSearch(
                            appData.getAccessControlURL(), 0, appData.EVENTS_PAGE_SIZE);
                    DatosBusqueda datosBusqueda = new DatosBusqueda(
                            subSystem.getEventType(), eventState.getEventState(), queryString);
                    response = HttpHelper.sendByteArray(
                            datosBusqueda.toJSON().toString().getBytes(), null, url);
                    searchTextView.setText(Html.fromHtml(getContext().getString(
                            R.string.search_query_info_msg, queryString)));
                    searchTextView.setVisibility(View.VISIBLE);
                } else {
                    String url = ServerPaths.getURLEventos(
                            appData.getAccessControlURL(), eventState, subSystem, appData.EVENTS_PAGE_SIZE, offset);
                    response = HttpHelper.getData(url, null);
                    searchTextView.setVisibility(View.GONE);
                }
                int statusCode = response.getStatusLine().getStatusCode();
                if(Respuesta.SC_OK == statusCode) {
                    Consulta consulta = Consulta.parse(EntityUtils.toString(response.getEntity()));
                    eventList = consulta.getEventos();
                } else errorLoadingEventsMsg = response.getStatusLine().toString();
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
        @Override public void deliverResult(List<Evento> events) {
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
        @Override public void onCanceled(List<Evento> apps) {
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
        protected void onReleaseResources(List<Evento> apps) {
            // For a simple List<> there is nothing to do.  For something
            // like a Cursor, we would close it here.
        }
    }

}
