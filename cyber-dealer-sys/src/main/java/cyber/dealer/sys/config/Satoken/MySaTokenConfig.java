package cyber.dealer.sys.config.Satoken;

import cn.dev33.satoken.interceptor.SaRouteInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class MySaTokenConfig implements WebMvcConfigurer {
    // 注册sa-token的登录拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册登录拦截器，并排除登录接口地址
        registry.addInterceptor(new SaRouteInterceptor())
                .excludePathPatterns(
                        "/user/doLogin"
                        , "/user/bemail"
                        , "/user/baddress"
                        , "/user/download"
                        , "/user/isLogin"
                        , "/user/getCode"
                        , "/user/emailRegister"
                        , "/user/doLoginEmail"
                        , "/user/sendVerificationCode"
                        , "/user/cache"
                        , "/user/updatePassword"
                        , "/user/getUserInfo"
                        , "/user/doReward"
                        , "/user/generateAddress"
                        , "/user/bindingAddress"
                        , "/user/getAddress"
                        , "/user/getBindAddress"
                        , "/user/sendCodePlus"
                        , "/user/create"
                        , "/user/addBlackList"
                        , "/user/resetPass"
                        , "/user/grantPoint"
                        , "/user/grantToken"
                        , "/user/getEmailByWeb3Wallet"
                        , "/user/getTokenByWallet"
                        , "/user/getFinalBetAmount"
                        , "/user/getRegBetAmount"
                        , "/user/checkWhiteList"
                        , "/user/addWhiteListEmail"
                        , "/user/getCurrentRound"
                        , "/user/searchInfo"
                        , "/user/login"
                        , "/user/query"
                        , "/user/queryGet"
                        , "/user/getWallet"
                        , "/contractInteractive/vote"
                        , "/contractInteractive/setMintRole"
                        , "/contractInteractive/erc1155Mint"
                        , "/contractInteractive/getMintRole"
                        , "/contractInteractive/erc721Mint"
                        , "/contractInteractive/erc1155Up"
                        , "/contractInteractive/erc1155Transfer"
                        , "/contractInteractive/erc721Transfer"
                        , "/contractInteractive/setUSDTAmount"
                        , "/contractInteractive/erc20Transfer"
                        , "/contractInteractive/transferThenMint"
                        , "/contractInteractive/balanceOfAll"
                        , "/contractInteractive/publicMint"
                        , "/level/invitation"
                        , "/business/nationallevel"
                        , "/business/deluserlevel"
                        , "/business/arealevel"
                        , "/business/partnerlevel"
                        , "/business/userlevel"
                        , "/business/subcommission"
                        , "/dealerHxLog/getAllLog"
                        , "/dealerHxLog/getDetail"
                        , "/dealerHxLog/getProxyByHx"
                        , "/re/setremarks"
                        , "/business/invuser"
                        , "/separate/dologin"
                        , "/level/eqaddr"
                        , "/connection/calculateTotalForce"
                        , "/swagger-ui.html/**"
                        , "/swagger-resources/**"
                        , "/webjars/**"
                        , "/v2/**"
                        , "/csrf"
                        , "/"
                )
                .addPathPatterns("/**");
    }
}
