package cyber.dealer.sys.controller;

import cn.dev33.satoken.util.SaResult;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cyber.dealer.sys.domain.CyberAgency;
import cyber.dealer.sys.domain.CyberUsers;
import cyber.dealer.sys.domain.CyberUsersRecord;
import cyber.dealer.sys.domain.vo.GeneralFormatVo;
import cyber.dealer.sys.mapper.CyberAgencyMapper;
import cyber.dealer.sys.mapper.CyberUsersMapper;
import cyber.dealer.sys.mapper.CyberUsersRecordMapper;
import cyber.dealer.sys.util.ContractCallUtil;
import cyber.dealer.sys.util.RedisUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.math.BigInteger;
import java.util.*;

/**
 * @author lfy
 * @Date 2022/4/25 11:59
 */
@RestController
@RequestMapping("connection")
@CrossOrigin
@Api(tags = "埋点的Controller")
@ApiIgnore
public class CalculateTheForceController {

    private final static List list = new ArrayList<String>() {
        {
            add("connectWallet");
            add("loginGame");
            add("buyBox");
            add("durationGame");
            add("downloadGame");
        }
    };
    private final static Map<String, Integer> map = new HashMap() {
        {
            put("connectWallet", 2);
            put("loginGame", 1);
            put("buyBox", 1);
            put("durationGame", 1);
            put("downloadGame", 2);
        }
    };

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private CyberUsersRecordMapper cyberUsersRecordMapper;

    @Autowired
    private CyberUsersMapper cyberUsersMapper;

    @Autowired
    private CyberAgencyMapper cyberAgencyMapper;

    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 埋点
     * @param generalFormatVo
     * @return
     */
    @PostMapping("general")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "action", value = "动作名称", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "email", value = "邮箱", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "time", value = "时间戳", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "parameter1", value = "", required = true, dataType = "String",defaultValue = ""),
            @ApiImplicitParam(name = "parameter2", value = "自定义参数", required = true, dataType = "String",defaultValue = ""),
            @ApiImplicitParam(name = "parameter3", value = "自定义参数", required = true, dataType = "String",defaultValue = ""),
    })
    @ApiOperation(value = "")
    public SaResult general(@RequestBody GeneralFormatVo generalFormatVo) {

        String url = "https://bitter-morning-feather.bsc.quiknode.pro/fceebe9e2581d5014f281b317cb43643e8f46222/";
        String contractAddress = "0x55d398326f99059fF775485246999027B3197955";

        CyberUsers cyberUsers = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, generalFormatVo.getEmail()));
        if (null == cyberUsers){
            return SaResult.error("email不存在");
        }

        if (cyberUsers.getWeb3Wallet()==null || cyberUsers.getWeb3Wallet().equals("")){
            return SaResult.error("Unbound wallet");
        }

        if (generalFormatVo.getAction().equals("connectWallet")){

            // 获取代币精度
            String connectWalletDecimals;
            boolean isDecimal= redisUtils.hasKey("connect_wallet_decimals");
            if (isDecimal){
                connectWalletDecimals = redisUtils.get("connect_wallet_decimals");
            }else {
                BigInteger erc20Decimals = ContractCallUtil.getErc20Decimals(url, contractAddress);
                connectWalletDecimals = erc20Decimals.toString();
                redisUtils.set("connect_wallet_decimals",connectWalletDecimals);
            }
            // 获取余额
            SaResult balance = ContractCallUtil.getBalance(url, contractAddress, cyberUsers.getWeb3Wallet());
            BigInteger data = (BigInteger) balance.getData();
            BigInteger ultimateBalance = data.divide(BigInteger.TEN.pow(Integer.parseInt(connectWalletDecimals)));
            int i = ultimateBalance.intValue();
            if (i<10){
                return new SaResult(500, "Insufficient Balance", i);
            }

            double v = 1.0 / 24 / 60;
            if (cyberUsers.getLevel()!=1){
                CyberAgency cyberAgency = cyberAgencyMapper.selectOne(new LambdaQueryWrapper<CyberAgency>().eq(CyberAgency::getUid,cyberUsers.getId()));
                if (cyberAgency!=null){
                    List<CyberUsers> list1 = cyberUsersMapper.selectList(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getInvId, cyberAgency.getId()));
                    if (list1!=null && list1.size()>0){
                        int size = 0;
                        for (CyberUsers users : list1) {
                            if (redisUtils.hasKey(users.getEmail()+"-connectWallet")){
                                size++;
                            }
                        }
                        if (size!=0){
                            double v1 = v*(size+1);
                            generalFormatVo.setParameter1(String.valueOf(v1));
                        }else {
                            generalFormatVo.setParameter1(String.valueOf(v));
                        }
                    }else {
                        generalFormatVo.setParameter1(String.valueOf(v));
                    }
                }else {
                    return SaResult.error("agency info missing");
                }
            }else {
                generalFormatVo.setParameter1(String.valueOf(v));
            }

            redisUtils.set(generalFormatVo.getEmail() + "-connectWallet", JSONObject.toJSONString(generalFormatVo), 24 * 60 * 60);


            if (cyberUsers.getInvId()!=null && !cyberUsers.getInvId().equals("")){
                CyberAgency cyberAgency = cyberAgencyMapper.selectOne(new LambdaQueryWrapper<CyberAgency>().eq(CyberAgency::getId,cyberUsers.getInvId()));
                if (cyberAgency!=null){
                    CyberUsers cyberUsers1 = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getId,cyberAgency.getUid()));
                    if (cyberUsers1!=null){
                        if (redisUtils.hasKey(cyberUsers1.getEmail()+"-connectWallet")){
                            String s = redisUtils.get(cyberUsers1.getEmail() + "-connectWallet");
                            GeneralFormatVo generalFormatVo1 = JSONObject.parseObject(s, GeneralFormatVo.class);
                            List<CyberUsers> list1 = cyberUsersMapper.selectList(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getInvId, cyberAgency.getId()));
                            int size = 0;
                            for (CyberUsers users : list1) {
                                if (redisUtils.hasKey(users.getEmail()+"-connectWallet")){
                                    size++;
                                }
                            }
                            double v1 = v*(size+1);
                            generalFormatVo1.setParameter1(String.valueOf(v1));
                            long expire = redisUtils.getExpire(cyberUsers1.getEmail() + "-connectWallet");
                            LambdaQueryWrapper<CyberUsersRecord> queryWrapper = new LambdaQueryWrapper<>();
                            queryWrapper.eq(CyberUsersRecord::getEmail,cyberUsers1.getEmail()).eq(CyberUsersRecord::getAction,"connectWallet");
                            CyberUsersRecord cyberUsersRecord = cyberUsersRecordMapper.selectOne(queryWrapper);
                            cyberUsersRecord.setParameter1(String.valueOf(v1));
                            cyberUsersRecord.setTime(new Date());
                            if (expire>0){
                                redisUtils.set(cyberUsers1.getEmail() + "-connectWallet",JSONObject.toJSONString(generalFormatVo1),expire);
                                cyberUsersRecordMapper.updateById(cyberUsersRecord);
                            }
                        }
                    }
                }
            }

            LambdaQueryWrapper<CyberUsersRecord> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CyberUsersRecord::getAction,generalFormatVo.getAction()).eq(CyberUsersRecord::getEmail,generalFormatVo.getEmail());
            CyberUsersRecord cyberUsersRecord = cyberUsersRecordMapper.selectOne(queryWrapper);
            if (null != cyberUsersRecord){
                cyberUsersRecord.setTime(new Date());
                cyberUsersRecord.setParameter1(generalFormatVo.getParameter1());
                cyberUsersRecord.setParameter2(generalFormatVo.getParameter2());
                cyberUsersRecord.setParameter3(generalFormatVo.getParameter3());
                cyberUsersRecordMapper.updateById(cyberUsersRecord);

            }else {
                if (cyberUsers.getInvId()!=null && !cyberUsers.getInvId().equals("")){
                    CyberAgency cyberAgency = cyberAgencyMapper.selectOne(new LambdaQueryWrapper<CyberAgency>().eq(CyberAgency::getId,cyberUsers.getInvId()));
                    if (cyberAgency!=null){
                        CyberUsers cyberUsers1 = cyberUsersMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getId,cyberAgency.getUid()));
                        if (cyberUsers1==null){
                            return SaResult.error("No user identity corresponding to the dealer was found");
                        }
                        cyberUsers1.setPersonalrewards(cyberUsers1.getPersonalrewards()+2.5d);
                        cyberUsers.setPersonalrewards(cyberUsers.getPersonalrewards()+2.5d);
                        cyberUsersMapper.updateById(cyberUsers1);
                        cyberUsersMapper.updateById(cyberUsers);
                    }
                }

                cyberUsersRecord = new CyberUsersRecord();
                cyberUsersRecord.setAction(generalFormatVo.getAction());
                cyberUsersRecord.setEmail(generalFormatVo.getEmail());
                cyberUsersRecord.setTime(new Date());
                cyberUsersRecord.setParameter1(generalFormatVo.getParameter1());
                cyberUsersRecord.setParameter2(generalFormatVo.getParameter2());
                cyberUsersRecord.setParameter3(generalFormatVo.getParameter3());
                cyberUsersRecordMapper.insert(cyberUsersRecord);
            }
            return SaResult.ok();
        }
        return SaResult.error();
    }

    /**
     * 计算总算力
     * @return
     */
    @GetMapping("calculateTotalForce")
    @ApiOperation(value ="查询总算力",notes = "")
    public Object calculateTotalForce() {
        Map<Object, Object> map = new HashMap();
//        Double calculate = Double.parseDouble(redisUtils.get("aof总算力"));
//        map.put("calculate", calculate);
//        if (calculate == 0.0){
//            return map;
//        }
        map.put("personal calculate", 1.0 / 24 / 60);
        return map;
    }
}
