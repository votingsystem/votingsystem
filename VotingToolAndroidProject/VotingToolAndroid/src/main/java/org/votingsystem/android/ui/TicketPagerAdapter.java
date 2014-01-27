package org.votingsystem.android.ui;

import android.app.SearchManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.TicketGridFragment;
import org.votingsystem.android.fragment.TicketUserInfoFragment;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.ChildPosition;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.GroupPosition;
import org.votingsystem.util.DateUtils;

import java.util.Calendar;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TicketPagerAdapter extends FragmentStatePagerAdapter
        implements PagerAdapterVS {

    public static final String TAG = "TicketPagerAdapter";

    private ChildPosition selectedChild = null;
    private GroupPosition selectedGroup = GroupPosition.TICKETS;

    private String searchQuery = null;
    private FragmentManager fragmentManager;
    private ViewPager viewPager;

    public TicketPagerAdapter(FragmentManager fragmentManager, ViewPager viewPager) {
        super(fragmentManager);
        this.fragmentManager = fragmentManager;
        this.viewPager = viewPager;
    }

    @Override public Fragment getItem(int position) {
        ChildPosition childPosition = selectedGroup.getChildList().get(position);
        Fragment selectedFragment = null;
        switch(childPosition) {
            case TICKET_USER_INFO:
                selectedFragment = new TicketUserInfoFragment();
                break;
            case TICKET_LIST:
                selectedFragment = new TicketGridFragment();
                break;
        }
        Bundle args = new Bundle();
        args.putString(SearchManager.QUERY, searchQuery);
        selectedFragment.setArguments(args);
        Log.d(TAG + ".getItem(...) ", "position:" + position + " - args: " + args +
                " - selectedFragment.getClass(): " + selectedFragment.getClass());
        return selectedFragment;
    }

    public String getSelectedChildDescription(AppContextVS context) {
        switch(selectedChild) {
            case TICKET_USER_INFO:
                return context.getLapseWeekLbl();
            case TICKET_LIST:
                return context.getString(R.string.tickets_list_lbl);
            default:
                return context.getString(R.string.unknown_event_state_lbl);
        }
    }

    public String getSelectedGroupDescription(AppContextVS context) {
        return selectedGroup.getDescription(context);
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public void selectItem(Integer groupPosition, Integer childPosition) {
        selectedChild = selectedGroup.getChildList().get(childPosition);
    }

    public void updateChildPosition(int childPosition) {
        selectedChild = selectedGroup.getChildList().get(childPosition);
    }

    public int getSelectedChildPosition() {
        return selectedGroup.getChildList().indexOf(selectedChild);
    }

    public int getSelectedGroupPosition() {
        return selectedGroup.getPosition();
    }

    public Drawable getLogo(AppContextVS context) {
        return context.getResources().getDrawable(selectedGroup.getLogo());
    }

    @Override public int getCount() {
        return selectedGroup.getChildList().size();
    }

}