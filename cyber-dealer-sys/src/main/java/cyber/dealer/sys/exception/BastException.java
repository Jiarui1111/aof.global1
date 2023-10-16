package cyber.dealer.sys.exception;

import cyber.dealer.sys.constant.ReturnNo;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BastException extends RuntimeException {
    private ReturnNo returnNo = ReturnNo.GLOBE_ERROR;
    String msg;

    public BastException(ReturnNo returnNo) {
        super("errorCode:" + returnNo.getCode() + " errorMessage:" + returnNo.getMessage());
        this.returnNo = returnNo;
    }

    public BastException(String msg) {
        super(" 错误信息：" + msg);
        this.msg = msg;
    }
}
