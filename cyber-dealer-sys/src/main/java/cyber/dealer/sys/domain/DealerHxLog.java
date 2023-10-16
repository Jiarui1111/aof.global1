package cyber.dealer.sys.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value ="dealer_hx_log")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DealerHxLog implements Serializable {

  @TableId
  private String hx;
  private String sender;
  private String reciver;
  private String status;
  private Double amount;
  private Integer readStatus;
  private String invCode;
  private Date updateTime;
}
