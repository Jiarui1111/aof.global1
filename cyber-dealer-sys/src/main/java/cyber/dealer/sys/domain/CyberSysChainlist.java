package cyber.dealer.sys.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName(value = "cyber_sys_chainlist")
@Data
public class CyberSysChainlist {

  @TableId
  private String chainId;
  private String chainUrl;
  private String chainListAddress;
  private String chainLastBlock;
  private String chainOfDelect;
}
