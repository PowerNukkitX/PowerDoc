package cn.powernukkitx.powerdoc.config;

import java.lang.annotation.*;

/**
 * 标注此函数能够在配置工作流中被使用
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Documented
public @interface Exposed {
}
