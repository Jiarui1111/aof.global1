package cyber.dealer.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cyber.dealer.sys.constant.ReturnNo;
import cyber.dealer.sys.domain.CyberAgency;
import cyber.dealer.sys.domain.CyberUsers;
import cyber.dealer.sys.domain.DealerHxLog;
import cyber.dealer.sys.exception.ExceptionCast;
import cyber.dealer.sys.mapper.CyberAgencyMapper;
import cyber.dealer.sys.mapper.CyberUsersMapper;
import cyber.dealer.sys.mapper.DealerHxLogMapper;
import cyber.dealer.sys.service.DealerHxLogService;
import cyber.dealer.sys.util.SubcommissionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DealerHxLogServiceImpl extends ServiceImpl<DealerHxLogMapper, DealerHxLog> implements DealerHxLogService {

    @Autowired
    private CyberUsersMapper cyberUsersMapper;

    @Autowired
    private DealerHxLogMapper dealerHxLogMapper;

    @Autowired
    private CyberAgencyMapper cyberAgencyMapper;

    @Override
    public Object getAllLog(String id, Integer size, Integer current) {
        if (null == id || id.equals("") || null == size || size.equals("") || null == current || current.equals("")){
            return "输入参数不正确";
        }
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getId,id));
        if (null == cyberUsers){
            ExceptionCast.cast(ReturnNo.AUTH_INVALID_EQOBJ);
        }
        int level = cyberUsers.getLevel();

        Page<DealerHxLog> page = new Page<>();
        page.setSize(size);
        page.setCurrent(current);
        if (level==1){
            System.out.println("------>user");
            return dealerHxLogMapper.selectPage(page,new LambdaQueryWrapper<DealerHxLog>().eq(DealerHxLog::getSender,cyberUsers.getAddress()).orderByDesc(DealerHxLog::getUpdateTime));
        }else {
            System.out.println("------>dealer");
            CyberAgency cyberAgency = cyberAgencyMapper.selectOne(new LambdaQueryWrapper<CyberAgency>().eq(CyberAgency::getUid,id));
            return dealerHxLogMapper.selectPage(page,new LambdaQueryWrapper<DealerHxLog>().eq(DealerHxLog::getInvCode,cyberAgency.getThreeClass()).orderByDesc(DealerHxLog::getUpdateTime));
        }
    }


    @Override
    public DealerHxLog getDetail(String hx) {
        return dealerHxLogMapper.selectOne(new LambdaQueryWrapper<DealerHxLog>().eq(DealerHxLog::getHx,hx));
    }

    @Override
    public List<CyberUsers> getProxyByHx(String hx) {
        String invCode = dealerHxLogMapper.selectOne(new LambdaQueryWrapper<DealerHxLog>().eq(DealerHxLog::getHx,hx)).getInvCode();
        CyberAgency cyberAgency = cyberAgencyMapper.selectOne(new LambdaQueryWrapper<CyberAgency>().eq(CyberAgency::getThreeClass,invCode));
        if (null==cyberAgency){
            System.out.println("------>邀请码不存在");
            ExceptionCast.cast(ReturnNo.AUTH_INVALID_CODE);
        }else {
            CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getId,cyberAgency.getUid()));
            List<CyberUsers> list = new ArrayList<>();
            if (null!=cyberUsers){
                list.add(cyberUsers);
                return SubcommissionUtils.getAllProxy(list,cyberUsers);
            }else {
                ExceptionCast.cast(ReturnNo.AUTH_INVALID_EQOBJ);
            }
        }
        return new ArrayList<>();
    }
}
