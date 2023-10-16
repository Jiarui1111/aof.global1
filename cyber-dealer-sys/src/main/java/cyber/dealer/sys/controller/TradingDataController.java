package cyber.dealer.sys.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cyber.dealer.sys.service.CyberTradingDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.crypto.Keys;
import springfox.documentation.annotations.ApiIgnore;

import static cyber.dealer.sys.util.Common.decorateReturnObject;

/**
 * @author lfy
 * @Date 2022/4/25 19:20
 */
@CrossOrigin
@RestController
@RequestMapping("trading")
@ApiIgnore
public class TradingDataController {

    @Autowired
    private CyberTradingDataService cyberTradingDataService;

    /**
     * 代理成本 分的的钱coin加减(前端未使用)
     * @param address
     * @return
     */
    @SaCheckLogin
    @GetMapping("getdata")
    public Object getData(String address){
        address = Keys.toChecksumAddress(address);
        return decorateReturnObject(cyberTradingDataService.getData(address));
    }


}
