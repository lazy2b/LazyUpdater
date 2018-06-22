package com.lazylibs.updater.view;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.Window;

import com.lazylibs.updater.R;
import com.lazylibs.updater.model.DownloadProgress;
import com.lazylibs.updater.model.DownloadResponseBody;

/**
 * Created by lazy2b on 18/6/11.
 */

public class DownloadProgressDialogFragment extends DialogFragment implements DownloadResponseBody.DownloadProgressListener {

    @SuppressLint("ValidFragment")
    private DownloadProgressDialogFragment() {
    }

    public static DownloadProgressDialogFragment newInstance(boolean isForceUpdate) {
        Bundle args = new Bundle();
        DownloadProgressDialogFragment fragment = new DownloadProgressDialogFragment();
        fragment.setArguments(args);
        fragment.setCancelable(!isForceUpdate);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        if (isCancelable()) {
//            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    dismissAllowingStateLoss();
//                }
//            });
//        }
        progressDialog.setMessage(getString(R.string.progress_dialog_downloading));
        progressDialog.show();
        return progressDialog;
    }

    @Override
    public ProgressDialog getDialog() {
        return (ProgressDialog) super.getDialog();
    }

    @Override
    public void updateProgress(DownloadProgress progress) {
        if (getDialog() != null) {
            getDialog().setMax(100);
            getDialog().setProgress(progress.getProgress());
        }
    }
}
