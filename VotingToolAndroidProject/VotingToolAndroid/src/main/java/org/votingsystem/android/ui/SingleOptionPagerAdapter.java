package org.votingsystem.android.ui;

import android.app.SearchManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.ReceiptGridFragment;
import org.votingsystem.android.fragment.RepresentativeGridFragment;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.ChildPosition;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.GroupPosition;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class SingleOptionPagerAdapter extends FragmentStatePagerAdapter
        implements PagerAdapterVS {

    public static final String TAG = SingleOptionPagerAdapter.class.getSimpleName();

    private ChildPosition selectedChild = null;
    private GroupPosition selectedGroup = null;

    private String searchQuery = null;
    private FragmentManager fragmentManager;
    private ViewPager viewPager;

    public SingleOptionPagerAdapter(FragmentManager fragmentManager, ViewPager viewPager) {
        super(fragmentManager);
        this.fragmentManager = fragmentManager;
        this.viewPager = viewPager;
    }

    @Override public Fragment getItem(int position) {
        Fragment selectedFragment = null;
        switch(selectedGroup) {
            case REPRESENTATIVES:
                selectedFragment = new RepresentativeGridFragment();
                break;
            case RECEIPTS:
                selectedFragment = new ReceiptGridFragment();
                break;
        }
        Bundle args = new Bundle();
        args.putString(SearchManager.QUERY, searchQuery);
        selectedFragment.setArguments(args);
        Log.d(TAG + ".getItem(...) ", "position:" + position + " - args: " + args);
        return selectedFragment;
    }

    public String getSelectedChildDescription(AppContextVS context) {
        switch(selectedGroup) {
            case REPRESENTATIVES: return context.getString(R.string.representatives_list_lbl);
            case RECEIPTS: return context.getString(R.string.receipt_list_lbl);
            case TICKETS: return context.getString(R.string.tickets_list_lbl);
        }
        return context.getString(R.string.unknown_drop_down_lbl);
    }

    public String getSelectedGroupDescription(AppContextVS context) {
        switch(selectedGroup) {
            case REPRESENTATIVES: return GroupPosition.REPRESENTATIVES.getDescription(context);
            case RECEIPTS: return GroupPosition.RECEIPTS.getDescription(context);
            case TICKETS: return GroupPosition.TICKETS.getDescription(context);
        }
        return context.getString(R.string.unknown_drop_down_lbl);
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public void selectItem(Integer groupPosition, Integer childPosition) {
        selectedGroup = GroupPosition.valueOf(groupPosition);
    }

    public void updateChildPosition(int childPosition) { }

    public int getSelectedChildPosition() {
        return  1;
    }

    public int getSelectedGroupPosition() {
        switch(selectedGroup) {
            case REPRESENTATIVES: return GroupPosition.REPRESENTATIVES.getPosition();
            case RECEIPTS: return GroupPosition.RECEIPTS.getPosition();
        }
        return -1;
    }

    public Drawable getLogo(AppContextVS context) {
        switch(selectedGroup) {
            case REPRESENTATIVES: return context.getResources().getDrawable(R.drawable.system_users_22);
            case RECEIPTS: return context.getResources().getDrawable(R.drawable.receipt_32);
            case TICKETS: return context.getResources().getDrawable(R.drawable.euro_32);
        }
        return context.getResources().getDrawable(R.drawable.mail_mark_unread_22);
    }

    @Override public int getCount() {
        return 1;//Container for groups without childs
    }

}