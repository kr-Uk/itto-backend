package com.itto.backend.domain.common.constants;

public final class BusinessConstants {

    private BusinessConstants() {}

    // --- Encounter ---
    public static final double ENCOUNTER_DISTANCE_METERS = 100.0;
    public static final int ENCOUNTER_COOLDOWN_MINUTES = 30;
    public static final int H3_RESOLUTION = 9;

    // --- Relationship monetization ---
    public static final int KNOTS_COST = 30;
    public static final int FREE_REQUESTS_PER_MONTH = 1;
    public static final int PREMIUM_REQUESTS_PER_MONTH = 6;
    public static final int FREE_CONVERTS_PER_MONTH = 1;
    public static final int PREMIUM_CONVERTS_PER_MONTH = 6;
    public static final int FREE_RESTORES_PER_MONTH = 0;
    public static final int PREMIUM_RESTORES_PER_MONTH = 3;
    public static final int RESTORE_KNOTS_COST = 30;

    // --- Subscription ---
    public static final int SUBSCRIPTION_LAUNCH_PRICE = 6900;
    public static final int SUBSCRIPTION_NORMAL_PRICE = 9900;
    public static final boolean PREMIUM_AD_FREE = true;

    // --- Packages ---
    public static final int SILMARI_PRICE = 7900;
    public static final int SILMARI_KNOTS = 80;
    public static final int SILMARI_FIRST_KNOTS = 160;
    public static final int MAEDUP_PRICE = 19900;
    public static final int MAEDUP_KNOTS = 250;
    public static final int MAEDUP_FIRST_KNOTS = 500;
    public static final int SILTARAE_PRICE = 49900;
    public static final int SILTARAE_KNOTS = 800;
    public static final int SILTARAE_FIRST_KNOTS = 1600;

    // --- Signup ---
    public static final int MAX_PHOTOS = 3;
    public static final int MIN_AGE = 19;

    // --- Contact hash ---
    public static final int MAX_HASH_COUNT = 5000;

    // --- Location ---
    public static final int MAX_EXCLUDED_AREAS = 3;
    public static final int MAX_EXCLUDED_CELLS = 20;

    // --- Rate limit ---
    public static final int LOGIN_MAX_ATTEMPTS = 5;
    public static final int LOGIN_LOCKOUT_MINUTES = 15;

    // --- Redis key prefixes ---
    public static final String LOCATION_USER_PREFIX = "location:user:";
    public static final String LOCATION_H3_PREFIX = "location:h3:";
    public static final String ENCOUNTER_COOLDOWN_PREFIX = "encounter:cooldown:";
    public static final String REFRESH_TOKEN_PREFIX = "refresh:";
    public static final String LOGIN_ATTEMPT_PREFIX = "login:attempt:";
}
