package cyber.dealer.sys.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cyber.dealer.sys.constant.ReturnObject;
import cyber.dealer.sys.domain.CyberAgency;
import cyber.dealer.sys.domain.CyberUsers;
import cyber.dealer.sys.domain.CyberUsersRemarks;
import cyber.dealer.sys.domain.EmailWallet;
import cyber.dealer.sys.domain.vo.searchInfoVo;
import cyber.dealer.sys.exception.ExceptionCast;
import cyber.dealer.sys.mapper.CyberAgencyMapper;
import cyber.dealer.sys.mapper.CyberUsersRemarksMapper;
import cyber.dealer.sys.mapper.EmailWalletMapper;
import cyber.dealer.sys.service.CyberUsersService;
import cyber.dealer.sys.mapper.CyberUsersMapper;
import cyber.dealer.sys.util.*;
import io.reactivex.annotations.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Keys;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static cyber.dealer.sys.constant.ReturnNo.*;
import static cyber.dealer.sys.util.Common.getRandomString;

/**
 * @author lfy
 * @description 针对表【cyber_users】的数据库操作Service实现
 * @createDate 2022-04-22 16:33:47
 */
@Service
@Slf4j
public class CyberUsersServiceImpl extends ServiceImpl<CyberUsersMapper, CyberUsers>
        implements CyberUsersService {

    private final static List list = new ArrayList<String>() {
        {
            add("connectWallet");
            add("loginGame");
            add("buyBox");
            add("durationGame");
            add("downloadGame");
        }
    };

    @Autowired
    private CyberUsersRemarksMapper cyberUsersRemarksMapper;

    @Autowired
    private CyberUsersMapper cyberUsersMapper;

    @Autowired
    private CyberAgencyMapper cyberAgencyMapper;

    @Autowired
    private EmailWalletMapper emailWalletMapper;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public ReturnObject<Object> invitation(String addr, String icode, String email, String nickname) {
//        if (!WalletUtils.isValidAddress(addr)) {
//            // 不合法直接返回错误
//            ExceptionCast.cast(AUTH_INVALID_ADDR);
//        }
        if (email.isEmpty()) {
            email = "cyberpop_email";
        }
        if (nickname.isEmpty()) {
            nickname = "cyberpop_user";
        }
        QueryWrapper<CyberAgency> queryWrapper = new QueryWrapper<>();
        int level = 0;
        CyberAgency cyberAgency = null;
        if (icode != null) {
            if (icode.length() == 8) {
                queryWrapper.eq("one_class", icode);
                level = 4;
                //用来邀请区域级
            } else if (icode.length() == 7) {
                queryWrapper.eq("two_class", icode);
                level = 3;
                //用来邀请伙伴级级
            } else if (icode.length() == 6) {
                queryWrapper.eq("three_class", icode);
                level = 2;
                //用来邀请用户级级
            } else {
                //用户无邀请码
                ExceptionCast.cast(AUTH_INVALID_CODE);
            }
            cyberAgency = cyberAgencyMapper.selectOne(queryWrapper);
            if (cyberAgency == null) {
                ExceptionCast.cast(AUTH_INVALID_CODE);
            }
        } else {
            ExceptionCast.cast(AUTH_INVALID_CODE);
        }

        //如果被邀请人为用户或者管理必须先删除在进行添加
        QueryWrapper<CyberUsers> queryWrappers = new QueryWrapper<>();
        queryWrappers.eq("address", addr);
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(queryWrappers);
//        QueryWrapper<CyberAgency> queryWrapperss = new QueryWrapper<>();
//        queryWrapperss.eq("address", addr);
//        CyberAgency cyberAgency1 = cyberAgencyMapper.selectOne(queryWrapperss);
        if (cyberUsers != null) {
            ExceptionCast.cast(AUTH_INVALID_RECODE);
        }
        //条件不成立 创建
        return createInvitation(addr, level, cyberAgency.getId(), email, nickname);
    }

    @Override
    public ReturnObject<Object> getData(String email) {
        QueryWrapper<CyberUsers> queryWrapper = new QueryWrapper<>();
        queryWrapper.select(CyberUsers.class, info -> !info.getColumn().equals("password")).eq("email", email);
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(queryWrapper);

        if (cyberUsers == null) {
            return new ReturnObject<>(AUTH_INVALID_EQOBJ, "The email has not been registered yet");
        }

        if (cyberUsers.getLevel() == 1){
            return new ReturnObject<>();
        }

        QueryWrapper<CyberAgency> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("uid", cyberUsers.getId());
        CyberAgency cyberAgency = cyberAgencyMapper.selectOne(queryWrapper1);
        if (cyberAgency == null) {
            return new ReturnObject<>(AUTH_USER_AGENCY_MISS, "Dealer data loss");
        }

        queryWrapper.clear();
        queryWrapper.select(CyberUsers.class, info -> !info.getColumn().equals("password")).eq("inv_id", cyberAgency.getId());

        // 所有下级
        List<CyberUsers> users = cyberUsersMapper.selectList(queryWrapper);

        //取出所有的备注
        List<CyberUsersRemarks> cyberUsersRemarks = cyberUsersRemarksMapper.selectList(null);
        Map<String, Object> map = new HashMap();
        List list1 = new ArrayList();
        List list2 = new ArrayList();
        List list3 = new ArrayList();
        for (CyberUsers user : users) {
            Map<String, String> convert = ObjectToMapUtil.convert(user);
            //备注赋予
            for (CyberUsersRemarks cyberUsersRemarks1 : cyberUsersRemarks) {
                if (cyberUsersRemarks1.getAddress().equals(email)) {
                    if (convert.get("email").equals(cyberUsersRemarks1.getToaddress())) {
                        convert.put("remarks", cyberUsersRemarks1.getRemarks());
                        break;
                    }
                }
            }
            convert.putIfAbsent("remarks", "cyber_user");
            convert.putAll(setRedisTo(user.getEmail()));
            if (user.getLevel() == 1) {
                list1.add(convert);
            } else if (user.getLevel() == 2) {
                list2.add(convert);
            } else {
                // （节点不能升级为顶级节点，若升级，则此处需更改）
                list3.add(convert);
            }
        }
        if (list1.size() != 0) {
            map.put("level1", list1);
        }
        if (list2.size() != 0) {
            map.put("level2", list2);
        }
        if (list3.size() != 0) {
            map.put("level3", list3);
        }
        map.put("OneClass", cyberAgency.getOneClass());
        map.put("twoClass", cyberAgency.getTwoClass());
        map.put("threeClass", cyberAgency.getThreeClass());

        return new ReturnObject<>(map);
    }

    @Override
    public ReturnObject<Object> eqAddress(String addr) {

//        if (!WalletUtils.isValidAddress(addr)) {
//            // 不合法直接返回错误
//            ExceptionCast.cast(AUTH_INVALID_ADDR);
//        }
        addr = Keys.toChecksumAddress(addr);
        QueryWrapper<CyberUsers> cyberUsers = new QueryWrapper<>();
        cyberUsers.select(CyberUsers.class, info -> !info.getColumn().equals("password")).eq("address", addr);
        CyberUsers cyberUsers1 = cyberUsersMapper.selectOne(cyberUsers);

        if (cyberUsers1 == null) {
            ExceptionCast.cast(AUTH_INVALID_ADDR);
        }
//        if (StpUtil.isDisable(cyberUsers1.getId())) {
//            return new ReturnObject<>("该账号已被封禁");
//        }
//        if (cyberUsers1.getLevel() == 1) {
//            ExceptionCast.cast(AUTH_INVALID_EQQX);
//        }

//        DetermineThereisBadge(cyberUsers1.getLevel(), addr);
        StpUtil.login(cyberUsers1.getId());
        Map map = new HashMap();
        List list1 = new ArrayList();
//        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
//        map.put(tokenInfo.getTokenName(), tokenInfo.getTokenValue());


        Map<String, String> convert = ObjectToMapUtil.convert(cyberUsers1);
        map.putAll(convert);
        list1.add(map);
        list1.add(true);
        return new ReturnObject<>(list1);
    }

    @Override
    public ReturnObject<Object> outAddress(String email) {
        QueryWrapper<CyberUsers> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(queryWrapper);
        if (cyberUsers == null) {
            ExceptionCast.cast(AUTH_INVALID_EQADMINEX);
        }
        StpUtil.logout(cyberUsers.getId());
        return new ReturnObject<>(true);
    }

    @Override
    public ReturnObject<Object> findAll(String address) {
        LambdaQueryWrapper<CyberUsers> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(CyberUsers.class, info -> !info.getColumn().equals("password")).eq(CyberUsers::getAddress, address);
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(queryWrapper);

        if (cyberUsers == null) {
            return new ReturnObject<>("无数据");
        }

        if (StpUtil.isDisable(cyberUsers.getId())) {
            ExceptionCast.cast(AUTH_INVALID_BANNED);
        }

        LambdaQueryWrapper<CyberAgency> cyberAgencyLambdaQueryWrapper = new LambdaQueryWrapper<>();
        cyberAgencyLambdaQueryWrapper.eq(CyberAgency::getId, cyberUsers.getId());
        CyberAgency cyberAgency = cyberAgencyMapper.selectOne(cyberAgencyLambdaQueryWrapper);

        queryWrapper.clear();
        queryWrapper.select(CyberUsers.class, info -> !info.getColumn().equals("password")).eq(CyberUsers::getInvId, cyberAgency.getId());
        List<CyberUsers> cyberUsers1 = cyberUsersMapper.selectList(queryWrapper);

        //把第一级的做成Map
        Map<String, String> convert = ObjectToMapUtil.convert(cyberUsers);
        Integer level = cyberUsers.getLevel();//level确定他有几级分类
        if (cyberUsers1.size() == 0) {
            return new ReturnObject<>(cyberUsers);
        }

        for (CyberUsers cyberUs : cyberUsers1) {
            if (cyberUs.getLevel() == 1) {

            }
        }
        return null;
    }

    @Override
    public Object setNikename(String nikename, String email) {
        LambdaUpdateWrapper<CyberUsers> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper
                .eq(CyberUsers::getEmail, email)
                .set(CyberUsers::getNikename, nikename)
        ;
        CyberUsers cyberUsers = new CyberUsers();
        cyberUsers.setNikename(nikename);
        return cyberUsersMapper.update(cyberUsers, queryWrapper) == 1;
    }

    @Override
    public Object getuser(CyberUsers one) {
        Map map = setRedisTo(one.getEmail());
        Map<String, String> convert = ObjectToMapUtil.convert(one);
        convert.putAll(map);
        return convert;
    }

    private Map setRedisTo(String email) {
        Map map = new HashMap();
        String key = email + "-connectWallet";
        if (redisUtils.hasKey(key)) {
            long expire = redisUtils.getExpire(key);
            long onlineTime = 24 * 60 * 60 - expire;
            map.put("connectWallet", true);
            map.put("onlineTime", onlineTime);
        } else {
            map.put("connectWallet", false);
        }
//        String hashrate = String.valueOf(get(email));
//        map.put("hashrate", hashrate);
//        long expire1 = redisUtils.getExpire(address+"-loginGame");
//        long gameTime = 24 * 60 * 60 - expire1;
//        map.put("gameTime", gameTime);
        return map;
    }

    @Override
    public ReturnObject<Object> doLoginEmail(@NonNull String email, @NonNull String password) {
        LambdaQueryWrapper<CyberUsers> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(CyberUsers.class, info -> !info.getColumn().equals("password")).eq(CyberUsers::getEmail, email).eq(CyberUsers::getPassword, password);
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(queryWrapper);
        if (null != cyberUsers){
            StpUtil.login(cyberUsers.getId());
            List list1 = new ArrayList();
            Map<String, String> convert = ObjectToMapUtil.convert(cyberUsers);
            list1.add(convert);
            list1.add(true);
            return new ReturnObject<>(list1);
        }
        return new ReturnObject<>(AUTH_EMAIL_PASSWORD_FAIL, "Incorrect username or password->"+email+":"+password);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReturnObject<Object> createInvitation(String addr, int level, Long uid, String email, String nickname) {

        //当前等级不能邀请当前等级
        CyberAgency cyberAgencys = new CyberAgency();
        CyberUsers cyberUserss = new CyberUsers();
        String inv2 = getRandomString(7);
        String inv3 = getRandomString(6);
        if (level == 4) {
            //区域
            cyberAgencys.setTwoClass(inv2);
            cyberAgencys.setThreeClass(inv3);
        } else if (level == 3) {
            //国家
            cyberAgencys.setThreeClass(inv3);
        } else if (level == 2) {
            //用户
            cyberUserss.setNikename(nickname);
            cyberUserss.setEmail(email);
            cyberUserss.setLevel(level - 1);
            cyberUserss.setInvId(uid);
            cyberUserss.setAddress(addr);
            cyberUserss.setCreateTime(new Date());
            cyberUserss.setUpdateTime(new Date());
            cyberUsersMapper.insert(cyberUserss);
            return new ReturnObject<>(cyberUserss);
        }

        cyberUserss.setEmail(email);
        cyberUserss.setNikename(nickname);
        cyberUserss.setLevel(level - 1);
        cyberUserss.setInvId(uid);
        cyberUserss.setAddress(addr);
        cyberUserss.setCreateTime(new Date());
        cyberUserss.setUpdateTime(new Date());
        cyberUsersMapper.insert(cyberUserss);

        cyberAgencys.setAddress(addr);
        cyberAgencys.setUid(cyberUserss.getId());
        cyberAgencys.setCreateTime(new Date());
        cyberAgencys.setUpdateTime(new Date());
        cyberAgencyMapper.insert(cyberAgencys);
        return new ReturnObject<>(cyberAgencys);
    }

    @Override
    public ReturnObject<Object> upGrade(String email, String address, String invCode) {

        if (Objects.equals(address, "0")) {
            address = "";
        }

        if (Objects.equals(invCode, "0")) {
            invCode = "";
        }
        address = Keys.toChecksumAddress(address);

        LambdaQueryWrapper<CyberUsers> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(CyberUsers.class, info -> !info.getColumn().equals("password")).eq(CyberUsers::getEmail, email);
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(queryWrapper);
        if (cyberUsers == null) {
            ExceptionCast.cast(AUTH_INVALID_EQOBJ);
        }

        LambdaQueryWrapper<CyberUsers> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(CyberUsers::getAddress,address);
        CyberUsers cyberUsers2 = cyberUsersMapper.selectOne(queryWrapper1);
        if (null != cyberUsers2){
            ExceptionCast.cast(AUTH_INVALID_ADDRS);
        }

        if (cyberUsers.getLevel()!=1){
            ExceptionCast.cast(AUTH_INVALID_EQLEVEL);
        }
        String inv1 = getRandomString(8);
        String inv2 = getRandomString(7);
        String inv3 = getRandomString(6);

        LambdaQueryWrapper<CyberAgency> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (invCode.length() == 7) {
            lambdaQueryWrapper.eq(CyberAgency::getTwoClass,invCode);
            CyberAgency cyberAgency = cyberAgencyMapper.selectOne(lambdaQueryWrapper);
            if (null == cyberAgency){
                ExceptionCast.cast(AUTH_INVALID_CODE);
            }
            CyberUsers cyberUsers1 = cyberUsersMapper.selectById(cyberAgency.getUid());
            if (null == cyberUsers1){
                ExceptionCast.cast(AUTH_INVALID_EQOBJ);
            }


            cyberUsers.setInvId(cyberAgency.getId());
            cyberUsers.setAddress(address);
            cyberUsers.setLevel(2);
            cyberUsers.setUpdateTime(new Date());
            cyberUsersMapper.updateById(cyberUsers);

            CyberAgency cyberAgency1 = new CyberAgency();
            cyberAgency1.setUid(cyberUsers.getId());
            cyberAgency1.setThreeClass(inv3);
            cyberAgency1.setAddress(address);
            cyberAgency1.setCreateTime(new Date());
            cyberAgency1.setUpdateTime(new Date());
            cyberAgencyMapper.insert(cyberAgency1);

        } else if (invCode.length() == 8) {
            lambdaQueryWrapper.eq(CyberAgency::getOneClass,invCode);
            CyberAgency cyberAgency = cyberAgencyMapper.selectOne(lambdaQueryWrapper);
            if (null == cyberAgency){
                ExceptionCast.cast(AUTH_INVALID_CODE);
            }
            CyberUsers cyberUsers1 = cyberUsersMapper.selectById(cyberAgency.getUid());
            if (null == cyberUsers1){
                ExceptionCast.cast(AUTH_INVALID_EQOBJ);
            }

            cyberUsers.setInvId(cyberAgency.getId());
            cyberUsers.setAddress(address);
            cyberUsers.setLevel(3);
            cyberUsers.setUpdateTime(new Date());


            CyberAgency cyberAgency1 = new CyberAgency();
            cyberAgency1.setUid(cyberUsers.getId());
            cyberAgency1.setTwoClass(inv2);
            cyberAgency1.setThreeClass(inv3);
            cyberAgency1.setAddress(address);
            cyberAgency1.setCreateTime(new Date());
            cyberAgency1.setUpdateTime(new Date());

            if (cyberUsers1.getLevel()==4){
                cyberUsers.setSubLevel(1);
                cyberAgency1.setOneClass(inv1);
            }else {
                cyberUsers.setSubLevel(cyberUsers1.getSubLevel()+1);

                if (cyberUsers1.getSubLevel()!=7){
                    cyberAgency1.setOneClass(inv1);
                }
            }
            cyberUsersMapper.updateById(cyberUsers);
            cyberAgencyMapper.insert(cyberAgency1);
        } else {
            ExceptionCast.cast(AUTH_INVALID_CODE);
        }
        return new ReturnObject<>(cyberUsers);
    }

    @Override
    public SaResult getPass() {
        boolean login = StpUtil.isLogin();
        if (login){
            String s = StpUtil.getLoginId().toString();
            CyberUsers cyberUsers = cyberUsersMapper.selectById(s);
            return SaResult.data(cyberUsers.getPassword());
        }
        return SaResult.error("未登录，请登录");
    }

    @Override
    public SaResult getAddressByEmail(String email) {
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, email));
        if (null != cyberUsers){
            return SaResult.data(cyberUsers.getAddress());
        }
        return SaResult.error("邮箱不存在");
    }

    @Override
    public SaResult emailRegister(String email, String password, String verifyCode) {
        String code = redisUtils.get("regis#"+email);
        LambdaQueryWrapper<CyberUsers> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CyberUsers::getEmail, email);
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(queryWrapper);
        if (cyberUsers != null) {
            return SaResult.error("邮箱已经注册");
        } else {
            if (null != code && code.equals(verifyCode)){

                CyberUsers cyberUsers1 = new CyberUsers();
                cyberUsers1.setEmail(email);
                cyberUsers1.setPassword(password);
                cyberUsers1.setCreateTime(new Date());
                cyberUsers1.setUpdateTime(new Date());
                cyberUsers1.setLevel(1);
                cyberUsers1.setNikename("cyberpop_user");
                cyberUsersMapper.insert(cyberUsers1);
                redisUtils.del("regis#"+email);
                return SaResult.ok();
            }else {
                return SaResult.error("验证码错误或无效，请重新获取");
            }
        }
    }

    @Override
    public SaResult getCode(String email) {
        String message;
        if (SendMailUtil.isEmail(email)) {
            ValueOperations<String, String> operations = redisTemplate.opsForValue();
            if (operations.get("regis#"+email) != null) {
                message = "验证码已发送,请"+redisTemplate.getExpire("regis#"+email)+"秒后重试";
            } else {
                StringBuilder code = SendMailUtil.CreateCode();
                try {
                    int i = SendMailUtil.EmailWithMailgun(email, String.valueOf(code));
                    if (i == 200 || i == 202) {
                        operations.set("regis#"+email, String.valueOf(code));
                        redisTemplate.expire("regis#"+email, 90, TimeUnit.SECONDS);
                        message = "验证码发送成功";
                    } else {
                        message = "验证码发送失败";
                    }
                } catch (Exception e) {
                    message = "验证码发送异常";
                }
            }
        } else {
            message = "邮箱格式不正确";
        }
        return SaResult.data(message);
    }

    @Override
    public SaResult sendVerifyCode(String email, String code) {
        String message;
        if (SendMailUtil.isEmail(email)) {
            ValueOperations<String, String> operations = redisTemplate.opsForValue();
            if (operations.get("assign#"+email) != null) {
                message = "验证码已发送,请"+redisTemplate.getExpire("assign#"+email)+"秒后重试";
            } else {
                try {
                    int i = SendMailUtil.EmailWithMailgun(email, String.valueOf(code));
                    if (i == 200 || i == 202) {
                        operations.set("assign#"+email, String.valueOf(code));
                        redisTemplate.expire("assign#"+email, 90, TimeUnit.SECONDS);
                        message = "验证码发送成功";
                    } else {
                        message = "验证码发送失败";
                    }
                } catch (Exception e) {
                    message = "验证码发送异常";
                }
            }
        } else {
            message = "邮箱格式不正确";
        }
        return SaResult.data(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult generateAddress(String email, String password, String invcode) {
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, email));
        EmailWallet emailWallet = emailWalletMapper.selectOne(new LambdaQueryWrapper<EmailWallet>().eq(EmailWallet::getEmail, email));
        if (null != cyberUsers && null != emailWallet){
            log.info("user and wallet all exists");
            return SaResult.ok(cyberUsers.getAddress());
        }
        if (null == cyberUsers && null != emailWallet){
            log.warn("only wallet");
            emailWalletMapper.deleteById(emailWallet.getEmail());
        }

        String fileName;
        String address;
        try {
            fileName = WalletUtil.generateWalletFile();
            address = Keys.toChecksumAddress(WalletUtil.getAddress(fileName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (null == cyberUsers){
            log.info("no user");

            if (password == null || password.equals("")){
                log.info("no pass");

                password = redisUtils.get("pass_"+email);
                if (password == null || password.equals("")){
                    log.info("redis no pass");
                    return SaResult.error("无密码");
                }
            }
            CyberUsers cyberUsers1 = new CyberUsers();
            if (invcode == null || invcode.equals("")){
                invcode = redisUtils.get("code_" + email);
            }
            if (invcode!=null && !invcode.equals("") && invcode.length()==6){
                CyberAgency cyberAgency = cyberAgencyMapper.selectOne(new LambdaQueryWrapper<CyberAgency>().eq(CyberAgency::getThreeClass,invcode));
                if (null !=cyberAgency){
                    CyberUsers cyberUsers2 = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getId,cyberAgency.getUid()));
                    if (cyberUsers2 == null){
                        return SaResult.error("数据库数据异常");
                    }
                    cyberUsers1.setInvId(cyberAgency.getId());
                }
            }
            try {
                cyberUsers1.setAddress(address)
                        .setLevel(1)
                        .setEmail(email)
                        .setPassword(password)
                        .setCreateTime(new Date())
                        .setUpdateTime(new Date())
                        .setNikename("cyberpop_user");

                cyberUsersMapper.insert(cyberUsers1);
            }catch (Exception e){
//                    System.out.println("operations quickly");
            }
        }else {
            cyberUsers.setAddress(address);
            try {
                cyberUsersMapper.updateById(cyberUsers);
            }catch (Exception e){

            }
        }
        EmailWallet emailWallet1 = EmailWallet.builder()
                .email(email)
                .wallet(fileName)
                .build();

        try {
            emailWalletMapper.insert(emailWallet1);
        }catch (Exception e){
//                System.out.println("operations quickly too");
        }
        return SaResult.ok(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult upgradeNoCode(String email) {
        LambdaQueryWrapper<CyberUsers> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CyberUsers::getEmail,email);
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(queryWrapper);
        if (null == cyberUsers) {
            return SaResult.error("邮箱不存在");
        }

        if (cyberUsers.getLevel() != 1){
            return SaResult.error("只能对用户升级");
        }

        cyberUsers.setLevel(2);
        cyberUsers.setUpdateTime(new Date());

        CyberAgency cyberAgency = new CyberAgency();
        cyberAgency.setUid(cyberUsers.getId());
        cyberAgency.setAddress(cyberUsers.getAddress());
        cyberAgency.setCreateTime(new Date());
        cyberAgency.setUpdateTime(new Date());
        cyberAgency.setThreeClass(getRandomString(6));

        cyberUsersMapper.updateById(cyberUsers);
        cyberAgencyMapper.insert(cyberAgency);
        return SaResult.ok();
    }

    @Override
    public SaResult getUserInfo(String email) {
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, email));
        if (null == cyberUsers){
            return SaResult.error("Email does not exist");
        }else {
            Map map = new HashMap();
            map.put("address",cyberUsers.getAddress());
            if (cyberUsers.getWeb3Wallet()==null){
                map.put("we3Wallet","");
            }
            map.put("web3Wallet",cyberUsers.getWeb3Wallet());
            map.put("personalrewards",cyberUsers.getPersonalrewards());
            return SaResult.data(map);
        }
    }

    @Override
    public SaResult doReward(String email, String amount, String date) {
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail,email));
        if (cyberUsers == null){
            return SaResult.error("Email does not exist");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 将日期时间字符串解析为LocalDate对象
        LocalDate date1 = LocalDate.parse(date, formatter);
        LocalDate date2 = LocalDate.parse(dateFormat.format(new Date()), formatter);
        if (date1.equals(date2)){
            cyberUsers.setPersonalrewards(cyberUsers.getPersonalrewards()+Double.parseDouble(amount));
            cyberUsersMapper.updateById(cyberUsers);
            log.info(email + "->doReward-" + amount);
            return SaResult.ok();
        }else {
            return SaResult.error("Date does not meet the conditions");
        }
    }

    @Override
    public Object sendCodePlus(String user) {
        String url = "https://server.aof.games:5555/api/user/send_email_code";

        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // 设置请求方法为POST
            con.setRequestMethod("POST");

            // 添加请求头
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            JSONObject requestParams = new JSONObject();
            requestParams.put("user", user);

            // 发送POST请求并写入请求体
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(requestParams.toString());
            wr.flush();
            wr.close();


            int responseCode = con.getResponseCode();
//            System.out.println("send_email_code Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return JSONObject.parseObject(response.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object create(String user,String password, String verifyCode) {
        String url = "https://server.aof.games:5555/api/user/create";
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // 设置请求方法为POST
            con.setRequestMethod("POST");

            // 添加请求头
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            JSONObject requestParams = new JSONObject();
            requestParams.put("user", user);
            requestParams.put("password", password);
            requestParams.put("verify_code", verifyCode);

            // 发送POST请求并写入请求体
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(requestParams.toString());
            wr.flush();
            wr.close();


            int responseCode = con.getResponseCode();
//            System.out.println("create Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return JSONObject.parseObject(response.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object resetPass(String user,String password, String verifyCode) {
        String url = "https://server.aof.games:5555/api/user/reset_password";
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // 设置请求方法为POST
            con.setRequestMethod("POST");

            // 添加请求头
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            JSONObject requestParams = new JSONObject();
            requestParams.put("user", user);
            requestParams.put("password", password);
            requestParams.put("verify_code", verifyCode);

            // 发送POST请求并写入请求体
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(requestParams.toString());
            wr.flush();
            wr.close();


            int responseCode = con.getResponseCode();
//            System.out.println("reset_password Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return JSONObject.parseObject(response.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public SaResult getVote(String email) {
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, email));
        if (cyberUsers!=null){
            boolean b = redisUtils.hasKey("aofpoint->" + email);
            if (b){
                int point = Integer.parseInt(redisUtils.get("aofpoint->"+email));
                return SaResult.data(point);
            }else {
                return SaResult.data(0);
            }
        }else {
            return SaResult.error("this email is not exists");
        }
    }

    @Override
    public SaResult finalBet(String teamId, String address) {
        String finalStartKey = "finalStart->";
        String finalEndKey = "finalEnd->";
        Long startTime = Long.valueOf(redisUtils.get(finalStartKey));
        Long endTime = Long.valueOf(redisUtils.get(finalEndKey));
        Long now = Instant.now().getEpochSecond();
        if (now<=startTime){
            return SaResult.error("The voting has not yet begun");
        }

        if (now>=endTime){
            return SaResult.error("The vote has been closed");
        }

//        Set<String> finTeam = (Set<String>) redisTemplate.opsForValue().get(finalTeamKey);
//        if (!finTeam.contains(teamId)){
//            return SaResult.error("Please select a valid team");
//        }
        Set<String> keys = redisTemplate.keys("fin->*");
        for (String key : keys) {
            Set<String> set = (Set<String>) redisTemplate.opsForValue().get(key);
            if (set.contains(address)){
                return SaResult.error("You have already voted");
            }
        }
//        for (String s : finTeam) {
//            if (redisUtils.hasKey("fin->"+s)){
//
//            }
//        }

        boolean b = redisUtils.hasKey("fin->" + teamId);
        if (b){
            Set<String> set = (Set<String>) redisTemplate.opsForValue().get("fin->"+teamId);
            set.add(address);
            redisUtils.set("fin->"+teamId, set);
        }else {
            Set set = new HashSet<String>();
            set.add(address);
            redisUtils.set("fin->"+teamId, set);
        }
        return SaResult.ok("Voting successful");
    }

    @Override
    public SaResult getFinalBetAmount() {
        Map map = new HashMap();
//        Set<String> finTeam = (Set<String>) redisTemplate.opsForValue().get(finalTeamKey);
//        for (String s : finTeam) {
//            if (redisUtils.hasKey("fin->"+s)){
//                Set<String> set = (Set<String>) redisTemplate.opsForValue().get("fin->"+s);
//                map.put(s, set.size());
//            }else {
//                map.put(s, 0);
//            }
//        }

        Set<String> keys = redisTemplate.keys("fin->*");
        for (String key : keys) {
            Set<String> set = (Set<String>) redisTemplate.opsForValue().get(key);
            map.put(key.substring(5), set.size());
        }

        return SaResult.data(map);
    }

    @Override
    public SaResult getFinalBetStatus(String address) {
//        Set<String> finTeam = (Set<String>) redisTemplate.opsForValue().get(finalTeamKey);
//        for (String s : finTeam) {
//            if (redisUtils.hasKey("fin->"+s)){
//                Set<String> set = (Set<String>) redisTemplate.opsForValue().get("fin->"+s);
//                if (set.contains(address)){
//                    return SaResult.get(200, "true", s);
//                }
//            }
//        }
        Set<String> keys = redisTemplate.keys("fin->*");
        for (String key : keys) {
            Set<String> set = (Set<String>) redisTemplate.opsForValue().get(key);
            if (set.contains(address)){
                return SaResult.get(200, "true", key.substring(5));
            }
        }

        return SaResult.data("Not yet voted");
    }

    @Override
    public SaResult regularBet(String roundId, String teamId, String other, String address) {
        String regStartKey = "regStart->"+roundId+"->";
        String regEndKey = "regEnd->"+roundId+"->";
        Long startTime = Long.valueOf(redisUtils.get(regStartKey));
        Long endTime = Long.valueOf(redisUtils.get(regEndKey));
        Long now = Instant.now().getEpochSecond();
        if (now<=startTime){
            return SaResult.error("The voting has not yet begun");
        }

        if (now>=endTime){
            return SaResult.error("The vote has been closed");
        }

        String key1 = "reg->roundId"+roundId+"team"+teamId;
        String key2 = "reg->roundId"+roundId+"team"+other;
        boolean b = redisUtils.hasKey(key1);
        boolean c = redisUtils.hasKey(key2);
        if (b){
            Set<String> set = (Set<String>) redisTemplate.opsForValue().get(key1);
            boolean contains = set.contains(address);
            if (contains){
                return SaResult.error("You have already voted");
            }else {
                if (c){
                    Set<String> set1 = (Set<String>) redisTemplate.opsForValue().get(key2);
                    boolean contains1 = set1.contains(address);
                    if (contains1){
                        return SaResult.error("Voting failed, you have already voted for the opposing camp");
                    }else {
                        set.add(address);
                        redisUtils.set(key1, set);
                        return SaResult.ok("Voting successful");
                    }
                }else {
                    set.add(address);
                    redisUtils.set(key1, set);
                    return SaResult.ok("Voting successful");
                }
            }
        }else {
            if (c){
                Set<String> set = (Set<String>) redisTemplate.opsForValue().get(key2);
                boolean contains = set.contains(address);
                if (contains){
                    return SaResult.error("Voting failed, you have already voted for the opposing camp");
                }else {
                    Set set1 = new HashSet<String>();
                    set1.add(address);
                    redisUtils.set(key1, set1);
                    return SaResult.ok("Voting successful");
                }
            }else {
                Set set = new HashSet<String>();
                set.add(address);
                redisUtils.set(key1, set);
                return SaResult.ok("Voting successful");
            }
        }
    }

    @Override
    public SaResult getRegBetAmount(String roundId, String teamId, String other) {
        String key1 = "reg->roundId"+roundId+"team"+teamId;
        String key2 = "reg->roundId"+roundId+"team"+other;

        boolean b = redisUtils.hasKey(key1);
        boolean c = redisUtils.hasKey(key2);
        Map map = new HashMap();
        if (b){
            Set<String> set = (Set<String>) redisTemplate.opsForValue().get(key1);
            map.put(teamId, set.size());
        }else {
            map.put(teamId, 0);
        }

        if (c){
            Set<String> set = (Set<String>) redisTemplate.opsForValue().get(key2);
            map.put(other, set.size());
        }else {
            map.put(other, 0);
        }

        return SaResult.data(map);
    }

    @Override
    public SaResult getRegBetStatus(String roundId, String teamId, String other, String address) {
        String key1 = "reg->roundId"+roundId+"team"+teamId;
        String key2 = "reg->roundId"+roundId+"team"+other;

        if (redisUtils.hasKey(key1)){
            Set<String> set = (Set<String>) redisTemplate.opsForValue().get(key1);
            if (set.contains(address)){
                return SaResult.get(200, "true", teamId);
            }
        }

        if (redisUtils.hasKey(key2)){
            Set<String> set = (Set<String>) redisTemplate.opsForValue().get(key2);
            if (set.contains(address)){
                return SaResult.get(200, "true", other);
            }
        }

        return SaResult.data("Not yet voted");
    }

    @Override
    public SaResult grantPoint(String email, String amount) {
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, email));
        if (null == cyberUsers){
            return SaResult.error("The email has not been registered yet");
        }
        int l = Integer.parseInt(amount);
        if (l<1){
            return SaResult.error("illegal parameter");
        }

        boolean b = redisUtils.hasKey("aofpoint->" + email);
        if (b){
            int point = Integer.parseInt(redisUtils.get("aofpoint->" + email));
            redisUtils.set("aofpoint->" + email, point+l);
        }else {
            redisUtils.set("aofpoint->" + email, l);
        }
        return SaResult.ok();
    }

    @Override
    public SaResult grantToken(String address, String amount) {
        Double l = Double.parseDouble(amount);
        if (l<0){
            return SaResult.error("illegal parameter");
        }

        boolean b = redisUtils.hasKey("aof->" + address.toLowerCase());
        if (b){
            Double token = Double.parseDouble(redisUtils.get("aof->" + address.toLowerCase()));
            redisUtils.set("aof->" + address.toLowerCase(), token+l);
        }else {
            redisUtils.set("aof->" + address.toLowerCase(), l);
        }
        return SaResult.ok();
    }

    @Override
    public SaResult getEmailByWeb3Wallet(String address) {
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getWeb3Wallet, address));
        if (null == cyberUsers){
            return SaResult.error("Invalid wallet address");
        }
        return SaResult.data(cyberUsers.getEmail());
    }

    @Override
    public SaResult getTokenByWallet(String address) {
        String s1 = address.toLowerCase();
        String key = "aof->" + s1;
        boolean b = redisUtils.hasKey(key);
        if (b){
            String s = redisUtils.get(key);
            return SaResult.data(Double.parseDouble(s));
        }
        return SaResult.error("Invalid wallet address");
    }

    @Override
    public Object login(String user,String password) {
        String url = "https://server.aof.games:5555/api/user/login";
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // 设置请求方法为POST
            con.setRequestMethod("POST");

            // 添加请求头
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            con.setRequestProperty("Accept", "/");
            JSONObject requestParams = new JSONObject();
            requestParams.put("user", user);
            requestParams.put("password", password);

            // 发送POST请求并写入请求体
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(requestParams.toString());
            wr.flush();
            wr.close();


            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return JSONObject.parseObject(response.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object refreshToken(HttpServletRequest request) {
        String url = "https://server.aof.games:5555/api/refresh_token";
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // 设置请求方法为POST
            con.setRequestMethod("POST");

            String lobbyToken = request.getHeader("lobbyToken");

            // 添加请求头
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            con.setRequestProperty("Accept", "/");
            con.setRequestProperty("lobbyToken", lobbyToken);
            JSONObject requestParams = new JSONObject();

            // 发送POST请求并写入请求体
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(requestParams.toString());
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
//            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return JSONObject.parseObject(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object info(HttpServletRequest request, String mail) {
        String url = "https://server.aof.games:5555/api/user/info";
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // 设置请求方法为POST
            con.setRequestMethod("POST");

            String lobbyToken = request.getHeader("lobbyToken");
            if (null == lobbyToken){
                System.out.println("no token ----------");
            }

            // 添加请求头
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            con.setRequestProperty("Accept", "/");
            con.setRequestProperty("lobbyToken", lobbyToken);
            JSONObject requestParams = new JSONObject();

            // 发送POST请求并写入请求体
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(requestParams.toString());
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
//            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            JSONObject jsonResponse = JSONObject.parseObject(response.toString());
            JSONObject jsonObject = jsonResponse.getJSONObject("data");
            System.out.println(jsonResponse.toString());
            if(jsonResponse == null) {
                Map<String, String> map = redisTemplate.opsForHash().entries("AofUser" + mail);
                if(!map.isEmpty()) return SaResult.data(map);
                else return SaResult.error( "No personal information found");
            }
            String normalData = jsonResponse.getJSONObject("data").getString("normal");
            String rareData = jsonResponse.getJSONObject("data").getString("rare");
            String sRareData = jsonResponse.getJSONObject("data").getString("srare");
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("normal", normalData);
            responseMap.put("rare", rareData);
            responseMap.put("srare", sRareData);
            redisTemplate.opsForHash().putAll("AofUser" + mail, responseMap);
            return JSONObject.parseObject(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object viewUser(int playerId, String email) {
        if(playerId == 1 && email != null) {
            String url = "https://server.aof.games:5555/api/user/view_user";
            try {
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                // 设置请求方法为POST
                con.setRequestMethod("POST");

                // 添加请求头
                con.setRequestProperty("User-Agent", "Mozilla/5.0");
                con.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                con.setRequestProperty("Accept", "/");
                JSONObject requestParams = new JSONObject();
                requestParams.put("playerId", playerId);
                requestParams.put("email", email);

                // 发送POST请求并写入请求体
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(requestParams.toString());
                wr.flush();
                wr.close();

                int responseCode = con.getResponseCode();
//                System.out.println("Response Code : " + responseCode);

                //读取响应头
                Map<String, List<String>> headers = con.getHeaderFields();
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();
                    System.out.println(key + ": " + values);
                }

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "UTF-8"));

                String inputLine;
                StringBuffer responseBody = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    responseBody.append(inputLine);
                }
                in.close();

                System.out.println("Response Body : " + responseBody.toString());
                return JSONObject.parseObject(responseBody.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }else if(email == null) {
            String url = "https://server.aof.games:5555/api/user/view_user";
            try {
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                // 设置请求方法为POST
                con.setRequestMethod("POST");

                // 添加请求头
                con.setRequestProperty("User-Agent", "Mozilla/5.0");
                con.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                con.setRequestProperty("Accept", "/");
                JSONObject requestParams = new JSONObject();
                requestParams.put("playerId", playerId);

                // 发送POST请求并写入请求体
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(requestParams.toString());
                wr.flush();
                wr.close();

                int responseCode = con.getResponseCode();
//                System.out.println("Response Code : " + responseCode);

                //读取响应头
                Map<String, List<String>> headers = con.getHeaderFields();
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();
                    System.out.println(key + ": " + values);
                }

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "UTF-8"));

                String inputLine;
                StringBuffer responseBody = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    responseBody.append(inputLine);
                }
                in.close();

                System.out.println("Response Body : " + responseBody.toString());
                return JSONObject.parseObject(responseBody.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        return null;
    }

    @Override
    public Object warInfo() {
        String url = "https://server.aof.games:5555/api/war/info";
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // 设置请求方法为POST
            con.setRequestMethod("POST");

            // 添加请求头
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            con.setRequestProperty("Accept", "/");
            JSONObject requestParams = new JSONObject();

            // 发送POST请求并写入请求体
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(requestParams.toString());
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "UTF-8"));

            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return JSONObject.parseObject(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object troopInfo(int troopId) {
        String url = "https://server.aof.games:5555/api/troop/info";
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // 设置请求方法为POST
            con.setRequestMethod("POST");

            // 添加请求头
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            con.setRequestProperty("Accept", "/");
            JSONObject requestParams = new JSONObject();
            requestParams.put("troopId", troopId);

            // 发送POST请求并写入请求体
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(requestParams.toString());
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "UTF-8"));

            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return JSONObject.parseObject(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object searchInfo() {
        Object responseObj = warInfo();
        JSONObject responseJson = (JSONObject) responseObj;
        JSONArray troopListArray = responseJson.getJSONObject("data").getJSONArray("troopList");
        if(troopListArray == null) {
            SaResult.error( "The information obtained is incomplete");
        }

        int[] troopList = new int[troopListArray.size()];
        for (int i = 0; i < troopListArray.size(); i++) {
            troopList[i] = troopListArray.getInteger(i);
        }

        List<Object[]> membersList = new ArrayList<>();
        List<Object> troopInfoObjs = new ArrayList<>();
        for (int troopId : troopList) {
            Object troopInfoObj = troopInfo(troopId);
            troopInfoObjs.add(troopInfoObj);
            JSONObject troopInfoJson = (JSONObject) troopInfoObj;

            // 提取 member 字段并添加到列表中
            JSONArray memberArray = troopInfoJson.getJSONObject("data").getJSONArray("members");
            Object[] members = memberArray.toArray();
            membersList.add(members);
        }
        Object[][] membersArray = membersList.toArray(new Object[0][]);

        List<Integer> playerIdsList = new ArrayList<>();
        for (Object[] member : membersArray) {
            for (Object obj : member) {
                if (obj instanceof JSONObject) {
                    int playerId = ((JSONObject) obj).getIntValue("playerId");
                    playerIdsList.add(playerId);
                }
            }
        }

        List<searchInfoVo> nameidList = new ArrayList<>();
        boolean allNamesCached = true;
        for (int playerId : playerIdsList) {
            String cachedName = redisUtils.get("AofUser" + playerId);
            if(cachedName != null) {
                nameidList.add(new searchInfoVo(playerId, cachedName));
            }else {
                allNamesCached = false;
                break;
            }
        }

        if(allNamesCached) {
            Map<String, Object> map = new HashMap<>();
            if(responseObj != null) map.put("Current race information", responseObj);
            if(troopInfoObjs != null) map.put("Team Information", troopInfoObjs);
            if(nameidList != null) map.put("player name", nameidList);
            if(map.size() != 0) return map;
            return SaResult.error("The information obtained is incomplete");
        }

        nameidList.clear();
        for (int playerId : playerIdsList) {
            String cachedName  = redisUtils.get("AofUser" + playerId);
            if(cachedName == null) {
                Object userObj = viewUser(playerId, null);
                JSONObject userJson = (JSONObject) userObj;
                String name = userJson.getJSONObject("data").getString("name");
                System.out.println(userJson.getJSONObject("data").toString() + "----1169");
                redisUtils.set("AofUser" + playerId, name);
                nameidList.add(new searchInfoVo(playerId, name));
            }else {
                nameidList.add(new searchInfoVo(playerId, cachedName));
            }
        }

        Map<String, Object> map = new HashMap<>();
        if(responseObj != null) map.put("Current race information", responseObj);
        if(troopInfoObjs != null) map.put("Team Information", troopInfoObjs);
        if(nameidList != null) map.put("player name", nameidList);
        if(map.size() != 0) return map;
        return SaResult.error( "The information obtained is incomplete");
    }
}