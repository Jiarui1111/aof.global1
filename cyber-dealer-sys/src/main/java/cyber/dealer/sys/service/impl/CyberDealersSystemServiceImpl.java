package cyber.dealer.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cyber.dealer.sys.domain.CyberDealersSystem;
import cyber.dealer.sys.service.CyberDealersSystemService;
import cyber.dealer.sys.mapper.CyberDealersSystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author lfy
 * @description 针对表【cyber_dealers_system】的数据库操作Service实现
 * @createDate 2022-05-09 18:09:08
 */
@Service
public class CyberDealersSystemServiceImpl extends ServiceImpl<CyberDealersSystemMapper, CyberDealersSystem>
        implements CyberDealersSystemService {


    @Autowired
    private CyberDealersSystemMapper cyberDealersSystemMapper;

    @Override
    public Object setReward(Integer reward) {
        LambdaUpdateWrapper<CyberDealersSystem> lambdaUpdateWrapper = new LambdaUpdateWrapper();
        lambdaUpdateWrapper
                .set(CyberDealersSystem::getSystemval, reward)
                .eq(CyberDealersSystem::getSystemkey, "CalculateTheReward");
        CyberDealersSystem cyberDealersSystem = new CyberDealersSystem();
        cyberDealersSystem.setSystemval(String.valueOf(reward));
        return cyberDealersSystemMapper.update(cyberDealersSystem, lambdaUpdateWrapper) == 1;
    }

    @Override
    public Object getcommission(Integer level) {
        LambdaUpdateWrapper<CyberDealersSystem> lambdaUpdateWrapper = new LambdaUpdateWrapper();
        lambdaUpdateWrapper.eq(CyberDealersSystem::getSystemkey, "level" + level);
        CyberDealersSystem cyberDealersSystem = cyberDealersSystemMapper.selectOne(lambdaUpdateWrapper);
        return cyberDealersSystem;
    }

    @Override
    public Object setcommission(Integer level, Double commission) {
        LambdaUpdateWrapper<CyberDealersSystem> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(CyberDealersSystem::getSystemkey, "level" + level)
                .set(CyberDealersSystem::getSystemval, commission);
        CyberDealersSystem cyberDealersSystem = new CyberDealersSystem();
        cyberDealersSystem.setSystemval(String.valueOf(commission));
        return cyberDealersSystemMapper.update(cyberDealersSystem, lambdaUpdateWrapper) == 1;
    }
}




