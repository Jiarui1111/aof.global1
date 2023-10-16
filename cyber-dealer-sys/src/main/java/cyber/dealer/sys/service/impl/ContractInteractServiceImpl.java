package cyber.dealer.sys.service.impl;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cyber.dealer.sys.domain.CyberSysChainlist;
import cyber.dealer.sys.domain.CyberSystem;
import cyber.dealer.sys.domain.CyberUsers;
import cyber.dealer.sys.domain.EmailWallet;
import cyber.dealer.sys.mapper.CyberSysChainlistMapper;
import cyber.dealer.sys.mapper.CyberSystemMapper;
import cyber.dealer.sys.mapper.CyberUsersMapper;
import cyber.dealer.sys.mapper.EmailWalletMapper;
import cyber.dealer.sys.service.ContractInteractService;
import cyber.dealer.sys.util.ContractCallUtil;
import cyber.dealer.sys.util.RedisUtils;
import cyber.dealer.sys.util.WalletUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;


/**
 * Author hw
 * Date 2023/3/30 14:10
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ContractInteractServiceImpl extends ServiceImpl<CyberUsersMapper, CyberUsers> implements ContractInteractService {

    @Autowired
    private CyberUsersMapper userMapper;

    @Autowired
    private EmailWalletMapper emailWalletMapper;

    @Autowired
    private CyberSysChainlistMapper cyberSysChainlistMapper;

    @Autowired
    private CyberSystemMapper cyberSystemMapper;

    @Autowired
    private RedisUtils redisUtils;

    @Override
    public SaResult erc20Transfer(String chainId, String contractAddress, String email, String to, String amount) {
        CyberSysChainlist cyberSysChainlist = cyberSysChainlistMapper.selectById(chainId);
        if (null == cyberSysChainlist){
            return SaResult.error("this chainId is not exists");
        }

        CyberUsers cyberUsers = userMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail,email));
        if (null == cyberUsers){
            return SaResult.error("this email is not exists");
        }

        if (null == cyberUsers.getAddress() || "".equals(cyberUsers.getAddress())){
            return SaResult.error("no address");
        }

        EmailWallet emailWallet = emailWalletMapper.selectById(email);
        String secretKey = WalletUtil.getSecretKey(emailWallet.getWallet());
        try {
            return ContractCallUtil.erc20Transfer(chainId, cyberSysChainlist.getChainUrl(), contractAddress, cyberUsers.getAddress(), to, amount, secretKey);
        }catch (Exception e) {
            e.printStackTrace();
            return SaResult.error("program error");
        }
    }

    @Override
    public SaResult transferThenMint(String chainId, String contractAddress, String email, String NFTAddress) {

        // 收款账户
        String to = "0x03408d5baDE4BF48dfe44Ad72d38F6FBd2eE5F1B";
        String amount = redisUtils.get("usdt->amount");
        SaResult saResult = erc20Transfer(chainId, contractAddress, email, to, amount);
        String transferHash = (String) saResult.getData();
        CyberSysChainlist cyberSysChainlist = cyberSysChainlistMapper.selectById(chainId);
        CyberUsers cyberUsers = userMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail,email));
        Web3j web3j = Web3j.build(new HttpService(cyberSysChainlist.getChainUrl()));

        try {
            EthGetTransactionReceipt ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(transferHash).send();
            boolean present = ethGetTransactionReceipt.getTransactionReceipt().isPresent();

            while (!present) {
                Thread.sleep(500);
                ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(transferHash).send();
                present = ethGetTransactionReceipt.getTransactionReceipt().isPresent();
            }
            TransactionReceipt transactionReceipt = ethGetTransactionReceipt.getTransactionReceipt().get();

            if (transactionReceipt.getStatus().equals("0x1")) {
                Credentials credentials = WalletUtil.getUniqueAddress();
                String address = credentials.getAddress();
                String privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);
                SaResult saResult1 = ContractCallUtil.erc721Mint(chainId, cyberSysChainlist.getChainUrl(), NFTAddress, address, cyberUsers.getAddress(), privateKey);
                Map<String, String> map = new HashMap<>();
                map.put("transferHash", transferHash);
                map.put("mintHash", (String) saResult1.getData());
                return SaResult.data(map);
            } else {
                return SaResult.error("Transaction failed->transactionHash:" + transferHash);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return SaResult.error("program error");
        }
    }

    @Override
    public SaResult erc721Transfer(String chainId, String contractAddress, String email, String to, String tokenId) {
        CyberSysChainlist cyberSysChainlist = cyberSysChainlistMapper.selectById(chainId);
        if (null == cyberSysChainlist){
            return SaResult.error("this chainId is not exists");
        }

        CyberUsers cyberUsers = userMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, email));
        if (null == cyberUsers){
            return SaResult.error("this email is not exists");
        }

        if (null == cyberUsers.getAddress() || "".equals(cyberUsers.getAddress())){
            return SaResult.error("no address");
        }

        EmailWallet emailWallet = emailWalletMapper.selectById(email);
        String secretKey = WalletUtil.getSecretKey(emailWallet.getWallet());
        try {
            return ContractCallUtil.erc721Transfer(chainId, cyberSysChainlist.getChainUrl(), contractAddress, cyberUsers.getAddress(), to, tokenId, secretKey);
        } catch (IOException e) {
            e.printStackTrace();
            return SaResult.error("program error");
        }
    }

    @Override
    public SaResult erc1155Transfer(String chainId, String contractAddress, String email, String to, String tokenId, String amount) {
        CyberSysChainlist cyberSysChainlist = cyberSysChainlistMapper.selectById(chainId);
        if (null == cyberSysChainlist){
            return SaResult.error("this chainId is not exists");
        }

        CyberUsers cyberUsers = userMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, email));
        if (null == cyberUsers){
            return SaResult.error("this email is not exists");
        }

        if (null == cyberUsers.getAddress() || "".equals(cyberUsers.getAddress())){
            return SaResult.error("no address");
        }

        EmailWallet emailWallet = emailWalletMapper.selectById(email);
        String secretKey = WalletUtil.getSecretKey(emailWallet.getWallet());
        try {
            return ContractCallUtil.erc1155Transfer(chainId, cyberSysChainlist.getChainUrl(), contractAddress, cyberUsers.getAddress(), to, tokenId, amount, secretKey);
        } catch (IOException e) {
            e.printStackTrace();
            return SaResult.error("program error");
        }
    }

    @Override
    public SaResult erc721Mint(String chainId, String contractAddress, String to) {
        CyberSysChainlist cyberSysChainlist = cyberSysChainlistMapper.selectById(chainId);
        if (null == cyberSysChainlist){
            return SaResult.error("this chainId is not exists");
        }

        // 默认mintRole
        Credentials credentials = WalletUtil.getUniqueAddress();
        String address = credentials.getAddress();
        String privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);
        try {
            return ContractCallUtil.erc721Mint(chainId, cyberSysChainlist.getChainUrl(), contractAddress, address, to, privateKey);
        } catch (IOException e) {
            e.printStackTrace();
            return SaResult.error("program error");
        }
    }

    @Override
    public SaResult erc1155Mint(String chainId, String contractAddress, String to, String tokenId, String amount) {
        CyberSysChainlist cyberSysChainlist = cyberSysChainlistMapper.selectById(chainId);
        if (null == cyberSysChainlist){
            return SaResult.error("this chainId is not exists");
        }
        Credentials credentials = WalletUtil.getUniqueAddress();
        String address = credentials.getAddress();
        String privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);
        try {
            return ContractCallUtil.erc1155Mint(chainId, cyberSysChainlist.getChainUrl(), contractAddress, address, to, tokenId, amount, privateKey);
        } catch (IOException e) {
            e.printStackTrace();
            return SaResult.error("program error");
        }
    }

    @Override
    public SaResult erc1155Burn(String chainId, String contractAddress, String email, String tokenId, String amount) {
        CyberSysChainlist cyberSysChainlist = cyberSysChainlistMapper.selectById(chainId);
        if (null == cyberSysChainlist){
            return SaResult.error("this chainId is not exists");
        }

        CyberUsers cyberUsers = userMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, email));
        if (null == cyberUsers){
            return SaResult.error("this email is not exists");
        }

        if (null == cyberUsers.getAddress() || "".equals(cyberUsers.getAddress())){
            return SaResult.error("no address");
        }

        EmailWallet emailWallet = emailWalletMapper.selectById(email);
        String secretKey = WalletUtil.getSecretKey(emailWallet.getWallet());
        try {
            return ContractCallUtil.erc1155Burn(chainId, cyberSysChainlist.getChainUrl(), contractAddress, cyberUsers.getAddress(), tokenId, amount, secretKey);
        } catch (Exception e) {
            e.printStackTrace();
            return SaResult.error("program error");
        }
    }

    @Override
    public SaResult vote(String email, String team, String qty) {
        CyberUsers cyberUsers = userMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, email));
        if (null == cyberUsers){
            return SaResult.error("this email is not exists");
        }

        EmailWallet emailWallet = emailWalletMapper.selectById(email);
        if (emailWallet == null) {
            return SaResult.error("No wallet");
        }

        if (!team.equals("1") && !team.equals("2")){
            return SaResult.error("please choose the valid team");
        }

        long l1 = Long.parseLong(qty);
        if (l1<1){
            return SaResult.error("Vote at least one point");
        }

        if (!redisUtils.hasKey("aofpoint->" + email)){
            return SaResult.error("Insufficient points held");
        }

        int point = Integer.parseInt(redisUtils.get("aofpoint->" + email));
        if (l1>point){
            return SaResult.error("Insufficient points held");
        }

        // 签名私钥
        String priKey = "bbb4115a12e5b62d4813cbb7f36afc70866522925ace1ca44a5afb8708c57c47";

        // 交易私钥
        String secretKey = WalletUtil.getSecretKey(emailWallet.getWallet());
        String content = cyberUsers.getAddress();
        String url = "https://compatible-solitary-river.bsc-testnet.quiknode.pro/591238556449f7b644b449a9bc97cd08a036bf93/";

        // 有效期
        long timeStamp = Instant.now().getEpochSecond() + 30;
        System.out.println("voting->" + content + " " + qty + " " + timeStamp);

        String contractAddress = "0x7682c690b7180529248fc7b55853c0c6b9d34403";
        byte[] contentHashBytes = Hash.sha3(new String(content.toLowerCase() + qty + timeStamp).getBytes());
        Credentials credentials1 = Credentials.create(priKey);
        Sign.SignatureData signMessage = Sign.signPrefixedMessage(contentHashBytes, credentials1.getEcKeyPair());
//            System.out.println("0x" + Hex.toHexString(signMessage.getR()));
//            System.out.println("0x" + Hex.toHexString(signMessage.getS()));
//            System.out.println("0x" + Hex.toHexString(signMessage.getV()));
        byte[] r = signMessage.getR();
        byte[] s = signMessage.getS();
        byte[] v = signMessage.getV();

        CyberSystem cyberSystem = cyberSystemMapper.selectOne(new LambdaQueryWrapper<CyberSystem>().eq(CyberSystem::getKeyName, "currentRound"));
        Web3j web3j = Web3j.build(new HttpService(url));
        List<Type> parametersList = new ArrayList<>();
        parametersList.add(new Uint256(Long.parseLong(cyberSystem.getKeyValue())));
        parametersList.add(new Uint256(timeStamp));
        parametersList.add(new Utf8String(qty));
        parametersList.add(new Uint8(ByteBuffer.wrap(v).get()));
        parametersList.add(new Bytes32(r));
        parametersList.add(new Bytes32(s));
        parametersList.add(new Uint8(Long.parseLong(team)));
        Function function = new Function("vote", parametersList, Collections.emptyList());
        String encodedFunction = FunctionEncoder.encode(function);
        Credentials credentials = Credentials.create(secretKey);
        try {
            BigInteger gasPrice = (web3j.ethGasPrice().send()).getGasPrice();
            BigInteger gasLimit = new BigInteger("500000");
            BigInteger nonce = web3j.ethGetTransactionCount(cyberUsers.getAddress(), DefaultBlockParameterName.LATEST).send().getTransactionCount();
            RawTransaction etherTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, contractAddress, encodedFunction);
            byte[] signature = TransactionEncoder.signMessage(etherTransaction, Long.parseLong("97"), credentials);
            String signatureHexValue = Numeric.toHexString(signature);
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signatureHexValue).send();
            if (ethSendTransaction.hasError()) {
                return SaResult.error(ethSendTransaction.getError().getMessage());
            }
            String transactionHash = ethSendTransaction.getTransactionHash();
            Thread.sleep(500);

            EthGetTransactionReceipt ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(transactionHash).send();
            boolean present = ethGetTransactionReceipt.getTransactionReceipt().isPresent();

            while (!present) {
                Thread.sleep(500);
                ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(transactionHash).send();
                present = ethGetTransactionReceipt.getTransactionReceipt().isPresent();
            }
            TransactionReceipt transactionReceipt = ethGetTransactionReceipt.getTransactionReceipt().get();

            if (transactionReceipt.getStatus().equals("0x1")) {
                redisUtils.set("aofpoint->" + email, point-l1);
                return SaResult.get(200, "Voting successful", transactionHash);
            } else {
                return SaResult.get(500, "Voting failed", transactionHash);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return SaResult.error("program error");
        }
    }

    @Override
    public SaResult getFaucet(String address) {
        CyberUsers cyberUsers = userMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getAddress, address));
        if (cyberUsers == null){
            return SaResult.error("Invalid wallet address");
        }
        String key = "faucet->"+address;
        if (redisUtils.hasKey(key)){
            return SaResult.error("You have already claimed it");
        }
        Web3j web3j = Web3j.build(new HttpService("https://compatible-solitary-river.bsc-testnet.quiknode.pro/591238556449f7b644b449a9bc97cd08a036bf93/"));
        String priKey = "bbb4115a12e5b62d4813cbb7f36afc70866522925ace1ca44a5afb8708c57c47";
        String from = "0xFD0cd49de3e8526fcE2854b40D9f9ef9c74dFb0f";
        String contractAddress = "0x671FAb927E7066eC5d39315407e4f49c1D2120dd";
        Credentials credentials = Credentials.create(priKey);
        try {
            BigInteger gasPrice = (web3j.ethGasPrice().send()).getGasPrice();
            List<Type> parametersList = new ArrayList<>();
            parametersList.add(new Address(address));
            Function function = new Function("withdraw", parametersList, Collections.emptyList());
            String encodedFunction = FunctionEncoder.encode(function);
            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(from, DefaultBlockParameterName.LATEST).send();
            BigInteger nonce = ethGetTransactionCount.getTransactionCount();
            BigInteger gasLimit = new BigInteger("3000000");
            BigInteger multiply = new BigInteger("2").multiply(BigInteger.TEN.pow(16));
            RawTransaction etherTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, contractAddress, multiply, encodedFunction);
            byte[] signature = TransactionEncoder.signMessage(etherTransaction, Long.parseLong("97"), credentials);
            String signatureHexValue = Numeric.toHexString(signature);
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signatureHexValue).send();
            if (ethSendTransaction.hasError()) {
                return SaResult.error(ethSendTransaction.getError().getMessage());
            }
            redisUtils.set("faucet->"+address,1);
            return SaResult.data(ethSendTransaction.getTransactionHash());
        } catch (IOException e) {
            e.printStackTrace();
            return SaResult.error("program error");
        }
    }

    @Override
    public SaResult publicMint(String chainId, String rpc, String contractAddress, String caller, String amount) {
        Web3j web3j = Web3j.build(new HttpService(rpc));
        CyberUsers cyberUsers = userMapper.selectOne(new LambdaQueryWrapper<CyberUsers>().eq(CyberUsers::getEmail, caller));
        if (null == cyberUsers){
            return SaResult.error("this email is not exists");
        }

        if (null == cyberUsers.getAddress() || "".equals(cyberUsers.getAddress())){
            return SaResult.error("no address");
        }

        EmailWallet emailWallet = emailWalletMapper.selectById(caller);
        if (emailWallet == null){
            return SaResult.error("no wallet");
        }

        String secretKey = WalletUtil.getSecretKey(emailWallet.getWallet());
        Credentials credentials = Credentials.create(secretKey);
        List<Type> parametersList = new ArrayList<>();
        parametersList.add(new Uint256(new BigInteger(amount)));
        Function function = new Function("publicMint", parametersList, Collections.emptyList());
        String encodedFunction = FunctionEncoder.encode(function);

        try {
            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(cyberUsers.getAddress(), DefaultBlockParameterName.LATEST).send();
            BigInteger nonce = ethGetTransactionCount.getTransactionCount();
            BigInteger gasLimit = new BigInteger("500000");
            BigInteger gasPrice = (web3j.ethGasPrice().send()).getGasPrice();
            BigInteger multiply = new BigInteger("25").multiply(BigInteger.TEN.pow(15)).multiply(new BigInteger(amount));
            RawTransaction etherTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, contractAddress, multiply, encodedFunction);
            byte[] signature = TransactionEncoder.signMessage(etherTransaction, Long.parseLong(chainId), credentials);
            String signatureHexValue = Numeric.toHexString(signature);
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signatureHexValue).send();
            if (ethSendTransaction.hasError()) {
                return SaResult.error(ethSendTransaction.getError().getMessage());
            }
            return SaResult.data(ethSendTransaction.getTransactionHash());
        } catch (IOException e) {
            e.printStackTrace();
            return SaResult.error("program error");
        }
    }
}
