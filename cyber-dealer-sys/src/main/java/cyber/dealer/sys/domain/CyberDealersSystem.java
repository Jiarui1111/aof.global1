package cyber.dealer.sys.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 
 * @TableName cyber_dealers_system
 */
@TableName(value ="cyber_dealers_system")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CyberDealersSystem implements Serializable {
    /**
     * 
     */
    @TableId
    private Long id;

    /**
     * 
     */
    private String systemkey;

    /**
     * 
     */
    private String systemval;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}