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
 * @TableName cyber_agency
 */
@TableName(value ="cyber_agency")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CyberAgency implements Serializable {
    /**
     * 
     */
    @TableId
    private Long id;

    /**
     * 
     */
    private Long uid;

    /**
     * 全球级|最高
     */
    private String oneClass;

    /**
     * 国家级|国家负责人
     */
    private String twoClass;

    /**
     * 区域|区域负责人
     */
    private String threeClass;

    /**
     * 用户|只是用户
     */
    private String fourClass;

    /**
     * 地址
     */
    private String address;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}