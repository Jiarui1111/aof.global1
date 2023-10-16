package cyber.dealer.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cyber.dealer.sys.domain.CyberCommission;

import java.util.List;

/**
 * Author hw
 * Date 2023/2/17 11:54
 */
public interface CyberCommissionService extends IService<CyberCommission> {
    List<CyberCommission> selectList();
    Object updateCyberCommission(String id,String commissionOne,String commissionTwo);
}
