package cyber.dealer.sys.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

@TableName(value = "email_wallet")
@Data
@Builder
public class EmailWallet {

  @TableId
  private String email;
  private String wallet;
}
