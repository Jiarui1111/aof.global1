package cyber.dealer.sys.constant;

/**
 * @author zyg
 */
public class Global {

    public static String PANDA = "panda";
    public static String LOCAL = "local";

    public static String CREATE_TIME = "createTime";
    public static String UPDATE_TIME = "updateTime";

    /**
     * 以太坊自定义的签名消息都以以下字符开头
     * 参考 eth_sign in https://github.com/ethereum/wiki/wiki/JSON-RPC
     */
    public static final String PERSONAL_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";
    public static final String MESSAGE_PREFIX = "cyber-business: ";


    // Request中的变量名
    public static final String LOGIN_TOKEN_KEY = "authorization";
}
