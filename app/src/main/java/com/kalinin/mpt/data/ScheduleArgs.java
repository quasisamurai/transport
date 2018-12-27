package com.kalinin.mpt.data;

import com.kalinin.mpt.data.providers.BaseScheduleProvider;


public class ScheduleArgs {
    public BaseScheduleProvider.OperationType operationType;
    public TransportType transportType;
    public Route route;
    public CharSequence daysMask;
    public Direction direction;

    public Stop stop;

    public static ScheduleArgs asRoutesArgs(TransportType type) {
        ScheduleArgs args = new ScheduleArgs();
        args.operationType = BaseScheduleProvider.OperationType.ROUTES;
        args.transportType = type;

        return args;
    }

    public static ScheduleArgs asStopsArgs(TransportType type, Route route) {
        ScheduleArgs args = new ScheduleArgs();
        args.operationType = BaseScheduleProvider.OperationType.STOPS;
        args.transportType = type;
        args.route = route;

        return args;
    }

    public static ScheduleArgs asScheduleArgs(Stop stop) {
        ScheduleArgs args = new ScheduleArgs();
        args.operationType = BaseScheduleProvider.OperationType.SCHEDULE;

        args.stop = stop;

        return args;
    }
}
