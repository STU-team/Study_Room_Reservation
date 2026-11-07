package com.studyroom.entity;

/** 预约记录生命周期状态；CANCELLED 与 NO_SHOW 必须区分。 */
public enum ReservationStatus {
    BOOKED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED,
    NO_SHOW
}
