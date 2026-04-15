package com.nowbar.api.types

/**
 * All Samsung Health exercise types extracted from SportTypeConstants.
 * Values map directly to Samsung Health internal exercise type IDs.
 */
enum class ExerciseType(val code: Int, val category: ExerciseCategory) {
    // Special
    NONE(-1, ExerciseCategory.SPECIAL),
    MISC(0, ExerciseCategory.SPECIAL),

    // Walking & Running (1xxx)
    WALKING(1001, ExerciseCategory.RUNNING),
    RUNNING(1002, ExerciseCategory.RUNNING),
    STAIR_CLIMBING(1006, ExerciseCategory.RUNNING),
    TRACK_RUN(1007, ExerciseCategory.RUNNING),

    // Ball — Baseball/Cricket (2xxx)
    BASEBALL(2001, ExerciseCategory.BALL),
    SOFTBALL(2002, ExerciseCategory.BALL),
    CRICKET(2003, ExerciseCategory.BALL),

    // Precision Sports (3xxx)
    GOLF(3001, ExerciseCategory.GENERAL),
    BOWLING(3003, ExerciseCategory.GENERAL),

    // Team Sports (4xxx)
    FIELD_HOCKEY(4001, ExerciseCategory.BALL),
    RUGBY(4002, ExerciseCategory.BALL),
    BASKETBALL(4003, ExerciseCategory.BALL),
    SOCCER(4004, ExerciseCategory.BALL),
    HANDBALL(4005, ExerciseCategory.BALL),
    AMERICAN_FOOTBALL(4006, ExerciseCategory.BALL),

    // Volleyball (5xxx)
    VOLLEYBALL(5001, ExerciseCategory.BALL),
    BEACH_VOLLEYBALL(5002, ExerciseCategory.BALL),

    // Racket Sports (6xxx)
    SQUASH(6001, ExerciseCategory.BALL),
    TENNIS(6002, ExerciseCategory.BALL),
    BADMINTON(6003, ExerciseCategory.BALL),
    TABLE_TENNIS(6004, ExerciseCategory.BALL),
    RACQUETBALL(6005, ExerciseCategory.BALL),

    // Combat (7xxx)
    BOXING(7002, ExerciseCategory.GENERAL),
    MARTIAL_ARTS(7003, ExerciseCategory.GENERAL),

    // Dance (8xxx)
    BALLET(8001, ExerciseCategory.AEROBIC),
    DANCING(8002, ExerciseCategory.AEROBIC),
    BALLROOM_DANCE(8003, ExerciseCategory.AEROBIC),
    ZUMBA(8004, ExerciseCategory.AEROBIC),

    // Yoga & Pilates (9xxx)
    PILATES(9001, ExerciseCategory.GENERAL),
    YOGA(9002, ExerciseCategory.GENERAL),

    // Gym & Strength (10xxx)
    STRETCHING(10001, ExerciseCategory.GENERAL),
    JUMP_ROPE(10002, ExerciseCategory.AEROBIC),
    HULA_HOOPING(10003, ExerciseCategory.AEROBIC),
    PUSH_UP(10004, ExerciseCategory.FREE_WEIGHT),
    PULL_UP(10005, ExerciseCategory.FREE_WEIGHT),
    SIT_UP(10006, ExerciseCategory.FREE_WEIGHT),
    CIRCUIT_TRAINING(10007, ExerciseCategory.AEROBIC),
    MOUNTAIN_CLIMBER(10008, ExerciseCategory.FREE_WEIGHT),
    JUMPING_JACKS(10009, ExerciseCategory.AEROBIC),
    BURPEE_TEST(10010, ExerciseCategory.AEROBIC),
    BENCH_PRESS(10011, ExerciseCategory.FREE_WEIGHT),
    SQUAT(10012, ExerciseCategory.FREE_WEIGHT),
    LUNGE(10013, ExerciseCategory.FREE_WEIGHT),
    LEG_PRESS(10014, ExerciseCategory.WEIGHT_MACHINE),
    LEG_EXTENSION(10015, ExerciseCategory.WEIGHT_MACHINE),
    LEG_CURL(10016, ExerciseCategory.WEIGHT_MACHINE),
    BACK_EXTENSION(10017, ExerciseCategory.WEIGHT_MACHINE),
    LAT_PULL_DOWN(10018, ExerciseCategory.WEIGHT_MACHINE),
    DEADLIFT(10019, ExerciseCategory.FREE_WEIGHT),
    SHOULDER_PRESS(10020, ExerciseCategory.FREE_WEIGHT),
    FRONT_RAISE(10021, ExerciseCategory.FREE_WEIGHT),
    LATERAL_RAISE(10022, ExerciseCategory.FREE_WEIGHT),
    CRUNCH(10023, ExerciseCategory.FREE_WEIGHT),
    LEG_RAISE(10024, ExerciseCategory.FREE_WEIGHT),
    PLANK(10025, ExerciseCategory.FREE_WEIGHT),
    ARM_CURL(10026, ExerciseCategory.FREE_WEIGHT),
    ARM_EXTENSION(10027, ExerciseCategory.FREE_WEIGHT),
    SKATERS(10028, ExerciseCategory.AEROBIC),
    HIGH_KNEES(10029, ExerciseCategory.AEROBIC),

    // Outdoor / Cycling (11xxx)
    INLINE_SKATING(11001, ExerciseCategory.GENERAL),
    HANG_GLIDING(11002, ExerciseCategory.GENERAL),
    ARCHERY(11004, ExerciseCategory.GENERAL),
    HORSEBACK_RIDING(11005, ExerciseCategory.GENERAL),
    CYCLING(11007, ExerciseCategory.GENERAL),
    FRISBEE(11008, ExerciseCategory.GENERAL),
    ROLLER_SKATING(11009, ExerciseCategory.GENERAL),

    // Aerobic (12xxx)
    AEROBIC(12001, ExerciseCategory.AEROBIC),

    // Mountain / Hiking (13xxx)
    HIKING(13001, ExerciseCategory.MOUNTAIN),
    ROCK_CLIMBING(13002, ExerciseCategory.MOUNTAIN),
    BACKPACKING(13003, ExerciseCategory.MOUNTAIN),
    MOUNTAIN_BIKING(13004, ExerciseCategory.MOUNTAIN),
    ORIENTEERING(13005, ExerciseCategory.MOUNTAIN),

    // Water Sports (14xxx)
    SWIMMING(14001, ExerciseCategory.WATER),
    AQUAROBICS(14002, ExerciseCategory.WATER),
    CANOEING(14003, ExerciseCategory.WATER),
    SAILING(14004, ExerciseCategory.WATER),
    SCUBA_DIVING(14005, ExerciseCategory.WATER),
    SNORKELING(14006, ExerciseCategory.WATER),
    KAYAKING(14007, ExerciseCategory.WATER),
    KITE_SURFING(14008, ExerciseCategory.WATER),
    RAFTING(14009, ExerciseCategory.WATER),
    ROWING(14010, ExerciseCategory.WATER),
    WINDSURFING(14011, ExerciseCategory.WATER),
    YACHTING(14012, ExerciseCategory.WATER),
    WATER_SKIING(14013, ExerciseCategory.WATER),

    // Gym Machines (15xxx)
    STEP_MACHINE(15001, ExerciseCategory.WEIGHT_MACHINE),
    WEIGHT_MACHINE(15002, ExerciseCategory.WEIGHT_MACHINE),
    EXERCISE_BIKE(15003, ExerciseCategory.WEIGHT_MACHINE),
    ROWING_MACHINE(15004, ExerciseCategory.WEIGHT_MACHINE),
    TREADMILL(15005, ExerciseCategory.WEIGHT_MACHINE),
    ELLIPTICAL_TRAINER(15006, ExerciseCategory.WEIGHT_MACHINE),
    STAIR_MACHINE(15007, ExerciseCategory.WEIGHT_MACHINE),

    // Winter Sports (16xxx)
    CROSS_COUNTRY_SKIING(16001, ExerciseCategory.WINTER),
    SKIING(16002, ExerciseCategory.WINTER),
    ICE_DANCING(16003, ExerciseCategory.WINTER),
    ICE_SKATING(16004, ExerciseCategory.WINTER),
    ICE_HOCKEY(16006, ExerciseCategory.WINTER),
    SNOWBOARDING(16007, ExerciseCategory.WINTER),
    ALPINE_SKIING(16008, ExerciseCategory.WINTER),
    SNOWSHOEING(16009, ExerciseCategory.WINTER),

    // User-defined / Routine (51xxx)
    USER_DEFINED(51001, ExerciseCategory.USER_DEFINED),
    USER_DEFINED_WARM_UP(51002, ExerciseCategory.USER_DEFINED),
    USER_DEFINED_COOL_DOWN(51003, ExerciseCategory.USER_DEFINED),
    USER_DEFINED_BREAK(51004, ExerciseCategory.USER_DEFINED),
    USER_DEFINED_ROUTINE(51005, ExerciseCategory.USER_DEFINED),
    MULTI_SPORT(51006, ExerciseCategory.USER_DEFINED),

    // Special modes
    RUNNING_COACH(9001002, ExerciseCategory.RUNNING),
    SWIMMING_OUTDOOR(9014001, ExerciseCategory.WATER),
    OTHERS(99999, ExerciseCategory.SPECIAL),
    ALL(999999, ExerciseCategory.SPECIAL);

    companion object {
        private val codeMap = entries.associateBy { it.code }

        fun fromCode(code: Int): ExerciseType? = codeMap[code]

        fun fromCodeOrMisc(code: Int): ExerciseType = codeMap[code] ?: MISC

        /** Whether this exercise type needs GPS/map display. */
        fun needsMap(type: ExerciseType): Boolean = type in setOf(
            WALKING, RUNNING, CYCLING, HIKING, BACKPACKING,
            MOUNTAIN_BIKING, ORIENTEERING, INLINE_SKATING,
            ROLLER_SKATING, RUNNING_COACH
        )

        /** Whether this is a swimming exercise (indoor or outdoor). */
        fun isSwimming(type: ExerciseType): Boolean =
            type == SWIMMING || type == SWIMMING_OUTDOOR

        /** Whether this is a countable/rep-based exercise. */
        fun isCountable(type: ExerciseType): Boolean = type.category == ExerciseCategory.FREE_WEIGHT

        /** Whether this is a machine-based exercise. */
        fun isMachineExercise(type: ExerciseType): Boolean =
            type.category == ExerciseCategory.WEIGHT_MACHINE
    }
}

enum class ExerciseCategory {
    SPECIAL,
    RUNNING,
    BALL,
    GENERAL,
    AEROBIC,
    FREE_WEIGHT,
    WEIGHT_MACHINE,
    MOUNTAIN,
    WATER,
    WINTER,
    USER_DEFINED
}