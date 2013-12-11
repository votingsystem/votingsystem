package com.itextpdf.text;

import android.content.Context;

import com.itextpdf.text.error_messages.MessageLocalization;

/**
 * Created by jgzornoza on 11/12/13.
 */
public class Context_iTextVS {

    private Context androidContext;

    private static Context_iTextVS instance;

    private Context_iTextVS(Context context) {
        androidContext = context;
    }

    public static void init(Context context){
        instance = new Context_iTextVS(context);
        MessageLocalization.init();
    }

    public static Context_iTextVS getInstance(){
        return instance;
    }
    public Context getContext(){
        return androidContext;
    }


}
