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

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.MessageContentProvider;
import org.votingsystem.android.fragment.MessageFragment;
import org.votingsystem.model.ContextVS;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * @author jgzornozahttps://www.google.es/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=...%20sabeis%20que%20si%20hacen%20eso%20es%20porque%20no%20tienen%20absolutamente%20nada%20que%20merezca%20la%20pena
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessagesPagerActivity extends ActionBarActivity {

    public static final String TAG = MessagesPagerActivity.class.getSimpleName();

    private AppContextVS contextVS;
    private Cursor cursor = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getApplicationContext();
        setContentView(R.layout.pager_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int cursorPosition = getIntent().getIntExtra(ContextVS.CURSOR_POSITION_KEY, 0);
        LOGD(TAG + ".onCreate", "cursorPosition: " + cursorPosition +
                " - savedInstanceState: " + savedInstanceState);
        MessagePagerAdapter pagerAdapter = new MessagePagerAdapter(getSupportFragmentManager());
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(pagerAdapter);
        cursor = getContentResolver().query(MessageContentProvider.CONTENT_URI,null, null, null, null);
        cursor.moveToPosition(cursorPosition);
        mViewPager.setCurrentItem(cursorPosition);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
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

    @Override public void onPause() {
        super.onPause();
        if(cursor != null && !cursor.isClosed()) cursor.close();
    }

    class MessagePagerAdapter extends FragmentStatePagerAdapter {

        private Cursor cursor;

        public MessagePagerAdapter(FragmentManager fm) {
            super(fm);
            cursor = getContentResolver().query(MessageContentProvider.CONTENT_URI,
                    null, null, null, null);
        }

        @Override public Fragment getItem(int i) {
            LOGD(TAG + ".MessagePagerAdapter.getItem", " - item: " + i);
            cursor.moveToPosition(i);
            return MessageFragment.newInstance(cursor.getPosition());
        }

        @Override public int getCount() {
            return cursor.getCount();
        }

    }

}