package com.kalinin.mpt.activities;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.kalinin.mpt.R;
import com.kalinin.mpt.adapters.SavedStopPagerAdapter;
import com.kalinin.mpt.adapters.SavedStopRecyclerViewAdapter;
import com.kalinin.mpt.data.ScheduleCacheTask;
import com.kalinin.mpt.data.ScheduleUtils;
import com.kalinin.mpt.data.Stop;

/**
 * The configuration screen for the {@link StopScheduleWidget StopScheduleWidget} AppWidget.
 */
public class StopScheduleWidgetConfigureActivity extends AppCompatActivity implements SavedStopRecyclerViewAdapter.ViewHolder.ItemIterationListener {
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    ViewPager mViewPager;
    SavedStopPagerAdapter mPagerAdapter;

    public StopScheduleWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setResult(RESULT_CANCELED);

        setContentView(R.layout.activity_route_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mPagerAdapter = new SavedStopPagerAdapter(true, getSupportFragmentManager(), this);

        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            finish();
    }

    @Override
    public void onItemClicked(Stop stop, int position) {
        final Context context = StopScheduleWidgetConfigureActivity.this;

        ScheduleUtils.requestSchedule(getApplicationContext(), stop, null);

        new ScheduleCacheTask(getApplicationContext(), ScheduleCacheTask.Args.addWidgetSimpleStop(stop, mAppWidgetId), new ScheduleCacheTask.IScheduleReceiver() {
            @Override
            public void onResult(ScheduleCacheTask.Result result) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                StopScheduleWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        }).execute();
    }

    @Override
    public boolean onItemLongClicked(Stop stop, int position) {
        return false;
    }
}

