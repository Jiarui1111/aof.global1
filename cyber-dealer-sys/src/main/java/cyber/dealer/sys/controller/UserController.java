package cyber.dealer.sys.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cyber.dealer.sys.annotation.IpWhitelist;
import cyber.dealer.sys.constant.ReturnObject;
import cyber.dealer.sys.domain.*;
import cyber.dealer.sys.exception.ExceptionCast;
import cyber.dealer.sys.mapper.CyberAgencyMapper;
import cyber.dealer.sys.mapper.CyberSystemMapper;
import cyber.dealer.sys.mapper.CyberUsersMapper;
import cyber.dealer.sys.mapper.EmailWalletMapper;
import cyber.dealer.sys.service.CyberUsersService;
import cyber.dealer.sys.util.ObjectToMapUtil;
import cyber.dealer.sys.util.RedisUtils;
import io.lettuce.core.RedisException;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Keys;
import springfox.documentation.annotations.ApiIgnore;
import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static cyber.dealer.sys.constant.ReturnNo.*;
import static cyber.dealer.sys.util.Common.decorateReturnObject;

/**
 * @author lfy
 * @Date 2022/4/22 16:59
 */
@CrossOrigin
@RestController
@RequestMapping("user")
@Api(tags = "user页面的Controller")
@Slf4j
public class UserController {

    @Autowired
    private CyberUsersService cyberUsersService;

    @Autowired
    private CyberAgencyMapper cyberAgencyMapper;

    @Autowired
    private CyberUsersMapper cyberUsersMapper;

    @Autowired
    private CyberSystemMapper cyberSystemMapper;

    @Autowired
    private EmailWalletMapper emailWalletMapper;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("getdata")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value = "根据邮箱查询所有下级的接口", notes = "")
    @ApiIgnore
    public Object getData(String email) {
        return decorateReturnObject(cyberUsersService.getData(email));
    }

    @GetMapping("doLogin")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包地址", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value = "用户address登录的接口", notes = "")
    @ApiIgnore
    public Object doLogin(String address) {
        return decorateReturnObject(cyberUsersService.eqAddress(address));
    }

    @PostMapping("doLoginEmail")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value = "用户email登录的接口", notes = "")
    public Object doLoginEmail(HttpServletRequest request, String email, String password) {
        String header = request.getHeader("X-Forwarded-For");
        String header1 = request.getHeader("X-Real-IP");
        String remoteAddr = request.getRemoteAddr();

        System.out.println("X-Forwarded-For->"+header);
        System.out.println("X-Real-IP->"+header1);
        System.out.println("remoteAddr->"+remoteAddr);
        return decorateReturnObject(cyberUsersService.doLoginEmail(email, password));
    }

    @PostMapping("updatePassword")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "String", defaultValue = "")
    })
    @ApiOperation(value = "重置密码", notes = "")
    public SaResult updatePassword(String email, String password) {
        boolean update = cyberUsersService.lambdaUpdate().eq(CyberUsers::getEmail, email).set(CyberUsers::getPassword, password).update();
        if (update){
            return SaResult.ok("重置成功");
        }
        return SaResult.error("重置失败");
    }

    @PostMapping("bindingAddress")
    @IpWhitelist
    @Transactional(rollbackFor = Exception.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "address", value = "钱包", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value = "绑定web3钱包", notes = "")
    public Object bindingAddress(String email, String address) {
        address = Keys.toChecksumAddress(address);
        if (address.length() != 42) {
            return decorateReturnObject(new ReturnObject<>(AUTH_ADDRESS_RECODE, "Invalid address"));
        }
        CyberUsers one = cyberUsersService.lambdaQuery().eq(CyberUsers::getEmail, email).one();
        CyberUsers one1 = cyberUsersService.lambdaQuery().eq(CyberUsers::getWeb3Wallet, address).one();
        if (one == null){
            return decorateReturnObject(new ReturnObject<>(AUTH_INVALID_EQOBJ, "The email has not been registered yet"));
        }
//        if (one.getWeb3Wallet()!=null && !one.getWeb3Wallet().equals("")){
//            return decorateReturnObject(new ReturnObject<>(AUTH_ADDRESS_P_RECODE, "Already bound to wallet"));
//        }
        if (one1 != null) {
            return decorateReturnObject(new ReturnObject<>(AUTH_ADDRESS_P_RECODE, "The wallet has been bound"));
        }

        if (redisUtils.hasKey("bind->"+email)){
            return decorateReturnObject(new ReturnObject<>(AUTH_FREQUENT_BIND, "Frequent binding"));
        }

        String wallet = one.getWeb3Wallet();
//
        one.setWeb3Wallet(address);
        cyberUsersMapper.updateById(one);

        redisUtils.set("bind->"+email, 1, 1800);
        return decorateReturnObject(new ReturnObject<>(wallet));


    }

    @GetMapping("outLogin")
    @ApiIgnore
    public Object outLogin(String email) {
        return decorateReturnObject(cyberUsersService.outAddress(email));
    }

    @GetMapping("download")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value = "根据钱包埋点下载的接口", notes = "")
    @ApiIgnore
    public Object download(String address) {
        address = Keys.toChecksumAddress(address);
        return decorateReturnObject(new ReturnObject<>(cyberUsersService.lambdaUpdate().set(CyberUsers::getDownload, 1).eq(CyberUsers::getAddress, address).update()));
    }

    @GetMapping("downloademail")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value = "根据邮箱埋点下载的接口", notes = "")
    @ApiIgnore
    public Object downloademail(String email) {
        return decorateReturnObject(new ReturnObject<>(cyberUsersService.lambdaUpdate().set(CyberUsers::getDownload, 1).eq(CyberUsers::getEmail, email).update()));
    }

    @PostMapping("isLogin")
    @ApiIgnore
    public Object isLogin() {
        return "当前会话是否登录：" + StpUtil.isLogin() + "" + StpUtil.getLoginId();
    }

    /**
     * 禁用用户时长
     * @param address
     * @param day
     * @return
     */
    @PostMapping("disable")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "day", value = "天", required = true, dataType = "Integer", defaultValue = ""),
    })
    @ApiOperation(value = "禁用用户的接口(勿用未做权限分离)", notes = "")
    @ApiIgnore
    public Object isLogin(String address, Integer day) {
//        CyberUsers entity = cyberUsersService.lambdaQuery().eq(CyberUsers::getAddress, address).getEntity();
        LambdaQueryWrapper<CyberUsers> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CyberUsers::getAddress, address);
        CyberUsers one = cyberUsersService.getOne(queryWrapper);
        long time = 0;
        if (day == -1) {
            time = -1;
        } else if (day > 0) {
            time = day * 60 * 60 * 24;
        } else {
            ExceptionCast.cast(AUTH_INVALID_EQADMINEX);
        }
        StpUtil.kickout(one.getId());
        StpUtil.disable(one.getId(), time);
        return "当前会话是否登录：" + StpUtil.isLogin() + "" + StpUtil.getLoginId();
    }

    /**
     * 其他业务中断
     * @param address
     * @return
     */
    @PostMapping("getdatas")
    @ApiIgnore
    public Object getDatas(String address) {
        address = Keys.toChecksumAddress(address);
        return decorateReturnObject(cyberUsersService.findAll(address));
    }

    /**
     * 做了登录埋点等
     * @param email
     * @return
     */
    @GetMapping("getuser")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value = "获取用户及登录信息", notes = "")
    @ApiIgnore
    public Object getUser(String email) {
        QueryWrapper<CyberUsers> queryWrapper = new QueryWrapper<>();
        queryWrapper.select(CyberUsers.class, info -> !info.getColumn().equals("password")).eq("email", email);
        CyberUsers one = cyberUsersService.getOne(queryWrapper);
        if (one == null) {
            return decorateReturnObject(new ReturnObject<>(false));
        }
        return decorateReturnObject(new ReturnObject<>(cyberUsersService.getuser(one)));
    }

    /**
     * 根据email取别名
     * @param nikename
     * @param email
     * @return
     */
    @PutMapping("nickname")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "nikename", value = "别名", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "email", value = "邮箱", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value = "根据邮箱给其他用户取别名", notes = "")
    @ApiIgnore
    public Object setNickname(String nikename, String email) {
        return decorateReturnObject(new ReturnObject<>(cyberUsersService.setNikename(nikename, email)));
    }

    @GetMapping("getBindAddress")
    @IpWhitelist
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value = "查询web3钱包", notes = "")
    public SaResult getBindAddress(String email) {
        LambdaQueryWrapper<CyberUsers> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(CyberUsers::getEmail, email);
        CyberUsers entity = cyberUsersService.getOne(lambdaQueryWrapper);
        if (entity == null) {
            return SaResult.error("邮箱不存在");
        }
        return SaResult.data(entity.getWeb3Wallet());
    }

    @GetMapping("baddress")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value = "查询当前address是否存在绑定了什么邮箱的接口", notes = "")
    @ApiIgnore
    public Object bAddress(String address) {
        address = Keys.toChecksumAddress(address);
        LambdaQueryWrapper<CyberUsers> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(CyberUsers::getAddress, address);
        CyberUsers entity = cyberUsersService.getOne(lambdaQueryWrapper);
        if (entity == null) {
            return decorateReturnObject(new ReturnObject<>(true));
        }
        return decorateReturnObject(new ReturnObject<>(entity.getEmail()));
    }

    /**
     * 带分页查询(模糊)  位置人员汇总
     *
     * @param address
     * @param email
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("fselect")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "email", value = "邮箱", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "page", value = "第几页", required = true, dataType = "Integer", defaultValue = ""),
            @ApiImplicitParam(name = "pageSize", value = "数量", required = true, dataType = "Integer", defaultValue = ""),
    })
    @ApiOperation(value = "带分页查询(模糊)  位置人员汇总的接口", notes = "")
    @ApiIgnore
    public Object fselect(String address, String email, Integer page, Integer pageSize) {
//        address = Keys.toChecksumAddress(address);

        if ("".equals(address) && "".equals(email)) {
            return decorateReturnObject(new ReturnObject<>(false));
        }

        IPage<CyberUsers> pages = new Page<>(page, pageSize);
        LambdaQueryWrapper<CyberUsers> lambdaQueryWrap = new LambdaQueryWrapper<>();
        lambdaQueryWrap
                .select(CyberUsers.class, info -> !info.getColumn().equals("password"))
                .like(!"".equals(address), CyberUsers::getAddress, address)
                .like(!"".equals(email), CyberUsers::getEmail, email)
        ;

        IPage<CyberUsers> page1 = cyberUsersService.page(pages, lambdaQueryWrap);

        long pages1 = page1.getPages();
        List<CyberUsers> records = page1.getRecords();
        long total = page1.getTotal();
        long size = page1.getSize();
        if (records == null) {
            return decorateReturnObject(new ReturnObject<>(false));
        }

        List list = new ArrayList<>();
        String address1 = "";

        for (CyberUsers users : records) {
            int level1 = 0;
            int level2 = 0;
            int level3 = 0;
            if (0 != users.getInvId()) {
                if (users.getLevel() == 1) {
                    QueryWrapper<CyberAgency> queryWrapper1 = new QueryWrapper<>();
                    queryWrapper1.eq("id", users.getInvId());
                    CyberAgency cyberAgency = cyberAgencyMapper.selectOne(queryWrapper1);
                    address1 = cyberAgency.getAddress();
                } else {
//                    System.out.println(1);
                    QueryWrapper<CyberAgency> queryWrapper1 = new QueryWrapper<>();
                    queryWrapper1.eq("id", users.getInvId());
                    CyberAgency cyberAgency = cyberAgencyMapper.selectOne(queryWrapper1);
                    address1 = cyberAgency.getAddress();

                    QueryWrapper<CyberAgency> queryWrapper3 = new QueryWrapper<>();
                    queryWrapper3.eq("uid", users.getId());
                    CyberAgency cyberAgency1 = cyberAgencyMapper.selectOne(queryWrapper3);

                    QueryWrapper<CyberUsers> queryWrapper2 = new QueryWrapper<>();
                    queryWrapper2.select(CyberUsers.class, info -> !info.getColumn().equals("password")).eq("inv_id", cyberAgency1.getId());
                    List<CyberUsers> cyberUsers1 = cyberUsersService.list(queryWrapper2);
                    for (CyberUsers cyberUsersss : cyberUsers1) {
                        if (cyberUsersss.getLevel() == 1) {
                            level1 += 1;
                        }
                        if (cyberUsersss.getLevel() == 2) {
                            level2 += 1;
                        }
                        if (cyberUsersss.getLevel() == 3) {
                            level3 += 1;
                        }
                    }
                }

                Map convert = ObjectToMapUtil.convert(users);
                convert.put("countlevel1", level1);
                convert.put("countlevel2", level2);
                convert.put("countlevel3", level3);
                convert.put("toAddress", address1);
                list.add(convert);
            } else {
                //没有上级
                if (users.getLevel() != 1) {
                    QueryWrapper<CyberAgency> queryWrapper3 = new QueryWrapper<>();
                    queryWrapper3.eq("uid", users.getId());
                    CyberAgency cyberAgency1 = cyberAgencyMapper.selectOne(queryWrapper3);

                    QueryWrapper<CyberUsers> queryWrapper2 = new QueryWrapper<>();
                    queryWrapper2.select(CyberUsers.class, info -> !info.getColumn().equals("password")).eq("inv_id", cyberAgency1.getId());
                    List<CyberUsers> cyberUsers1 = cyberUsersService.list(queryWrapper2);
//                    System.out.println(cyberUsers1);
                    for (CyberUsers list1 : cyberUsers1) {
                        if (list1.getLevel() == 1) {
                            level1 += 1;
                        }
                        if (list1.getLevel() == 2) {
                            level2 += 1;
                        }
                        if (list1.getLevel() == 3) {
                            level3 += 1;
                        }
                    }
                }

                Map convert = ObjectToMapUtil.convert(users);
                convert.put("countlevel1", level1);
                convert.put("countlevel2", level2);
                convert.put("countlevel3", level3);
                list.add(convert);
            }
        }


        Map map = new HashMap();
        map.put("page", pages1);
        map.put("total", total);
        map.put("size", size);
        list.add(map);

        return decorateReturnObject(new ReturnObject<>(list));
    }

    /**
     * 提现   (未使用 闭环)
     *
     * @param email
     * @param address
     * @param personalreward
     * @return
     */
    @GetMapping("alltransfer")
    @ApiIgnore
    @Transactional(rollbackFor = Exception.class)
    public synchronized Object allTransfer(String email, String address, Double personalreward) {
        if (StringUtils.isBlank(email) || StringUtils.isBlank(address)) {
            ExceptionCast.cast(AUTH_TRANSFER_ALLTRANSFER);
        }
        QueryWrapper<CyberUsers> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .select(CyberUsers.class, info -> !info.getColumn().equals("password"))
                .eq("email", email)
                .eq("address", address);
        CyberUsers one = cyberUsersService.getOne(queryWrapper);
        Double personalrewards = Double.valueOf(one.getPersonalrewards());

        if (personalrewards < personalreward) {
            ExceptionCast.cast(AUTH_TRANSFER_ALLTRANSFER);
        }

        double v = personalrewards - personalreward;

        if (v <= 0) {
            ExceptionCast.cast(AUTH_TRANSFER_ALLTRANSFER);
        }

        UpdateWrapper<CyberUsers> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("email", email);
        updateWrapper.eq("address", address);
        updateWrapper.set("personalrewards", v);
        boolean update = cyberUsersService.update(updateWrapper);
        return decorateReturnObject(new ReturnObject<>(update));
    }

//    @GetMapping("getCode")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "收件人", dataType = "String", paramType = "query"),
    })
    @ApiOperation(value = "发送验证码")
    public SaResult getCode(String email){
        return cyberUsersService.getCode(email);
    }

//    @PostMapping("emailRegister")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "verifyCode", value = "验证码", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value ="邮箱注册",notes = "")
    @ApiIgnore
    @Transactional(rollbackFor = Exception.class)
    public SaResult setUser(String email, String password, String verifyCode) {
        return cyberUsersService.emailRegister(email, password, verifyCode);
    }

//    @PostMapping("upGrade")
    @ApiOperation(value = "公域用户升级", notes = "通过邮箱注册的公域用户升级接口")
    @ApiIgnore
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "address", value = "钱包地址", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "invCode", value = "邀请码", dataType = "String", paramType = "query"),
    })
    public Object upGrade(String email,String address,String invCode){
        return decorateReturnObject(cyberUsersService.upGrade(email, address, invCode));
    }

    @GetMapping("getPass")
    @ApiOperation(value = "查询密码")
    @ApiIgnore
    public SaResult getPass(){
        return cyberUsersService.getPass();
    }

    @GetMapping("/getAddress")
    @IpWhitelist
    @ApiOperation(value = "查询内置钱包")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", dataType = "String", paramType = "query")
    })
    public SaResult getAddressByEmail(String email){
        return cyberUsersService.getAddressByEmail(email);
    }

    @PostMapping("/sendVerificationCode")
    @ApiOperation(value = "发送指定验证码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "收件人", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "code", value = "验证码", dataType = "String", paramType = "query"),
    })
    public SaResult sendVerifyCode(String email, String code){
        return cyberUsersService.sendVerifyCode(email, code);
    }

    @PostMapping("/generateAddress")
    @IpWhitelist
    @ApiOperation(value = "生成内置钱包并进行邀请码（可选）注册")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "password", value = "密码", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "invcode", value = "邀请码", dataType = "String", paramType = "query")
    })
    public SaResult generateAddress(String email, String password, String invcode){
        return cyberUsersService.generateAddress(email, password, invcode);
    }

    @PostMapping("/upgradeNoCode")
    @ApiOperation(value = "无邀请码升级")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", dataType = "String", paramType = "query")
    })
    public SaResult upgradeNoCode(String email){
        return cyberUsersService.upgradeNoCode(email);
    }

    @GetMapping("/getUserInfo")
    @IpWhitelist
    @ApiOperation(value = "免登录获取用户信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", dataType = "String", paramType = "query"),
    })
    public SaResult getUserInfo(String email){
        return cyberUsersService.getUserInfo(email);
    }

    @PostMapping("/doReward")
    @IpWhitelist
    @ApiOperation(value = "游戏对局奖励")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "amount", value = "奖励数量", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "date", value = "对局时间,格式（2023-06-19 12:33:14）", dataType = "String", paramType = "query"),
    })
    public SaResult doReward(String email, String amount, String date){
        return cyberUsersService.doReward(email, amount, date);
    }

    @PostMapping("/cache")
    @ApiOperation(value = "密码邀请码缓存")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "password", value = "密码", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "invcode", value = "邀请码", dataType = "String", paramType = "query"),
    })
    public SaResult cache(String email, String password, String invcode){
        try {
            boolean set = redisUtils.set("pass_" + email, password);
            boolean set1 = redisUtils.set("code_" + email, invcode);
            if (set && set1){
                return SaResult.ok("缓存成功");
            }else {
                return SaResult.error("缓存失败");
            }
        }catch (RedisException e){
            return SaResult.error("redis异常");
        }
    }

    @GetMapping("/sendCodePlus")
    @ApiOperation(value = "发送验证码（封装）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "user", value = "邮箱", required = true, dataType = "String", paramType = "query"),
    })
    public Object sendCodePlus(HttpServletRequest request, String user) {

        List<String> blackList = (List) redisTemplate.opsForValue().get("blackList");
        if (null != blackList){
            for (String s : blackList) {
                if (user.contains(s)){
                    return "Blacklist access prohibited";
                }
            }
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null) {
            ip = request.getRemoteAddr();
        }

        boolean b = redisUtils.hasKey("totalAmount");
        if (b){
            String s = redisUtils.get("totalAmount");
            if (Integer.valueOf(s)>=20){
                return "Prohibit frequent operations, please try in a minute";
            }
            int a = Integer.valueOf(s);
            a++;
            long expire = redisUtils.getExpire("totalAmount");
            if (expire>0){
                redisUtils.set("totalAmount",a,expire);
            }else {
                redisUtils.set("totalAmount",1,60);
            }
        }else {
            redisUtils.set("totalAmount",1,60);
        }

        if (!ip.equals("18.138.69.156")){
            boolean ip_limt = redisUtils.hasKey(ip + "_count");
            if (ip_limt) {
                String s = redisUtils.get(ip + "_count");
                if  (Integer.valueOf(s)>=10){
                    long expire = redisUtils.getExpire(ip + "_count");
                    long l = expire / 60;
                    if (l>0){
                        return "Current IP frequent operations,Please try again in "+l+" minutes";
                    }else {
                        redisUtils.set(ip + "_count",1,3600);
                    }
                }else {
                    int a = Integer.valueOf(s);
                    a++;
                    long expire = redisUtils.getExpire(ip + "_count");
                    if (expire>0){
                        redisUtils.set(ip + "_count",a,expire);
                    }else {
                        redisUtils.set(ip + "_count",1,3600);
                    }
                }
            } else {
                redisUtils.set(ip + "_count",1,3600);
            }
        }
        return cyberUsersService.sendCodePlus(user);
    }

    @GetMapping("/create")
    @ApiOperation(value = "create（封装）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "user", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "password", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "verifyCode", value = "", dataType = "String", paramType = "query"),
    })
    public Object createPlus(String user,String password, String verifyCode){
        return cyberUsersService.create(user,password,verifyCode);
    }

    @PostMapping("/addBlackList")
    @ApiOperation(value = "添加黑名单邮箱")
//    @IpWhitelist
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "邮箱后缀", dataType = "String", paramType = "query")
    })
    public SaResult addBlackList(String name){
        boolean isBlackList = redisUtils.hasKey("blackList");
        if (isBlackList){
            List<String> blackList = (List<String>) redisTemplate.opsForValue().get("blackList");
            blackList.add(name);
            redisUtils.set("blackList",blackList);
        }else {
            List<String> list = new ArrayList<>();
            list.add(name);
            redisUtils.set("blackList",list);
        }
        return SaResult.ok();
    }

    @PostMapping("/resetPass")
    @ApiOperation(value = "重置密码(封装)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "user", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "password", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "verifyCode", value = "", required = true, dataType = "String", paramType = "query")
    })
    public Object resetPass(String user, String password, String verifyCode){
        return cyberUsersService.resetPass(user, password, verifyCode);
    }

    @PostMapping("addWhiteList")
    @ApiOperation(value = "添加白名单ip", notes = "接口访问白名单ip")
//    @IpWhitelist
    public SaResult addWhileList(String ip){
        boolean whiteList = redisUtils.hasKey("whiteList");
        if (whiteList){
            List<String> list = (List<String>) redisTemplate.opsForValue().get("whiteList");
            list.add(ip);
            redisUtils.set("whiteList",list);
        }else {
            List<String> list = new ArrayList<>();
            list.add(ip);
            redisUtils.set("whiteList",list);
        }
        return SaResult.ok();
    }

//    @PostMapping("deleteWhiteListIp")
    @IpWhitelist
    @ApiOperation(value = "")
    public SaResult deleteWhiteListIp(String ip){
        List<String> list = (List<String>) redisTemplate.opsForValue().get("whiteList");
        list.remove(ip);
        redisUtils.set("whiteList",list);
        return SaResult.ok();
    }

    @PostMapping("/getVote")
    @ApiOperation(value = "根据邮箱查询积分")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "", required = true, dataType = "String", paramType = "query")
    })
    public SaResult getVote(String email){
        return cyberUsersService.getVote(email);
    }

    @PostMapping("/grantPoint")
    @IpWhitelist
    @ApiOperation(value = "增加积分接口（白名单无登录）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "amount", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public SaResult grantPoint(String email, String amount){
        return cyberUsersService.grantPoint(email, amount);
    }

    @PostMapping("/grantToken")
    @IpWhitelist
    @ApiOperation(value = "增加代币接口（白名单无登录）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "amount", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public SaResult grantToken(String address, String amount){
        return cyberUsersService.grantToken(address, amount);
    }

    @PostMapping("/getTokenByWallet")
    @ApiOperation(value = "根据redis特定钱包查询AOF Token")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public SaResult getTokenByWallet(String address){
        return cyberUsersService.getTokenByWallet(address);
    }

    @PostMapping("/getEmailByWeb3Wallet")
    @ApiOperation(value = "根据web3钱包查询邮箱")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public SaResult getEmailByWeb3Wallet(String address){
        return cyberUsersService.getEmailByWeb3Wallet(address);
    }

    @PostMapping("/connectRecord")
    @ApiOperation(value = "记录推特及discord连接情况")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "type", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public SaResult connectRecord(String email, String type){
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, email));
        if (cyberUsers == null){
            return SaResult.error("The email has not been registered yet");
        }

        if (!type.equals("twitter") && !type.equals("discord")){
            return SaResult.error("type error");
        }

        boolean b = redisUtils.hasKey("TDC->" + email);
        if (b){
            List<String> list = (List<String>) redisTemplate.opsForValue().get("TDC->" + email);
            if (list.size()==1){
                if (!list.get(0).equals(type)){
                    list.add(type);
                    redisUtils.set("TDC->" + email, list);
                    return SaResult.ok();
                }else {
                    return SaResult.data("It has already been bound");
                }
            }else {
                return SaResult.data("All bound");
            }
        }else {
            List<String> list = new ArrayList<>();
            list.add(type);
            redisUtils.set("TDC->" + email,list);
            return SaResult.ok();
        }
    }

    @PostMapping("/firstBoundGet")
    @ApiOperation(value = "首次绑定twitter及discord领取10积分")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public SaResult firstBoundGet(String email){
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, email));
        if (cyberUsers == null){
            return SaResult.error("The email has not been registered yet");
        }

        // 是否领取
        boolean b = redisUtils.hasKey("TDCGet->" + email);
        if (b){
            return SaResult.error("You have already claimed it");
        }
        // 绑定记录
        boolean b1 = redisUtils.hasKey("TDC->" + email);
        if (!b1){
            return SaResult.error("Please bind Twitter and Discord first");
        }
        List<String> list = (List<String>) redisTemplate.opsForValue().get("TDC->" + email);
        if (list.size()==1){
            if (list.get(0).equals("twitter")){
                return SaResult.error("Please bind discord first");
            }else {
                return SaResult.error("Please bind to Twitter first");
            }
        }else {
            boolean b2 = redisUtils.hasKey("aofpoint->" + email);
            if (b2){
                int point = Integer.parseInt(redisUtils.get("aofpoint->"+email));
                redisUtils.set("aofpoint->"+email, point+10);
            }else {
                redisUtils.set("aofpoint->"+email,10);
            }
            redisUtils.set("TDCGet->" + email,1);
            return SaResult.ok();
        }
    }

    @PostMapping("/regularBet")
    @ApiOperation(value = "常规赛记录（只投一次）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roundId", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "teamId", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "other", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "address", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public SaResult regularBet(String roundId, String teamId, String other, String address){
        return cyberUsersService.regularBet(roundId, teamId, other, address);
    }

    @PostMapping("/getRegBetAmount")
    @ApiOperation(value = "常规赛投票情况")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roundId", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "teamId", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "other", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public SaResult getRegBetAmount(String roundId, String teamId, String other){
        return cyberUsersService.getRegBetAmount(roundId, teamId, other);
    }

    @PostMapping("/getRegBetStatus")
    @ApiOperation(value = "个人常规赛投票状态")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roundId", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "teamId", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "other", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "address", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public SaResult getRegBetStatus(String roundId, String teamId, String other, String address){
        return cyberUsersService.getRegBetStatus(roundId, teamId, other, address);
    }

    @PostMapping("/finalBet")
    @ApiOperation(value = "总决赛记录（只投一次）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "teamId", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "address", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public SaResult finalBet(String teamId, String address){
        return cyberUsersService.finalBet(teamId, address);
    }

    @PostMapping("/getFinalBetAmount")
    @ApiOperation(value = "总决赛投票情况")
    public SaResult getFinalBetAmount(){
        return cyberUsersService.getFinalBetAmount();
    }

    @PostMapping("/getFinalBetStatus")
    @ApiOperation(value = "个人总决赛投票状态")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public SaResult getFinalBetStatus(String address){
        return cyberUsersService.getFinalBetStatus(address);
    }

    @PostMapping("/checkWhiteList")
    @ApiOperation(value = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public SaResult checkWhiteList(String email){
        boolean b = redisUtils.hasKey("checkema->" + email);
        if (b){
            return SaResult.ok("true");
        }

        CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, email));
        if (cyberUsers == null){
            return SaResult.error("The email has not been registered yet");
        }

        if (cyberUsers.getWeb3Wallet()==null || cyberUsers.getWeb3Wallet().equals("")){
            return SaResult.error("Not yet bound to wallet");
        }

        boolean c = redisUtils.hasKey("checkadd->" + cyberUsers.getWeb3Wallet().toLowerCase());

        if (c){
            return SaResult.ok("true");
        }else {
            return SaResult.error("false");
        }
    }

//    @PostMapping("/addAofToken")
    @ApiOperation(value = "手动导入领取奖励的玩家信息（仅一次）")
    public SaResult addredis(){
        String addressString = "";

        List<String> addressList = new ArrayList<>();
        String removedSpacesStr = addressString.replaceAll("\\s+", "");
        for (int i = 0; i < removedSpacesStr.length(); i += 42) {
            addressList.add(removedSpacesStr.substring(i, i + 42));
        }

        System.out.println(addressList.size());
        for (String s : addressList) {
            CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getAddress, s));
            if (cyberUsers == null){
                System.out.println(s);
            }
        }
        return SaResult.ok();
    }

    @PostMapping("/addWhiteListEmail")
    @IpWhitelist
    public SaResult addemail (String email, String pass){
        if (!pass.equals("test111")){
            return SaResult.error();
        }
        redisUtils.set("checkema->"+email,1);
        return SaResult.ok();
    }

    @PostMapping("/login")
    @ApiOperation(value = "登录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "user", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "password", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public Object login(String user, String password) {
        return cyberUsersService.login(user, password);
    }

    @PostMapping("/refreshToken")
    @ApiOperation(value = "刷新token")
    public Object refreshToken(HttpServletRequest request) {
        return cyberUsersService.refreshToken(request);
    }

    @PostMapping("/info")
    @ApiOperation(value = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "mail", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public Object info(HttpServletRequest request, String mail) {
        return cyberUsersService.info(request, mail);
    }

    @PostMapping("/viewUser")
    @ApiOperation(value = "查询别人信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "playerId", value = "", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "email", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public Object viewUser(int playerId, String email) {
        return cyberUsersService.viewUser(playerId, email);
    }

    @PostMapping("/warInfo")
    @ApiOperation(value = "查看当前的赛事信息")
    @ApiImplicitParams({
    })
    public Object warInfo() {
        return cyberUsersService.warInfo();
    }

    @PostMapping("/troopInfo")
    @ApiOperation(value = "查看队伍信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "troopId", value = "", required = true, dataType = "String", paramType = "query"),
    })
    public Object troopInfo(int troopId) {
        return cyberUsersService.troopInfo(troopId);
    }


    @PostMapping("/searchInfo")
    @ApiOperation(value = "查看信息")
    @ApiImplicitParams({
    })
    public Object searchInfo() {
        return cyberUsersService.searchInfo();
    }

    @PostMapping("/addFinalTime")
    @IpWhitelist
    @ApiOperation(value = "将总决赛投票时间添加到redis")
    public SaResult addFinalTeamTime(String startTime, String endTime){
        String finalStartKey = "finalStart->";
        String finalEndKey = "finalEnd->";
        redisUtils.set(finalStartKey, startTime);
        redisUtils.set(finalEndKey, endTime);
        return SaResult.ok();
    }

    @PostMapping("/addRegTime")
    @IpWhitelist
    @ApiOperation(value = "将常规赛投票时间添加到redis")
    public SaResult addFinalTeamTime(String roundId, String startTime, String endTime){
        String regStartKey = "regStart->"+roundId+"->";
        String regEndKey = "regEnd->"+roundId+"->";

        redisUtils.set(regStartKey, startTime);
        redisUtils.set(regEndKey, endTime);
        return SaResult.ok();
    }

    @PostMapping("/getCurrentRound")
    @ApiOperation(value = "查询当前比赛场次")
    public SaResult getCurrentRound(){
        CyberSystem cyberSystem = cyberSystemMapper.selectOne(new LambdaQueryWrapper<CyberSystem>().eq(CyberSystem::getKeyName, "currentRound"));
        if (cyberSystem!=null){
            return SaResult.data(cyberSystem.getKeyValue());
        }
        return SaResult.error("No relevant information found");
    }

//    @GetMapping("/exportVoteInfo")
    @IpWhitelist
    @ApiOperation(value = "导出投票信息")
    public SaResult exportUserInfo(String roundId, String teamId){
        String key;
        String sheetName;
        if (roundId.equals("final")){
            key = "fin->" + teamId;
            sheetName = "finals_" + teamId;
        }else {
            key = "reg->roundId"+roundId+"team"+teamId;
            sheetName = "regular_season" + roundId + "round" + teamId;
        }
        if (redisUtils.hasKey(key)){
            Set<String> set = (Set<String>) redisTemplate.opsForValue().get(key);
// 添加元素到集合中

// 创建工作簿和工作表
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("sheet1");

// 创建表头
            Row headerRow = sheet.createRow(0);
            Cell headerCell = headerRow.createCell(0);
            headerCell.setCellValue("address");

// 创建数据行
            int rowNum = 1;
            for (String data : set) {
                Row row = sheet.createRow(rowNum++);
                Cell cell = row.createCell(0);
                cell.setCellValue(data);
            }

// 将数据写入文件
            try (FileOutputStream outputStream = new FileOutputStream(sheetName + ".xlsx")) {
                workbook.write(outputStream);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return SaResult.ok();
    }

//    @GetMapping("/voteExport")
    public SaResult voteExport(String pattern, String index,String coum1, String coum2,  String name) throws IOException {


        Set<String> keys = redisTemplate.keys(pattern +"*");
        System.out.println(keys.size());
        Map<String, String> map = new HashMap<>();
        for (String key : keys) {
            String s = redisUtils.get(key);
            map.put(key.substring(Integer.parseInt(index)), s);
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data");

// 创建标题行
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue(coum1);
        headerRow.createCell(1).setCellValue(coum2);

// 填充数据行
        int rowIdx = 1;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }

// 将数据写入Excel文件
        try (FileOutputStream outputStream = new FileOutputStream(name + ".xlsx")) {
            workbook.write(outputStream);
        }
        return SaResult.ok();
    }

    @GetMapping("/getWallet")
    @IpWhitelist
    @ApiOperation(value = "获取私钥文件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", dataType = "String", paramType = "query")
    })
    public SaResult getWallet(String email) {
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, email));
        if (cyberUsers == null){
            return SaResult.error("this email is not exists");
        }

        EmailWallet emailWallet = emailWalletMapper.selectById(email);
        if (emailWallet == null){
            log.error(email + "->no wallet->" + email);
            return SaResult.error("Wallet does not exists");
        }else {
            return SaResult.data(emailWallet.getWallet());
        }
    }
}
