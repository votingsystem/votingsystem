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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.votingsystem.android.activity.MainActivity;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.RepresentativePagerActivity;
import org.votingsystem.android.contentprovider.RepresentativeContentProvider;
import org.votingsystem.android.service.RepresentativeService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import java.text.Collator;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

public class RepresentativeListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener {

    public static final String TAG = "RepresentativeListFragment";

    private static TextView searchTextView;
    private static TextView emptyResultsView;
    private static GridView gridView;
    private AtomicBoolean progressVisible = null;
    private View mProgressContainer;
    private View mListContainer;
    private RepresentativeListAdapter mAdapter = null;
    private String queryStr = null;
    private static ContextVS contextVS = null;

    private Long offset = new Long(0);
    private Integer firstVisiblePosition = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equalsIgnoreCase(ContextVS.HTTP_DATA_INITIALIZED_ACTION_ID)){
            int responseStatus = intent.getIntExtra(ContextVS.HTTP_RESPONSE_STATUS_KEY, -1);
            Log.d(TAG + ".broadcastReceiver.onReceive(...)", "status: " + responseStatus +
                    " - extras: " + intent.getExtras());
            if(ResponseVS.SC_OK == responseStatus) {
                offset = intent.getLongExtra(ContextVS.OFFSET_KEY, 0L);
                RepresentativeContentProvider.setNumTotalRepresentatives(
                        intent.getLongExtra(ContextVS.NUM_TOTAL_KEY, 0L));
                Log.d(TAG + ".broadcastReceiver.onReceive(...)", " - offset: " + offset +
                    " - numTotal: " + RepresentativeContentProvider.getNumTotalRepresentatives());
            } else showMessage(contextVS.getMessage("connErrorCaption"),
                    intent.getStringExtra(ContextVS.HTTP_RESPONSE_DATA_KEY));
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

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = contextVS.getInstance(getActivity().getApplicationContext());
        if(contextVS.getAccessControl() == null) {
            Intent intent = new Intent(getActivity().getApplicationContext(), MainActivity.class);
            startActivity(intent);
        }
        if (getArguments() != null) {
            queryStr = getArguments().getString(SearchManager.QUERY);
        }
        Log.d(TAG +  ".onCreate(...)", "args: " + getArguments());
        setHasOptionsMenu(true);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(ContextVS.HTTP_DATA_INITIALIZED_ACTION_ID));
        //Prepare the loader. Either re-connect with an existing one or start a new one.
        getLoaderManager().initLoader(ContextVS.REPRESENTATIVE_LOADER_ID, null, this);
    };

    private void showMessage(String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "caption: " + caption + "  - showMessage: " + message);
        AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
        builder.setTitle(caption).setMessage(message).show();
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        Log.d(TAG +  ".onCreateView(..)", "savedInstanceState: " + savedInstanceState);
        View rootView = inflater.inflate(R.layout.representative_list_fragment, container, false);
        searchTextView = (TextView) rootView.findViewById(R.id.search_query);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        mAdapter = new RepresentativeListAdapter(getActivity().getApplicationContext(), null,false);
        gridView.setAdapter(mAdapter);
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
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
        rootView.setBackgroundColor(Color.WHITE);
        return rootView;
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        Log.i(TAG, ".onLongListItemClick - id: " + id);
        return true;
    }

    public void showProgressIndicator(boolean showProgress, boolean animate){
        if (progressVisible != null && progressVisible.get() == showProgress) return;
        if (progressVisible == null) progressVisible = new AtomicBoolean(showProgress);
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
        if (gridView.getAdapter() == null || gridView.getAdapter().getCount() == 0) return ;
        /* maybe add a padding */
        boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount;
        if(loadMore && !progressVisible.get() && offset <
                RepresentativeContentProvider.getNumTotalRepresentatives() &&
                totalItemCount < RepresentativeContentProvider.getNumTotalRepresentatives()) {
            Log.d(TAG +  ".onScroll(...)", "loadMore - firstVisibleItem: " + firstVisibleItem +
                    " - visibleItemCount:" + visibleItemCount + " - totalItemCount:" + totalItemCount);
            firstVisiblePosition = firstVisibleItem;
            loadHttpItems(new Long(totalItemCount));
        }
    }

    private void loadHttpItems(Long offset) {
        showProgressIndicator(true, true);
        Intent startIntent = new Intent(getActivity().getApplicationContext(),
                RepresentativeService.class);
        startIntent.putExtra(ContextVS.URL_KEY, contextVS.getAccessControl().
                getRepresentativesURL(offset, ContextVS.REPRESENTATIVE_PAGE_SIZE));
        getActivity().startService(startIntent);
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

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ContextVS.OFFSET_KEY, offset);
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
        Log.d(TAG +  ".onSaveInstanceState(...)", "outState: " + outState);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(TAG +  ".onCreateOptionsMenu(..)", "onCreateOptionsMenu");
        inflater.inflate(R.menu.event_list_fragment, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG +  ".onOptionsItemSelected(..)", "Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.reload:
                Log.d(TAG +  ".onOptionsItemSelected(..)", "Reloading EventListFragment");
                getLoaderManager().restartLoader(ContextVS.REPRESENTATIVE_LOADER_ID, null, this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        Log.d(TAG +  ".onListItemClick(...)", "Clicked item - position:" + position +
                " -id: " + id);
        Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        Long representativeId = cursor.getLong(cursor.getColumnIndex(
                RepresentativeContentProvider.ID_COL));
        Intent intent = new Intent(getActivity().getApplicationContext(), RepresentativePagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        startActivity(intent);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(TAG + ".onCreateLoader(...)", "");
        CursorLoader loader = new CursorLoader(this.getActivity(),
                RepresentativeContentProvider.CONTENT_URI, null, null, null, null);
        return loader;
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG + ".onLoadFinished(...)", " - cursor.getCount(): " + cursor.getCount() +
                " - firstVisiblePosition: " + firstVisiblePosition);
        if(RepresentativeContentProvider.getNumTotalRepresentatives() == null) loadHttpItems(offset);
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
        Log.d(TAG + ".onStop()", " - onStop - ");
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", "onDestroy");
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    public class RepresentativeListAdapter  extends CursorAdapter {

        private LayoutInflater inflater = null;

        public RepresentativeListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            inflater = LayoutInflater.from(context);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return inflater.inflate(R.layout.row_representative, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            if(cursor != null) {
                String fullName = cursor.getString(cursor.getColumnIndex(
                        RepresentativeContentProvider.FULL_NAME_COL));
                int numRepresentations = cursor.getInt(cursor.getColumnIndex(
                        RepresentativeContentProvider.NUM_REPRESENTATIONS_COL));
                LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                linearLayout.setBackgroundColor(Color.WHITE);
                TextView representativeName = (TextView)view.findViewById(R.id.representative_name);
                TextView delegationInfo = (TextView) view.findViewById(R.id.representative_delegations);
                representativeName.setText(fullName);
                delegationInfo.setText(ContextVS.getMessage("representationsMessage", String.valueOf(numRepresentations)));
                ImageView imgView = (ImageView)view.findViewById(R.id.representative_icon);
                //imgView.setImageDrawable();
            }
        }

    }


}