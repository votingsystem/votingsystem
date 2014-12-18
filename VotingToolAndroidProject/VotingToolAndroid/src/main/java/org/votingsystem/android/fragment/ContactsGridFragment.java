package org.votingsystem.android.fragment;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
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
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.ContactPagerActivity;
import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.android.util.LogUtils.LOGD;

public class ContactsGridFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener {

    public static final String TAG = ContactsGridFragment.class.getSimpleName();

    public enum Mode {SEARCH, CONTACT}

    private View rootView;
    private GridView gridView;
    private String queryStr = null;
    private Mode mode = Mode.CONTACT;
    private AppContextVS contextVS = null;
    private Integer firstVisiblePosition = null;
    private String broadCastId = ContactsGridFragment.class.getSimpleName();
    private static final int loaderId = 0;
    private AtomicBoolean isProgressDialogVisible = new AtomicBoolean(false);
    private ResponseVS searchResponseVS;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            setProgressDialogVisible(false);
            if(ResponseVS.SC_CONNECTION_TIMEOUT == responseVS.getStatusCode()) {
                if(gridView.getAdapter().getCount() == 0)
                    rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
            }
            if(responseVS != null && responseVS.getTypeVS() == TypeVS.REPRESENTATIVE_REVOKE) {
                MessageDialogFragment.showDialog(responseVS.getStatusCode(), responseVS.getCaption(),
                        responseVS.getNotificationMessage(), getFragmentManager());
            } else if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                MessageDialogFragment.showDialog(responseVS, getFragmentManager());
            }
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        Bundle data = getArguments();
        if (data != null && data.containsKey(SearchManager.QUERY)) {
            queryStr = data.getString(SearchManager.QUERY);
        }
        if(savedInstanceState != null) {
            mode = (Mode) savedInstanceState.getSerializable(ContextVS.STATE_KEY);
        }
        LOGD(TAG +  ".onCreate", "args: " + getArguments() + " - loaderId: " + loaderId +
                " - queryStr: " + queryStr + " - mode: " + mode);
        if(queryStr != null && mode != Mode.SEARCH) {
            mode = Mode.SEARCH;
            new ContactsFetcher().execute(queryStr);
        }
        setHasOptionsMenu(true);
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG +  ".onCreateView", "savedInstanceState: " + savedInstanceState);
        if(Mode.CONTACT == mode) {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(
                    getString(R.string.contacts_lbl));
        } else {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(
                    getString(R.string.search_result));
        }
        rootView = inflater.inflate(R.layout.contacts_grid, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        if(savedInstanceState != null) {
            searchResponseVS = savedInstanceState.getParcelable(ContextVS.RESPONSEVS_KEY);
            try {
                ContactListAdapter adapter = new ContactListAdapter(
                        UserVS.parseList(searchResponseVS.getMessageJSON()), contextVS);
                gridView.setAdapter(adapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Parcelable gridState = savedInstanceState.getParcelable(ContextVS.LIST_STATE_KEY);
            gridView.onRestoreInstanceState(gridState);
        } else {
            ContactDBListAdapter adapter = new ContactDBListAdapter(getActivity(), null,false);
            gridView.setAdapter(adapter);
        }
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
        return rootView;
    }

    @Override public void onScrollStateChanged(AbsListView view, int scrollState) { }

    @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                   int totalItemCount) { }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        LOGD(TAG + ".onLongListItemClick", "id: " + id);
        return true;
    }

    private void setProgressDialogVisible(final boolean isVisible) {
        if(isVisible && isProgressDialogVisible.get()) return;
        isProgressDialogVisible.set(isVisible);
        //bug, without Handler triggers 'Can not perform this action inside of onLoadFinished'
        new Handler(){
            @Override public void handleMessage(Message msg) {
                if (isVisible) {
                    ProgressDialogFragment.showDialog(
                            getString(R.string.loading_data_msg),
                            getString(R.string.loading_info_msg),
                            getFragmentManager());
                } else ProgressDialogFragment.hide(getFragmentManager());
            }
        }.sendEmptyMessage(UIUtils.EMPTY_MESSAGE);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
        outState.putSerializable(ContextVS.STATE_KEY, mode);
        if(searchResponseVS != null) outState.putParcelable(ContextVS.RESPONSEVS_KEY,
                searchResponseVS);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG +  ".onOptionsItemSelected(..)", "Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        LOGD(TAG +  ".onListItemClick", "Clicked item - position:" + position + " -id: " + id);
        Intent intent = new Intent(getActivity(), ContactPagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        intent.putExtra(ContextVS.STATE_KEY, mode);
        intent.putExtra(ContextVS.RESPONSEVS_KEY, searchResponseVS);
        startActivity(intent);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        LOGD(TAG + ".onCreateLoader", "onCreateLoader");
        String selection = UserContentProvider.TYPE_COL + " =? ";
        CursorLoader loader = new CursorLoader(this.getActivity(),
                UserContentProvider.CONTENT_URI, null, selection,
                new String[]{UserVS.Type.CONTACT.toString()}, null);
        return loader;
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        LOGD(TAG + ".onLoadFinished", " - cursor.getCount(): " + cursor.getCount() +
                " - firstVisiblePosition: " + firstVisiblePosition);
        if(firstVisiblePosition != null) cursor.moveToPosition(firstVisiblePosition);
        firstVisiblePosition = null;
        ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
        if(cursor.getCount() == 0) {
            rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        } else rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
    }

    @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        LOGD(TAG + ".onLoaderReset", "");
        ((CursorAdapter)gridView.getAdapter()).swapCursor(null);
    }

    public class ContactDBListAdapter extends CursorAdapter {

        public ContactDBListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return LayoutInflater.from(context).inflate(R.layout.contact_card, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            if(cursor != null) {
                String fullName = cursor.getString(cursor.getColumnIndex(
                        UserContentProvider.FULL_NAME_COL));
                String nif = cursor.getString(cursor.getColumnIndex(UserContentProvider.NIF_COL));
                ((TextView)view.findViewById(R.id.fullname)).setText(fullName);
                ((TextView) view.findViewById(R.id.nif)).setText(nif);
            }
        }
    }

    public class ContactListAdapter extends BaseAdapter {

        private List<UserVS> itemList;
        private Context context;

        public ContactListAdapter(List<UserVS> itemList, Context ctx) {
            this.itemList = itemList;
            this.context = ctx;
        }

        @Override public int getCount() {
            return itemList.size();
        }

        @Override public Object getItem(int position) {
            return itemList.get(position);
        }

        @Override public long getItemId(int position) {
            return position;
        }

        @Override public View getView(int position, View itemView, ViewGroup parent) {
            UserVS userVS = itemList.get(position);
            if (itemView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                itemView = inflater.inflate(R.layout.contact_card, null);
            }
            ((TextView)itemView.findViewById(R.id.fullname)).setText(userVS.getFullName());
            ((TextView) itemView.findViewById(R.id.nif)).setText(userVS.getNif());
            return itemView;
        }
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    public class ContactsFetcher extends AsyncTask<String, String, ResponseVS> {

        public ContactsFetcher() { }

        @Override protected void onPreExecute() { setProgressDialogVisible(true); }

        @Override protected ResponseVS doInBackground(String... params) {
            String contactsURL = contextVS.getCooinServer().getSearchServiceURL(params[0]);
            searchResponseVS = HttpHelper.getData(contactsURL, ContentTypeVS.JSON);
            return searchResponseVS;
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(ResponseVS responseVS) {
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    ContactListAdapter adapter = new ContactListAdapter(
                            UserVS.parseList(responseVS.getMessageJSON()), contextVS);
                    gridView.setAdapter(adapter);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                            getString(R.string.exception_lbl), ex.getMessage(), getFragmentManager());
                }
            } else {
                MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,getString(R.string.error_lbl),
                        responseVS.getMessage(), getFragmentManager());
            }
            setProgressDialogVisible(false);
        }
    }
}