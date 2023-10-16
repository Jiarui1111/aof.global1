package cyber.dealer.sys.controller;

import cyber.dealer.sys.domain.CyberUsers;
import cyber.dealer.sys.domain.DealerHxLog;
import cyber.dealer.sys.service.DealerHxLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@RestController
@RequestMapping("/dealerHxLog")
@CrossOrigin
@Api(tags = "处理佣金相关")
@ApiIgnore
public class DealerHxLogController {

    @Autowired
    private DealerHxLogService dealerHxLogService;

    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "经销商/用户的id", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "size", value = "每页几条", required = true, dataType = "int", defaultValue = ""),
            @ApiImplicitParam(name = "current", value = "第几页", required = true, dataType = "int", defaultValue = ""),
    })
    @ApiOperation(value ="经销商查询与其相关的记录/用户查询购买记录",notes = "")
    @GetMapping("/getAllLog")
    public Object getAllLog(String id, int size, int current){
        return dealerHxLogService.getAllLog(id,size,current);
    }


    @ApiImplicitParams({
            @ApiImplicitParam(name = "hx", value = "", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value ="查询tx记录详情",notes = "")
    @GetMapping("/getDetail")
    public DealerHxLog getDetail(String hx){
        return dealerHxLogService.getDetail(hx);
    }


    @ApiImplicitParams({
            @ApiImplicitParam(name = "hx", value = "", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value ="查询相关的经销商",notes = "")
    @GetMapping("/getProxyByHx")
    public List<CyberUsers> getProxyByHx(String hx){
        return dealerHxLogService.getProxyByHx(hx);
    }
}
