package cyber.dealer.sys.exception;


import cyber.dealer.sys.constant.ReturnNo;

public class ExceptionCast {
    public static void cast(ReturnNo resultCode) {
        throw new BastException(resultCode);
    }
    public static void castStr(String msg) {
        throw new CustomStrException(msg);
    }
}
