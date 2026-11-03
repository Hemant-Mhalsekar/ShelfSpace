package com.shelfspace.reservation;

/** WAITING in queue -> READY (copy held for 48h) -> FULFILLED, or EXPIRED / CANCELLED. */
public enum ReservationStatus { WAITING, READY, FULFILLED, EXPIRED, CANCELLED }
