package cyber.dealer.sys.tack;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cyber.dealer.sys.domain.CyberUsers;
import cyber.dealer.sys.domain.vo.GeneralFormatVo;
import cyber.dealer.sys.mapper.CyberUsersMapper;
import cyber.dealer.sys.service.CyberUsersService;
import cyber.dealer.sys.util.HttpURLConnectionUtil;
import cyber.dealer.sys.util.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;

/**
 * @author lfy
 * @Date 2022/4/25 15:17
 */
@Configuration
@EnableScheduling
public class BalanceTake {

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
    private RedisTemplate redisTemplate;

    @Autowired
    private CyberUsersMapper cyberUsersMapper;

    @Autowired
    private CyberUsersService userService;

    @Value("${myParam}")
    private boolean myParam;

    //    @Scheduled(fixedRate = 1000 * 60 * 60, initialDelay = 1000)
    public void synchronousCoin() {
        List<CyberUsers> list = userService.lambdaQuery().list();
        System.out.println(list);
        for (int i = 0; i < list.size(); i++) {
            String mumbaiUrl = "https://testquery.cyberpop.online/tokensOfOwner?chainId=43113&contractAddress=0x78F66E37e9fE077d2F0126E3a26e6FB0D14F2BB0&accounts=" + list.get(i).getAddress();
            String fujiUrl = "https://testquery.cyberpop.online/tokensOfOwner?chainId=80001&contractAddress=0x37e769d34Cb48fb074fDd181bB4d803fBD49C712&accounts=" + list.get(i).getAddress();
            String mumbaiQ = HttpURLConnectionUtil.doGet(mumbaiUrl);
            String fujiQ = HttpURLConnectionUtil.doGet(fujiUrl);
            Map mumbaimap = JSON.parseObject(mumbaiQ, Map.class);
            Map fujimap = JSON.parseObject(fujiQ, Map.class);
            List mumbaidata = (List) mumbaimap.get("data");
            List fujidata = (List) fujimap.get("data");
//            userService.saveCoin(mumbaidata.size(), fujidata.size(), String.valueOf(list.get(i).getAddr()));
            userService.lambdaUpdate()
                    .set(CyberUsers::getFujiCoin, fujidata.size())
                    .set(CyberUsers::getMubaiCoin, mumbaidata.size())
                    .eq(CyberUsers::getAddress, list.get(i).getAddress()).update();
        }


    }

//    @Scheduled(fixedRate = 1000 * 60)
    public void getCalculateTotalForce() {
        QueryWrapper<CyberUsers> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("email");

        // 确保数据库数据都有邮箱，不然会有异常
        List<CyberUsers> list = cyberUsersMapper.selectList(queryWrapper);
        int calculate = 0;
        for (CyberUsers cyberUsers : list) {
            if (redisUtils.hasKey(cyberUsers.getEmail() + "-connectWallet")){
                calculate +=2;
            }
        }
        redisUtils.set("aof总算力", calculate);
    }

    @Scheduled(fixedRate = 1000 * 60)
    public void addDiamond() {
        if (myParam){
            List<String> values = redisTemplate.opsForValue().multiGet(redisTemplate.keys("*-connectWallet*"));
            if (values!=null && values.size()>0){
                for (String value : values) {
                    GeneralFormatVo generalFormatVo = JSONObject.parseObject(value, GeneralFormatVo.class);
                    try {
                        QueryWrapper<CyberUsers> queryWrapper1 = new QueryWrapper<>();
                        queryWrapper1.eq("email", generalFormatVo.getEmail());
                        CyberUsers cyberUsers = cyberUsersMapper.selectOne(queryWrapper1);
                        queryWrapper1.clear();
                        if (cyberUsers == null) {
                            return;
                        }
                        double v = Double.parseDouble(generalFormatVo.getParameter1());
                        cyberUsers.setPersonalrewards(cyberUsers.getPersonalrewards()+v);
                        cyberUsersMapper.updateById(cyberUsers);
                    }catch (Exception e){
                        System.out.println(generalFormatVo.getEmail()+"->problem data");
                    }
                }
            }
        }

    }
}
