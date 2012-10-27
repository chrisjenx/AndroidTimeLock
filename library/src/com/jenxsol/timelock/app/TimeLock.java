package com.jenxsol.timelock.app;

import java.lang.ref.SoftReference;
import java.util.Calendar;
import java.util.Date;

import com.jenxsol.timelock.BuildConfig;
import com.jenxsol.timelock.utils.DialogSupport;
import com.jenxsol.timelock.utils.SoftHashSet;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

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
     * Last set activity when navigating, remember can be null if the last
     * activity is killed. we never hold strong reference too it.
     */
    private static SoftReference<Activity> mLastActivity = null;

    /**
     * Static set of activities, these should be held as long as the activities
     * are not being destroyed. in which case we don't care as they will be
     * created going back to them.<br>
     * needs a bit of testing, if any oom are caused by this would like to
     * know..
     */
    private static final SoftHashSet<Activity> mActivityStack = new SoftHashSet<Activity>(10);

    /**
     * @hide
     * @return
     */
    static SoftHashSet<Activity> getActivityStack()
    {
        return mActivityStack;
    }

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
     * <p>
     * Its worth noting that this method is not always quick especially on old
     * devices with slow IO. I recommend you wrap this in an async task. I shall
     * try to improve this in the future.
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
     * Date the app build was created.
     */
    private final Date mAppCreatedDate;
    /**
     * Context. SHould use this for dialogs
     */
    private Application mApp;
    /**
     * Has app expired.
     */
    private boolean mHasExpired = false;

    // Internal Consts
    private static boolean enable = BuildConfig.DEBUG;
    private static long timeout = TimeLengths.WEEK;
    private static TimeOutEffect timeOutEffect = TimeOutEffect.KILL_DIALOG;
    private static String mKillMessage = "This is a development build, which has now expired. Please aquire a newer version.";
    private static String mKillTitle = "App is too old";

    protected TimeLock(Context ctx)
    {
        setContext(ctx);
        mAppCreatedDate = TimeLockSupport.getApplicationBuildDate(ctx);
    }

    private TimeLock setContext(Context ctx)
    {
        if (ctx == null)
        {
            throw new InstantiationError("You need to pass a valid context to time lock");
        }
        if (ctx instanceof Application)
        {
            mApp = (Application) ctx;
        }
        else if (ctx instanceof Activity)
        {
            mApp = ((Activity) ctx).getApplication();
        }
        else if (ctx instanceof Service)
        {
            mApp = ((Service) ctx).getApplication();
        }
        else
        {
            throw new InstantiationError(
                    "Your context must be able to provide the application object (Activity/Service/Applicaiton objects");
        }
        if (ctx instanceof Activity)
        {

            mActivityStack.add((Activity) ctx);
            mLastActivity = new SoftReference<Activity>((Activity) ctx);
        }

        return this;
    }

    /**
     * Sets the KillDialog text, as well as setting the TimeOutEffect to
     * {@link TimeOutEffect#KILL_DIALOG}
     * 
     * @param title
     *            dialog title
     * @param message
     *            dialog message
     * @return self
     * @since 1.1
     */
    public TimeLock setKillDialog(String title, String message)
    {
        mKillTitle = title;
        mKillMessage = message;
        timeOutEffect = TimeOutEffect.KILL_DIALOG;
        return this;
    }

    /**
     * Fires a check of now vs the app creation date and the settings provided.
     * 
     * @return
     */
    public TimeLock check()
    {
        if (mApp != null && mLastActivity == null || mLastActivity.get() == null)
        {
            // Means they have called check from the application class
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
     * By default it is based of Build.DEBUG, meaning when building to your
     * device/emulator it is enabled and disabled when exporting the app.
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
        // Wont auto fire check on application class
        if (!TimeLockSupport.isApplication(mApp)) doCheck();
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

    /**
     * Will return the date when the app will stop working
     * 
     * @since 1.2
     * @return date in the future (unless its already past)
     */
    public Date getExpiresDate()
    {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(mAppCreatedDate);
        cal.add(Calendar.SECOND, (int) (timeout / 1000));
        return cal.getTime();
    }

    /**
     * Will return the remaining amount of time before the app build expires in
     * millis
     * 
     * @return millis until the App expires.. this can be a negative number
     *         (meaning its expired)
     */
    public long getRemainingMillis()
    {
        final Calendar cal = Calendar.getInstance();
        final long nowLong = cal.getTimeInMillis();
        cal.setTime(mAppCreatedDate);
        cal.add(Calendar.SECOND, (int) (timeout / 1000));
        final long futureLong = cal.getTimeInMillis();
        return nowLong - futureLong;
    }

    private void doCheck()
    {
        if (enable) Log.d(TAG, "TimeLock - Check Started");
        if (!enable) return;
        if (timeout <= 0) return;

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
            mHasExpired = true;
        }

    }

    /**
     * Will pull apart the mLastActivityRefrence.
     * 
     * @return null or the last activity
     */
    private Activity getLastActivity()
    {
        if (null == mLastActivity || null == mLastActivity.get())
        {
            mLastActivity = null;
        }
        return null;
    }

    /**
     * Will look at the TimeOutEffect and perform based on that
     */
    private void handleExit()
    {
        // We have called expired before.. so skip repeating messages to users
        // if (mHasExpired)
        // {
        // TimeLockSupport.exit(mCtx);
        // }
        Handler h = new Handler(Looper.getMainLooper());
        switch (timeOutEffect)
        {

            case KILL_DIALOG:
                if (getLastActivity() != null)
                {
                    DialogSupport.timeOutDialog(getLastActivity(), mKillTitle, mKillMessage);
                    break;
                }
            case KILL_TOAST:
                // TODO show toast and go bye bye
                Toast.makeText(mApp, mKillMessage, Toast.LENGTH_LONG).show();
                h.postDelayed(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        TimeLockSupport.exit(mApp);
                    }
                }, 4000);
                break;
            case ASSASSINATE:
                // Good by :'(
                TimeLockSupport.exit(mApp);
                break;

            case NONE:
            default:
                // TODO: how should be handle this?
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
