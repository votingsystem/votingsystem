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

package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;

public class ModalProgressDialogFragment extends DialogFragment {

    public static final String TAG = ModalProgressDialogFragment.class.getSimpleName();

    private TextView progress_text;
    private String progressMessage = null;
    private String caption = null;

    public static ModalProgressDialogFragment showDialog(
            FragmentManager fragmentManager, String progressMessage, String caption) {
        ModalProgressDialogFragment dialog = new ModalProgressDialogFragment();
        Bundle args = new Bundle();
        args.putString(ContextVS.MESSAGE_KEY, progressMessage);
        args.putString(ContextVS.CAPTION_KEY, caption);
        dialog.setArguments(args);
        dialog.show(fragmentManager, ModalProgressDialogFragment.TAG);
        return dialog;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            progressMessage = savedInstanceState.getString(ContextVS.MESSAGE_KEY);
            progress_text.setText(progressMessage);
        }
        this.setCancelable(false);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        progressMessage = getArguments().getString(ContextVS.MESSAGE_KEY);
        caption = getArguments().getString(ContextVS.CAPTION_KEY);
        View view = getActivity().getLayoutInflater().inflate(R.layout.modal_progress_dialog, null);
        progress_text = (TextView) view.findViewById(R.id.progress_text);
        ((TextView) view.findViewById(R.id.caption_text)).setText(caption);
        ((ProgressBar) view.findViewById(R.id.progress_bar)).getIndeterminateDrawable().
                setColorFilter(0xFFFF0000, android.graphics.PorterDuff.Mode.MULTIPLY);
        progress_text.setText(progressMessage);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(view);
        return builder.create();
    }

    public void setProgressMessage(String progressMessage) {
        this.progressMessage = progressMessage;
        progress_text.setText(progressMessage);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ContextVS.MESSAGE_KEY, progressMessage);
        outState.putString(ContextVS.CAPTION_KEY, caption);
    }

}