package cyber.dealer.sys.aspects;

import cn.dev33.satoken.util.SaResult;
import cyber.dealer.sys.annotation.IpWhitelist;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * Author hw
 * Date 2023/7/6
 */
@Aspect
@Component
@Slf4j
public class IpWhitelistAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    // 限制除特定ip及网页请求之外的ip访问
    @Around("@annotation(ipWhitelist)")
    public Object checkIp(ProceedingJoinPoint joinPoint, IpWhitelist ipWhitelist) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String  ip= request.getHeader("X-Forwarded-For");
        if (ip == null) {
            ip = request.getRemoteAddr();
        }
        List<String> whiteList = (List<String>) redisTemplate.opsForValue().get("whiteList");

        for (String s : whiteList) {
            if (ip.contains(s)){
                return joinPoint.proceed();
            }
        }
        log.error(ip + " ->Illegal access denied");
        return SaResult.error("Illegal access denied");
    }
}
