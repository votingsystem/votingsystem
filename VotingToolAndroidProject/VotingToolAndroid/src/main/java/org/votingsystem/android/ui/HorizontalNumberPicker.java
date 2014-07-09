package org.votingsystem.android.ui;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.votingsystem.android.R;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class HorizontalNumberPicker extends LinearLayout {

    public static final String TAG = HorizontalNumberPicker.class.getSimpleName();

    private EditText edit_text;
    private Button btn_plus;
    private Button btn_minus;
    private BigDecimal maxValue = new BigDecimal(0);

    private AtomicBoolean isLongPressed = new AtomicBoolean(false);

    private Handler handler;

    final Runnable incrementRunnable = new Runnable(){
        @Override public void run() {
            incrementAmount();
        }
    };

    final Runnable decrementRunnable = new Runnable(){
        @Override public void run() {
            decrementAmount();
        }
    };


    public HorizontalNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater layoutInflater = (LayoutInflater)context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.horizontal_number_picker, this);
        edit_text = (EditText) findViewById(R.id.edit_text);
        btn_plus = (Button) findViewById(R.id.btn_plus);
        btn_plus.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                incrementAmount();
            }
        });
        btn_minus = (Button) findViewById(R.id.btn_minus);

        handler = new Handler();

        btn_minus.setOnTouchListener(new OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    isLongPressed.set(true);
                    decrementAmount();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    isLongPressed.set(false);
                    handler.removeCallbacks(decrementRunnable);
                }
                return true;
            };
        });
        btn_plus.setOnTouchListener(new OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    isLongPressed.set(true);
                    incrementAmount();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    isLongPressed.set(false);
                    handler.removeCallbacks(incrementRunnable);
                }
                return true;
            };
        });
    }

    public void setMaxValue(String maxValue) {
        this.maxValue = new BigDecimal(maxValue);
    }

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }


    public BigDecimal getValue() {
        BigDecimal result = new BigDecimal(edit_text.getText().toString());
        return result;
    }

    private void incrementAmount() {
        BigDecimal result = getValue().add(new BigDecimal(10));
        if (result.compareTo(maxValue) < 0) {
            edit_text.setText(result.toString());
        }
        if(isLongPressed.get()) handler.postDelayed(incrementRunnable, 200);
    }

    private void decrementAmount() {
        BigDecimal result = getValue().add(new BigDecimal(10).negate());
        if (result.compareTo(new BigDecimal(0)) >= 0) {
            edit_text.setText(result.toString());
        }
        if(isLongPressed.get()) handler.postDelayed(decrementRunnable, 200);
    }

}
