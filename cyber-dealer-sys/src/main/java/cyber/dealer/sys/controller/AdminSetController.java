package cyber.dealer.sys.controller;

import cyber.dealer.sys.domain.AdminDataSetusdt;
import cyber.dealer.sys.service.AdminDataSetusdtService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * @author lfy
 * @Date 2022/10/14 11:12
 */
@RestController
@RequestMapping("admin")
@CrossOrigin
@Api(tags = "USDT收入量的Controller")
@ApiIgnore
public class AdminSetController {


    @Resource
    private AdminDataSetusdtService adminDataSetusdtService;


    /**
     * USDT收入量 数据展示页面
     * @param usdt
     * @param timestamp
     * @return
     */
    @GetMapping("{usdt}/{timestamp}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "usdt", value = "usdt数量", required = true, dataType = "String", paramType = "query",defaultValue = ""),
            @ApiImplicitParam(name = "timestamp", value = "时间戳", required = true, dataType = "String", paramType = "query",defaultValue = ""),
    })
    @ApiOperation(value ="USDT收入量设置接口",notes = "")
    public Object setUsdt(@Valid @PathVariable("usdt") String usdt, @Valid @PathVariable("timestamp") String timestamp) {

        AdminDataSetusdt build = AdminDataSetusdt.builder()
                .adminSetUsdt(usdt)
                .adminTime(timestamp).build();

        AdminDataSetusdt one = adminDataSetusdtService.lambdaQuery().eq(AdminDataSetusdt::getAdminTime, timestamp).one();
        boolean save = false;

        if (one == null) {
            save = adminDataSetusdtService.save(build);
        } else {
            save = adminDataSetusdtService.lambdaUpdate().eq(AdminDataSetusdt::getAdminTime, timestamp)
                    .set(AdminDataSetusdt::getAdminSetUsdt, usdt).update();
        }
        return save;
    }

    /**
     * 获取数据 USDT收入量最新
     * @return
     */
    @GetMapping("getUsetValues")
    @ApiOperation(value ="查询USDT收入量接口",notes = "")
    public Object getUsetValues() {
        List<AdminDataSetusdt> list = adminDataSetusdtService.lambdaQuery().orderByDesc(AdminDataSetusdt::getAdminTime).list();

        if (list.size() == 0) {
            return null;
        }

        return list;
    }


}