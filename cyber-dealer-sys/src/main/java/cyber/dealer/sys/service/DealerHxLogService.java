package cyber.dealer.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cyber.dealer.sys.domain.CyberUsers;
import cyber.dealer.sys.domain.DealerHxLog;

import java.util.List;

public interface DealerHxLogService extends IService<DealerHxLog> {
    Object getAllLog(String id, Integer size, Integer current);
    DealerHxLog getDetail(String hx);
    List<CyberUsers> getProxyByHx(String hx);
}
