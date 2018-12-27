package com.kalinin.mpt.data;


public class SelectableRoute extends Route {
    public boolean selected = false;
    public SelectableRoute(Route route)
    {
        super(route);
    }
}
