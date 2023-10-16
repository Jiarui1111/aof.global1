package cyber.dealer.sys.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cyber.dealer.sys.domain.CyberCommission;
import cyber.dealer.sys.domain.CyberUsers;
import cyber.dealer.sys.mapper.CyberAgencyMapper;
import cyber.dealer.sys.mapper.CyberCommissionMapper;
import cyber.dealer.sys.mapper.CyberUsersMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class SubcommissionUtils {
    @Autowired
    private CyberUsersMapper cyberUsersMapper;

    @Autowired
    private CyberCommissionMapper cyberCommissionMapper;

    @Autowired
    private CyberAgencyMapper cyberAgencyMapper;

    private static SubcommissionUtils subcommissionUtils;


    @PostConstruct
    public void init (){
        subcommissionUtils = this;
        subcommissionUtils.cyberUsersMapper = this.cyberUsersMapper;
        subcommissionUtils.cyberCommissionMapper = this.cyberCommissionMapper;
        subcommissionUtils.cyberAgencyMapper = this.cyberAgencyMapper;
    }

    // 查询当前用户的邀请人
    public static CyberUsers getSuperior(CyberUsers cyberUsers){
        if (null==cyberUsers.getInvId() || cyberUsers.getInvId().equals("") || cyberUsers.getInvId() == 0){
            System.out.println("id:--->"+cyberUsers.getId()+"invId:--->"+cyberUsers.getInvId());
            return null;
        }else {
            String uid = subcommissionUtils.cyberAgencyMapper.selectById(cyberUsers.getInvId()).getUid().toString();
            return subcommissionUtils.cyberUsersMapper.selectById(uid);
        }
    }

    // 给当前对象分佣
    public static int subcommission(CyberUsers cyberUsers,String amount){
        int level = cyberUsers.getLevel();
        Double balance = cyberUsers.getPersonalrewards();
        Double amount1 = Double.parseDouble(amount);

        LambdaQueryWrapper<CyberCommission> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (level==4){
            lambdaQueryWrapper.eq(CyberCommission::getSubLevel,0);
        }else {
            lambdaQueryWrapper.eq(CyberCommission::getSubLevel,cyberUsers.getSubLevel());
        }
        float ratio = subcommissionUtils.cyberCommissionMapper.selectOne(lambdaQueryWrapper).getCommissionOne();
        balance = ratio*amount1 + balance;
        cyberUsers.setPersonalrewards(balance);
        return subcommissionUtils.cyberUsersMapper.updateById(cyberUsers);
    }


    // 查询所有的经销商
    public static List<CyberUsers> getAllProxy(List<CyberUsers> list,CyberUsers cyberUsers){
        CyberUsers cyberUsers1 = getSuperior(cyberUsers);
        if (null==cyberUsers1 || list.size()==10){
            return list;
        }else {
            list.add(cyberUsers1);
            return getAllProxy(list,cyberUsers1);
        }
    }
}
