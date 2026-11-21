package com.truckingsystem.common;

import com.truckingsystem.entity.LoadStatus;
public class InvalidLoadTransitionException extends RuntimeException {
    public InvalidLoadTransitionException(LoadStatus current, LoadStatus requested) {
        super("Load cannot transition from " + current + " to " + requested);
    }
}
