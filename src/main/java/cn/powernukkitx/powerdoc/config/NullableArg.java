package cn.powernukkitx.powerdoc.config;

import java.lang.annotation.*;

/**
 * 标注此参数不是必须的
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NullableArg {
}
