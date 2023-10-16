package cyber.dealer.sys.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomStrException extends RuntimeException {
    String msg;

    public CustomStrException(String msg) {
        super("errorMessage: " + msg);
        this.msg = msg;
    }
}
