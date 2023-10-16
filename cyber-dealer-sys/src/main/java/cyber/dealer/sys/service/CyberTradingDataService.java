package cyber.dealer.sys.service;

import cyber.dealer.sys.constant.ReturnObject;
import cyber.dealer.sys.domain.CyberTradingData;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author lfy
* @description 针对表【cyber_trading_data】的数据库操作Service
* @createDate 2022-04-25 19:15:21
*/
public interface CyberTradingDataService extends IService<CyberTradingData> {

    ReturnObject<Object> getData(String address);
}
