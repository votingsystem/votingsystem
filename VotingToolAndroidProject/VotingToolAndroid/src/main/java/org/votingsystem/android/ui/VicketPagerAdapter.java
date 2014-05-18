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
import org.votingsystem.android.fragment.TransactionVSGridFragment;
import org.votingsystem.android.fragment.VicketUserInfoFragment;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.ChildPosition;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.GroupPosition;

import java.util.Calendar;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class VicketPagerAdapter extends FragmentStatePagerAdapter
        implements PagerAdapterVS {

    public static final String TAG = VicketPagerAdapter.class.getSimpleName();

    private ChildPosition selectedChild = null;
    private GroupPosition selectedGroup = GroupPosition.VICKETS;

    private String searchQuery = null;
    private FragmentManager fragmentManager;
    private ViewPager viewPager;

    public VicketPagerAdapter(FragmentManager fragmentManager, ViewPager viewPager) {
        super(fragmentManager);
        this.fragmentManager = fragmentManager;
        this.viewPager = viewPager;
    }

    @Override public Fragment getItem(int position) {
        ChildPosition childPosition = selectedGroup.getChildList().get(position);
        Fragment selectedFragment = null;
        switch(childPosition) {
            case VICKET_USER_INFO:
                selectedFragment = new VicketUserInfoFragment();
                break;
            case VICKET_LIST:
                selectedFragment = new TransactionVSGridFragment();
                break;
        }
        Bundle args = new Bundle();
        args.putString(SearchManager.QUERY, searchQuery);
        selectedFragment.setArguments(args);
        Log.d(TAG + ".getItem(...) ", "position:" + position + " - args: " + args +
                " - selectedFragment.getClass(): " + ((Object)selectedFragment).getClass());
        return selectedFragment;
    }

    public String getSelectedChildDescription(AppContextVS context) {
        switch(selectedChild) {
            case VICKET_USER_INFO:
                return context.getLapseWeekLbl(Calendar.getInstance());
            case VICKET_LIST:
                return context.getString(R.string.vickets_list_lbl);
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