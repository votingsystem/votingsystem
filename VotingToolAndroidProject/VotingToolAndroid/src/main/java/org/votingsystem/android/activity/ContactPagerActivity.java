package org.votingsystem.android.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.android.fragment.ContactFragment;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.ResponseVS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ContactPagerActivity extends ActionBarActivity {

    public static final String TAG = ContactPagerActivity.class.getSimpleName();

    private Cursor cursor = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int cursorPosition = getIntent().getIntExtra(ContextVS.CURSOR_POSITION_KEY, -1);
        LOGD(TAG + ".onCreate", "cursorPosition: " + cursorPosition +
                " - savedInstanceState: " + savedInstanceState);
        ResponseVS searchResponseVS = getIntent().getExtras().getParcelable(ContextVS.RESPONSEVS_KEY);
        UserVS userVS = (UserVS) getIntent().getExtras().getSerializable(ContextVS.USER_KEY);
        if(searchResponseVS != null || userVS != null) {
            try {
                final List<UserVS> userVSList = new ArrayList<>();
                if(userVS != null) userVSList.addAll(Arrays.asList(userVS));
                else userVSList.addAll(UserVS.parseList(searchResponseVS.getMessageJSON()));
                updateActionBarTitle(userVSList.iterator().next().getFullName());
                ContactPagerAdapter pagerAdapter = new ContactPagerAdapter(
                        getSupportFragmentManager(), userVSList);
                mViewPager.setAdapter(pagerAdapter);
                mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                    @Override public void onPageSelected(int position) {
                        updateActionBarTitle(userVSList.get(position).getFullName());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }  else {
            ContactDBPagerAdapter pagerAdapter = new ContactDBPagerAdapter(
                    getSupportFragmentManager());
            mViewPager.setAdapter(pagerAdapter);
            String selection = UserContentProvider.TYPE_COL + " =? ";
            cursor = getContentResolver().query(UserContentProvider.CONTENT_URI, null, selection,
                    new String[]{UserVS.Type.CONTACT.toString()}, null);
            cursor.moveToFirst();
            mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override public void onPageSelected(int position) {
                    cursor.moveToPosition(position);
                    updateActionBarTitle(cursor.getString(cursor.getColumnIndex(
                        UserContentProvider.FULL_NAME_COL)));
                }
            });
            mViewPager.setCurrentItem(cursorPosition);
        }

    }

    private void updateActionBarTitle(String fullName) {
        getSupportActionBar().setTitle(getString(R.string.contact_lbl));
        getSupportActionBar().setSubtitle(fullName);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class ContactDBPagerAdapter extends FragmentStatePagerAdapter {

        private Cursor cursor;

        public ContactDBPagerAdapter(FragmentManager fm) {
            super(fm);
            cursor = getContentResolver().query(
                    UserContentProvider.CONTENT_URI, null, null, null, null);
        }

        @Override public Fragment getItem(int i) {
            LOGD(TAG + ".ContactPagerAdapter.getItem", " - item: " + i);
            cursor.moveToPosition(i);
            Long contactId = cursor.getLong(cursor.getColumnIndex(UserContentProvider.ID_COL));
            return ContactFragment.newInstance(contactId);
        }

        @Override public int getCount() {
            return cursor.getCount();
        }
    }

    class ContactPagerAdapter extends FragmentStatePagerAdapter {

        private List<UserVS> itemList;
        public ContactPagerAdapter(FragmentManager fm, List<UserVS> itemList) {
            super(fm);
            this.itemList = itemList;
        }

        @Override public Fragment getItem(int i) {
            LOGD(TAG + ".ContactPagerAdapter.getItem", " - item: " + i);
            return ContactFragment.newInstance(itemList.get(i));
        }

        @Override public int getCount() {
            return itemList.size();
        }
    }

}