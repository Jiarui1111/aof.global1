package cyber.dealer.sys.controller;

import cn.dev33.satoken.util.SaResult;
import cyber.dealer.sys.annotation.IpWhitelist;
import cyber.dealer.sys.domain.CyberSysChainlist;
import cyber.dealer.sys.service.ContractInteractService;
import cyber.dealer.sys.util.ContractCallUtil;
import cyber.dealer.sys.util.RedisUtils;
import cyber.dealer.sys.util.WalletUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;


/**
 * Author hw
 * Date 2023/3/30 14:17
 */
@RestController
@RequestMapping("/contractInteractive")
@Api(tags = "合约交互controller")
@CrossOrigin
public class ContractInteractController {

    @Autowired
    private ContractInteractService contractInteractiveService;

    @Autowired
    private RedisUtils redisUtils;

    @PostMapping("/erc20Transfer")
    @IpWhitelist
    @ApiOperation(value = "erc20代币转账", notes = "此接口仅适用通过generateAddress接口注册的用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "chainId", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "contractAddress", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "email", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "to", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "amount", value = "", dataType = "String", paramType = "query"),
    })
    public SaResult erc20Transfer(String chainId, String contractAddress, String email, String to, String amount){
        return contractInteractiveService.erc20Transfer(chainId, contractAddress, email, to, amount);
    }

    @PostMapping("/transferThenMint")
    @IpWhitelist
    @ApiOperation(value = "扣除usdt并mint")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "chainId", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "contractAddress", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "NFTAddress", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "email", value = "", dataType = "String", paramType = "query"),
    })
    public SaResult transferThenMint(String chainId, String contractAddress, String email, String NFTAddress){
        try {
            return contractInteractiveService.transferThenMint(chainId, contractAddress, email, NFTAddress);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/setUSDTAmount")
    @IpWhitelist
    @ApiOperation(value = "设置usdt转账金额")
    public SaResult setUSDTAmount(String amount){
        redisUtils.set("usdt->amount", amount);
        return SaResult.ok();
    }

    @PostMapping("/erc721Transfer")
    @IpWhitelist
    @ApiOperation(value = "erc721代币转账", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "chainId", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "contractAddress", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "email", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "to", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "tokenId", value = "", dataType = "String", paramType = "query"),
    })
    public SaResult erc721Transfer(String chainId, String contractAddress, String email, String to, String tokenId){
        return contractInteractiveService.erc721Transfer(chainId, contractAddress, email, to, tokenId);
    }

    @PostMapping("/erc1155Transfer")
    @IpWhitelist
    @ApiOperation(value = "erc1155代币转账", notes = "此接口仅适用通过generateAddress接口注册的用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "chainId", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "contractAddress", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "email", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "to", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "tokenId", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "amount", value = "", dataType = "String", paramType = "query"),
    })
    public SaResult erc1155Transfer(String chainId, String contractAddress, String email, String to, String tokenId, String amount){
        return contractInteractiveService.erc1155Transfer(chainId, contractAddress, email, to, tokenId, amount);
    }

    @PostMapping("/erc721Mint")
    @ApiOperation(value = "erc721Mint", notes = "白名单直接调用")
    @IpWhitelist
    @ApiImplicitParams({
            @ApiImplicitParam(name = "chainId", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "contractAddress", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "to", value = "", dataType = "String", paramType = "query"),
    })
    public SaResult erc721Mint(String chainId, String contractAddress, String to){
        return contractInteractiveService.erc721Mint(chainId, contractAddress, to);
    }

    @PostMapping("/erc1155Mint")
    @IpWhitelist
    @ApiOperation(value = "erc1155Mint")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "chainId", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "contractAddress", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "to", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "tokenId", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "amount", value = "", dataType = "String", paramType = "query"),
    })
    public SaResult erc1155Mint(String chainId, String contractAddress, String to, String tokenId, String amount){
        return contractInteractiveService.erc1155Mint(chainId, contractAddress, to, tokenId, amount);
    }

    @PostMapping("/erc1155Up")
    @IpWhitelist
    @ApiOperation(value = "NFT升级")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "chainId", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "contractAddress", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "email", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "tokenId", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "amount", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "newTokenId", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "newAmount", value = "", dataType = "String", paramType = "query"),
    })
    public SaResult erc1155Up(String chainId, String contractAddress, String email, String tokenId, String amount, String newTokenId, String newAmount){
        SaResult saResult = contractInteractiveService.erc1155Burn(chainId, contractAddress, email, tokenId, amount);
        String from = (String) saResult.getData();
        if (saResult.getCode()==200){
            Map map = new HashMap();
            map.put("burnHash",saResult.getData());
            SaResult saResult1 = contractInteractiveService.erc1155Mint(chainId, contractAddress, from, newTokenId, newAmount);
            map.put("mintHash",saResult1.getData());
            return SaResult.data(map);
        }
        return saResult;
    }

    @PostMapping("/vote")
    @ApiOperation(value = "投票")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "team", value = "队伍", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "qty", value = "数量", dataType = "String", paramType = "query"),
    })
    public SaResult vote(String email, String team, String qty){
        return contractInteractiveService.vote(email, team, qty);
    }

    @PostMapping("/getFaucet")
    @ApiOperation(value = "领水")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包地址", dataType = "String", paramType = "query"),
    })
    public SaResult getFaucet(String address){
        return contractInteractiveService.getFaucet(address);
    }

    @PostMapping("/setMintRole")
    @IpWhitelist
    public SaResult setMintRole(String privateKey){
        try {
            return WalletUtil.setUniqueAddress(privateKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/getMintRole")
    public SaResult getMintRole(){
        return SaResult.data(WalletUtil.getUniqueAddress().getAddress());
    }

    @GetMapping("/balanceOfAll")
    @ApiOperation(value = "查询erc721资产")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "url", value = "rpc", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "contractAddress", value = "合约地址", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "address", value = "钱包地址", dataType = "String", paramType = "query"),
    })
    @IpWhitelist
    public SaResult balanceOfAll(String url, String contractAddress, String address){
        SaResult balance = ContractCallUtil.getBalance(url, contractAddress, address);
        BigInteger data = (BigInteger) balance.getData();
        return SaResult.data(data);
    }

    @GetMapping("/publicMint")
    @ApiOperation(value = "头像合约publicMint方法")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "chainId", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "rpc", value = "", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "contractAddress", value = "合约地址", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "caller", value = "邮箱", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "amount", value = "数量", dataType = "String", paramType = "query"),
    })
    @IpWhitelist
    public SaResult publicMint(String chainId, String rpc, String contractAddress, String caller, String amount){
        return contractInteractiveService.publicMint(chainId, rpc, contractAddress, caller, amount);
    }
}
