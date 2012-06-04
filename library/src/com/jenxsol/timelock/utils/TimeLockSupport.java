package com.jenxsol.timelock.utils;

import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.jenxsol.timelock.app.TimeLock;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;

public class TimeLockSupport
{

    /**
     * Will grab the date that the app was built and return it too you as a
     * Date();
     * 
     * @param app
     * @return the date the app was built, otherwise
     */
    public static final Date getApplicationBuildDate(Context app)
    {
        long time = 0;
        try
        {
            ApplicationInfo ai = app.getPackageManager()
                    .getApplicationInfo(app.getPackageName(), 0);
            ZipFile zf = new ZipFile(ai.sourceDir);
            ZipEntry ze = zf.getEntry("classes.dex");
            time = ze.getTime();
            // String s = SimpleDateFormat.getInstance().format(new
            // java.util.Date(time));

        } catch (Exception e)
        {
            return new Date(time);
        }
        return new Date(time);
    }

    /**
     * Auto exit checker, will basically exit how ever it knows best
     * 
     * @param ctx
     */
    public static final void exit(Context ctx)
    {
        if (ctx instanceof Activity)
        {
            ((Activity) ctx).finish();
            for (Activity a : TimeLock.getActivityStack())
            {
                if (null != a)
                {
                    a.finish();
                }
            }

        } else
        {
            System.exit(0);
        }
    }

    /**
     * Checks if is application, very trivial
     * 
     * @param ctx
     * @return
     */
    public static final boolean isApplication(Context ctx)
    {
        if (ctx instanceof Application)
        {
            return true;
        }
        return false;
    }
}
