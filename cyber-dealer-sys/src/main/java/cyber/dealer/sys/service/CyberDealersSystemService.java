package cyber.dealer.sys.service;

import cyber.dealer.sys.domain.CyberDealersSystem;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author lfy
* @description 针对表【cyber_dealers_system】的数据库操作Service
* @createDate 2022-05-09 18:09:08
*/
public interface CyberDealersSystemService extends IService<CyberDealersSystem> {

    Object setReward(Integer reward);

    Object getcommission(Integer level);

    Object setcommission(Integer level, Double commission);
}
