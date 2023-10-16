package cyber.dealer.sys.service;

import cn.dev33.satoken.util.SaResult;
import cyber.dealer.sys.constant.ReturnObject;
import cyber.dealer.sys.domain.CyberUsers;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;

/**
 * @author lfy
 * @description 针对表【cyber_users】的数据库操作Service
 * @createDate 2022-04-22 16:33:47
 */
public interface CyberUsersService extends IService<CyberUsers> {

    ReturnObject<Object> invitation(String addr, String icode,String email, String nickname);

    ReturnObject<Object> getData(String addr);

    ReturnObject<Object> eqAddress(String addr);

    ReturnObject<Object> outAddress(String address);

    ReturnObject<Object> findAll(String address);

    Object setNikename(String nikename, String address);

    Object getuser(CyberUsers one);

    ReturnObject<Object> doLoginEmail(String email, String password);

    SaResult emailRegister(String email, String password, String verifyCode);

    ReturnObject<Object> upGrade(String email, String address, String invCode);

    SaResult getPass();

    SaResult getAddressByEmail(String email);

    SaResult getCode (String email);

    SaResult sendVerifyCode(String receiver, String code);

    SaResult generateAddress(String email, String password, String invcode);

    SaResult upgradeNoCode(String email);

    SaResult getUserInfo(String email);

    SaResult doReward(String email, String amount, String date);

    Object sendCodePlus(String email);

    Object create(String user, String password, String verifyCode);

    Object resetPass(String user, String password, String verifyCode);

    SaResult getVote(String email);

    SaResult finalBet(String teamId, String address);

    SaResult getFinalBetAmount();

    SaResult getFinalBetStatus(String address);

    SaResult regularBet(String roundId, String teamId, String other, String address);

    SaResult getRegBetAmount(String roundId, String teamId, String other);

    SaResult getRegBetStatus(String roundId, String teamId, String other, String address);

    SaResult grantPoint(String email, String amount);

    SaResult grantToken(String address, String amount);

    SaResult getEmailByWeb3Wallet(String address);

    SaResult getTokenByWallet(String address);

    Object login(String user, String password);

    Object refreshToken(HttpServletRequest request);

    Object info(HttpServletRequest request, String mail);

    Object viewUser(int playerId, String email);

    Object warInfo();

    Object troopInfo(int troopId);

    Object searchInfo();
}
