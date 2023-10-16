package cyber.dealer.sys.controller;

import cyber.dealer.sys.constant.ReturnObject;
import cyber.dealer.sys.exception.ExceptionCast;
import cyber.dealer.sys.service.CyberDealersSystemService;
import cyber.dealer.sys.util.RedisUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.HashMap;
import java.util.Map;

import static cyber.dealer.sys.constant.ReturnNo.FIELD_NOTVALID;
import static cyber.dealer.sys.util.Common.decorateReturnObject;

/**
 * @author lfy
 * @Date 2022/5/12 10:28
 */
@CrossOrigin
@RestController
@RequestMapping("sys")
@Api(tags = "admin页面系统设置的Controller")
@ApiIgnore
public class systemController {

    @Autowired
    private CyberDealersSystemService cyberDealersSystemService;

    @Autowired
    private RedisUtils redisUtils;

    //设置奖励池
    @PutMapping("setreward")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "reward", value = "奖励池", required = true, dataType = "Integer", defaultValue = ""),
    })
    @ApiOperation(value ="设置奖励池的接口",notes = "")
    public Object setReward(Integer reward) {
        if (reward < 0) {
            ExceptionCast.cast(FIELD_NOTVALID);
        }
        return decorateReturnObject(new ReturnObject<>(cyberDealersSystemService.setReward(reward)));
    }

    /**
     * 设置level 对应的佣金奖励
     * @param level
     * @param commission
     * @return
     */
    @PutMapping("commission")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "level", value = "等级", required = true, dataType = "Integer", defaultValue = ""),
            @ApiImplicitParam(name = "commission", value = "百分比", required = true, dataType = "Double", defaultValue = ""),
    })
    @ApiOperation(value ="设置level 对应的佣金奖励",notes = "")
    public Object setcommission(Integer level, Double commission) {
        if (level < 0 && commission <= 1) {
            ExceptionCast.cast(FIELD_NOTVALID);
        }
        return decorateReturnObject(new ReturnObject<>(cyberDealersSystemService.setcommission(level, commission)));
    }

    /**
     * 获取level 对应的佣金奖励
     */
    @GetMapping("commission")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "level", value = "等级", required = true, dataType = "Integer", defaultValue = ""),
    })
    @ApiOperation(value ="获取level 对应的佣金奖励",notes = "")
    public Object getcommission(Integer level) {
        if (level != 3 && level != 2 && level != 4) {
            ExceptionCast.cast(FIELD_NOTVALID);
        }
        return decorateReturnObject(new ReturnObject<>(cyberDealersSystemService.getcommission(level)));
    }

    /**
     * 系统设置里
     * 获取pv1 pv2 游戏打金
     */
    @GetMapping("getGameTokensRules")
    @ApiImplicitParams({
    })
    @ApiOperation(value ="获取pv1 pv2 游戏打金的接口",notes = "")
    public Object getGameTokensRules() {
        Map<String, Object> map = new HashMap<>();
        map.put("directgold", redisUtils.get("directgold"));
        map.put("pv1", redisUtils.get("pv1"));
        map.put("pv2", redisUtils.get("pv2"));
        map.put("pvp", redisUtils.get("pvp"));
        return decorateReturnObject(new ReturnObject<>(map));
    }

    /**
     * 系统设置里
     * 设置pv1 pv2 游戏打金
     *
     * @param pv1
     * @param pv2
     * @param pvp
     * @param directgold
     * @return
     */
    @PutMapping("setGameTokensRules")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pv1", value = "pv1等级", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "pv2", value = "pv2等级", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "pvp", value = "pvp等级", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "directgold", value = "直接打金", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value ="获取pv1 pv2 游戏打金的接口",notes = "")
    public Object setGameTokensRules(String pv1, String pv2, String pvp, String directgold) {
        redisUtils.set("directgold", directgold, -1);
        redisUtils.set("pv1", pv1, -1);
        redisUtils.set("pv2", pv2, -1);
        redisUtils.set("pvp", pvp, -1);
        return decorateReturnObject(new ReturnObject<>(true));
    }

}
