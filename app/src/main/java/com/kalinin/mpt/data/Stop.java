package com.kalinin.mpt.data;

import java.io.Serializable;


public class Stop implements Serializable {
    public Route route;
    public ScheduleDays days;
    public Direction direction;
    public int id;

    public String name;

    // additional fields
    public ScheduleType scheduleType;

    public Stop(Route route, ScheduleDays days, Direction direction, String name, int id, ScheduleType scheduleType) {
        this.route = route;
        this.days = days;
        this.direction = direction;
        this.name = name;
        this.id = id;
        this.scheduleType = scheduleType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Stop stop = (Stop) o;

        if (!route.equals(stop.route)) return false;
        if (!days.equals(stop.days)) return false;
        if (!direction.equals(stop.direction)) return false;
        if (id != stop.id) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + route.hashCode();
        result = 31 * result + days.hashCode();
        result = 31 * result + direction.hashCode();
        result = 31 * result + id;
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(route).append(",");
        sb.append(days).append(",");
        sb.append(direction.getId()).append(",");
        sb.append(name).append(",");
        sb.append(id).append(",");

        return sb.toString();
    }
}
