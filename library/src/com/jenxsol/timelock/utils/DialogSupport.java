package com.jenxsol.timelock.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;

public class DialogSupport
{

    /**
     * Show a time out dialog that when cancelled / finished will kill the app.
     * It's quite dirty but thats the point. It shouldn't try to do anything
     * else.
     * 
     * @param ctx
     *            the current app context
     * @param title
     *            the dialog title
     * @param message
     *            the message to tell the user, make it relative informative!
     */
    public static final void timeOutDialog(final Context ctx, final String title,
            final String message)
    {
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle(title);
        b.setMessage(message);
        b.setIcon(android.R.drawable.ic_dialog_info);
        b.setCancelable(false);
        b.setOnCancelListener(new OnCancelListener()
        {

            @Override
            public void onCancel(DialogInterface dialog)
            {
                TimeLockSupport.exit(ctx);
            }
        });
        b.setPositiveButton(android.R.string.ok, new OnClickListener()
        {

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                TimeLockSupport.exit(ctx);
            }
        });
        try
        {
            b.create().show();
        } catch (Exception e)
        {
        }
    }

}
