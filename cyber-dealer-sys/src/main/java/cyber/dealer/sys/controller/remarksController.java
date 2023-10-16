package cyber.dealer.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import cyber.dealer.sys.constant.ReturnObject;
import cyber.dealer.sys.domain.CyberUsersRemarks;
import cyber.dealer.sys.exception.ExceptionCast;
import cyber.dealer.sys.mapper.CyberUsersRemarksMapper;
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
 * @Date 2022/5/5 20:14
 */
@RestController
@RequestMapping("re")
@CrossOrigin
@Api(tags = "admin页面增加功能的Controller")
@ApiIgnore
public class remarksController {

    @Autowired
    private CyberUsersRemarksMapper cyberUsersRemarksMapper;


    /**
     * 设置 备注
     * @param address
     * @param toaddress
     * @param remarks
     * @return
     */

    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包地址", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "toaddress", value = "需要设置备注的钱包地址", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "remarks", value = "备注名称", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value ="设置备注的接口",notes = "")
    @PutMapping("setremarks")
    public Object setRemarks(String address, String toaddress, String remarks) {

        if (address == null) {
            ExceptionCast.cast(FIELD_NOTVALID);
        }
        if (remarks == null) {
            ExceptionCast.cast(FIELD_NOTVALID);
        }
        if (toaddress == null) {
            ExceptionCast.cast(FIELD_NOTVALID);
        }
//        address = Keys.toChecksumAddress(address);
//        toaddress = Keys.toChecksumAddress(toaddress);

        CyberUsersRemarks cyberUsersRemarks = new CyberUsersRemarks();
        cyberUsersRemarks.setRemarks(remarks);
        cyberUsersRemarks.setAddress(address);
        cyberUsersRemarks.setToaddress(toaddress);

        QueryWrapper queryWrapper = new QueryWrapper();
        Map<String, Object> map = new HashMap<>();
        map.put("address", address);
        map.put("toaddress", toaddress);
        queryWrapper.allEq(map);
        CyberUsersRemarks cyberUsersRemarks1 = cyberUsersRemarksMapper.selectOne(queryWrapper);
        if (cyberUsersRemarks1 == null) {
            return decorateReturnObject(new ReturnObject<>(cyberUsersRemarksMapper.insert(cyberUsersRemarks) == 1));
        }
        UpdateWrapper<CyberUsersRemarks> remarksR = new UpdateWrapper<>();
        remarksR.eq("address", address);
        remarksR.eq("toaddress", toaddress);
        remarksR.set("remarks", remarks);
        return decorateReturnObject(new ReturnObject<>(cyberUsersRemarksMapper.update(cyberUsersRemarks, remarksR) == 1));
    }
}
