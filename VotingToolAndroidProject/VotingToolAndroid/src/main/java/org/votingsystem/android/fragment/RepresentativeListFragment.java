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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import org.votingsystem.android.activity.EventPagerActivity;
import org.votingsystem.android.activity.GenericFragmentContainerActivity;
import org.votingsystem.android.activity.MainActivity;
import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.UserVSResponse;
import org.votingsystem.util.HttpHelper;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RepresentativeListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<List<UserVS>>, AbsListView.OnScrollListener {

    public static final String TAG = "RepresentativeListFragment";
    private static final int REPRESENTATIVE_LOADER_ID = 1;

    private static TextView searchTextView;
    private static TextView emptyResultsView;
    private static ListView listView;
    private boolean mListShown;
    private View mProgressContainer;
    private View mListContainer;
    private RepresentativeListAdapter mAdapter = null;
    private String queryStr = null;
    private int offset = 0;
    private static ContextVS contextVS = null;
    private AtomicBoolean isLoading = null;

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
        contextVS = contextVS.getInstance(getActivity().getBaseContext());
        if(contextVS.getAccessControl() == null) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
        if (getArguments() != null) {
            queryStr = getArguments().getString(SearchManager.QUERY);
            offset = getArguments().getInt("offset");
        }
        Log.d(TAG +  ".onCreate(...)", "args: " + getArguments());
        setRetainInstance(true);
    };


    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
             Bundle savedInstanceState) {
        Log.d(TAG +  ".onCreateView(..)", "savedInstanceState: " + savedInstanceState);
        View rootView = inflater.inflate(R.layout.representative_list_fragment, container, false);
        searchTextView = (TextView) rootView.findViewById(R.id.search_query);
        listView = (ListView) rootView.findViewById(android.R.id.list);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v,pos,id);
            }
        });
        listView.setOnScrollListener(this);

        emptyResultsView = (TextView) rootView.findViewById(android.R.id.empty);
        searchTextView.setVisibility(View.GONE);
        mProgressContainer = rootView.findViewById(R.id.progressContainer);
        mListContainer =  rootView.findViewById(R.id.listContainer);
        mListShown = true;
        setHasOptionsMenu(true);
        isLoading = new AtomicBoolean(false);
        return rootView;
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        Log.i(TAG, ".onLongListItemClick - id: " + id);
        return true;
    }

    public void setListShown(boolean shown, boolean animate){
        if (mListShown == shown) return;
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

    @Override public void onScroll(AbsListView view, int firstVisibleItem,
             int visibleItemCount, int totalItemCount) {
        Log.d(TAG +  ".onScroll(...)", "firstVisibleItem: " + firstVisibleItem +
                " - visibleItemCount:" + visibleItemCount + " - totalItemCount:" + totalItemCount);
        if (getListAdapter() == null) return ;
        if (getListAdapter().getCount() == 0) return ;


        if(firstVisibleItem + visibleItemCount == 20 && !isLoading.get()) {
            Bundle args = new Bundle();
            args.putInt("offset", 557);
            isLoading.set(true);
            getLoaderManager().restartLoader(REPRESENTATIVE_LOADER_ID, args, this);
        }
    }

    @Override public void onScrollStateChanged(AbsListView view, int scrollState) {
        Log.d(TAG +  ".onScrollStateChanged(...)", "scrollState: " + scrollState);
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG +  ".onActivityCreated(...)", "savedInstanceState: " + savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        getView().setBackgroundColor(Color.WHITE);
        if(savedInstanceState != null) return;
        mAdapter = new RepresentativeListAdapter(getActivity());
        setListAdapter(mAdapter);
        // Start out with a progress indicator.
        setListShown(false, true);
        // Prepare the loader.  Either re-connect with an existing one or start a new one.
        getLoaderManager().initLoader(REPRESENTATIVE_LOADER_ID, null, this);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
                getLoaderManager().restartLoader(0, null, this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(TAG +  ".onListItemClick(...)", "Item clicked: " + id);
        UserVS userVS = ((UserVS) getListAdapter().getItem(position));
        Intent intent = new Intent(getActivity(), EventPagerActivity.class);
        intent.putExtra(GenericFragmentContainerActivity.REQUEST_FRAGMENT_KEY,
                RepresentativeFragment.class.getName());
        intent.putExtra(RepresentativeFragment.REPRESENTATIVE_ID_KEY, userVS.getId());
        startActivity(intent);
    }

    @Override public Loader<List<UserVS>> onCreateLoader(int id, Bundle args) {
        Log.d(TAG +  ".onCreateLoader(..)", "id: " + id + " - args: " + args);
        return new RepresentativeListLoader(getActivity());
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

    @Override public void onLoadFinished(Loader<List<UserVS>> loader, List<UserVS> data) {
        Log.i(TAG +  ".onLoadFinished", "onLoadFinished - data: " +
                ((data == null) ? "NULL":data.size()));
        mAdapter.setData(data);
        if (isResumed()) setListShown(true, true);
        else setListShown(true, false);
    }

    @Override public void onLoaderReset(Loader<List<UserVS>> loader) {
        Log.i(TAG +  ".onLoaderReset(...)", "onLoaderReset");
        mAdapter.setData(null);
    }

    public class RepresentativeListAdapter extends ArrayAdapter<UserVS> {

        private final LayoutInflater mInflater;

        public RepresentativeListAdapter(Context context) {
            super(context, R.layout.row_representative);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<UserVS> data) {
            clear();
            if (data != null) {
                for(UserVS userVS : data) {
                    add(userVS);
                }
            }
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) view = mInflater.inflate(
                    R.layout.row_representative, parent, false);
            else view = convertView;
            UserVS userVS = (UserVS) getItem(position);
            if (userVS != null) {
                LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                linearLayout.setBackgroundColor(Color.WHITE);
                TextView representativeName = (TextView)view.findViewById(R.id.representative_name);
                TextView delegationInfo = (TextView) view.findViewById(R.id.representative_delegations);
                representativeName.setText(userVS.getFullName());
                delegationInfo.setText(ContextVS.getMessage("representationsMessage", userVS.getNumRepresentations().toString()));
                ImageView imgView = (ImageView)view.findViewById(R.id.representative_icon);
                //imgView.setImageDrawable();
            }
            return view;
        }
    }


    public static class RepresentativeListLoader extends AsyncTaskLoader<List<UserVS>> {

        List<UserVS> representatives;

        public RepresentativeListLoader(Context context) {
            super(context);
        }

        @Override public List<UserVS> loadInBackground() {
            Log.d(TAG + ".RepresentativeListLoader.loadInBackground()", "loadInBackground");
            List<UserVS> representativeList = null;
            try {
                ResponseVS responseVS = HttpHelper.getData(contextVS.getAccessControl().
                        getRepresentativesURL(), null);
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    UserVSResponse response = UserVSResponse.parse(responseVS.getMessage());
                    representativeList = response.getUsers();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return representativeList;
        }

        /**
         * Called when there is new data to deliver to the client.  The
         * super class will take care of delivering it; the implementation
         * here just adds a little more logic.
         */
        @Override public void deliverResult(List<UserVS> representatives) {
            Log.d(TAG + ".deliverResult", "deliverResult - representatives.size(): " +
                    (representatives == null? " NULL " : representatives.size()));
            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(representatives);
            }
        }

        @Override protected void onStartLoading() {
            Log.d(TAG + ".onStartLoading()", "onStartLoading");
            forceLoad();
        }

        @Override protected void onStopLoading() {
            Log.d(TAG + ".onStopLoading()", "onStopLoading");
            cancelLoad();
        }

        @Override public void onCanceled(List<UserVS> representativeList) {
            super.onCanceled(representativeList);
        }

        @Override protected void onReset() {
            super.onReset();
            onStopLoading();
        }

    }

}
