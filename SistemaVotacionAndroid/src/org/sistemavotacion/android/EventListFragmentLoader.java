package org.sistemavotacion.android;

import static org.sistemavotacion.android.Aplicacion.*;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.android.ui.VoteReceiptListScreen;
import org.sistemavotacion.json.DeJSONAObjeto;
import org.sistemavotacion.json.DeObjetoAJSON;
import org.sistemavotacion.modelo.Consulta;
import org.sistemavotacion.modelo.DatosBusqueda;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.Tipo;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.EnumTab;
import org.sistemavotacion.util.HttpHelper;
import org.sistemavotacion.util.ServerPaths;
import org.sistemavotacion.util.SubSystem;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView.FindListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

public class EventListFragmentLoader extends FragmentActivity {
	
	public static final String TAG = "EventListFragmentLoader";
	
	public static final String MENU_SEARCH_TITLE = "MENU_SEARCH_TITLE";
	
	private static String errorLoadingEventsMsg = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();

        // Create the list fragment and add it as our sole content.
        if (fm.findFragmentById(android.R.id.content) == null) {
        	EventListFragment list = new EventListFragment();
            fm.beginTransaction().add(android.R.id.content, list).commit();
        }
    }
    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<Evento> ALPHA_COMPARATOR = new Comparator<Evento>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(Evento object1, Evento object2) {
            return sCollator.compare(object1.getAsunto(), object2.getAsunto());
        }
    };
    
    public static class EventEntry {
    	
    	Evento event;
    	
        public EventEntry(Evento event) {
            this.event = event;
        }

        public Evento getEvento() {
            return event;
        }

    }
    
    public static class EventListFragment extends ListFragment
			implements LoaderManager.LoaderCallbacks<List<EventEntry>> {
		
		EventListAdapter mAdapter = null;
		EnumTab enumTab = null;
		SubSystem subSystem = SubSystem.VOTING;
        String queryStr = null;
        int offset = 0;
		
		@Override public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
	        Bundle args = getArguments();
	        String enumTabStr = null;
	        String subSystemStr = "";
	        if (args != null) {
	        	enumTabStr = args.getString("enumTab");
	        	if(enumTabStr != null) enumTab = EnumTab.valueOf(enumTabStr);
	        	subSystemStr = args.getString("subSystem");
	        	if(subSystemStr != null) subSystem = SubSystem.valueOf(subSystemStr);
	        	queryStr = args.getString(SearchManager.QUERY);
	        	offset = args.getInt("offset");
	        }
	        if(Aplicacion.INSTANCIA == null)
	        	getActivity().setTitle(subSystemStr);
	        else getActivity().setTitle(Aplicacion.INSTANCIA.getSubsystemDesc(subSystem));
	        setHasOptionsMenu(true);
	        Log.d(TAG +  ".EventListFragment.onCreate(..)", " - enumTab: " + enumTab + 
	        		" - subSystem: " + subSystem + " - queryStr: " + queryStr);
		};

		
		@Override public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			setEmptyText(getActivity().getApplicationContext().
					getString(R.string.empty_search_lbl));
			getView().setBackgroundColor(Color.WHITE);
			// We have a menu item to show in action bar.
			setHasOptionsMenu(true);
			// Create an empty adapter we will use to display the loaded data.
			mAdapter = new EventListAdapter(getActivity());
			setListAdapter(mAdapter);
			// Start out with a progress indicator.
			setListShown(false);	
			// Prepare the loader.  Either re-connect with an existing one,
			// or start a new one.
			getLoaderManager().initLoader(0, null, this);
		}

		

		@Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {	
			Log.d(TAG +  "onCreateOptionsMenu(..)", " onCreateOptionsMenu - onCreateOptionsMenu");			
			inflater.inflate(R.menu.main, menu);
	        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ||
	        		Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				 // Associate searchable configuration with the SearchView
			    SearchManager searchManager =
			           (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
			    SearchView searchView =
			            (SearchView) menu.findItem(R.id.action_search).getActionView();
			    searchView.setSearchableInfo(
			            searchManager.getSearchableInfo(getActivity().getComponentName()));
	        	//1 -> index of documents menu item on main.xml
	        	menu.getItem(1).setVisible(false);
	        }
		}
		
		@Override public boolean onOptionsItemSelected(MenuItem item) {
			Log.d(TAG +  ".EventListFragment.onOptionsItemSelected", 
					" - Title: " + item.getTitle() + " - ItemId: " + item.getItemId());
			Intent intent = null;
			if(MENU_SEARCH_TITLE.equals(item.getTitle()))
				getActivity().onSearchRequested();
			switch (item.getItemId()) { 
				case R.id.action_search:
					getActivity().onSearchRequested();
					break;
				case R.id.votings:
					Aplicacion.INSTANCIA.setSelectedSubsystem(SubSystem.VOTING);
					break;
				case R.id.manifests:
					Aplicacion.INSTANCIA.setSelectedSubsystem(SubSystem.MANIFESTS);
					break;
				case R.id.claims:
					Aplicacion.INSTANCIA.setSelectedSubsystem(SubSystem.CLAIMS);
					break;	
				case R.id.reload:
					intent =  new Intent(getActivity(), FragmentTabsPager.class);
					break;
				case R.id.get_cert:
	          	  	switch(Aplicacion.INSTANCIA.getEstado()) {
				    	case SIN_CSR:
				    		intent = new Intent(getActivity(), Aplicacion.class);
				    		break;
				    	case CON_CSR:
				    		intent = new Intent(getActivity(), UserCertResponseForm.class);
				    		break;
				    	case CON_CERTIFICADO:
							AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
				    		builder.setTitle(getString(R.string.
				    				menu_solicitar_certificado));
				    		builder.setMessage(Html.fromHtml(
				    				getString(R.string.request_cert_again_msg)));
				    		builder.setPositiveButton(getString(
				    				R.string.ok_button), new DialogInterface.OnClickListener() {
				    		            public void onClick(DialogInterface dialog, int whichButton) {
				    		            	Intent intent = new Intent(getActivity(), UserCertRequestForm.class);
				    		            	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				    		            	startActivity(intent);
				    		            }
				    					});
				    		builder.setNegativeButton(getString(
				    				R.string.cancelar_button), null);
				    		builder.show();
	          	  	}
					break;	
				case R.id.receipt_list:
					intent = new Intent(getActivity(), VoteReceiptListScreen.class);
					break;	
				case R.id.publish_document:
					showPublishDialog();
					break;						
			
			}
      	  	if(intent != null) {
          	  	//intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      	  		intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            	startActivity(intent);
      	  	}
	        return true;
		};
		
		private void showPublishDialog(){
            Dialog dialog = new AlertDialog.Builder(getActivity())
            	.setTitle(R.string.publish_document_lbl).setIcon(R.drawable.view_detailed_32)
            	.setItems(R.array.publish_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
 					   Intent intent = new Intent(getActivity(), WebActivity.class);
 					   switch (which) { 
 						   case 0:
 							   intent.putExtra(WebActivity.SCREEN_EXTRA_KEY, 
 									   WebActivity.Screen.PUBLISH_VOTING.toString());
 							   break;
 						   case 1:
 							   intent.putExtra(WebActivity.SCREEN_EXTRA_KEY, 
 									   WebActivity.Screen.PUBLISH_MANIFEST.toString());
 							   break;
 						   case 2:
 							   intent.putExtra(WebActivity.SCREEN_EXTRA_KEY, 
 									   WebActivity.Screen.PUBLISH_CLAIM.toString());
 							   break;
 					   }
 					   startActivity(intent);
                        //String[] items = getResources().getStringArray(R.array.select_dialog_items);
                        //"You selected: " + which + " , " + items[which]
                    }
                })
                .create();
            dialog.show();
            	
            	/*
            	.setItems(R.array.select_dialog_items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    String[] items = getResources().getStringArray(R.array.select_dialog_items);
                    new AlertDialog.Builder(AlertDialogSamples.this)
                            .setMessage("You selected: " + which + " , " + items[which])
                            .show();
                }
            })
            .create();
			
			
			
			
			
			
			PopupMenu popupMenu = new PopupMenu(getActivity(), getActivity().findViewById(R.id.operations));
			popupMenu.getMenuInflater().inflate(R.menu.publish_document, popupMenu.getMenu());    
			popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { 
				   @Override
				   public boolean onMenuItemClick(MenuItem item) {
					   Intent intent = new Intent(getActivity(), WebActivity.class);
					   switch (item.getItemId()) { 
						   case R.id.publish_voting:
							   intent.putExtra(WebActivity.SCREEN_EXTRA_KEY, 
									   WebActivity.Screen.PUBLISH_VOTING.toString());
							   break;
						   case R.id.publish_manifest:
							   intent.putExtra(WebActivity.SCREEN_EXTRA_KEY, 
									   WebActivity.Screen.PUBLISH_MANIFEST.toString());
							   break;
						   case R.id.publish_claim:
							   intent.putExtra(WebActivity.SCREEN_EXTRA_KEY, 
									   WebActivity.Screen.PUBLISH_CLAIM.toString());
							   break;
					   }
					   startActivity(intent);
					   return true;
				   }
			  });
	        popupMenu.show();*/
		}
		
		@Override public void onListItemClick(ListView l, View v, int position, long id) {
			Log.d(TAG +  "EventListFragment.onListItemClick", "Item clicked: " + id);
	        Evento event = ((EventEntry) getListAdapter().getItem(position)).getEvento();
	        Aplicacion.INSTANCIA.setEventoSeleccionado(event);
			Intent intent = null;
			if (event.getTipo().equals(Tipo.EVENTO_VOTACION))
				intent = new Intent(getActivity(), VotingEventScreen.class);
			 else intent = new Intent(getActivity(), EventScreen.class);
			startActivity(intent);
		}
		
		@Override public Loader<List<EventEntry>> onCreateLoader(int id, Bundle args) {
	        Log.d(TAG +  ".EventListFragment.onCreateLoader(..)", " - enumTab: " + enumTab + " - subSystem: " + subSystem);
			return new EventListLoader(getActivity(), subSystem, enumTab, queryStr, offset);
		}
		
	    @Override public void onAttach(Activity activity) {
	    	Log.i(TAG +  ".EventListFragment.onAttach", " - activity: " + activity.getClass().getName());
	        super.onAttach(activity);
	    }
		
	    @Override public void onStop() {
	        super.onStop();
	        //mActivity.removeFragment(this);
	    	Log.d(TAG +  ".onStop()", " - onStop - ");
	    }
	    
		@Override public void onLoadFinished(Loader<List<EventEntry>> loader, List<EventEntry> data) {
			Log.i(TAG +  ".EventListFragment.onLoadFinished", " - onLoadFinished ");
			if(errorLoadingEventsMsg == null) {
				mAdapter.setData(data);
				if(Aplicacion.INSTANCIA != null)
					Aplicacion.INSTANCIA.checkConnection();
			} else {
				setEmptyText(getAppString(R.string.connection_error_msg));
				errorLoadingEventsMsg = null;			
			}
			if (isResumed()) {
			    setListShown(true);
			} else {
			    setListShownNoAnimation(true);
			}
		}
		
		@Override public void onLoaderReset(Loader<List<EventEntry>> loader) {
			Log.i(TAG +  ".EventListFragment.onLoaderReset", " - onLoaderReset ");
			mAdapter.setData(null);
		}
		
		
	    public static class EventListAdapter extends ArrayAdapter<EventEntry> {
	    	
	        private final LayoutInflater mInflater;
	
	        public EventListAdapter(Context context) {
	            super(context, R.layout.row_evento);
	            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        }
	
	        public void setData(List<EventEntry> data) {
	            clear();
	            if (data != null) {
	                for (EventEntry event : data) {
	                    add(event);
	                }
	            }
	        }
	
	        /**
	         * Populate new items in the list.
	         */
	        @Override public View getView(int position, View convertView, ViewGroup parent) {
	            View view;
	            if (convertView == null) {
	                view = mInflater.inflate(R.layout.row_evento, parent, false);
	            } else {
	                view = convertView;
	            }
	            Evento evento = getItem(position).getEvento();
	            if (evento != null) {
	            	LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
	            	linearLayout.setBackgroundColor(Color.WHITE);
	                TextView subject = (TextView) view.findViewById(R.id.event_subject);
	                TextView dateInfo = (TextView) view.findViewById(R.id.event_date_info);
	                TextView author = (TextView) view.findViewById(R.id.event_author);
	                
	                subject.setText(evento.getAsunto());
	                String dateInfoStr = null;
	                ImageView imgView = (ImageView)view.findViewById(R.id.event_icon);
	                switch(evento.getEstadoEnumValue()) {
		                case ACTIVO:
		                	imgView.setImageResource(R.drawable.open);
		                	dateInfoStr = "<b>" + getAppString(R.string.remain_lbl, 
	                				DateUtils.getElpasedTimeHoursFromNow(evento.getFechaFin()))  +"</b>";
		                	break;
		                case PENDIENTE_COMIENZO:
		                	imgView.setImageResource(R.drawable.pending);
		                	dateInfoStr = "<b>" + getAppString(R.string.inicio_lbl) + "</b>: " + 
		                			DateUtils.getShortSpanishStringFromDate(evento.getFechaInicio()) + " - " + 
                					"<b>" + getAppString(R.string.fin_lbl) + "</b>: " + 
		                			DateUtils.getShortSpanishStringFromDate(evento.getFechaFin());
		                	break;
		                case FINALIZADO:
		                	imgView.setImageResource(R.drawable.closed);
		                	dateInfoStr = "<b>" + getAppString(R.string.inicio_lbl) + "</b>: " + 
		                			DateUtils.getShortSpanishStringFromDate(evento.getFechaInicio()) + " - " + 
                					"<b>" + Aplicacion.INSTANCIA.
			                		getAppString(R.string.fin_lbl) + "</b>: " + 
		                			DateUtils.getShortSpanishStringFromDate(evento.getFechaFin());
		                	break;
	                }
	                if(dateInfoStr != null) dateInfo.setText(Html.fromHtml(dateInfoStr));
	                else dateInfo.setVisibility(View.GONE);
	                if(evento.getUsuario() != null && !"".equals(
	                		evento.getUsuario().getNombreCompleto())) {
		                String authorStr =  "<b>" + getAppString(R.string.author_lbl) + "</b>: " + 
		                		evento.getUsuario().getNombreCompleto();
		                author.setText(Html.fromHtml(authorStr));
	                } else author.setVisibility(View.GONE);
	            }
	            return view;
	        }
	    }

	    
	    /**
	     * A custom Loader that loads events
	     */
	    public static class EventListLoader extends AsyncTaskLoader<List<EventEntry>> {
	
	        List<EventEntry> events;
	        SubSystem subSystem;
			EnumTab enumTab;
			String queryString;
			int offset = 0;
	        
	        public EventListLoader(Context context) {
	            super(context);
	        }
	
	        public EventListLoader(Context context, SubSystem subSystem,
					EnumTab enumTab, String queryString, int offset) {
	        	super(context);
	        	this.subSystem = subSystem;
	        	this.enumTab = enumTab;
	        	this.queryString = queryString;
	        	this.offset = offset;
			}
	
			/**
	         * This is where the bulk of our work is done.  This function is
	         * called in a background thread and should generate a new set of
	         * data to be published by the loader.
	         */
	        @Override public List<EventEntry> loadInBackground() {
	        	Log.d(TAG + ".EventListLoader.loadInBackground()", " - subSystem: " + subSystem + " - enumTab: " + enumTab);

	            List<EventEntry> eventEntryList = null;  
	            try {
	            	HttpResponse response = null;
	                if(queryString != null) {
	                	String url = ServerPaths.getURLSearch(
	                			Aplicacion.CONTROL_ACCESO_URL, 0, Aplicacion.EVENTS_PAGE_SIZE);
	                	DatosBusqueda datosBusqueda = new DatosBusqueda(
	                			subSystem.getEventType(), enumTab.getEventState(), queryString);
	                	response = HttpHelper.sendData(DeObjetoAJSON.
	                			obtenerDatosBusqueda(datosBusqueda), url);
	                } else {
	                	String url = ServerPaths.getURLEventos(
	    	            		Aplicacion.CONTROL_ACCESO_URL, enumTab, subSystem, Aplicacion.EVENTS_PAGE_SIZE, offset);
	                	response = HttpHelper.obtenerInformacion(url);
	                }
	                int statusCode = response.getStatusLine().getStatusCode();
	                if(Respuesta.SC_OK == statusCode) {
	                	Consulta consulta = DeJSONAObjeto.obtenerConsultaEventos(
	                			EntityUtils.toString(response.getEntity()));
	                	eventEntryList = new ArrayList<EventEntry>();
	                	for(Evento evento:consulta.getEventos()) {
	                		eventEntryList.add(new EventEntry(evento));
	                	}
	                } else errorLoadingEventsMsg = response.getStatusLine().toString();
	            } catch (Exception ex) {
	            	Log.e(TAG + ".doInBackground", ex.getMessage(), ex);
	            	errorLoadingEventsMsg = getAppString(R.string.connection_error_msg);
	            }
	            return eventEntryList;
	        }
	
	        /**
	         * Called when there is new data to deliver to the client.  The
	         * super class will take care of delivering it; the implementation
	         * here just adds a little more logic.
	         */
	        @Override public void deliverResult(List<EventEntry> events) {
	            if (isReset()) {
	                // An async query came in while the loader is stopped.  We
	                // don't need the result.
	                if (events != null) {
	                    onReleaseResources(events);
	                }
	            }
	            List<EventEntry> oldEvents = events;
	            this.events = events;
	
	            if (isStarted()) {
	                // If the Loader is currently started, we can immediately
	                // deliver its results.
	                super.deliverResult(events);
	            }
	
	            // At this point we can release the resources associated with
	            // 'oldApps' if needed; now that the new result is delivered we
	            // know that it is no longer in use.
	            if (oldEvents != null) {
	                onReleaseResources(oldEvents);
	            }
	        }
	
	        /**
	         * Handles a request to start the Loader.
	         */
	        @Override protected void onStartLoading() {
	            Log.d(TAG + ".onStartLoading()", " - onStartLoading");
	            if (events != null) {
	                // If we currently have a result available, deliver it
	                // immediately.
	                deliverResult(events);
	            }
	            forceLoad();
	        }
	
	        /**
	         * Handles a request to stop the Loader.
	         */
	        @Override protected void onStopLoading() {
	        	Log.d(TAG + ".onStopLoading()", " - onStopLoading");
	            cancelLoad();
	        }
	
	        /**
	         * Handles a request to cancel a load.
	         */
	        @Override public void onCanceled(List<EventEntry> apps) {
	            super.onCanceled(apps);
	            onReleaseResources(apps);
	        }
	
	        /**
	         * Handles a request to completely reset the Loader.
	         */
	        @Override protected void onReset() {
			    super.onReset();
			    onStopLoading();
			    events = null;
	        }
	
	        /**
	         * Helper function to take care of releasing resources associated
	         * with an actively loaded data set.
	         */
	        protected void onReleaseResources(List<EventEntry> apps) {
	            // For a simple List<> there is nothing to do.  For something
	            // like a Cursor, we would close it here.
	        }
	        
	    }
	
	}
    
}