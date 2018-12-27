package com.kalinin.mpt.widgets;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViewsService;

import com.kalinin.mpt.data.Stop;
import com.kalinin.mpt.helpers.ExtraHelper;

public class StopScheduleWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Bundle extras = intent.getBundleExtra(ExtraHelper.BUNDLE_EXTRA);
        if (extras == null)
            return null;

        Stop stop = (Stop)extras.getSerializable(ExtraHelper.STOP_EXTRA);
        if (stop == null)
            return null;

        return new StopScheduleWidgetRemoteViewsFactory(getApplicationContext(), stop);
    }
}
