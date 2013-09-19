/*
 * Copyright 2011 - Jose. J. García Zornoza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sistemavotacion.android;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import org.sistemavotacion.android.ui.HomeDropdownListAdapter;
import org.sistemavotacion.util.EnumTab;
import org.sistemavotacion.util.SearchSuggestionProvider;
import org.sistemavotacion.util.SubSystem;
import org.sistemavotacion.util.SubSystemChangeListener;

import java.util.ArrayList;


/**
 */
public class FragmentTabsPager extends FragmentActivity 
		implements SubSystemChangeListener {
	
	public static final String TAG = "FragmentTabsPager";

    TabHost mTabHost;
    ViewPager  mViewPager;
    TabsAdapter mTabsAdapter;
    
    private int offsetOpenTab = 0;
    private int offsetPendingTab = 0;
    private int offsetClosedTab = 0;
    
    private static HomeDropdownListAdapter homeAdapter;

	public static FragmentTabsPager INSTANCIA;
	private static SubSystem selectedSubSystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	
    	super.onCreate(savedInstanceState);
        INSTANCIA = this;
        Log.d(TAG + ".onCreate()", " - onCreate");
        if(homeAdapter == null) {
        	Log.d(TAG + ".onCreate()", " - arrancando homeAdapter");
        	homeAdapter = new HomeDropdownListAdapter(this);   	
        }
        
        setContentView(R.layout.fragment_tabs_pager);
        TextView searchTextView = (TextView) findViewById(R.id.search_query);
        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        mViewPager = (ViewPager)findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

        Bundle queryBundle = new Bundle();
        final Intent queryIntent = getIntent();
        if (Intent.ACTION_SEARCH.equals(queryIntent.getAction())) {
            final String queryString = queryIntent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG + ".onCreate()", " - queryString: " + queryString);
            // Record the query string in the recent queries suggestions provider.
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, 
                    SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
            suggestions.saveRecentQuery(queryString, null);
            if(queryIntent.getBundleExtra(SearchManager.APP_DATA) != null) 
            	queryBundle = queryIntent.getBundleExtra(SearchManager.APP_DATA);
            queryBundle.putString(SearchManager.QUERY, queryString);
            searchTextView.setText(Html.fromHtml(getString(
            		R.string.search_query_info_msg, queryString)));
        } else {
        	searchTextView.setVisibility(View.GONE);
        }
        
        Bundle openBundle = new Bundle(queryBundle);
        openBundle.putString("enumTab", EnumTab.OPEN.toString());
        openBundle.putInt("offset", offsetOpenTab);
        
        Bundle pendingBundle = new Bundle(queryBundle);
        pendingBundle.putString("enumTab", EnumTab.PENDING.toString());
        openBundle.putInt("offset", offsetPendingTab);
        
        Bundle closedBundle = new Bundle(queryBundle);
        closedBundle.putString("enumTab", EnumTab.CLOSED.toString());
        openBundle.putInt("offset", offsetClosedTab);
        
        if(Aplicacion.INSTANCIA != null) {
        	selectedSubSystem = Aplicacion.INSTANCIA.getSelectedSubsystem();
        	Aplicacion.INSTANCIA.addSubSystemChangeListener(this);
        	openBundle.putString("subSystem", selectedSubSystem.toString());
        	pendingBundle.putString("subSystem", selectedSubSystem.toString());
            closedBundle.putString("subSystem", selectedSubSystem.toString());
        }
        
        mTabsAdapter.addTab(mTabHost.newTabSpec(
        		EnumTab.OPEN.toString()).setIndicator(getTabDesc(EnumTab.OPEN, selectedSubSystem)),
        		EventListFragmentLoader.EventListFragment.class, openBundle);
        mTabsAdapter.addTab(mTabHost.newTabSpec(
        		EnumTab.PENDING.toString()).setIndicator(getTabDesc(EnumTab.PENDING, selectedSubSystem)),
        		EventListFragmentLoader.EventListFragment.class, pendingBundle);
        mTabsAdapter.addTab(mTabHost.newTabSpec(
        		EnumTab.CLOSED.toString()).setIndicator(getTabDesc(EnumTab.CLOSED, selectedSubSystem)),
        		EventListFragmentLoader.EventListFragment.class, closedBundle);
        
        TabWidget tabs = (TabWidget)findViewById(android.R.id.tabs);
        for(int j = 0; j < tabs.getChildCount(); j++) {
        	TextView tv = (TextView) ((ViewGroup)tabs.getChildAt(j)).findViewById(android.R.id.title);
        	if(!(tabs.getChildAt(j) instanceof LinearLayout)) {
            	LayoutParams layoutParams = ((ViewGroup)tabs.getChildAt(j)).getLayoutParams();
            	layoutParams.height = 35;
            	tabs.requestLayout();	
        	}
        	tv.setTextColor(Color.parseColor(EnumTab.valueOf(j).getColor()));
	   		Typeface typeface = Typeface.create(tv.getTypeface(), Typeface.BOLD);
	   		tv.setTypeface(typeface);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
        		|| Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActionBar().setNavigationMode(android.app.ActionBar.NAVIGATION_MODE_LIST);
            getActionBar().setDisplayShowHomeEnabled(false);
            getActionBar().setDisplayShowTitleEnabled(false);
            getActionBar().setListNavigationCallbacks(homeAdapter, 
            		new android.app.ActionBar.OnNavigationListener(){

						@Override
						public boolean onNavigationItemSelected(
								int itemPosition, long itemId) {
							Log.d(TAG + ".onNavigationItemSelected(...)", 
									"- itemPosition: " + itemPosition + " - itemId: " + itemId);
							if(selectedSubSystem == SubSystem.valueOf(itemPosition)) return false;
							Aplicacion.INSTANCIA.setSelectedSubsystem(SubSystem.valueOf(itemPosition));
							return false;
						}});
            if(Aplicacion.INSTANCIA != null) {
            	getActionBar().setSelectedNavigationItem(selectedSubSystem.getPosition());
            }
        } else {
        	Log.d(TAG + ".onNavigationItemSelected(...)", " --- pre Honeycomb device");
        }
    }
	
    @Override
    protected void onNewIntent(Intent intent) {
    	Log.d(TAG + ".onNewIntent(...)", "- onNewIntent ");
    }
    
    private void clearSearchHistory() {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, 
        		SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
        suggestions.clearHistory();
    }
    
	@Override public boolean onSearchRequested() {
		Log.d(TAG + ".onSearchRequested(...)", "- onSearchRequested ");
        //Bundle appDataBundle = new Bundle();
        //appDataBundle.putString("param1", param1DataString);
		//startSearch(null, false, appDataBundle, false); 
        // Now call the Activity member function that invokes the Search Manager UI.
        startSearch(null, false, null, false); 
        // Returning true indicates that we did launch the search, instead of blocking it.
        return true;
	};

    
    @Override protected void onSaveInstanceState(Bundle outState) {
    	Log.d(TAG + ".onSaveInstanceState(...)", "- onSaveInstanceState ");
        super.onSaveInstanceState(outState);
        outState.putString("tab", mTabHost.getCurrentTabTag());
    }
    
	public void getViewedFragment(int position) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(
				"android:switcher:" + R.id.pager + ":" + position);
		Log.d(TAG + ".getViewedFragment(..)", "- position: " + position + " - currentTab: " + mTabHost.getCurrentTab());
		if(fragment != null)  // could be null if not instantiated yet
		{
			Log.d(TAG + ".getViewedFragment(..)", "- fragment: " + fragment.getClass().getName());
			if(fragment.getView() != null) {
			// no need to call if fragment's onDestroyView() 
			//has since been called.
			//fragment.updateDisplay(); // do what updates are required
			}
	  }
	}

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter
            implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final TabHost mTabHost;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        static final class TabInfo {
            private final String tag;
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(String _tag, Class<?> _class, Bundle _args) {
                tag = _tag;
                clss = _class;
                args = _args;
            }
        }

        static class DummyTabFactory implements TabHost.TabContentFactory {
            
        	private final Context mContext;
        	private View view;

            public DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
            	Log.d(TAG + ".DummyTabFactory.createTabContent(...)", "- tag: " + tag);
                View view = new View(mContext);
                view.setMinimumWidth(0);
                view.setMinimumHeight(0);
                return view;
            }
        }

        public TabsAdapter(FragmentActivity activity, TabHost tabHost, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mTabHost = tabHost;
            mViewPager = pager;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
        	String tag = tabSpec.getTag();
        	Log.d(TAG + ".TabsAdapter.addTab(...)", "- addTab - tag: " + tag);
            tabSpec.setContent(new DummyTabFactory(mContext));
            TabInfo info = new TabInfo(tag, clss, args);
            mTabs.add(info);
            mTabHost.addTab(tabSpec);
            notifyDataSetChanged();
        }

        
        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
        	Log.d(TAG + ".TabsAdapter.getItem(...)", "- position: " + position);
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
            //return EventListFragmentLoader.EventListFragment.newInstance(info.args);
        }

        @Override
        public void onTabChanged(String tabId) {
        	Log.d(TAG + ".TabsAdapter.onTabChanged(...)", "- tabId: " + tabId);
            int position = mTabHost.getCurrentTab();
            mViewPager.setCurrentItem(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
        	Log.d(TAG + ".TabsAdapter.onPageSelected(...)", "- position: " + position);
            // Unfortunately when TabHost changes the current tab, it kindly
            // also takes care of putting focus on it when not in touch mode.
            // The jerk.
            // This hack tries to prevent this from pulling focus out of our
            // ViewPager.
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
            
            INSTANCIA.getViewedFragment(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        	//Log.d(TAG + ".TabsAdapter.onPageScrollStateChanged(...)", "- state: " + state);
        }
    }
    
	public String getTabDesc (EnumTab tab, SubSystem selectedSubsystem) {
		String result = tab + " - " + selectedSubsystem;
		if(selectedSubsystem == null) {
			switch(tab) {
				case CLOSED:
					return getString(R.string.closed_voting_tab_lbl);
				case OPEN:
					return getString(R.string.open_voting_tab_lbl);
				case PENDING:
					return getString(R.string.pending_voting_tab_lbl);
				default: 
					Log.d(TAG, " - selectedSubsystem: " + selectedSubsystem 
							+ " - unknown tab: " + tab);
			}
		} else {
			switch(selectedSubsystem) {
				case CLAIMS:
					switch(tab) {
						case CLOSED:
							return getString(R.string.closed_claim_tab_lbl);
						case OPEN:
							return getString(R.string.open_claim_tab_lbl);
						case PENDING:
							return getString(R.string.pending_claim_tab_lbl);
						default: 
							Log.d(TAG, " - selectedSubsystem: " + selectedSubsystem 
									+ " - unknown tab: " + tab);
					}
					break;
				case MANIFESTS:
					switch(tab) {
						case CLOSED:
							return getString(R.string.closed_manifest_tab_lbl);
						case OPEN:
							return getString(R.string.open_manifest_tab_lbl);
						case PENDING:
							return getString(R.string.pending_manifest_tab_lbl);
						default: 
							Log.d(TAG, " - selectedSubsystem: " + selectedSubsystem 
									+ " - unknown tab: " + tab);
					}
					break;
				case VOTING:
					switch(tab) {
						case CLOSED:
							return getString(R.string.closed_voting_tab_lbl);
						case OPEN:
							return getString(R.string.open_voting_tab_lbl);
						case PENDING:
							return getString(R.string.pending_voting_tab_lbl);
						default: 
							Log.d(TAG, " - selectedSubsystem: " + selectedSubsystem 
									+ " - unknown tab: " + tab);
					}
					break;
				default:
					Log.d(TAG, " - unknown selectedSubsystem: " + selectedSubsystem);
			}
		}
		return result;
	}
    

	@Override
	public void onChangeSubSystem(SubSystem subSystem) {
		Log.d(TAG + ".onChangeSubSystem(...)", "- subSystem: " + subSystem);
		// TODO Auto-generated method stub
		
	}
	
}
