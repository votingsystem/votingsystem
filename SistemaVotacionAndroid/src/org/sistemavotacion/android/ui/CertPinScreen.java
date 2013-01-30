package org.sistemavotacion.android.ui;

import org.sistemavotacion.android.R;

import com.actionbarsherlock.app.SherlockDialogFragment;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
/**
 * Para evitar que salga bien hay que poner en las actividades que lo utilicen:
 * android:screenOrientation="portrait"
 * Basado en com.android.internal.policy.impl.SimUnlockScreen.java de Android-Level7
 */
public class CertPinScreen extends SherlockDialogFragment 
	implements View.OnClickListener {

	public static final String TAG = "CertPinScreen";
	
	public static final int PASSWORD_LENGTH = 4;
	
    private CertPinScreenCallback mCallback;

    private TextView mHeaderText;
    private TextView voteValueText;
    private TextView mPinText;
    private TextView mOkButton;
    private View mBackSpaceButton;
    private final int[] mEnteredPin = {0, 0, 0, 0, 0, 0, 0, 0};
    private int mEnteredDigits = 0;

    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    
    Context mContext;
    
    View pinScreenView;
    
    public static CertPinScreen newInstance(CertPinScreenCallback callback, 
    		String message, boolean headerVisibility) {
    	CertPinScreen certPinScreen = new CertPinScreen();
    	certPinScreen.setCallback(callback);
        Bundle args = new Bundle();
        args.putString("message", message);
        args.putBoolean("isHeaderVisible", headerVisibility);
        certPinScreen.setArguments(args);
        return certPinScreen;
    }
    
    private void setCallback(CertPinScreenCallback callback) {
    	mCallback = callback;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme_Panel);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	Log.d(TAG + ".onResume", "onCreateView -- ");
        pinScreenView = inflater.
        		inflate(R.layout.pin_screen_portrait, container, true);
        new TouchInput();
        mHeaderText = (TextView) pinScreenView.findViewById(R.id.headerText);
        
        voteValueText = (TextView) pinScreenView.findViewById(R.id.voteValueText);
        
        mPinText = (TextView) pinScreenView.findViewById(R.id.pinDisplay);
        mBackSpaceButton = pinScreenView.findViewById(R.id.backspace);
        mBackSpaceButton.setOnClickListener(this);
        mOkButton = (TextView) pinScreenView.findViewById(R.id.ok);
        mHeaderText.setText(R.string.keyguard_password_enter_pin_code);
        mPinText.setFocusable(false);
        mOkButton.setOnClickListener(this);
        pinScreenView.setFocusableInTouchMode(true);
        setMessage(getArguments().getString("message"));
        if(getArguments().getBoolean("isHeaderVisible")) {
        	mHeaderText.setVisibility(View.VISIBLE);
        } else mHeaderText.setVisibility(View.GONE);
        //getDialog().getWindow().setLayout(300, 300);
        return pinScreenView;
    }
    
    public void setMessage(String message) {
    	if(message == null) message = "";
    	voteValueText.setText(message);
    }
    
    public void resetPin() {
    	mPinText.setText("");
        mEnteredDigits = 0;
    }
    
    @Override public void onResume() {
    	Log.d(TAG + ".onResume", "onResume -- ");
    	super.onResume();
        mHeaderText.setText(R.string.keyguard_password_enter_pin_code);
        mPinText.setText("");
        mEnteredDigits = 0;
    }


    public void onClick(View v) {
        if (v == mBackSpaceButton) {
            final Editable digits = mPinText.getEditableText();
            final int len = digits.length();
            if (len > 0) {
                digits.delete(len-1, len);
                mEnteredDigits--;
            }
        } else if (v == mOkButton) {
            checkPin();
        }
    }


    private void checkPin() {
        // make sure that the pin is at least 4 digits long.
        if (mEnteredDigits < PASSWORD_LENGTH) {
            mHeaderText.setText(mContext.getString(R.string.pin_short, PASSWORD_LENGTH));
            //mPinText.setText("");
            //mEnteredDigits = 0;
            return;
        } else  mCallback.setPin(mPinText.getText().toString());

        /*new CheckCertPin(mPinText.getText().toString()) {
            void certResponse(boolean success) {
                if (progressDialog != null) {
                    progressDialog.hide();
                }
                if (success) {
                    
                } else {
                    mHeaderText.setText(R.string.keyguard_password_wrong_pin_code);
                    mPinText.setText("");
                    mEnteredDigits = 0;
                }
            }
        }.start();*/
    }

    @Override public void onStop() {
        super.onStop();
    	Log.d(TAG +  ".onStop()", " - onStop - ");
    }
    
    @Override public void onPause() {
        super.onPause();
    	Log.d(TAG +  ".onPause()", " - onPause - ");
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	Log.d(TAG + ".onKeyDown", " - onKeyDown -- ");
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mCallback.setPin(null);
            return true;
        }
        final char match = event.getMatch(DIGITS);
        if (match != 0) {
            reportDigit(match - '0');
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (mEnteredDigits > 0) {
                mPinText.onKeyDown(keyCode, event);
                mEnteredDigits--;
            }
            return true;
        }
        
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            checkPin();
            mCallback.setPin(mPinText.getText().toString());
            return true;
        }
        return false;
    }

    private void reportDigit(int digit) {
        if (mEnteredDigits == 0) {
            mPinText.setText("");
        }
        if (mEnteredDigits == PASSWORD_LENGTH) {
            return;
        }
        mPinText.append(Integer.toString(digit));
        mEnteredPin[mEnteredDigits++] = digit;
    }

    /**
     * Since request can block, we want to run the request in a separate thread
     * with a callback.
     */
    private abstract class CheckCertPin extends Thread {

        private final String mPin;

        protected CheckCertPin(String pin) {
            mPin = pin;
        }

        abstract void certResponse(boolean success);

        @Override
        public void run() {
            /*try {
                final boolean result = ITelephony.Stub.asInterface(ServiceManager
                        .checkService("phone")).supplyPin(mPin);
                post(new Runnable() {
                    public void run() {
                        certResponse(result);
                    }
                });
            } catch (RemoteException e) {
                post(new Runnable() {
                    public void run() {
                        certResponse(false);
                    }
                });
            }*/
        }
    }
    
    /**
     * Helper class to handle input from touch dialer.  Only relevant when
     * the keyboard is shut.
     */
    private class TouchInput implements View.OnClickListener {
    	
        private TextView mZero;
        private TextView mOne;
        private TextView mTwo;
        private TextView mThree;
        private TextView mFour;
        private TextView mFive;
        private TextView mSix;
        private TextView mSeven;
        private TextView mEight;
        private TextView mNine;
        private TextView mCancelButton;

        private TouchInput() {
            mZero = (TextView)pinScreenView.findViewById(R.id.zero);
            mOne = (TextView)pinScreenView.findViewById(R.id.one);
            mTwo = (TextView)pinScreenView.findViewById(R.id.two);
            mThree = (TextView)pinScreenView.findViewById(R.id.three);
            mFour = (TextView)pinScreenView.findViewById(R.id.four);
            mFive = (TextView)pinScreenView.findViewById(R.id.five);
            mSix = (TextView)pinScreenView.findViewById(R.id.six);
            mSeven = (TextView)pinScreenView.findViewById(R.id.seven);
            mEight = (TextView)pinScreenView.findViewById(R.id.eight);
            mNine = (TextView)pinScreenView.findViewById(R.id.nine);
            mCancelButton = (TextView)pinScreenView.findViewById(R.id.cancel);

            mZero.setText("0");
            mOne.setText("1");
            mTwo.setText("2");
            mThree.setText("3");
            mFour.setText("4");
            mFive.setText("5");
            mSix.setText("6");
            mSeven.setText("7");
            mEight.setText("8");
            mNine.setText("9");

            mZero.setOnClickListener(this);
            mOne.setOnClickListener(this);
            mTwo.setOnClickListener(this);
            mThree.setOnClickListener(this);
            mFour.setOnClickListener(this);
            mFive.setOnClickListener(this);
            mSix.setOnClickListener(this);
            mSeven.setOnClickListener(this);
            mEight.setOnClickListener(this);
            mNine.setOnClickListener(this);
            mCancelButton.setOnClickListener(this);
        }


        public void onClick(View v) {
            if (v == mCancelButton) {
                mCallback.setPin(null);
                return;
            }

            final int digit = checkDigit(v);
            if (digit >= 0) {
                reportDigit(digit);
            }
        }

        private int checkDigit(View v) {
            int digit = -1;
            if (v == mZero) {
                digit = 0;
            } else if (v == mOne) {
                digit = 1;
            } else if (v == mTwo) {
                digit = 2;
            } else if (v == mThree) {
                digit = 3;
            } else if (v == mFour) {
                digit = 4;
            } else if (v == mFive) {
                digit = 5;
            } else if (v == mSix) {
                digit = 6;
            } else if (v == mSeven) {
                digit = 7;
            } else if (v == mEight) {
                digit = 8;
            } else if (v == mNine) {
                digit = 9;
            }
            return digit;
        }
    }
}
