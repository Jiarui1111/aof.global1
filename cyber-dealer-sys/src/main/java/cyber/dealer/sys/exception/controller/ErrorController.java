package cyber.dealer.sys.exception.controller;

import cyber.dealer.sys.constant.ReturnNo;
import cyber.dealer.sys.constant.ReturnObject;
import cyber.dealer.sys.util.Common;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/error/", produces = "application/json;charset=UTF-8")
public class ErrorController  {

    @GetMapping(value = "404")
    public Object error_404() {
        return Common.decorateReturnObject(new ReturnObject<>(ReturnNo.RESOURCE_ID_NOT_EXIST));
    }

    @GetMapping(value = "500")
    public Object error_500() {
        return Common.decorateReturnObject(new ReturnObject<>(ReturnNo.SPRINGBOOT_ERROR));
    }
}
