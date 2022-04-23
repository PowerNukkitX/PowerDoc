package cn.powernukkitx.powerdoc.utils;

import java.util.function.Function;

@SuppressWarnings("DuplicatedCode")
public final class NullUtils {
    private NullUtils() {

    }

    public static <T> T Ok(T nullable, T ifNull) {
        if (nullable == null) {
            return ifNull;
        } else {
            return nullable;
        }
    }

    public static <T, R> R Ok(T nullable, Function<T, R> ifNotNull, R ifNull) {
        if (nullable == null) {
            return ifNull;
        } else {
            return ifNotNull.apply(nullable);
        }
    }

    public static <T, R1, R2> R2 Ok(T nullable, Function<T, R1> ifNotNull, Function<R1, R2> ifStillNotNull, R2 ifNull) {
        if (nullable == null) {
            return ifNull;
        } else {
            var tmp = ifNotNull.apply(nullable);
            if (tmp == null) {
                return ifNull;
            } else {
                return ifStillNotNull.apply(tmp);
            }
        }
    }

    public static <T, R1, R2, R3> R3 Ok(T nullable, Function<T, R1> ifNotNull, Function<R1, R2> ifStillNotNull, Function<R2, R3> ifStillNotNull2, R3 ifNull) {
        if (nullable == null) {
            return ifNull;
        } else {
            var tmp = ifNotNull.apply(nullable);
            if (tmp == null) {
                return ifNull;
            } else {
                var tmp2 = ifStillNotNull.apply(tmp);
                if (tmp2 == null) {
                    return ifNull;
                } else {
                    return ifStillNotNull2.apply(tmp2);
                }
            }
        }
    }

    public static <T, R1, R2, R3, R4> R4 Ok(T nullable, Function<T, R1> ifNotNull, Function<R1, R2> ifStillNotNull, Function<R2, R3> ifStillNotNull2,
                                            Function<R3, R4> ifStillNotNull3, R4 ifNull) {
        if (nullable == null) {
            return ifNull;
        } else {
            var tmp = ifNotNull.apply(nullable);
            if (tmp == null) {
                return ifNull;
            } else {
                var tmp2 = ifStillNotNull.apply(tmp);
                if (tmp2 == null) {
                    return ifNull;
                } else {
                    var tmp3 = ifStillNotNull2.apply(tmp2);
                    if (tmp3 == null) {
                        return ifNull;
                    } else {
                        return ifStillNotNull3.apply(tmp3);
                    }
                }
            }
        }
    }

    public static <T, R1, R2, R3, R4, R5> R5 Ok(T nullable, Function<T, R1> ifNotNull, Function<R1, R2> ifStillNotNull, Function<R2, R3> ifStillNotNull2,
                                                Function<R3, R4> ifStillNotNull3, Function<R4, R5> ifStillNotNull4, R5 ifNull) {
        if (nullable == null) {
            return ifNull;
        } else {
            var tmp = ifNotNull.apply(nullable);
            if (tmp == null) {
                return ifNull;
            } else {
                var tmp2 = ifStillNotNull.apply(tmp);
                if (tmp2 == null) {
                    return ifNull;
                } else {
                    var tmp3 = ifStillNotNull2.apply(tmp2);
                    if (tmp3 == null) {
                        return ifNull;
                    } else {
                        var tmp4 = ifStillNotNull3.apply(tmp3);
                        if (tmp4 == null) {
                            return ifNull;
                        } else {
                            return ifStillNotNull4.apply(tmp4);
                        }
                    }
                }
            }
        }
    }
}
