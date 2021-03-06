package com.kalinin.mpt.data.providers;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import com.kalinin.mpt.R;
import com.kalinin.mpt.data.Direction;
import com.kalinin.mpt.data.InternetUtils;
import com.kalinin.mpt.data.Route;
import com.kalinin.mpt.data.Schedule;
import com.kalinin.mpt.data.ScheduleArgs;
import com.kalinin.mpt.data.ScheduleDays;
import com.kalinin.mpt.data.ScheduleError;
import com.kalinin.mpt.data.ScheduleType;
import com.kalinin.mpt.data.ScheduleUtils;
import com.kalinin.mpt.data.Stop;
import com.kalinin.mpt.data.Stops;
import com.kalinin.mpt.data.Timepoint;
import com.kalinin.mpt.data.TransportType;
import com.kalinin.mpt.helpers.UrlBuilder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anton on 6/25/2017.
 */

public class MosgortransScheduleProvider extends BaseScheduleProvider {
    private static final String LOG_TAG = "MosgortransSP";
    private static final String PROVIDER_ID = "mosgortrans";
    private static final String BASE_METADATA_URL = "http://www.mosgortrans.org/pass3/request.ajax.php";
    private static final String BASE_SCHEDULE_URL = "http://www.mosgortrans.org/pass3/shedule.printable.php";

    private static final int FIRST_HOUR = 5;

    // for some reason Mosgortrans sends these strings in a list of routes
    private static final List<String> EXCLUDED_ROUTE_NAMES = Arrays.asList("route", "stations", "streets");

    private static final List<Timepoint.Color> NOTE_COLORS = Arrays.asList(Timepoint.Color.RED, Timepoint.Color.GREEN, Timepoint.Color.BLUE);

    // Mosgortrans for some reason mangles '№' into 'в„–' in stop names
    private static String fixStopName(String route) {
        return route.replace("в„–", "№");
    }

    private class NoteData
    {
        public String note;
        public Timepoint.Color color;
    }

    @NonNull
    private List<Route> getRoutes(TransportType transportType) throws ScheduleProviderException {
        throwIfNoInternet();

        String url = constructMetadataUrl(MetadataListType.ROUTES, transportType);
        Log.i(LOG_TAG, String.format("getRoutes: Fetching '%s'", url));

        List<String> routeNames = InternetUtils.fetchUrlAsStringList(url);

        if (routeNames == null)
            throw new ScheduleProviderException(ScheduleError.ErrorCode.URL_FETCH_FAILED);

        List<Route> routes = new ArrayList<>();

        for (String name : routeNames)
            if (!EXCLUDED_ROUTE_NAMES.contains(name))
                routes.add(new Route(transportType, name, name, getProviderId()));

        return routes;
    }

    @NonNull
    private List<String> getDaysMasks(Route route) throws ScheduleProviderException {
        throwIfNoInternet();

        String url = constructMetadataUrl(MetadataListType.DAYS_MASKS, route.transportType, route.name);
        Log.i(LOG_TAG, String.format("getDaysMasks: Fetching '%s'", url));

        List<String> masks = InternetUtils.fetchUrlAsStringList(url);

        if (masks == null)
            throw new ScheduleProviderException(ScheduleError.ErrorCode.URL_FETCH_FAILED);

        return masks;
    }

    @NonNull
    private List<Direction> getDirections(Route route, String daysMask) throws ScheduleProviderException {
        throwIfNoInternet();

        // TODO: 6/25/2017 return just 2 directions without query

        String url = constructMetadataUrl(MetadataListType.DIRECTIONS, route.transportType, route.name, daysMask);
        Log.i(LOG_TAG, String.format("getDirections: Fetching '%s'", url));

        List<String> directionsNames = InternetUtils.fetchUrlAsStringList(url);

        if (directionsNames == null)
            throw new ScheduleProviderException(ScheduleError.ErrorCode.URL_FETCH_FAILED);

        if (directionsNames.size() != 2) {
            Log.e(LOG_TAG, String.format("getDirections(%s, %s, %s): Unusual direction list: has %d items, expected 2", route.transportType.name(), route, daysMask, directionsNames.size()));
        }

        List<Direction> directions = new ArrayList<>();

        for (int i = 0; i < directionsNames.size(); i++) {
            String id = (i == 0) ? "AB" : "BA";
            Direction direction = new Direction(id);
            direction.setName(directionsNames.get(i));
            directions.add(direction);
        }

        return directions;
    }

    @NonNull
    private List<Stop> getStops(Route route, ScheduleDays days, Direction direction) throws ScheduleProviderException {
        throwIfNoInternet();

        String url = constructMetadataUrl(MetadataListType.STOPS, route.transportType, route.name, days.daysMask, direction);
        Log.i(LOG_TAG, String.format("getStops: Fetching '%s'", url));
        List<String> stopList = InternetUtils.fetchUrlAsStringList(url);

        if (stopList == null)
            throw new ScheduleProviderException(ScheduleError.ErrorCode.URL_FETCH_FAILED);

        List<Stop> stops = new ArrayList<>();
        for (int i = 0; i < stopList.size(); i++) {
            String stopName = fixStopName(stopList.get(i));
            Stop stop = new Stop(route, days, direction, stopName, i, ScheduleType.TIMEPOINTS);
            stops.add(stop);
        }

        return stops;
    }

    @NonNull
    private Stops getStops(Route route) throws ScheduleProviderException {
        if (!route.providerId.equals(getProviderId()))
            throw new ScheduleProviderException(ScheduleError.ErrorCode.WRONG_PROVIDER);

        Log.i(LOG_TAG, "Loading stops for route " + route.toString());

        Map<Stops.StopConfiguration, List<Stop>> stopsMap = new HashMap<>();
        List<Direction> directions = new ArrayList<>();
        List<ScheduleDays> scheduleDays = new ArrayList<>();

        for (String mask : getDaysMasks(route)) {
            ScheduleDays days = new ScheduleDays(mask, mask, FIRST_HOUR);
            if (!scheduleDays.contains(days))
                scheduleDays.add(days);

            for (Direction direction : getDirections(route, mask)) {
                if (!directions.contains(direction))
                    directions.add(direction);

                Stops.StopConfiguration configuration = new Stops.StopConfiguration(direction, days);

                List<Stop> stops = getStops(route, days, direction);
                if (stops.isEmpty()) {
                    Log.w(LOG_TAG, String.format("Stops empty for configuration '%s'", configuration.toString()));
                    continue;
                }

                Pair<String, String> endpoints = ScheduleUtils.inferDirectionEndpoints(direction.getName(), stops);
                direction.setEndpoints(endpoints.first, endpoints.second);

                stopsMap.put(configuration, stops);
            }
        }

        Stops stops = new Stops(stopsMap, directions, scheduleDays);

        if (!stops.hasStops()) {
            Log.w(LOG_TAG, String.format("There are no stops for route '%s'", route.toString()));
            throw new ScheduleProviderException(ScheduleError.ErrorCode.NO_STOPS);
        }

        return stops;
    }

    @NonNull
    private Schedule getSchedule(Stop stop) throws ScheduleProviderException {
        if (stop == null)
            throw new ScheduleProviderException(ScheduleError.ErrorCode.INVALID_STOP);
        if (!stop.route.providerId.equals(getProviderId()))
            throw new ScheduleProviderException(ScheduleError.ErrorCode.WRONG_PROVIDER);

        throwIfNoInternet();

        List<Timepoint> timepoints = new ArrayList<>();

        String url = constructScheduleUrl(stop);
        if (url == null)
            throw new ScheduleProviderException(ScheduleError.ErrorCode.INTERNAL_ERROR);

        Log.i(LOG_TAG, String.format("getSchedule: Fetching '%s'", url));

        try {
            Document doc = Jsoup.connect(url).get();

            Element warning = doc.selectFirst("td[class=warning]");
            if (warning != null)
                throw new ScheduleProviderException(ScheduleError.ErrorCode.INVALID_SCHEDULE_URL);

            Map<String, NoteData> notes = new HashMap<>();
            Element legendTitle = doc.selectFirst("table + p");
            Element legend = legendTitle.nextElementSibling();
            while (true) {
                if (!legend.tag().getName().equals("p"))
                    break;

                Element legendIdElement = null;
                Elements legendIdElements = legend.getElementsByTag("b");
                for (Element el : legendIdElements)
                    legendIdElement = el;

                if (legendIdElement == null)
                    continue;

                String noteId = legendIdElement.text();

                NoteData data = new NoteData();
                data.note = legend.text().replaceFirst(noteId, "").replace("-", "").trim();
                data.color = NOTE_COLORS.get(notes.size() % NOTE_COLORS.size());

                notes.put(noteId, data);

                legend = legend.nextElementSibling();
            }

            Elements timeTags = doc.select("span[class~=(?:hour|minute)]");

            int hour = -1;
            for (Element tag : timeTags) {
                String tagClass = tag.className();
                String tagText = tag.text();

                if (tagText.isEmpty())
                    continue;

                int value;
                String noteId = null;
                try {
                    String[] words = tagText.split(" ");
                    if (words.length == 0) {
                        Log.e(LOG_TAG, String.format("Failed to parse data '%s'", tagText));
                        throw new ScheduleProviderException(ScheduleError.ErrorCode.PARSING_ERROR);
                    }

                    if (words.length > 1)
                        noteId = words[1];
                    value = Integer.parseInt(words[0]);
                } catch (NumberFormatException e) {
                    Log.e(LOG_TAG, String.format("Failed to parse data '%s'", tagText));
                    throw new ScheduleProviderException(ScheduleError.ErrorCode.PARSING_ERROR);
                }

                switch (tagClass) {
                    case "hour":
                        hour = value;
                        break;
                    case "minute":
                        if (noteId != null) {
                            NoteData noteData = notes.get(noteId);
                            timepoints.add(new Timepoint(hour, value, noteData.color, noteData.note));
                        } else {
                            timepoints.add(new Timepoint(hour, value));
                        }
                        break;

                    default:
                        Log.e(LOG_TAG, String.format("Unknown tag class '%s'", tagClass));
                        throw new ScheduleProviderException(ScheduleError.ErrorCode.PARSING_ERROR);
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new ScheduleProviderException(ScheduleError.ErrorCode.INTERNET_NOT_AVAILABLE);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ScheduleProviderException(ScheduleError.ErrorCode.INTERNAL_ERROR);
        }

        if (timepoints.isEmpty())
            throw new ScheduleProviderException(ScheduleError.ErrorCode.EMPTY_SCHEDULE);

        Schedule schedule = new Schedule();
        schedule.setAsTimepoints(stop, timepoints);

        return schedule;
    }

    private static String getTransportTypeId(TransportType type) {
        switch (type) {
            case BUS:
                return "avto";
            case TRAM:
                return "tram";
            case TROLLEY:
                return "trol";
        }

        return "";
    }

    private static String getMetadataListTypeId(MetadataListType type) {
        switch (type) {
            case ROUTES:
                return "ways";
            case DAYS_MASKS:
                return "days";
            case DIRECTIONS:
                return "directions";
            case STOPS:
                return "waypoints";
        }

        return "";
    }

    private static String constructMetadataUrl(MetadataListType listType, TransportType transportType, String route, String daysMask, String directionId) {
        final String LIST_TYPE_PARAM = "list";
        final String TRANSPORT_TYPE_PARAM = "type";
        final String ROUTE_PARAM = "way";
        final String DAYS_MASK_PARAM = "date";
        final String DIRECTION_PARAM = "direction";

        return Uri.parse(BASE_METADATA_URL).buildUpon()
                .appendQueryParameter(LIST_TYPE_PARAM, getMetadataListTypeId(listType))
                .appendQueryParameter(TRANSPORT_TYPE_PARAM, getTransportTypeId(transportType))
                .appendQueryParameter(ROUTE_PARAM, route)
                .appendQueryParameter(DAYS_MASK_PARAM, daysMask)
                .appendQueryParameter(DIRECTION_PARAM, directionId)
                .build().toString();
    }

    private static String constructMetadataUrl(MetadataListType listType, TransportType transportType) {
        return constructMetadataUrl(listType, transportType, "", "", "");
    }

    private static String constructMetadataUrl(MetadataListType listType, TransportType transportType, String route) {
        return constructMetadataUrl(listType, transportType, route, "", "");
    }

    private static String constructMetadataUrl(MetadataListType listType, TransportType transportType, String route, String daysMask) {
        return constructMetadataUrl(listType, transportType, route, daysMask, "");
    }

    private static String constructMetadataUrl(MetadataListType listType, TransportType transportType, String route, String daysMask, Direction direction) {
        String directionId = direction != null ? direction.getId() : "";

        return constructMetadataUrl(listType, transportType, route, daysMask, directionId);
    }

    private static String constructScheduleUrl(Stop stop) {
        final String TRANSPORT_TYPE_PARAM = "type";
        final String ROUTE_PARAM = "way";
        final String DAYS_MASK_PARAM = "date";
        final String DIRECTION_PARAM = "direction";
        final String WAYPOINT_PARAM = "waypoint";

        try {
            UrlBuilder builder = new UrlBuilder(BASE_SCHEDULE_URL, "windows-1251");
            builder.appendParam(TRANSPORT_TYPE_PARAM, getTransportTypeId(stop.route.transportType))
                    .appendParam(ROUTE_PARAM, stop.route.name)
                    .appendParam(DAYS_MASK_PARAM, stop.days.daysMask)
                    .appendParam(DIRECTION_PARAM, stop.direction.getId())
                    .appendParam(WAYPOINT_PARAM, String.valueOf(stop.id));

            return builder.build();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Result runProvider(ScheduleArgs args) throws ScheduleProviderException {
        Result result = new Result();

        switch (args.operationType) {
            case TYPES:
                result.transportTypes = getTransportTypes();
                break;
            case ROUTES:
                result.routes = getRoutes(args.transportType);
                break;
            case STOPS:
                result.stops = getStops(args.route);
                break;
            case SCHEDULE:
                result.schedule = getSchedule(args.stop);
                break;
        }

        return result;
    }

    private List<TransportType> getTransportTypes() {
        return new ArrayList<>(Arrays.asList(
                TransportType.BUS,
                TransportType.TRAM,
                TransportType.TROLLEY
        ));
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public String getProviderName(Context context) {
        return context.getString(R.string.provider_name_mosgortrans);
    }

    private enum MetadataListType {
        ROUTES,
        DAYS_MASKS,
        DIRECTIONS,
        STOPS
    }
}
