package cyber.dealer.sys.controller;

import cyber.dealer.sys.domain.CyberCommission;
import cyber.dealer.sys.service.CyberCommissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * Author hw
 * Date 2023/2/17 11:49
 */

@RestController
@RequestMapping("/commission")
@CrossOrigin
@Api(value = "设置分佣比例")
@ApiIgnore
public class CyberCommissionController {

    @Autowired
    private CyberCommissionService cyberCommissionService;

    @ApiOperation(value ="查询分佣比例",notes = "")
    @GetMapping("/getAllCommission")
    public List<CyberCommission> getAllCommission(){
        return cyberCommissionService.selectList();
    }

    @ApiOperation(value = "修改分佣比例")
    @PostMapping("/updateCommission")
    public Object updateCommission(String id,String commissionOne,String commissionTwo){
        return cyberCommissionService.updateCyberCommission(id,commissionOne,commissionTwo);
    }
}
