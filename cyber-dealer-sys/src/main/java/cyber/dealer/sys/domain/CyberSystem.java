package cyber.dealer.sys.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName(value = "cyber_system")
@Data
public class CyberSystem {

  @TableId
  private Long id;
  private String keyName;
  private String keyValue;
  private Long createTime;
  private Long updateTime;
  private Long deleted;

}
