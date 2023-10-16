package cyber.dealer.sys.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cyber.dealer.sys.domain.CyberUsers;
import cyber.dealer.sys.exception.ExceptionCast;
import cyber.dealer.sys.mapper.CyberUsersMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.dev33.satoken.stp.StpInterface;
import static cyber.dealer.sys.constant.ReturnNo.AUTH_INVALID_EQQX;

/**
 * 自定义权限验证接口扩展
 */
@Component    // 保证此类被SpringBoot扫描，完成Sa-Token的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private CyberUsersMapper cyberUsersMapper;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 本list仅做模拟，实际项目中要根据具体业务逻辑来查询权限
        List<String> list = new ArrayList<String>();
        LambdaQueryWrapper<CyberUsers> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CyberUsers::getId, (String) loginId);
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(queryWrapper);

        if (cyberUsers != null) {
            if (cyberUsers.getLevel() == 1) {
                list.add("super-admin");
                list.add("user-add");
                list.add("user-delete");
                list.add("user-update");
                list.add("user-get");
            } else if (cyberUsers.getLevel() == 2) {
                list.add("user-get");
            } else if (cyberUsers.getLevel() == 3) {
                list.add("user-get");
            } else {
                ExceptionCast.cast(AUTH_INVALID_EQQX);
            }
        } else {
            ExceptionCast.cast(AUTH_INVALID_EQQX);
        }
        return list;
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 本list仅做模拟，实际项目中要根据具体业务逻辑来查询角色
        List<String> list = new ArrayList<String>();
        list.add("admin");
        list.add("super-admin");
        return list;
    }

}
