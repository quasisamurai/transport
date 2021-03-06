package com.kalinin.mpt.data;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.kalinin.mpt.R;
import com.kalinin.mpt.activities.StopScheduleWidget;
import com.kalinin.mpt.data.providers.BaseScheduleProvider;
import com.kalinin.mpt.helpers.StringUtils;

import org.jsoup.helper.StringUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;



public class ScheduleUtils {
    private static final String LOG_TAG = "ScheduleUtils";

    public static Pair<String, String> inferDirectionEndpoints(String directionName, List<Stop> stops) {
        final String DELIMETER = " - ";

        List<String> stopNames = new ArrayList<>();

        for (Stop stop : stops)
            stopNames.add(stop.name);

        String[] splitResult = directionName.split(DELIMETER);

        if (splitResult.length == 2)
            return new Pair<>(splitResult[0], splitResult[1]);

        if (stopNames.size() >= 2) {
            String from = stopNames.get(0);
            String to = stopNames.get(stopNames.size() - 1);

            String supposedDirectionName = from + DELIMETER + to;

            if (directionName.equals(supposedDirectionName))
                return new Pair<>(from, to);
        }

        for (int i = 0; i < splitResult.length - 1; i++) {
            List<String> fromList = new ArrayList<>();
            List<String> toList = new ArrayList<>();

            for (int p = 0; p < splitResult.length; p++) {
                if (p <= i)
                    fromList.add(splitResult[p]);
                else
                    toList.add(splitResult[p]);
            }

            String from = StringUtil.join(fromList, DELIMETER);
            String to = StringUtil.join(toList, DELIMETER);

            if (stopNames.contains(from) && stopNames.contains(to))
                return new Pair<>(from, to);
        }

        Log.e(LOG_TAG, String.format("Failed to infer endpoints for '%s'", directionName));

        // TODO: 6/10/2018 return something more meaningful
        return new Pair<>("???", "???");
    }

    public static CharSequence daysMaskToString(Context context, CharSequence mask) {
        CharSequence result;
        if (mask.equals("1111111")) {
            result = context.getString(R.string.date_all);
        } else if (mask.equals("1111100")) {
            result = context.getString(R.string.date_weekdays);
        } else if (mask.equals("0000011")) {
            result = context.getString(R.string.date_weekends);
        } else {
            boolean shortDays = StringUtils.countMatches(mask, '1') > 1;

            final int[] dayIds;
            final int[] dayIdsCapital;
            if (shortDays) {
                dayIds = new int[]{
                        R.string.date_monday_short,
                        R.string.date_tuesday_short,
                        R.string.date_wednesday_short,
                        R.string.date_thursday_short,
                        R.string.date_friday_short,
                        R.string.date_saturday_short,
                        R.string.date_sunday_short
                };
                dayIdsCapital = new int[]{
                        R.string.date_monday_short_capital,
                        R.string.date_tuesday_short_capital,
                        R.string.date_wednesday_short_capital,
                        R.string.date_thursday_short_capital,
                        R.string.date_friday_short_capital,
                        R.string.date_saturday_short_capital,
                        R.string.date_sunday_short_capital
                };
            } else {
                dayIds = new int[]{
                        R.string.date_monday,
                        R.string.date_tuesday,
                        R.string.date_wednesday,
                        R.string.date_thursday,
                        R.string.date_friday,
                        R.string.date_saturday,
                        R.string.date_sunday
                };
                dayIdsCapital = new int[]{
                        R.string.date_monday_capital,
                        R.string.date_tuesday_capital,
                        R.string.date_wednesday_capital,
                        R.string.date_thursday_capital,
                        R.string.date_friday_capital,
                        R.string.date_saturday_capital,
                        R.string.date_sunday_capital
                };
            }

            List<CharSequence> dayStrings = new ArrayList<>();
            for (int i = 0; i < mask.length(); i++) {
                if (mask.charAt(i) == '1') {
                    if (dayStrings.isEmpty()) {
                        dayStrings.add(context.getString(dayIdsCapital[i]));
                    } else {
                        dayStrings.add(context.getString(dayIds[i]));
                    }
                }
            }

            result = TextUtils.join(context.getString(R.string.date_delimiter), dayStrings);
        }

        // TODO: 6/3/2017 Capitalize first letter
        return result;
    }

    public static String scheduleDaysToString(Context context, ScheduleDays days) {
        Season season = days.season;
        CharSequence maskStr = daysMaskToString(context, days.daysMask);

        if (season == Season.ALL)
            return context.getString(R.string.schedule_days_all_seasons, maskStr);

        String seasonStr = seasonToString(context, season);

        return context.getString(R.string.schedule_days_seasons, maskStr, seasonStr);
    }

    public static String seasonToString(Context context, Season season) {
        switch (season) {
            case WINTER:
                return context.getString(R.string.season_winter);
            case SUMMER:
                return context.getString(R.string.season_summer);
            case ALL:
                return null;
        }

        Log.e(LOG_TAG, String.format("Unknown season %s!", season));
        return "";
    }

    public static String formatShortTimeInterval(Context context, long minutes) {
        if (minutes < 0)
            return "";

        if (minutes < 60) {
            return context.getString(R.string.interval_min, minutes);
        } else {
            long hours = minutes / 60;
            return context.getString(R.string.interval_hour, hours);
        }
    }

    public static String getCountdownText(Context context, Timepoint timepoint, boolean parenthesis) {
        long diffInMinutes = timepoint.minutesFromNow();
        if (diffInMinutes > 0) {
            String intervalStr = ScheduleUtils.formatShortTimeInterval(context, diffInMinutes);
            int strId = parenthesis ? R.string.schedule_next_in_parenthesis : R.string.schedule_next_in;
            return context.getString(strId, intervalStr);
        } else if (diffInMinutes == 0) {
            return context.getString(R.string.schedule_now);
        } else {
            return context.getString(R.string.schedule_late);
        }
    }

    public static int getCountdownColor(Context context, Timepoint timepoint) {
        long diffInMinutes = timepoint.minutesFromNow();

        // TODO: 3/21/2018 extract these values somewhere
        int closeThreshold = 5;
        int mediumThreshold = 10;

        @ColorRes int color;
        if (diffInMinutes < 0)
            color = R.color.next_in_late_color;
        else if (diffInMinutes <= closeThreshold)
            color = R.color.next_in_close_color;
        else if (diffInMinutes <= mediumThreshold)
            color = R.color.next_in_medium_color;
        else
            color = R.color.next_in_far_color;

        return context.getResources().getColor(color);
    }

    public static Calendar getTimepointCalendar(Timepoint timepoint, int firstHour) {
        Calendar timepointCalendar = Calendar.getInstance();

        int currentHour = timepointCalendar.get(Calendar.HOUR_OF_DAY);

        int hourOffset = 0;

        // it's between 0 and 'firstHour' now, so the schedule should start a day before;
        if (currentHour < firstHour)
            hourOffset -= 24;

        // hour is between 0 and 'firstHour', thus it's the next day
        if (timepoint.hour < firstHour)
            hourOffset += 24;

        int hour = timepoint.hour + hourOffset;
        int minute = timepoint.minute;

        timepointCalendar.set(Calendar.HOUR_OF_DAY, 0);
        timepointCalendar.set(Calendar.MINUTE, 0);
        timepointCalendar.set(Calendar.SECOND, 0);
        timepointCalendar.set(Calendar.MILLISECOND, 0);

        timepointCalendar.add(Calendar.HOUR, hour);
        timepointCalendar.add(Calendar.MINUTE, minute+1);
        timepointCalendar.add(Calendar.MILLISECOND, -1);

        return timepointCalendar;
    }

    public static @DrawableRes int getTransportIcon(TransportType transportType) {
        switch(transportType) {
            case BUS:
                return R.drawable.bus_in_circle;
            case TROLLEY:
                return R.drawable.trolley_in_circle;
            case TRAM:
                return R.drawable.tram_in_circle;
        }

        return R.drawable.ufo_in_circle;
    }

    public static @DrawableRes int getTransportWidgetBack(TransportType transportType) {
        switch(transportType) {
            case BUS:
                return R.drawable.widget_header_bus_back;
            case TROLLEY:
                return R.drawable.widget_header_trolley_back;
            case TRAM:
                return R.drawable.widget_header_tram_back;
        }

        return R.color.ufo_color;
    }

    public static void requestSchedule(final Context context, final Stop stop, final IScheduleResultListener listener) {
        Log.i(LOG_TAG, String.format("Requested schedule for stop '%s'", stop.toString()));

        new ScheduleCacheTask(context, ScheduleCacheTask.Args.getSchedule(stop), new ScheduleCacheTask.IScheduleReceiver() {
            @Override
            public void onResult(ScheduleCacheTask.Result result) {
                final Schedule cachedSchedule = result.schedule;

                final boolean hasCached = cachedSchedule != null;
                // TODO: 3/18/2018 use error codes instead
                if (hasCached) {
                    Log.i(LOG_TAG, "Found saved schedule");
                    if (listener != null)
                        listener.onCachedSchedule(cachedSchedule);
                } else {
                    Log.i(LOG_TAG, "Schedule isn't saved");
                }

                Log.i(LOG_TAG, "Fetching schedule from net");

                BaseScheduleProvider.getUnitedProvider().createAndRunTask(
                        ScheduleArgs.asScheduleArgs(stop),
                        new ScheduleProviderTask.IScheduleReceiver() {
                            @Override
                            public void onScheduleProviderExecuted(BaseScheduleProvider.Result result) {
                                if (result.error == null) {
                                    Log.i(LOG_TAG, "Schedule fetched");

                                    Schedule freshSchedule = result.schedule;
                                    if (cachedSchedule != null && freshSchedule != null && freshSchedule.equals(cachedSchedule)) {
                                        Log.i(LOG_TAG, "Schedule hasn't changed");
                                        return;
                                    }

                                    Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                                    intent.setComponent(new ComponentName(context, StopScheduleWidget.class));
                                    context.sendBroadcast(intent);

                                    if (listener != null)
                                        listener.onFreshSchedule(freshSchedule);

                                    new ScheduleCacheTask(context, ScheduleCacheTask.Args.saveSchedule(freshSchedule), new ScheduleCacheTask.IScheduleReceiver() {
                                        @Override
                                        public void onResult(ScheduleCacheTask.Result result) {
                                            Log.i(LOG_TAG, "Schedule saved");
                                            if (listener != null)
                                                listener.onScheduleCached(!hasCached);
                                        }
                                    }).execute();
                                } else {
                                    Log.e(LOG_TAG, String.format("Error while refreshing schedule: %s", result.error.code));
                                    if (listener != null)
                                        listener.onError(result.error);
                                }
                            }
                        }
                );
            }
        }).execute();
    }

    public interface IScheduleResultListener {
        void onCachedSchedule(Schedule schedule);
        void onFreshSchedule(Schedule schedule);
        void onScheduleCached(boolean first);
        void onError(ScheduleError error);
    }
}
