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

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.RepresentativeGridFragment;
import org.votingsystem.android.fragment.RepresentativeOperationsFragment;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.ChildPosition;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.GroupPosition;
import org.votingsystem.model.ContextVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class RepresentativeNavigationPagerAdapter extends FragmentStatePagerAdapter
        implements PagerAdapterVS {

    public static final String TAG = "RepresentativeNavigationPagerAdapter";

    private ChildPosition selectedChild = GroupPosition.REPRESENTATIVES.getChildList().get(0);

    private String searchQuery = null;
    private FragmentManager fragmentManager;
    private ViewPager viewPager;
    //private Fragment operationsFragment;

    public RepresentativeNavigationPagerAdapter(FragmentManager fragmentManager, ViewPager viewPager) {
        super(fragmentManager);
        this.fragmentManager = fragmentManager;
        this.viewPager = viewPager;
    }

    @Override public Fragment getItem(int position) {
        Fragment selectedFragment = null;
        ChildPosition childPosition = GroupPosition.REPRESENTATIVES.getChildList().get(position);
        switch(childPosition) {
            case REPRESENTATIVE_LIST:
                selectedFragment = new RepresentativeGridFragment();
                break;
            case REPRESENTATIVE_OPERATION:
                selectedFragment = new RepresentativeOperationsFragment();
                break;
        }
        Bundle args = new Bundle();
        args.putString(SearchManager.QUERY, searchQuery);
        selectedFragment.setArguments(args);
        Log.d(TAG + ".getItem(...) ", "position:" + position + " - args: " + args);
        return selectedFragment;
    }

    public String getSelectedChildDescription(Context context) {
        switch(selectedChild) {
            case REPRESENTATIVE_LIST:
                return ContextVS.getMessage("representativeListDescription");
            case REPRESENTATIVE_OPERATION:
                return ContextVS.getMessage("representativeOperationsDescription");
            default:
                return context.getString(R.string.unknown_drop_down_lbl);
        }

    }

    public String getSelectedGroupDescription(Context context) {
        return GroupPosition.REPRESENTATIVES.getDescription(context);
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public void selectItem(int groupPosition, int childPosition) {
        selectedChild = GroupPosition.REPRESENTATIVES.getChildList().get(childPosition);
    }

    public void updateChildPosition(int childPosition) {
        selectedChild = GroupPosition.REPRESENTATIVES.getChildList().get(childPosition);
    }

    public int getSelectedChildPosition() {
        return  GroupPosition.REPRESENTATIVES.getChildList().indexOf(selectedChild);
    }

    public int getSelectedGroupPosition() {
        return GroupPosition.REPRESENTATIVES.getPosition();
    }

    public Drawable getLogo(Context context) {
        return context.getResources().getDrawable(R.drawable.system_users_22);
    }

    @Override public int getCount() {
        return GroupPosition.REPRESENTATIVES.getChildList().size();
    }

}