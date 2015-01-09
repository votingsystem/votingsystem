package org.votingsystem.android.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.votingsystem.android.R;
import org.votingsystem.model.AddressVS;
import org.votingsystem.model.Country;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AddressFormFragment extends Fragment {

	public static final String TAG = AddressFormFragment.class.getSimpleName();

    private EditText address;
    private EditText postal_code;
    private EditText city;
    private EditText province;
    private Spinner country_spinner;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        // if set to true savedInstanceState will be allways null
        setHasOptionsMenu(true);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "progressVisible: ");
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.address_form, container, false);
        getActivity().setTitle(getString(R.string.address_lbl));
        Button cancelButton = (Button) rootView.findViewById(R.id.cancel_lbl);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        address = (EditText)rootView.findViewById(R.id.address);
        postal_code = (EditText)rootView.findViewById(R.id.postal_code);
        province = (EditText)rootView.findViewById(R.id.province);
        country_spinner = (Spinner)rootView.findViewById(R.id.country_spinner);
        Country country = Country.valueOf(getResources().getConfiguration().locale.getLanguage().
                toUpperCase());
        if(country != null) country_spinner.setSelection(country.getPosition());
        city = (EditText)rootView.findViewById(R.id.location);
        Button save_button = (Button) rootView.findViewById(R.id.save_button);
        save_button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                submitForm();
            }
        });
        return rootView;
    }

    @Override public void onResume() {
        super.onResume();
    }

    @Override public void onPause() {
        super.onPause();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
		LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
		switch (item.getItemId()) {
	    	case android.R.id.home:
                getActivity().onBackPressed();
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}

    private void submitForm() {
    	InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
  		imm.hideSoftInputFromWindow(city.getWindowToken(), 0);
      	if (validateForm ()) {
            AddressVS addressVS = new AddressVS();
            addressVS.setName(address.getText().toString());
            addressVS.setPostalCode(postal_code.getText().toString());
            addressVS.setCity(city.getText().toString());
            addressVS.setProvince(province.getText().toString());
            addressVS.setCountry(Country.getByPosition(country_spinner.getSelectedItemPosition()));
      	}
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        LOGD(TAG + ".showMessage", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }

    private boolean validateForm () {
    	LOGD(TAG + ".validateForm()", "country position: " + country_spinner.getSelectedItemPosition());
        if(TextUtils.isEmpty(address.getText().toString())){
            showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.enter_address_error_lbl));
            return false;
        }
        if(TextUtils.isEmpty(postal_code.getText().toString())){
            showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.enter_postal_code_error_lbl));
            return false;
        }
        if(TextUtils.isEmpty(city.getText().toString())){
            showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.enter_city_error_lbl));
            return false;
        }
        if(TextUtils.isEmpty(province.getText().toString())){
            showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.enter_province_error_lbl));
            return false;
        }
    	return true;
    }

}