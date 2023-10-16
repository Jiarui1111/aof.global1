package cyber.dealer.sys.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@TableName(value = "cyber_commission")
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CyberCommission implements Serializable {
    @TableId
    private Integer id;
    private Integer subLevel;
    private Float commissionOne;
    private Float commissionTwo;
}
