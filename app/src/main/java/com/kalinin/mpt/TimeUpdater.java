package com.kalinin.mpt;

import android.content.Context;
import android.util.Log;

import com.kalinin.mpt.data.ScheduleUtils;
import com.kalinin.mpt.data.Timepoint;
import com.kalinin.mpt.data.Timepoints;

import java.util.Calendar;



public class TimeUpdater implements Runnable {
    private static final String LOG_TAG = "TimeUpdater";
    private final Timepoints mTimepoints;
    private Listener mListener;

    private int mMaxCountdowns;
    private int mMaxLateMinutes;

    public interface Listener {
        void onTimeUpdated(TimeUpdater timeUpdater);
    }

    public TimeUpdater(Timepoints timepoints, Context context) {
        mTimepoints = timepoints;

        mMaxCountdowns = context.getResources().getInteger(R.integer.time_updater_max_countdowns);
        mMaxLateMinutes = context.getResources().getInteger(R.integer.time_updater_max_late_minutes);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void run() {
        if (mTimepoints == null)
            return;

        synchronized (mTimepoints) {
            Log.i(LOG_TAG, "Updating time...");
            Calendar nowCalendar = Calendar.getInstance();

            int shownCountdowns = 0;
            int firstHourPos = -1;
            for (Timepoint timepoint : mTimepoints.getTimepoints()) {
                Calendar timepointCalendar = ScheduleUtils.getTimepointCalendar(timepoint, mTimepoints.getFirstHour());

                timepoint.millisFromNow = (timepointCalendar.getTimeInMillis() - nowCalendar.getTimeInMillis());

                boolean isLate = false;
                if (timepoint.millisFromNow > -mMaxLateMinutes * 60 * 1000 && timepoint.millisFromNow < 0)
                    isLate = true;

                timepoint.isEnabled = timepoint.millisFromNow > 0 || isLate;

                if (shownCountdowns < mMaxCountdowns && timepoint.isEnabled()) {
                    timepoint.isCountdownShown = true;
                    shownCountdowns += 1;
                } else {
                    timepoint.isCountdownShown = isLate;
                }

                if (firstHourPos == -1 && timepoint.isEnabled)
                    firstHourPos = mTimepoints.getHourPos(timepoint.hour);
            }

            mTimepoints.firstActiveHourPos = firstHourPos;
        }

        if (mListener != null)
            mListener.onTimeUpdated(this);
    }
}
