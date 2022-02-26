package cn.powernukkitx.powerdoc.config;

import java.lang.annotation.*;

/**
 * 标注能够在配置工作流中使用的函数的参数
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Arg {
    String value();
}
