package cyber.dealer.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cyber.dealer.sys.constant.ReturnObject;
import cyber.dealer.sys.domain.CyberAgency;
import cyber.dealer.sys.domain.CyberUsers;
import cyber.dealer.sys.exception.ExceptionCast;
import cyber.dealer.sys.service.CyberAgencyService;
import cyber.dealer.sys.service.CyberUsersService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Keys;
import springfox.documentation.annotations.ApiIgnore;

import java.util.HashMap;
import java.util.Map;

import static cyber.dealer.sys.constant.ReturnNo.AUTH_INVALID_CODE;
import static cyber.dealer.sys.util.Common.decorateReturnObject;

/**
 * @author lfy
 * @Date 2022/4/22 17:18
 */
@RestController
@RequestMapping("level")
@CrossOrigin
@Api(tags = "当时未归类其他分类的Controller")
@ApiIgnore
public class LevelController {

    @Autowired
    private CyberUsersService cyberUsersService;

    @Autowired
    private CyberAgencyService cyberAgencyService;


    /**
     * 从其他地方进去的用户 都注册到这个邀请码下
     * @param addr
     * @param icode
     * @param email
     * @param nikename
     * @return
     */
    @ApiImplicitParams({
            @ApiImplicitParam(name = "addr", value = "钱包地址", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "icode", value = "邀请码", required = false, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "email", value = "邮箱", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "nickname", value = "别名", required = true, dataType = "String",defaultValue = ""),
    })
    @ApiOperation(value ="从官网进入+邀请码链接接口",notes = "")
    @PostMapping("invitation")
    public Object invitation(
            @RequestParam(required = true) String addr,
            @RequestParam(value="icode",required = false, defaultValue = "8BvDNz") String icode,
//            @RequestParam(value="icode",required = false, defaultValue = "uzG7x5") String icode,
            @RequestParam(required = true) String email,
            @RequestParam(required = true) String nikename) {
        addr = Keys.toChecksumAddress(addr);
        return decorateReturnObject(cyberUsersService.invitation(addr, icode, email, nikename));
    }


    /**
     * 查询是否存在address 返回level
     * @param addr
     * @return
     */
    @ApiImplicitParams({
            @ApiImplicitParam(name = "addr", value = "钱包地址", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value ="查询addresss是否存在接口",notes = "")
    @GetMapping("eqaddr")
    public Object eqAddr(String addr) {
        addr = Keys.toChecksumAddress(addr);
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("address", addr);
        CyberUsers entity = cyberUsersService.getOne(queryWrapper);
        Map map = new HashMap();
        if (entity == null) {
            return decorateReturnObject(new ReturnObject<>(true));
        }
        if (entity.getInvId() == 0) {
            map.put("level", entity.getLevel());
            return decorateReturnObject(new ReturnObject<>(map));
        }
        QueryWrapper queryWrappers = new QueryWrapper<>();
        queryWrappers.eq("id", entity.getInvId());
        CyberAgency one = cyberAgencyService.getOne(queryWrappers);
        if (one == null) {
            ExceptionCast.cast(AUTH_INVALID_CODE);
        }
        String Inv_level = null;
        if (entity.getLevel() == 1) {
            Inv_level = one.getThreeClass();
        }
        if (entity.getLevel() == 2) {
            Inv_level = one.getTwoClass();
        }
        if (entity.getLevel() == 3) {
            Inv_level = one.getOneClass();
        }
        if (Inv_level == null) {
            ExceptionCast.cast(AUTH_INVALID_CODE);
        }
        map.put("inv_level", Inv_level);
        map.put("level", entity.getLevel());
        map.put("data", false);
        return decorateReturnObject(new ReturnObject<>(map));
    }


}
