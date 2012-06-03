package com.jenxsol.timelock.app;

import java.lang.ref.SoftReference;
import java.util.Date;

import com.jenxsol.timelock.BuildConfig;
import com.jenxsol.timelock.utils.DialogSupport;
import com.jenxsol.timelock.utils.TimeLockSupport;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * Make sure to configure this and initialise it in the {@link Application}
 * object.
 * 
 * @author Christopher Jenkins
 * @version 1.0
 * 
 */
public class TimeLock
{

    private static final String TAG = "timelock";

    private static SoftReference<TimeLock> mSelf;

    /**
     * <p>
     * Initialise the time lock library.
     * </p>
     * <p>
     * By default this class is enabled by default when
     * {@link BuildConfig#DEBUG} is true, which is when you generate a debug
     * build (running inside eclipse).<br>
     * Then false if exporting the application.<br>
     * This can be overridden with {@link #setEnabled(boolean)}
     * </p>
     * <p>
     * Default Time Lock is 1 Week. This can be overridden with
     * {@link #setTimeOut(long)}.
     * </p>
     * <p>
     * Default is for the App to show a dialog.
     * </p>
     * 
     * 
     * This holds a {@link SoftReference} to the {@link TimeLock} to reduce full
     * checks for the App being out dated.
     * 
     * @param app
     * @return {@link TimeLock} instance
     */
    public static TimeLock get(Context ctx)
    {
        if (null == mSelf || null == mSelf.get())
            mSelf = new SoftReference<TimeLock>(new TimeLock(ctx));
        mSelf.get().setContext(ctx);
        return mSelf.get();
    }

    /**
     * Context. SHould use this for dialogs
     */
    private Context mCtx;
    /**
     * Date the app build was created.
     */
    private final Date mAppCreatedDate;

    // Internal Consts
    private static boolean enable = BuildConfig.DEBUG;
    private static long timeout = TimeLengths.WEEK;
    private static TimeOutEffect timeOutEffect = TimeOutEffect.KILL_DIALOG;
    private static String mKillMessage = "This is a development build, which has now expired. please aquire a newer version";
    private static String mKillTitle = "App is too old";

    protected TimeLock(Context ctx)
    {
        mCtx = ctx;
        mAppCreatedDate = TimeLockSupport.getApplicationBuildDate(ctx);
    }

    public TimeLock setContext(Context ctx)
    {
        mCtx = ctx;
        return this;
    }

    /**
     * Fires a check of now vs the app creation date and the settings provided.
     * 
     * @return
     */
    public TimeLock check()
    {
        if (mCtx instanceof Application)
        {
            throw new InstantiationError(
                    "Please don't call check() from the application object, do it from onCreate of your activities");
        }
        doCheck();
        return this;
    }

    /**
     * Set the time locking featured to enabled or not. If enabling again will
     * force a re-check of the time. (Meaning it will kill the app if its now
     * moved past the build time limit).
     * 
     * @return self.
     */
    public TimeLock setEnabled(boolean enable)
    {
        boolean check = (!TimeLock.enable && enable);
        TimeLock.enable = enable;
        if (check)
        {
            doCheck();
        }
        return this;
    }

    /**
     * This is the time out in millis from when the app is built. The Class
     * defaults to 1 Week. You can use {@link TimeLengths} for built in lengths.<Br>
     * Setting this time out will check for a vaild time again. Meaning if the
     * app has expired it will kill it.
     * 
     * @param timeLength
     *            the length of time the app is valid for, this is only a
     *            measurement not a future time.<br>
     *            So for a 1 hour build pass in 3600000. Or to make your life
     *            easy! Use {@link TimeLengths}.
     * @return self
     */
    public TimeLock setTimeOut(long timeLength)
    {
        timeout = timeLength;
        doCheck();
        return this;
    }

    /**
     * Gets the App creation date
     * 
     * @return date of the app creation, if it fails it will return unix epoc
     *         time. which you can guess is... well wrong!
     */
    public Date getCreatedDate()
    {
        return mAppCreatedDate;
    }

    private void doCheck()
    {
        if (enable)
            Log.d(TAG, "TimeLock - Check Started");
        if (!enable)
            return;
        if (timeout <= 0)
            return;

        // This check will be improved to use NTP server and TimeZone to make
        // sure we are always right. But for rough locking its fine for now.

        final Date created = getCreatedDate();
        final Date now = new Date(System.currentTimeMillis());
        long createdTime = created.getTime();
        createdTime += timeout;
        final long nowTime = now.getTime();

        if (createdTime < nowTime)
        {
            if (enable)
                Log.d(TAG, "TimeLock - App expired by " + (nowTime - createdTime) + " millis");
            handleExit();
        }

    }

    /**
     * Will look at the TimeOutEffect and perform based on that
     */
    private void handleExit()
    {
        switch (timeOutEffect)
        {
        case NONE:
            break;
        case KILL_TOAST:
        case KILL_DIALOG:
            DialogSupport.timeOutDialog(mCtx, mKillTitle, mKillMessage);
            break;
        case ASSASSINATE:
            // Good by :'(
            System.exit(0);
            break;

        default:
            break;

        }
    }

    /**
     * This enum contains type of time out effects. From NONE, to ASSASSINATE.
     * 
     * @author Chris Jenkins
     * 
     */
    public static enum TimeOutEffect
    {
        /**
         * Does nothing..
         */
        NONE,
        /**
         * Kill and show toast
         */
        KILL_TOAST,
        /**
         * Kill and show dialog.. probably best.. tell the user why!
         */
        KILL_DIALOG,
        /**
         * What! where'd the app go! Oh no's!
         */
        ASSASSINATE;
    }

    public static interface TimeLengths
    {
        /**
         * Could set the time out this short, but not sure why, maybe to remind
         * you to keep building during dev?
         */
        public static final long HOUR = 1000 * 60 * 60;
        public static final long DAY = HOUR * 24;
        public static final long WEEK = DAY * 7;
        /**
         * Two weeks for my American friends.
         */
        public static final long FORTNIGHT = WEEK * 2;
        /**
         * A four week month so 28 days
         */
        public static final long MONTH = WEEK * 4;
    }
}
