package org.votingsystem.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ConfirmImageActivity extends ActionBarActivity {
	
	public static final String TAG = ConfirmImageActivity.class.getSimpleName();

    private ImageView image;
    private Uri imageUri;
    private Bitmap printedBitmap;


    @Override protected void onCreate(Bundle savedInstanceState) {
        //boolean isTablet = getResources().getBoolean(R.bool.isTablet); this doesn't work
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.confirm_image_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        image = (ImageView) findViewById(R.id.selected_image);
        imageUri = (Uri) getIntent().getParcelableExtra(ContextVS.URI_KEY);
        String title = getIntent().getStringExtra(ContextVS.CAPTION_KEY);
        if(imageUri != null) {
            try {
                title = getString(R.string.confirm_selection_msg);
                ParcelFileDescriptor parcelFileDescriptor =
                        getContentResolver().openFileDescriptor(imageUri, "r");
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                printedBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                parcelFileDescriptor.close();
                image.setImageBitmap(printedBitmap);
                Button accept_button = (Button) findViewById(R.id.accept_button);
                accept_button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        sendResult(Activity.RESULT_OK);
                    }
                });
                Button cancel_button = (Button) findViewById(R.id.cancel_button);
                cancel_button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        finish();
                    }
                });
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(title);
    }

    private void sendResult(int result) {
        Intent resultIntent = null;
        if(result == Activity.RESULT_OK) {
            resultIntent = new Intent();
            resultIntent.setData(imageUri);
            LOGD(TAG + ".sendResult", "printedBitmap.getWidth(): " + printedBitmap.getWidth() +
                    " - view.getHeight(): " + printedBitmap.getHeight());
            //image.setDrawingCacheEnabled(true);
            //Bitmap capturedBitmap = Bitmap.createBitmap(image.getDrawingCache());
            //image.setDrawingCacheEnabled(false);
            Bitmap capturedBitmap = printedBitmap;
            BigDecimal imageHeight = new BigDecimal(printedBitmap.getHeight());
            BigDecimal imageWidth = new BigDecimal(printedBitmap.getWidth());
            BigDecimal scaleFactor = imageWidth.divide(
                    new BigDecimal(ContextVS.MAX_REPRESENTATIVE_IMAGE_WIDTH), 2, RoundingMode.CEILING);
            imageHeight = imageHeight.divide(scaleFactor, 2, RoundingMode.CEILING);
            imageWidth = new BigDecimal(ContextVS.MAX_REPRESENTATIVE_IMAGE_WIDTH);
            LOGD(TAG + ".onActivityResult", "imageWidth: " + imageWidth +
                    " - imageHeight: " + imageHeight + " - scaleFactor: " + scaleFactor);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(capturedBitmap,
                    imageWidth.intValue(), imageHeight.intValue(), true);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            LOGD(TAG + ".onActivityResult", "captured byteArray length: " + byteArray.length);
            resultIntent.putExtra(ContextVS.IMAGE_KEY, byteArray);
        }
        setResult(result, resultIntent);
        finish();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}