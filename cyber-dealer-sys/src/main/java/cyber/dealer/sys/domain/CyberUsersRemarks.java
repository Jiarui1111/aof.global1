package cyber.dealer.sys.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 
 * @TableName cyber_users_remarks
 */
@TableName(value ="cyber_users_remarks")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CyberUsersRemarks implements Serializable {
    /**
     * 
     */
    private Long id;

    /**
     * 
     */
    private String toaddress;

    /**
     * 
     */
    private String address;

    /**
     * 
     */
    private String remarks;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}