package cyber.dealer.sys.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

/**
 * @TableName admin_data_setusdt
 */
@TableName(value ="admin_data_setusdt")
@Data
@Builder
public class AdminDataSetusdt implements Serializable {
    
    @TableId
    private Long adminId;

    private String adminSetUsdt;

    private String adminTime;

    private static final long serialVersionUID = 1L;
}