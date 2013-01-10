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

package org.sistemavotacion.android.ui;

import org.sistemavotacion.android.R;

import android.content.Context;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Para evitar que salga bien hay que poner en las actividades que lo utilicen:
 * android:screenOrientation="portrait"
 * Basado en com.android.internal.policy.impl.SimUnlockScreen.java de Android-Level7
 */
public class CertPinScreen extends LinearLayout implements View.OnClickListener {
	
	public static final String TAG = "CertPinScreen";
	
	public static final int PASSWORD_LENGTH = 4;
	
    private final CertPinScreenCallback mCallback;

    private TextView mHeaderText;
    private TextView mPinText;
    private TextView mOkButton;
    private View mBackSpaceButton;
    private final int[] mEnteredPin = {0, 0, 0, 0, 0, 0, 0, 0};
    private int mEnteredDigits = 0;

    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    
    Context mContext;

    public CertPinScreen(Context context, CertPinScreenCallback callback) {
        super(context);
        this.mContext = context;
        mCallback = callback;
        LayoutInflater.from(context).inflate(R.layout.pin_screen_portrait, this, true);
        new TouchInput();
        mHeaderText = (TextView) findViewById(R.id.headerText);
        mPinText = (TextView) findViewById(R.id.pinDisplay);
        mBackSpaceButton = findViewById(R.id.backspace);
        mBackSpaceButton.setOnClickListener(this);
        mOkButton = (TextView) findViewById(R.id.ok);
        mHeaderText.setText(R.string.keyguard_password_enter_pin_code);
        mPinText.setFocusable(false);
        mOkButton.setOnClickListener(this);
        setFocusableInTouchMode(true);
    }

    public void setMessage(String message) {
    	 mHeaderText.setText(message);
    }
    
    public void resetPin() {
    	mPinText.setText("");
        mEnteredDigits = 0;
    }
    
    /** {@inheritDoc} */
    public void onResume() {
    	Log.d(TAG + ".onResume", "onResume -- ");
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


    public boolean onKeyDown(int keyCode, KeyEvent event) {
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
            mZero = (TextView) findViewById(R.id.zero);
            mOne = (TextView) findViewById(R.id.one);
            mTwo = (TextView) findViewById(R.id.two);
            mThree = (TextView) findViewById(R.id.three);
            mFour = (TextView) findViewById(R.id.four);
            mFive = (TextView) findViewById(R.id.five);
            mSix = (TextView) findViewById(R.id.six);
            mSeven = (TextView) findViewById(R.id.seven);
            mEight = (TextView) findViewById(R.id.eight);
            mNine = (TextView) findViewById(R.id.nine);
            mCancelButton = (TextView) findViewById(R.id.cancel);

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
