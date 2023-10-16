package cyber.dealer.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cyber.dealer.sys.domain.CyberCommission;
import cyber.dealer.sys.mapper.CyberCommissionMapper;
import cyber.dealer.sys.service.CyberCommissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author hw
 * Date 2023/2/17 12:07
 */

@Service
public class CyberCommissionServiceImpl extends ServiceImpl<CyberCommissionMapper,CyberCommission> implements CyberCommissionService{

    @Autowired
    private CyberCommissionMapper cyberCommissionMapper;

    @Override
    public List<CyberCommission> selectList() {
        return cyberCommissionMapper.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public Object updateCyberCommission(String id, String commissionOne, String commissionTwo) {
        if (null == id || id.equals("")){
            return "输入参数不正确";
        }
        CyberCommission cyberCommission = cyberCommissionMapper.selectById(id);
        if (null!=commissionOne && !commissionOne.isEmpty()){
            cyberCommission.setCommissionOne(Float.parseFloat(commissionOne));
        }
        if (null!=commissionTwo && !commissionTwo.isEmpty()){
            cyberCommission.setCommissionTwo(Float.parseFloat(commissionTwo));
        }
        return cyberCommissionMapper.updateById(cyberCommission);
    }
}
