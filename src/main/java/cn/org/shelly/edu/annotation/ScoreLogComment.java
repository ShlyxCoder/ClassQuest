package cn.org.shelly.edu.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScoreLogComment {
    String value() default "标记当前方法需要做加减分数日志的处理，后期可做aop";
}
