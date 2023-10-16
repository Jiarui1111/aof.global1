package cyber.dealer.sys.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 
 * @TableName cyber_trading_data
 */
@TableName(value ="cyber_trading_data")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CyberTradingData implements Serializable {
    /**
     * 
     */
    @TableId
    private Long id;

    /**
     * 
     */
    private String address;

    /**
     * 
     */
    private Integer level;

    /**
     * 
     */
    private Integer balance;

    /**
     * 
     */
    private Date time;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}