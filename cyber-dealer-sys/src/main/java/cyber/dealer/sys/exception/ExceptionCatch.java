package cyber.dealer.sys.exception;


import cyber.dealer.sys.constant.ReturnNo;
import cyber.dealer.sys.constant.ReturnObject;
import cyber.dealer.sys.util.Common;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static cyber.dealer.sys.constant.ReturnNo.GLOBE_ERROR;


@ControllerAdvice
@Slf4j
public class ExceptionCatch {

    // 将我们知道的不可预知异常存入map，用于抛出自定义的错误信息
    private static ConcurrentMap<Class<? extends Throwable>, ReturnNo> EXCEPTIONS = new ConcurrentHashMap<>();

    static {

    }

    // 处理可预知异常
    @ExceptionHandler(BastException.class)
    @ResponseBody
    public Object customException(BastException bastException) {
        log.error("catch com.bastion.forward.exception:{}/r/n com.bastion.forward.exception：", bastException.getMessage(), bastException);
        ReturnNo resultCode = bastException.getReturnNo();
        return Common.decorateReturnObject(new ReturnObject<>(resultCode));
    }

    // 处理可预知异常
    @ExceptionHandler(CustomStrException.class)
    @ResponseBody
    public String customException(CustomStrException customStrException) {
        log.error("catch com.bastion.forward.exception:{}/r/n com.bastion.forward.exception：", customStrException.getMessage(), customStrException);
        return customStrException.getMsg();
    }

    // 处理不可预知异常
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Object exception(Exception exception) {
        log.error("catch com.bastion.forward.exception:{}/r/n com.bastion.forward.exception：", exception.getMessage(), exception);

        ReturnNo code = EXCEPTIONS.get(exception.getClass());
        if (code != null) {
            return Common.decorateReturnObject(new ReturnObject<>(code));
        }
        return Common.decorateReturnObject(new ReturnObject<>(GLOBE_ERROR));
    }

}
