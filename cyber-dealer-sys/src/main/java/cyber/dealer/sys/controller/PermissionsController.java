package cyber.dealer.sys.controller;

import cn.dev33.satoken.stp.StpUtil;
import cyber.dealer.sys.constant.ReturnObject;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import static cyber.dealer.sys.util.Common.decorateReturnObject;
/**
 * @author lfy
 * @Date 2022/4/27 14:21
 */
@RestController
@RequestMapping("permissions")
@CrossOrigin
@ApiIgnore
public class PermissionsController {

    /**
     * 查询权限 无用
     * @return
     */
    @GetMapping("getPer")
    public Object getPermis(){
        return decorateReturnObject(new ReturnObject<>(StpUtil.hasPermission("user-update")));
    }

    @GetMapping("getPers")
    public Object getPermiss(){
        return decorateReturnObject(new ReturnObject<>(StpUtil.hasPermission("user-update")));
    }
}
