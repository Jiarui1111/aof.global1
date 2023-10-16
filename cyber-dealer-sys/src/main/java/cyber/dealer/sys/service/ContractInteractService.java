package cyber.dealer.sys.service;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.extension.service.IService;
import cyber.dealer.sys.domain.CyberUsers;

import java.io.IOException;

/**
 * Author hw
 * Date 2023/3/30 14:07
 */
public interface ContractInteractService extends IService<CyberUsers> {

    SaResult erc20Transfer(String chainId, String contractAddress, String email, String to, String amount);

    SaResult transferThenMint(String chainId, String contractAddress, String email, String NFTAddress) throws IOException, InterruptedException;

    SaResult erc721Transfer(String chainId, String contractAddress, String email, String to, String tokenId);

    SaResult erc1155Transfer(String chainId, String contractAddress, String email, String to, String tokenId, String amount);

    SaResult erc721Mint(String chainId, String contractAddress, String to);

    SaResult erc1155Mint(String chainId, String contractAddress, String to, String tokenId, String amount);

    SaResult erc1155Burn(String chainId, String contractAddress, String email, String tokenId, String amount);

    SaResult vote(String email, String team, String qty);

    SaResult getFaucet(String address);

    SaResult publicMint(String chainId, String rpc, String contractAddress, String caller, String amount);
}