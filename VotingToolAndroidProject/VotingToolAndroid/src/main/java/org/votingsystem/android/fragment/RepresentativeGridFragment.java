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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.android.activity.MainActivity;
import org.votingsystem.android.activity.RepresentativePagerActivity;
import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.android.service.RepresentativeService;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;

import java.text.Collator;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

public class RepresentativeGridFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener {

    public static final String TAG = "RepresentativeGridFragment";

    private View rootView;
    private GridView gridView;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private AtomicBoolean hasHTTPConnection = new AtomicBoolean(true);
    private RepresentativeListAdapter adapter = null;
    private String queryStr = null;
    private ContextVS contextVS = null;
    private Long offset = new Long(0);
    private Integer firstVisiblePosition = null;
    private View progressContainer;
    private FrameLayout gridContainer;
    private int loaderId = -1;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                "intent.getExtras(): " + intent.getExtras());
        int responseStatusCode = intent.getIntExtra(ContextVS.RESPONSE_STATUS_KEY,
                ResponseVS.SC_ERROR);
        if(ResponseVS.SC_CONNECTION_TIMEOUT == responseStatusCode) {
            hasHTTPConnection.set(false);
        }
        String caption = intent.getStringExtra(ContextVS.CAPTION_KEY);
        String message = intent.getStringExtra(ContextVS.MESSAGE_KEY);
        showMessage(responseStatusCode, caption, message);
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
        loaderId = NavigatorDrawerOptionsAdapter.GroupPosition.REPRESENTATIVES.getLoaderId(
                NavigatorDrawerOptionsAdapter.ChildPosition.REPRESENTATIVE_LIST);
        queryStr = getArguments().getString(SearchManager.QUERY);
        Log.d(TAG +  ".onCreate(...)", "args: " + getArguments() + " - loaderId: " + loaderId);
        setHasOptionsMenu(true);
    };

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        Log.d(TAG +  ".onCreateView(..)", "savedInstanceState: " + savedInstanceState);
        rootView = inflater.inflate(R.layout.representative_grid_fragment, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        adapter = new RepresentativeListAdapter(getActivity().getApplicationContext(), null,false);
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
        progressContainer = rootView.findViewById(R.id.progressContainer);
        gridContainer =  (FrameLayout)rootView.findViewById(R.id.gridContainer);
        gridContainer.getForeground().setAlpha(0);
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
            hasHTTPConnection.set(savedInstanceState.getBoolean(ContextVS.RESPONSE_STATUS_KEY,true));
            if(savedInstanceState.getBoolean(ContextVS.LOADING_KEY, false)) showProgress(true, true);
        }
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        Log.d(TAG + ".onLongListItemClick(...)", "id: " + id);
        return true;
    }

    @Override public void onScrollStateChanged(AbsListView absListView, int i) { }

    @Override public void onScroll(AbsListView view, int firstVisibleItem,
               int visibleItemCount, int totalItemCount) {
        if (gridView.getAdapter() == null || gridView.getAdapter().getCount() == 0) return ;
        /* maybe add a padding */
        boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount;
        if(loadMore && !progressVisible.get() && offset <
                UserContentProvider.getNumTotalRepresentatives() &&
                totalItemCount < UserContentProvider.getNumTotalRepresentatives()) {
            Log.d(TAG +  ".onScroll(...)", "loadMore - firstVisibleItem: " + firstVisibleItem +
                    " - visibleItemCount:" + visibleItemCount + " - totalItemCount:" + totalItemCount);
            firstVisiblePosition = firstVisibleItem;
            loadHttpItems(new Long(totalItemCount));
        }
    }

    private void loadHttpItems(Long offset) {
        Log.d(TAG +  ".loadHttpItems(...)", "offset: " + offset + " - hasHTTPConnection: " +
                hasHTTPConnection.get());
        if(!hasHTTPConnection.get()) {
            showProgress(false, true);
            if(gridView.getAdapter().getCount() == 0)
                rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        }
        else {
            showProgress(true, true);
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    RepresentativeService.class);
            startIntent.putExtra(ContextVS.URL_KEY, contextVS.getAccessControl().
                    getRepresentativesURL(offset, ContextVS.REPRESENTATIVE_PAGE_SIZE));
            startIntent.putExtra(ContextVS.CALLER_KEY, this.getClass().getName());
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.ITEMS_REQUEST);
            getActivity().startService(startIntent);
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(TAG +  ".onCreateOptionsMenu(..)", "onCreateOptionsMenu");
        inflater.inflate(R.menu.eventvs_grid_fragment, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG +  ".onOptionsItemSelected(..)", "Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.reload:
                hasHTTPConnection.set(true);
                rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
                getLoaderManager().restartLoader(loaderId, null, this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        Log.d(TAG +  ".onListItemClick(...)", "Clicked item - position:" + position +
                " -id: " + id);
        Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        Intent intent = new Intent(getActivity().getApplicationContext(),
                RepresentativePagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        startActivity(intent);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(TAG + ".onCreateLoader(...)", "");
        String selection = UserContentProvider.TYPE_COL + " =? ";
        CursorLoader loader = new CursorLoader(this.getActivity(),
                UserContentProvider.CONTENT_URI, null, selection,
                new String[]{UserVS.Type.REPRESENTATIVE.toString()}, null);
        return loader;
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG + ".onLoadFinished(...)", " - cursor.getCount(): " + cursor.getCount() +
                " - firstVisiblePosition: " + firstVisiblePosition);
        if(UserContentProvider.getNumTotalRepresentatives() == null)
            loadHttpItems(offset);
        else {
            showProgress(false, true);
            if(firstVisiblePosition != null) cursor.moveToPosition(firstVisiblePosition);
            firstVisiblePosition = null;
            ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
            if(cursor.getCount() == 0) {
                rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
            } else rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
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

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) progressContainer.startAnimation(AnimationUtils.loadAnimation(
                    getActivity().getApplicationContext(), android.R.anim.fade_in));
            progressContainer.setVisibility(View.VISIBLE);
            gridContainer.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } else {
            if (animate) progressContainer.startAnimation(AnimationUtils.loadAnimation(
                    getActivity().getApplicationContext(), android.R.anim.fade_out));
            progressContainer.setVisibility(View.GONE);
            gridContainer.getForeground().setAlpha(0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }
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
                        UserContentProvider.FULL_NAME_COL));
                int numRepresentations = cursor.getInt(cursor.getColumnIndex(
                        UserContentProvider.NUM_REPRESENTATIONS_COL));
                LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                linearLayout.setBackgroundColor(Color.WHITE);
                TextView representativeName = (TextView)view.findViewById(R.id.representative_name);
                TextView delegationInfo = (TextView) view.findViewById(
                        R.id.representative_delegations);
                representativeName.setText(fullName);
                delegationInfo.setText(context.getString(R.string.num_representations_lbl,
                        String.valueOf(numRepresentations)));
                ImageView imgView = (ImageView)view.findViewById(R.id.representative_icon);
                //imgView.setImageDrawable();
            }
        }
    }


    @Override public void onStop() {
        super.onStop();
        Log.d(TAG + ".onStop()", " - onStop - ");
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", "onDestroy");
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ContextVS.OFFSET_KEY, offset);
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
        outState.putBoolean(ContextVS.RESPONSE_STATUS_KEY, hasHTTPConnection.get());
        Log.d(TAG +  ".onSaveInstanceState(...)", "outState: " + outState);
    }

    @Override public void onResume() {
        Log.d(TAG + ".onResume() ", "");
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(this.getClass().getName()));
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }
}