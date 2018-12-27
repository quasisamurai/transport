package com.kalinin.mpt.data;

import java.io.Serializable;



public class StopListItem implements Serializable {
    public Stop stop;
    public CharSequence next;
    public boolean selected;

    public StopListItem(Stop stop, CharSequence next, boolean selected) {
        this.stop = stop;
        this.next = next;
        this.selected = selected;
    }
}
