package cyber.dealer.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cyber.dealer.sys.constant.ReturnObject;
import cyber.dealer.sys.domain.CyberAgency;
import cyber.dealer.sys.domain.CyberTradingData;
import cyber.dealer.sys.domain.CyberUsers;
import cyber.dealer.sys.mapper.CyberAgencyMapper;
import cyber.dealer.sys.mapper.CyberUsersMapper;
import cyber.dealer.sys.service.CyberTradingDataService;
import cyber.dealer.sys.mapper.CyberTradingDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author lfy
 * @description 针对表【cyber_trading_data】的数据库操作Service实现
 * @createDate 2022-04-25 19:15:21
 */
@Service
public class CyberTradingDataServiceImpl extends ServiceImpl<CyberTradingDataMapper, CyberTradingData>
        implements CyberTradingDataService {

    @Autowired
    private CyberTradingDataMapper cyberTradingDataMapper;

    @Autowired
    private CyberUsersMapper cyberUsersMapper;

    @Autowired
    private CyberAgencyMapper cyberAgencyMapper;

    @Override
    public ReturnObject<Object> getData(String address) {

        LambdaQueryWrapper<CyberUsers> cyberUsersQa = new LambdaQueryWrapper<>();
        cyberUsersQa.eq(CyberUsers::getAddress, address);
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(cyberUsersQa);

        LambdaQueryWrapper<CyberAgency> cyberAgencyQa = new LambdaQueryWrapper<>();
        cyberAgencyQa.eq(CyberAgency::getUid, cyberUsers.getId());
        CyberAgency cyberAgency = cyberAgencyMapper.selectOne(cyberAgencyQa);

        LambdaQueryWrapper<CyberUsers> cyberUsersQb = new LambdaQueryWrapper<>();
        cyberUsersQb.eq(CyberUsers::getInvId, cyberAgency.getId());
        List<CyberUsers> cyberUsers1 = cyberUsersMapper.selectList(cyberUsersQa);

        int CoinMian = 0;

        for (CyberUsers cyberU : cyberUsers1) {
            CoinMian += cyberU.getFujiCoin() + cyberU.getMubaiCoin();
        }


        return new ReturnObject<>(CoinMian);
    }
}




