package cyber.dealer.sys.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 *
 * @TableName cyber_users
 */
@TableName(value ="cyber_users")
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({ "password" })
public class CyberUsers implements Serializable {

  /**
   * 主键|邀请人id
   */
  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * 被邀请id
   */
  private Long invId;

  /**
   * 地址
   */
  private String address;

  private String web3Wallet;

  /**
   * 用户等级
   */
  private Integer level;

  /**
   * 币总数
   */
  private Long fujiCoin;

  /**
   * 币总数
   */
  private Double mubaiCoin;

  /**
   * 是否下载
   */
  private Integer download;

  /**
   * 创建时间
   */
  private Date createTime;

  /**
   * 更新时间
   */
  private Date updateTime;

  private String email;

  private String nikename;

  private Long dobadge;

  private Double personalrewards;

  private Long playgametimes;

  private String password;

  private Integer SubLevel;

  @TableField(exist = false)
  private static final long serialVersionUID = 1L;
}