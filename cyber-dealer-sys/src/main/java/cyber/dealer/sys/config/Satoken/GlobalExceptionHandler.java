package cyber.dealer.sys.config.Satoken;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.util.SaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.Date;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 全局异常拦截
     */
    @ExceptionHandler
    public SaResult handlerException(Exception e) {

        // 记录日志信息
        Throwable e2 = e.getCause();

        // ------------- 判断异常类型，提供个性化提示信息

        // 如果是未登录异常
        if (e instanceof NotLoginException) {
            return SaResult.get(500, "未登录异常", null);
        }
        log.warn("---> exception handling");
        // 打印堆栈，以供调试
        e.printStackTrace();

        // 如果是SQLException，并且指定了hideSql，则只返回sql error
        if ((e instanceof SQLException || e2 instanceof SQLException)) {
            // 无论是否打开隐藏sql，日志表记录的都是真实异常信息
            return SaResult.get(500, "数据库异常", null);
        }

        // 如果是redis连接异常 ( 由于redis连接异常，系统已经无法正常工作，所以此处需要立即返回 )
        else if (e instanceof RedisConnectionFailureException) {
            return SaResult.get(500, "缓存异常", null);
        }

        // 普通异常输出：500 + 异常信息
        else {
            return SaResult.get(500, "其他异常", null);
        }
    }
}

