package cyber.dealer.sys.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lfy
 * @Date 2022/4/19 14:17
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeneralFormatVo {
    //动作
//    public static final String GF_ACTION = "connectWallet";
//    public static final String GF_ACTION = "loginGame";
//    public static final String GF_ACTION = "buyBox";
//    public static final String GF_ACTION = "durationGame";

    public static final String GF_ACTION = "action";
    public static final String GF_ADDRESS = "email";
    public static final String GF_TIME = "time";
    public static final String GF_PARAMETER1 = "parameter1";
    public static final String GF_PARAMETER2 = "parameter2";
    public static final String GF_PARAMETER3 = "parameter3";
    //动作名称
    public String action;
    public String email;
    //时间戳
    public String time;
    //自定义参数
    public String invId;
    // 算力
    public String parameter1;
    public String parameter2;
    public String parameter3;
}
