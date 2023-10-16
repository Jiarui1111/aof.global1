package cyber.dealer.sys.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 返回的错误码
 * @author zyg
 */
public enum ReturnNo {
    //不加说明的状态码为200或201（POST）
    OK(0, "success"),
    /***************************************************
     *    系统级错误
     **************************************************/
    //状态码 500
    GLOBE_ERROR(55555, "Global异常"),
    SPRINGBOOT_ERROR(66666, "SB异常"),
    SERVER_ERROR(77777, "FUNCTION异常"),
    INTERNAL_SERVER_ERR(88888, "DB异常"),
    IGNORE(101, "跳过本次判断"),

    //所有需要登录才能访问的API都可能会返回以下错误
    //状态码 401
    AUTH_NEED_LOGIN(501, "请先登录"),
    AUTH_INVALID_JWT(502, "JWT不合法"),
    AUTH_JWT_EXPIRED(503, "JWT过期"),
    AUTH_INVALID_ADDR(504, "钱包地址不合法"),
    AUTH_INVALID_ADDRS(505, "钱包地址已经注册"),
    AUTH_INVALID_CODE(506, "邀请码有问题"),
    AUTH_INVALID_RECODE(507, "已有身份"),
    AUTH_INVALID_EQOBJ(508, "对象不存在"),
    AUTH_INVALID_EQQX(509, "权限不够"),
    AUTH_INVALID_EQADMINEX(510, "用户信息错误"),
    AUTH_INVALID_NODELAY(511, "该账号不为经销商"),
    AUTH_INVALID_BANNED(512, "账号已被封禁"),
    AUTH_INVALID_EQLEVEL(513, "level有问题"),
    AUTH_INVALID_EQEMAIL(514, "邮箱已被注册"),
    AUTH_TRANSFER_ALLTRANSFER(515, "信息不对"),
    AUTH_EMAIL_PASSWORD_FAIL(516, "邮箱或者密码有错误"),
    AUTH_ADDRESS_RECODE(517, "address有问题"),
    AUTH_ADDRESS_P_RECODE(518, "已有address"),


    // 用户等级不为1，但是未查询到经销商（误删经销商数据）
    AUTH_USER_AGENCY_MISS(519, "用户对应经销商身份丢失"),
    // 根据经销商查询对应的用户未查询到（误删用户数据）
    AUTH_AGENCY_USER_MISS(520, "经销商对应用户身份丢失"),
    AUTH_FREQUENT_BIND(521, "频繁绑定"),


    //以下错误码提示可以自行修改
    //--------------------------------------------
    //状态码 400
    FIELD_NOTVALID(503, "字段不合法"),
    RESOURCE_FALSIFY(511, "信息签名不正确"),
    IMG_FORMAT_ERROR(508, "图片格式不正确"),
    IMG_SIZE_EXCEED(509, "图片大小超限"),
    PARAMETER_MISSED(510, "缺少必要参数"),
    PARAM_PARSE_MISSED(511, "参数转换异常"),
    NUMBER_PARSE_MISSED(512, "数字转换异常"),
    DATE_PARSE_MISSED(513, "时间字段异常"),
    FORMAT_PARSE_MISSED(514, "SB反序列化异常"),
    FAST_PARSE_MISSED(515, "FS反序列化异常"),
    JACKSON_PARSE_MISSED(516, "JA反序列化异常"),


    //所有路径带id的API都可能返回此错误
    //状态码 404
    RESOURCE_ID_NOT_EXIST(404, "操作的资源id不存在"),
    PARAM_NOT_EXIST(405, "参数字段不存在"),
    FEIGN_NOT_EXIST(406, "Feign不存在"),
    USER_ID_NOT_EXIST(407, "用户不存在"),
    USER_ADDRESS_ID_NOT_EXIST(408, "用户地址不存在"),
    USER_NAME_NOT_EXIST(409, "用户名不存在"),
    PAYMENT_ID_NOT_EXIST(410, "结算ID不存在"),
    PAYMENT_NOT_EXIST(411, "结算内容不存在"),
    ACCOUNT_NOT_EXIST(412, "钱包账户不存在"),

    //状态码 403
    RESOURCE_ID_OUTSCOPE(505, "操作的资源id不是自己的对象"),
    FILE_NO_WRITE_PERMISSION(506, "目录文件夹没有写入的权限"),
    //状态码 200
    STATENOTALLOW(507, "当前状态禁止此操作"),
    /***************************************************
     *    其他模块错误码
     **************************************************/
    SOCKET_TIME_OUT(601, "连接超时"),


    SQL_CONSTRAINT_OUT(701, "数据库约束异常"),
    AOP_ERROR(702, "AOP异常"),

    /***************************************************
     *    商品模块错误码
     **************************************************/
    PAYMENT_ERROR(801, "转账存在未成功记录，请及时处理"),
    FROZEN_BALANCE_ZERO(802, "冻结余额不足"),
    GAS_ERROR(803, "GAS错误"),
    ACCOUNT_UPDATE_ERROR(804, "account更新失败"),
    WITHDRAWAL_UPDATE_ERROR(805, "withdrawal更新失败");

    private int code;
    private String message;
    private static final Map<Integer, ReturnNo> returnNoMap;

    static {
        returnNoMap = new HashMap();
        for (ReturnNo enum1 : values()) {
            returnNoMap.put(enum1.code, enum1);
        }
    }

    ReturnNo(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ReturnNo getByCode(int code1) {
        ReturnNo[] all = ReturnNo.values();
        for (ReturnNo returnNo : all) {
            if (returnNo.code == code1) {
                return returnNo;
            }
        }
        return null;
    }

    public static ReturnNo getReturnNoByCode(int code) {
        return returnNoMap.get(code);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
