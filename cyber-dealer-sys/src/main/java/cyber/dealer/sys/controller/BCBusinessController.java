package cyber.dealer.sys.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cyber.dealer.sys.constant.ReturnObject;
import cyber.dealer.sys.domain.CyberAgency;
import cyber.dealer.sys.domain.CyberUsers;
import cyber.dealer.sys.domain.DealerHxLog;
import cyber.dealer.sys.exception.ExceptionCast;
import cyber.dealer.sys.mapper.CyberAgencyMapper;
import cyber.dealer.sys.mapper.CyberUsersMapper;
import cyber.dealer.sys.mapper.DealerHxLogMapper;
import cyber.dealer.sys.util.RandomPasswordUtils;
import cyber.dealer.sys.util.SubcommissionUtils;
import cyber.dealer.sys.util.UpGradeCheckUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Keys;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Date;
import java.util.Objects;

import static cyber.dealer.sys.constant.ReturnNo.*;
import static cyber.dealer.sys.util.Common.decorateReturnObject;
import static cyber.dealer.sys.util.Common.getRandomString;

/**
 * @author lfy
 * @Date 2022/5/4 0:28
 */
@RestController
@RequestMapping("business")
@CrossOrigin
@Api(tags = "用户注册的Controller")
@ApiIgnore
public class BCBusinessController {

    @Autowired
    private CyberUsersMapper cyberUsersMapper;

    @Autowired
    private CyberAgencyMapper cyberAgencyMapper;

    @Autowired
    private DealerHxLogMapper dealerHxLogMapper;

    /**
     * 根据level address 删除用户 等级为1
     * @param address
     * @return
     */
    @DeleteMapping("deluserlevel")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包地址", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value ="根据level address 删除用户 等级为1接口",notes = "")
    public Object deluserlevel(String address) {
        QueryWrapper<CyberUsers> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("address", address);
        queryWrapper.eq("level", 1);
        return decorateReturnObject(new ReturnObject<>(cyberUsersMapper.delete(queryWrapper) == 1));
    }

    //注册国家级的  level ==4
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包地址", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "nickname", value = "别名", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "email", value = "邮箱地址", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "level", value = "等级", required = true, dataType = "String",defaultValue = ""),
    })
    @ApiOperation(value ="注册国家级的  level ==4 接口",notes = "")
    @PostMapping("nationallevel")
    public Object setNationallevel(String address,
                                   String nickname,
                                   String email,
                                   Integer level) {
        address = Keys.toChecksumAddress(address);
        if (level != 4) {
            ExceptionCast.cast(AUTH_INVALID_EQLEVEL);
        }

        QueryWrapper<CyberUsers> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("address", address);
        CyberUsers cyberUsers1 = cyberUsersMapper.selectOne(queryWrapper);

        QueryWrapper<CyberUsers> queryWrappe = new QueryWrapper<>();
        queryWrappe.eq("email", email);
        CyberUsers cyberUser = cyberUsersMapper.selectOne(queryWrappe);
        if (cyberUser != null) {
            ExceptionCast.cast(AUTH_INVALID_EQEMAIL);
        }

        if (cyberUsers1 != null) {
            ExceptionCast.cast(AUTH_INVALID_ADDRS);
        }

        CyberUsers cyberUsers = new CyberUsers();
        cyberUsers.setPassword(RandomPasswordUtils.getPassWordOne(8));
        cyberUsers.setAddress(address);
        cyberUsers.setNikename(nickname);
        cyberUsers.setEmail(email);
        cyberUsers.setDobadge(1L);
        cyberUsers.setLevel(4);
        cyberUsers.setCreateTime(new Date());
        cyberUsers.setUpdateTime(new Date());

        cyberUsersMapper.insert(cyberUsers);

        CyberAgency cyberAgency = new CyberAgency();
        cyberAgency.setUid(cyberUsers.getId());
        String inv1 = getRandomString(8);
        String inv2 = getRandomString(7);
        String inv3 = getRandomString(6);
        cyberAgency.setOneClass(inv1);
        cyberAgency.setTwoClass(inv2);
        cyberAgency.setThreeClass(inv3);
        cyberAgency.setCreateTime(new Date());
        cyberAgency.setUpdateTime(new Date());
        cyberAgency.setAddress(address);
        cyberAgencyMapper.insert(cyberAgency);
        return decorateReturnObject(new ReturnObject<>(cyberAgency));
//        return decorateReturnObject(cyberUsersService.inviter(addr.toLowerCase()));
    }

    //注册区域级级的  3
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包地址", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "nickname", value = "别名", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "email", value = "邮箱地址", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "level", value = "等级", required = true, dataType = "String",defaultValue = ""),
            @ApiImplicitParam(name = "sublevel", value = "区域代理等级", required = true, dataType = "String",defaultValue = ""),
    })
    @ApiOperation(value ="区域级注册  level ==3 接口",notes = "")
    @PostMapping("arealevel")
    public Object setArea(String address,
                          String nickname,
                          String email,
                          Integer level,
                          Integer sublevel) {
        address = Keys.toChecksumAddress(address);

        if (level != 3) {
            ExceptionCast.cast(AUTH_INVALID_EQLEVEL);
        }

        QueryWrapper<CyberUsers> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("address", address);
        CyberUsers cyberUsers1 = cyberUsersMapper.selectOne(queryWrapper);

        QueryWrapper<CyberUsers> queryWrappe = new QueryWrapper<>();
        queryWrappe.eq("email", email);
        CyberUsers cyberUser = cyberUsersMapper.selectOne(queryWrappe);
        if (cyberUser != null) {
            ExceptionCast.cast(AUTH_INVALID_EQEMAIL);
        }

        if (cyberUsers1 != null) {
            ExceptionCast.cast(AUTH_INVALID_ADDRS);
        }

        CyberUsers cyberUsers = new CyberUsers();
        cyberUsers.setPassword(RandomPasswordUtils.getPassWordOne(8));
        // 设置区域代理等级
        cyberUsers.setSubLevel(sublevel);
        cyberUsers.setAddress(address);
        cyberUsers.setNikename(nickname);
        cyberUsers.setEmail(email);
        cyberUsers.setDobadge(1L);
        cyberUsers.setLevel(3);
        cyberUsers.setCreateTime(new Date());
        cyberUsers.setUpdateTime(new Date());

        cyberUsersMapper.insert(cyberUsers);

        CyberAgency cyberAgency = new CyberAgency();
        cyberAgency.setUid(cyberUsers.getId());
        //伙伴级 只有3级密码
        String inv2 = getRandomString(7);
        String inv3 = getRandomString(6);
        String inv4 = getRandomString(8);
        if (sublevel!=8){
            cyberAgency.setOneClass(inv4);
        }
        cyberAgency.setTwoClass(inv2);
        cyberAgency.setThreeClass(inv3);
        cyberAgency.setCreateTime(new Date());
        cyberAgency.setUpdateTime(new Date());
        cyberAgency.setAddress(address);
        cyberAgencyMapper.insert(cyberAgency);
        return decorateReturnObject(new ReturnObject<>(cyberAgency));
//        return decorateReturnObject(cyberUsersService.inviter(addr.toLowerCase()));
    }

    //注册伙伴级的  level == 2
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包地址", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "nickname", value = "别名", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "email", value = "邮箱地址", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "level", value = "等级", required = true, dataType = "String",defaultValue = ""),
    })
    @ApiOperation(value ="伙伴级注册  level ==2 接口",notes = "")
    @PostMapping("partnerlevel")
    public Object setPartner(String address,
                             String nickname,
                             String email,
                             Integer level) {
        address = Keys.toChecksumAddress(address);
        if (level != 2) {
            ExceptionCast.cast(AUTH_INVALID_EQLEVEL);
        }

        QueryWrapper<CyberUsers> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("address", address);
        CyberUsers cyberUsers1 = cyberUsersMapper.selectOne(queryWrapper);

        QueryWrapper<CyberUsers> queryWrappe = new QueryWrapper<>();
        queryWrappe.eq("email", email);
        CyberUsers cyberUser = cyberUsersMapper.selectOne(queryWrappe);
        if (cyberUser != null) {
            ExceptionCast.cast(AUTH_INVALID_EQEMAIL);
        }

        if (cyberUsers1 != null) {
            ExceptionCast.cast(AUTH_INVALID_ADDRS);
        }

        CyberUsers cyberUsers = new CyberUsers();
        cyberUsers.setPassword(RandomPasswordUtils.getPassWordOne(8));
        cyberUsers.setAddress(address);
        cyberUsers.setNikename(nickname);
        cyberUsers.setEmail(email);
        cyberUsers.setDobadge(1L);
        cyberUsers.setLevel(2);
        cyberUsers.setCreateTime(new Date());
        cyberUsers.setUpdateTime(new Date());

        cyberUsersMapper.insert(cyberUsers);

        CyberAgency cyberAgency = new CyberAgency();
        cyberAgency.setUid(cyberUsers.getId());
        //伙伴级 只有3级密码
        String inv3 = getRandomString(6);
        cyberAgency.setThreeClass(inv3);
        cyberAgency.setCreateTime(new Date());
        cyberAgency.setUpdateTime(new Date());
        cyberAgency.setAddress(address);
        cyberAgencyMapper.insert(cyberAgency);
        return decorateReturnObject(new ReturnObject<>(cyberAgency));
//        return decorateReturnObject(cyberUsersService.inviter(addr.toLowerCase()));
    }


    // 用户级代理注册
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包地址", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "nickname", value = "别名", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "email", value = "邮箱地址", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "level", value = "等级", required = true, dataType = "String",defaultValue = ""),
    })
    @ApiOperation(value ="用户级代理注册",notes = "")
    @GetMapping("userlevel")
    @Transactional(rollbackFor = Exception.class)
    public Object setUser(String address,
                          String email,
                          Integer level) {

        address = Keys.toChecksumAddress(address);
        System.out.println(address);
        if (level != 1) {
            ExceptionCast.cast(AUTH_INVALID_EQLEVEL);
        }

        LambdaQueryWrapper<CyberUsers> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CyberUsers::getAddress, address);
        queryWrapper.eq(CyberUsers::getEmail, email);
        CyberUsers cyberUsers1 = cyberUsersMapper.selectOne(queryWrapper);

        CyberUsers cyberUsers = new CyberUsers();
        if (cyberUsers1 != null) {
            LambdaUpdateWrapper<CyberUsers> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(CyberUsers::getAddress, address);
            updateWrapper.eq(CyberUsers::getEmail, email);

            cyberUsers = new CyberUsers();
            cyberUsers.setEmail(email);
            cyberUsers.setAddress(address);
            cyberUsers.setUpdateTime(new Date());
            cyberUsersMapper.update(cyberUsers, updateWrapper);
        } else {
            LambdaQueryWrapper<CyberUsers> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(CyberUsers::getEmail, email);
            CyberUsers cyberUsers2 = cyberUsersMapper.selectOne(queryWrapper1);
            queryWrapper1.clear();
            queryWrapper1.eq(CyberUsers::getAddress, address);
            CyberUsers cyberUsers3 = cyberUsersMapper.selectOne(queryWrapper1);

            if (cyberUsers2 != null && cyberUsers3 == null) {
                System.out.println("有邮箱");
                //有邮箱 需要看address不一样覆盖
                LambdaUpdateWrapper<CyberUsers> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.set(CyberUsers::getAddress, address);
                updateWrapper.eq(CyberUsers::getEmail, email);

                cyberUsers = new CyberUsers();
                cyberUsers.setAddress(address);
                cyberUsers.setEmail(email);
                cyberUsers.setUpdateTime(new Date());
                cyberUsersMapper.update(cyberUsers, updateWrapper);
            } else if (cyberUsers3 != null) {
                System.out.println("我有address");
                //有address  邮箱不一样
                ExceptionCast.cast(AUTH_INVALID_ADDRS);
            } else {
                cyberUsers.setPassword(RandomPasswordUtils.getPassWordOne(8));
                cyberUsers.setAddress(address);
                cyberUsers.setEmail(email);
                cyberUsers.setDobadge(0L);
                cyberUsers.setLevel(level);
                cyberUsers.setCreateTime(new Date());
                cyberUsers.setUpdateTime(new Date());

                cyberUsersMapper.insert(cyberUsers);
            }
        }
        cyberUsers.setPassword("");
        return decorateReturnObject(new ReturnObject<>(cyberUsers));
    }

    /**
     * 从官网进入+邀请码链接接口
     * @param address
     * @param email
     * @param icode
     * @param nickname
     * @return
     */
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包地址", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "email", value = "邮箱地址", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "icode", value = "邀请码", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "nickname", value = "别名", required = true, dataType = "String",defaultValue = ""),
    })
    @ApiOperation(value ="从官网进入+邀请码链接接口",notes = "")
    @PostMapping("invuser")
    public Object invUser(String address,
                          String email,
                          String icode,
                          String nickname) {

        System.out.println("address:" + address + "email:" + email + "icode:" + icode + "nickname:" + nickname);

        if (Objects.equals(address, "0")) {
            address = "";
        }

        if (Objects.equals(icode, "0")) {
            icode = "";
        }

        if (Objects.equals(nickname, "0")) {
            nickname = "";
        }

        // 判断地址是否正确
        address = Keys.toChecksumAddress(address);

        if (nickname.isEmpty()) {
            nickname = "cyberpop_user";
        }

        LambdaQueryWrapper<CyberUsers> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CyberUsers::getEmail, email);
        CyberUsers cyberUsers1 = cyberUsersMapper.selectOne(queryWrapper);
        queryWrapper.clear();

        if (cyberUsers1 != null) {
            ExceptionCast.cast(AUTH_INVALID_EQEMAIL);
        }
        CyberUsers cyberUsers = new CyberUsers();
        CyberAgency cyberAgency = new CyberAgency();

        int level = 1;

        // 区域代理等级，默认为0，表示不是区域代理
        int subLevel = 0;
        if ("0x".equals(address)) {
            address = "";
        } else {
            queryWrapper.eq(CyberUsers::getAddress, address);
            CyberUsers cyberUsers2 = cyberUsersMapper.selectOne(queryWrapper);
            if (cyberUsers2 != null) {
                ExceptionCast.cast(AUTH_ADDRESS_P_RECODE);
            }
        }
        if (!icode.isEmpty()) {
            LambdaQueryWrapper<CyberAgency> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            if (icode.length() == 6) {
                lambdaQueryWrapper.eq(CyberAgency::getThreeClass, icode);
            } else if (icode.length() == 7) {
                level = 2;
                lambdaQueryWrapper.eq(CyberAgency::getTwoClass, icode);
            } else if (icode.length() == 8) {
                level = 3;
                // 邀请码为8位，可能是国家级代理或者区域级代理
                lambdaQueryWrapper.eq(CyberAgency::getOneClass,icode);
                CyberAgency cyberAgency1 = cyberAgencyMapper.selectOne(lambdaQueryWrapper);

                LambdaQueryWrapper<CyberUsers> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
                lambdaQueryWrapper1.eq(CyberUsers::getId,cyberAgency1.getUid());
                CyberUsers cyberUsers2 = cyberUsersMapper.selectOne(lambdaQueryWrapper1);
                if (cyberUsers2.getLevel()==4){
                    // 国家级8位邀请码
                    subLevel = 1;
                    cyberUsers.setSubLevel(subLevel);
                }else{
                    // 区域级8位邀请码
                    subLevel = cyberUsers2.getSubLevel()+1;
                    cyberUsers.setSubLevel(subLevel);
                }
            } else {
                ExceptionCast.cast(AUTH_INVALID_CODE);
            }
            CyberAgency cyberAgencys = cyberAgencyMapper.selectOne(lambdaQueryWrapper);

            if (cyberAgencys == null) {
                ExceptionCast.cast(AUTH_INVALID_CODE);
            }

            cyberUsers.setInvId(cyberAgencys.getId());
            cyberUsers.setAddress(address);
            cyberUsers.setLevel(level);
            cyberUsers.setEmail(email);
            cyberUsers.setPassword(RandomPasswordUtils.getPassWordOne(8));
            cyberUsers.setDobadge(0L);

        } else {
            // 此时邀请码为null
            ExceptionCast.cast(AUTH_INVALID_CODE);
        }
        cyberUsers.setCreateTime(new Date());
        cyberUsers.setUpdateTime(new Date());
        cyberUsers.setNikename(nickname);
        cyberUsersMapper.insert(cyberUsers);


        // 设置邀请码
        if (level != 1) {
            System.out.println("------>"+cyberUsers.getId());
            cyberAgency.setUid(cyberUsers.getId());
            String inv3 = getRandomString(6);
            String inv2 = getRandomString(7);
            String inv4 = getRandomString(8);
            if (level == 2) {
                // 伙伴级分配邀请码
                cyberAgency.setThreeClass(inv3);
            } else {
                // 区域1-8级分配邀请码
                cyberAgency.setTwoClass(inv2);
                cyberAgency.setThreeClass(inv3);
                if (subLevel!=8){
                    // 区域1-7级额外分配8位邀请码
                    cyberAgency.setOneClass(inv4);
                }
            }
            cyberAgency.setCreateTime(new Date());
            cyberAgency.setUpdateTime(new Date());
            cyberAgency.setAddress(address);
            cyberAgencyMapper.insert(cyberAgency);
        }

        cyberUsers.setPassword("");
        return decorateReturnObject(new ReturnObject<>(cyberUsers));
    }

    @PostMapping("subcommission")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "amount", value = "交易金额", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "invCode", value = "邀请码", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "hx", value = "交易hash", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value ="分佣",notes = "")
    @Transactional
    public Object purchaseCommission(String amount,
                                     String invCode,
                                     String hx) {
        System.out.println("amount-------->"+amount+"invCode------>"+invCode+"hx------>"+hx);
        if (null == amount || amount.equals("") || null == invCode || invCode.equals("") || null == hx || hx.equals("")){
            return "输入参数不正确";
        }
        DealerHxLog dealerHxLog = dealerHxLogMapper.selectOne(new LambdaQueryWrapper<DealerHxLog>().eq(DealerHxLog::getHx,hx));

        // 如果查询到交易记录存在并且交易状态完成且金额匹配，进行分佣
        if (dealerHxLog!=null && dealerHxLog.getStatus().equals("end") && dealerHxLog.getAmount()==Double.parseDouble(amount) && dealerHxLog.getReadStatus()!=1){
            System.out.println("------>Query qualified transaction records");
            CyberAgency cyberAgency = cyberAgencyMapper.selectOne(new LambdaQueryWrapper<CyberAgency>().eq(CyberAgency::getThreeClass,invCode));
            if (null == cyberAgency){
                ExceptionCast.cast(AUTH_INVALID_CODE);
            }
            CyberUsers cyberUsers = cyberUsersMapper.selectById(cyberAgency.getUid());
            if (null == cyberUsers){
                ExceptionCast.cast(AUTH_INVALID_EQOBJ);
            }
            if (cyberUsers.getLevel()==4){
                System.out.println("------>The invitation code filled in by the user is from the national agent");
                dealerHxLog.setReadStatus(1);
                int res1 = dealerHxLogMapper.updateById(dealerHxLog);
                int res2 = SubcommissionUtils.subcommission(cyberUsers,amount);
                return res1>0 && res2>0 ? 1:0;
            }else if (cyberUsers.getLevel()==3){
                System.out.println("------>The invitation code obtained by the user comes from the regional agent");
                dealerHxLog.setReadStatus(1);
                int res = dealerHxLogMapper.updateById(dealerHxLog);

                int subLevel = cyberUsers.getSubLevel();
                int res1 = SubcommissionUtils.subcommission(cyberUsers,amount);
                CyberUsers cyberUsers1 = SubcommissionUtils.getSuperior(cyberUsers);
                if (null==cyberUsers1){
                    return res1>0 && res>0 ? 1:0;
                }else {
                    int res2 = SubcommissionUtils.subcommission(cyberUsers1,amount);
                    if (subLevel==1){
                        return res1>0 && res2>0 && res>0 ? 1:0;
                    }else if (subLevel==2){
                        CyberUsers cyberUsers2 = SubcommissionUtils.getSuperior(cyberUsers1);
                        if (null!=cyberUsers2){
                            int res3 = SubcommissionUtils.subcommission(cyberUsers2,amount);
                            return res1>0 && res2>0 && res3>0 && res>0 ? 1:0;
                        }else {
                            return res1>0 && res2>0 && res>0 ? 1:0;
                        }
                    }else if (subLevel==3){
                        CyberUsers cyberUsers2 = SubcommissionUtils.getSuperior(cyberUsers1);
                        if (null!=cyberUsers2){ // 321
                            int res3 = SubcommissionUtils.subcommission(cyberUsers2,amount);
                            CyberUsers cyberUsers3 = SubcommissionUtils.getSuperior(cyberUsers2);
                            if (null!=cyberUsers3){ // 321国
                                int res4 = SubcommissionUtils.subcommission(cyberUsers3,amount);
                                return res1>0 && res2>0 && res3>0 && res4>0 && res>0 ? 1:0;
                            }else {
                                return res1>0 && res2>0 && res3>0 && res>0 ? 1:0;
                            }
                        }else {
                            return res1>0 && res2>0 && res>0 ? 1:0;
                        }
                    }else if (subLevel==4){
                        CyberUsers cyberUsers2 = SubcommissionUtils.getSuperior(cyberUsers1);
                        if (null!=cyberUsers2){ // 432
                            int res3 = SubcommissionUtils.subcommission(cyberUsers2,amount);
                            CyberUsers cyberUsers3 = SubcommissionUtils.getSuperior(cyberUsers2);
                            if (null!=cyberUsers3){ // 4321
                                int res4 = SubcommissionUtils.subcommission(cyberUsers3,amount);
                                CyberUsers cyberUsers4 = SubcommissionUtils.getSuperior(cyberUsers3);
                                if (null!=cyberUsers4){ // 4321国
                                    int res5 = SubcommissionUtils.subcommission(cyberUsers4,amount);
                                    return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res>0 ? 1:0;
                                }else {
                                    return res1>0 && res2>0 && res3>0 && res4>0 && res>0 ? 1:0;
                                }
                            }else {
                                return res1>0 && res2>0 && res3>0 && res>0 ? 1:0;
                            }
                        }else {
                            return res1>0 && res2>0 && res>0 ? 1:0;
                        }
                    }else if (subLevel==5){
                        CyberUsers cyberUsers2 = SubcommissionUtils.getSuperior(cyberUsers1);
                        if (null!=cyberUsers2){ // 543
                            int res3 =SubcommissionUtils.subcommission(cyberUsers2,amount);
                            CyberUsers cyberUsers3 = SubcommissionUtils.getSuperior(cyberUsers2);
                            if (null!=cyberUsers3){ // 5432
                                int res4 = SubcommissionUtils.subcommission(cyberUsers3,amount);
                                CyberUsers cyberUsers4 = SubcommissionUtils.getSuperior(cyberUsers3);
                                if (null!=cyberUsers4){ // 54321
                                    int res5 = SubcommissionUtils.subcommission(cyberUsers4,amount);
                                    CyberUsers cyberUsers5 = SubcommissionUtils.getSuperior(cyberUsers4);
                                    if (null!=cyberUsers5){ // 54321国
                                        int res6 = SubcommissionUtils.subcommission(cyberUsers5,amount);
                                        return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res6>0 && res>0 ? 1:0;
                                    }else {
                                        return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res>0 ? 1:0;
                                    }
                                }else {
                                    return res1>0 && res2>0 && res3>0 && res4>0 && res>0 ? 1:0;
                                }
                            }else {
                                return res1>0 && res2>0 && res3>0 && res>0 ? 1:0;
                            }
                        }else {
                            return res1>0 && res2>0 && res>0 ? 1:0;
                        }
                    }else if (subLevel==6){
                        CyberUsers cyberUsers2 = SubcommissionUtils.getSuperior(cyberUsers1);
                        if (null!=cyberUsers2){ // 654
                            int res3 = SubcommissionUtils.subcommission(cyberUsers2,amount);
                            CyberUsers cyberUsers3 = SubcommissionUtils.getSuperior(cyberUsers2);
                            if (null!=cyberUsers3){ // 6543
                                int res4 = SubcommissionUtils.subcommission(cyberUsers3,amount);
                                CyberUsers cyberUsers4 = SubcommissionUtils.getSuperior(cyberUsers3);
                                if (null!=cyberUsers4){ // 65432
                                    int res5 = SubcommissionUtils.subcommission(cyberUsers4,amount);
                                    CyberUsers cyberUsers5 = SubcommissionUtils.getSuperior(cyberUsers4);
                                    if (null!=cyberUsers5){ // 654321
                                        int res6 = SubcommissionUtils.subcommission(cyberUsers5,amount);
                                        CyberUsers cyberUsers6 = SubcommissionUtils.getSuperior(cyberUsers5);
                                        if (null!=cyberUsers6){ // 654321国
                                            int res7 = SubcommissionUtils.subcommission(cyberUsers6,amount);
                                            return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res6>0 && res7>0 && res>0 ? 1:0;
                                        }else {
                                            return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res6>0 && res>0 ? 1:0;
                                        }
                                    }else {
                                        return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res>0 ? 1:0;
                                    }
                                }else {
                                    return res1>0 && res2>0 && res3>0 && res4>0 && res>0 ? 1:0;
                                }
                            }else {
                                return res1>0 && res2>0 && res3>0 && res>0 ? 1:0;
                            }
                        }else {
                            return res1>0 && res2>0 && res>0 ? 1:0;
                        }
                    }else if (subLevel==7){
                        CyberUsers cyberUsers2 = SubcommissionUtils.getSuperior(cyberUsers1);
                        if (null!=cyberUsers2){ // 765
                            int res3 = SubcommissionUtils.subcommission(cyberUsers2,amount);
                            CyberUsers cyberUsers3 = SubcommissionUtils.getSuperior(cyberUsers2);
                            if (null!=cyberUsers3){ // 7654
                                int res4 = SubcommissionUtils.subcommission(cyberUsers3,amount);
                                CyberUsers cyberUsers4 = SubcommissionUtils.getSuperior(cyberUsers3);
                                if (null!=cyberUsers4){ // 76543
                                    int res5 = SubcommissionUtils.subcommission(cyberUsers4,amount);
                                    CyberUsers cyberUsers5 = SubcommissionUtils.getSuperior(cyberUsers4);
                                    if (null!=cyberUsers5){ // 765432
                                        int res6 = SubcommissionUtils.subcommission(cyberUsers5,amount);
                                        CyberUsers cyberUsers6 = SubcommissionUtils.getSuperior(cyberUsers5);
                                        if (null!=cyberUsers6){ // 7654321
                                            int res7 = SubcommissionUtils.subcommission(cyberUsers6,amount);
                                            CyberUsers cyberUsers7 = SubcommissionUtils.getSuperior(cyberUsers6);
                                            if (null!=cyberUsers7){ // 7654321国
                                                int res8 = SubcommissionUtils.subcommission(cyberUsers7,amount);
                                                return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res6>0 && res7>0 && res8>0 && res>0 ? 1:0;
                                            }else {
                                                return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res6>0 && res7>0 && res>0 ? 1:0;
                                            }
                                        }else {
                                            return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res6>0 && res>0 ? 1:0;
                                        }
                                    }else {
                                        return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res>0 ? 1:0;
                                    }
                                }else {
                                    return res1>0 && res2>0 && res3>0 && res4>0 && res>0 ? 1:0;
                                }
                            }else {
                                return res1>0 && res2>0 && res3>0 && res>0 ? 1:0;
                            }
                        }else {
                            return res1>0 && res2>0 && res>0 ? 1:0;
                        }
                    }else {
                        CyberUsers cyberUsers2 = SubcommissionUtils.getSuperior(cyberUsers1);
                        if (null!=cyberUsers2){ // 876
                            int res3 = SubcommissionUtils.subcommission(cyberUsers2,amount);
                            CyberUsers cyberUsers3 = SubcommissionUtils.getSuperior(cyberUsers2);
                            if (null!=cyberUsers3){ // 8765
                                int res4 = SubcommissionUtils.subcommission(cyberUsers3,amount);
                                CyberUsers cyberUsers4 = SubcommissionUtils.getSuperior(cyberUsers3);
                                if (null!=cyberUsers4){ // 87654
                                    int res5 = SubcommissionUtils.subcommission(cyberUsers4,amount);
                                    CyberUsers cyberUsers5 = SubcommissionUtils.getSuperior(cyberUsers4);
                                    if (null!=cyberUsers5){ // 876543
                                        int res6 = SubcommissionUtils.subcommission(cyberUsers5,amount);
                                        CyberUsers cyberUsers6 = SubcommissionUtils.getSuperior(cyberUsers5);
                                        if (null!=cyberUsers6){ // 8765432
                                            int res7 = SubcommissionUtils.subcommission(cyberUsers6,amount);
                                            CyberUsers cyberUsers7 = SubcommissionUtils.getSuperior(cyberUsers6);
                                            if (null!=cyberUsers7){ // 87654321
                                                int res8 = SubcommissionUtils.subcommission(cyberUsers7,amount);
                                                CyberUsers cyberUsers8 = SubcommissionUtils.getSuperior(cyberUsers7);
                                                if (null!=cyberUsers8){ // 87654321国
                                                    int res9 = SubcommissionUtils.subcommission(cyberUsers8,amount);
                                                    return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res6>0 && res7>0 && res8>0 && res9>0 && res>0 ? 1:0;
                                                }else {
                                                    return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res6>0 && res7>0 && res8>0 && res>0 ? 1:0;
                                                }
                                            }else {
                                                return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res6>0 && res7>0 && res>0 ? 1:0;
                                            }
                                        }else {
                                            return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res6>0 && res>0 ? 1:0;
                                        }
                                    }else {
                                        return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res>0 ? 1:0;
                                    }
                                }else {
                                    return res1>0 && res2>0 && res3>0 && res4>0 && res>0 ? 1:0;
                                }
                            }else {
                                return res1>0 && res2>0 && res3>0 && res>0 ? 1:0;
                            }
                        }else {
                            return res1>0 && res2>0 && res>0 ? 1:0;
                        }
                    }
                }
            }else {
                System.out.println("------>The invitation code obtained by the user comes from the partner-level agent");
                CyberUsers cyberUsers1 = SubcommissionUtils.getSuperior(cyberUsers);
                if (null!=cyberUsers1){ // 区域代理8级存在
                    dealerHxLog.setReadStatus(1);
                    int res = dealerHxLogMapper.updateById(dealerHxLog);

                    int res1 = SubcommissionUtils.subcommission(cyberUsers1,amount);
                    CyberUsers cyberUsers2 = SubcommissionUtils.getSuperior(cyberUsers1);
                    if (null!=cyberUsers2){ // 区域代理7级存在
                        int res2 = SubcommissionUtils.subcommission(cyberUsers2,amount);
                        CyberUsers cyberUsers3 = SubcommissionUtils.getSuperior(cyberUsers2);
                        if (null!=cyberUsers3){ // 区域代理6级存在
                            int res3 = SubcommissionUtils.subcommission(cyberUsers3,amount);
                            CyberUsers cyberUsers4 = SubcommissionUtils.getSuperior(cyberUsers3);
                            if (null!=cyberUsers4){ // 区域代理5级存在
                                int res4 = SubcommissionUtils.subcommission(cyberUsers4,amount);
                                CyberUsers cyberUsers5 = SubcommissionUtils.getSuperior(cyberUsers4);
                                if (null!=cyberUsers5){ // 区域代理4级存在
                                    int res5 = SubcommissionUtils.subcommission(cyberUsers5,amount);
                                    CyberUsers cyberUsers6 = SubcommissionUtils.getSuperior(cyberUsers5);
                                    if (null!=cyberUsers6){ // 区域代理3级存在
                                        int res6 = SubcommissionUtils.subcommission(cyberUsers6,amount);
                                        CyberUsers cyberUsers7 = SubcommissionUtils.getSuperior(cyberUsers6);
                                        if (null!=cyberUsers7){ // 区域代理2级存在
                                            int res7 = SubcommissionUtils.subcommission(cyberUsers7,amount);
                                            CyberUsers cyberUsers8 = SubcommissionUtils.getSuperior(cyberUsers7);
                                            if (null!=cyberUsers8){ // 区域代理1级存在
                                                int res8 = SubcommissionUtils.subcommission(cyberUsers8,amount);
                                                CyberUsers cyberUsers9 = SubcommissionUtils.getSuperior(cyberUsers8);
                                                if (null!=cyberUsers9){ // 国家级代理存在
                                                    int res9 = SubcommissionUtils.subcommission(cyberUsers9,amount);
                                                    return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res6>0 && res7>0 && res8>0 && res9>0 && res>0 ? 1:0;
                                                }else {
                                                    return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res6>0 && res7>0 && res8>0 && res>0 ? 1:0;
                                                }
                                            }else {
                                                return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res6>0 && res7>0 && res>0 ? 1:0;
                                            }
                                        }else {
                                            return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res6>0 && res>0 ? 1:0;
                                        }
                                    }else {
                                        return res1>0 && res2>0 && res3>0 && res4>0 && res5>0 && res>0 ? 1:0;
                                    }
                                }else {
                                    return res1>0 && res2>0 && res3>0 && res4>0 && res>0 ? 1:0;
                                }
                            }else {
                                return res1>0 && res2>0 && res3>0 && res>0 ? 1:0;
                            }
                        }else {
                            return res1>0 && res2>0 && res>0 ? 1:0;
                        }
                    }else {
                        return res1>0 && res>0 ? 1:0;
                    }
                }else {
                    // 提示信息，表示不参与分佣
                    return 2;
                }
            }
        }else {
            System.out.println("------>未查询到符合条件的交易记录");
            return 0;
        }
    }

    @PostMapping("upGradeDealer")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "invCode", value = "邀请码", dataType = "String", paramType = "query"),
    })
    @ApiOperation(value = "私域升级")
    public SaResult dealerUpgrade(String invCode){
        String loginId = (String) StpUtil.getLoginId();
        LambdaQueryWrapper<CyberUsers> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(CyberUsers::getId,loginId);

        // 要升级的用户
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(lambdaQueryWrapper);
        if (null == cyberUsers){
            return SaResult.error("用户信息有误");
        }

        LambdaQueryWrapper<CyberAgency> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
        if (invCode.length() == 7) {
            lambdaQueryWrapper1.eq(CyberAgency::getTwoClass, invCode);
        } else if (invCode.length() == 8) {
            lambdaQueryWrapper1.eq(CyberAgency::getOneClass,invCode);
        } else {
            return SaResult.error("邀请码无效");
        }

        CyberAgency cyberAgencys = cyberAgencyMapper.selectOne(lambdaQueryWrapper1);
        if (cyberAgencys == null) {
            return SaResult.error("邀请码有误");
        }

        LambdaQueryWrapper<CyberUsers> lambdaQueryWrapper2 = new LambdaQueryWrapper<>();
        lambdaQueryWrapper2.eq(CyberUsers::getId,cyberAgencys.getUid());
        // 上级用户
        CyberUsers cyberUsers1 = cyberUsersMapper.selectOne(lambdaQueryWrapper2);
        if (null == cyberUsers1){
            return SaResult.error("未查询到上级经销商对应的上级用户身份");
        }

        Integer level = cyberUsers.getLevel();
        Integer subLevel = 0;
        if (level==3){
            subLevel = cyberUsers.getSubLevel();
        }
        Integer upLevel = cyberUsers1.getLevel();
        Integer upSubLevel = 0;
        if (upLevel==3){
            upSubLevel = cyberUsers1.getSubLevel();
        }

        boolean isUpGrade = UpGradeCheckUtil.isUpGrade(invCode,level,subLevel,upLevel,upSubLevel);
        if (isUpGrade){
            String inv1 = getRandomString(8);
            String inv2 = getRandomString(7);
            String inv3 = getRandomString(6);
            if (level==1){
                cyberUsers.setInvId(cyberAgencys.getId());
                cyberUsers.setUpdateTime(new Date());


                CyberAgency cyberAgency = new CyberAgency();
                cyberAgency.setUid(cyberUsers.getId());
                cyberAgency.setThreeClass(inv3);
                cyberAgency.setAddress(cyberUsers.getAddress());
                cyberAgency.setCreateTime(new Date());
                cyberAgency.setUpdateTime(new Date());

                if (invCode.length()==8){
                    cyberUsers.setLevel(3);
                    cyberAgency.setTwoClass(inv2);
                    if (upLevel==4){
                        cyberUsers.setSubLevel(1);

                        cyberAgency.setOneClass(inv1);
                    }else {
                        cyberUsers.setSubLevel(upSubLevel+1);
                        if (upSubLevel!=7){
                            cyberAgency.setOneClass(inv1);
                        }
                    }
                }else {
                    cyberUsers.setLevel(2);
                }

                cyberUsersMapper.updateById(cyberUsers);
                cyberAgencyMapper.insert(cyberAgency);
                return SaResult.ok();
            }else if (level==2){
                cyberUsers.setInvId(cyberAgencys.getId());
                cyberUsers.setLevel(3);
                cyberUsers.setUpdateTime(new Date());

                CyberAgency cyberAgency = cyberAgencyMapper.selectOne(new LambdaQueryWrapper<CyberAgency>().eq(CyberAgency::getUid,cyberUsers.getId()));
                if (null == cyberAgency){
                    return SaResult.error("当前升级用户未查询到经销商身份");
                }

                cyberAgency.setTwoClass(inv2);
                cyberAgency.setUpdateTime(new Date());

                if (upLevel==4){
                    cyberUsers.setSubLevel(1);

                    cyberAgency.setOneClass(inv1);
                }else {
                    cyberUsers.setSubLevel(upSubLevel+1);

                    if (upSubLevel!=7){
                        cyberAgency.setOneClass(inv1);
                    }
                }

                cyberUsersMapper.updateById(cyberUsers);
                cyberAgencyMapper.updateById(cyberAgency);
                return SaResult.ok();
            }else {
                cyberUsers.setInvId(cyberAgencys.getId());
                cyberUsers.setUpdateTime(new Date());

                CyberAgency cyberAgency = cyberAgencyMapper.selectOne(new LambdaQueryWrapper<CyberAgency>().eq(CyberAgency::getUid,cyberUsers.getId()));
                if (null == cyberAgency){
                    return SaResult.error("当前升级用户未查询到经销商身份");
                }

                cyberAgency.setUpdateTime(new Date());

                if (upLevel==4){
                    cyberUsers.setSubLevel(1);
                }else {
                    cyberUsers.setSubLevel(upSubLevel+1);
                }

                if (subLevel==8){
                    cyberAgency.setOneClass(inv1);
                }

                cyberUsersMapper.updateById(cyberUsers);
                cyberAgencyMapper.updateById(cyberAgency);
                return SaResult.ok();
            }
        }else {
            return SaResult.error(UpGradeCheckUtil.errorMessage);
        }
    }
}