package cyber.dealer.sys.util;

import cn.dev33.satoken.util.SaResult;
import lombok.extern.slf4j.Slf4j;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
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
import java.util.concurrent.ExecutionException;

import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

@Slf4j
public class ContractCallUtil {

    private static List<TypeReference<?>> outList = Collections.emptyList();

    private static Web3j web3j;

    private static BigInteger gasPrice;

    private static BigInteger gasLimit = new BigInteger("3000000");

    private static void initWeb3j(String url){
        web3j = Web3j.build(new HttpService(url));
        try {
            gasPrice = (web3j.ethGasPrice().send()).getGasPrice();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static SaResult erc20Transfer(String chainId,String url, String contractAddress, String from, String to, String amount, String privateKey) throws IOException, ExecutionException, InterruptedException {
        BigInteger erc20Decimals = getErc20Decimals(url, contractAddress);
        Credentials credentials = Credentials.create(privateKey);
        List<Type> parametersList = new ArrayList<>();
        parametersList.add(new Address(to));
        parametersList.add(new Uint256(new BigInteger(amount).multiply(BigInteger.TEN.pow(erc20Decimals.intValue()))));
        Function function = new Function("transfer", parametersList, outList);
        String encodedFunction = FunctionEncoder.encode(function);
        return sendTransaction(chainId, credentials, getNonce(from), gasPrice, gasLimit, contractAddress, encodedFunction);
    }

    public static SaResult erc20Mint(Map<String,String> map) throws IOException {
        String chainId = map.get("chainId");
        String url = map.get("url");
        String contractAddress = map.get("contractAddress");

        // MintRole
        String address = map.get("address");
        String privateKey = map.get("privateKey");

        String to = map.get("to");
        BigInteger amount = BigInteger.valueOf(Long.parseLong(map.get("amount")));
        BigInteger erc20Decimals = getErc20Decimals(url, contractAddress);
        Credentials credentials = Credentials.create(privateKey);
        List<Type> parametersList = new ArrayList<>();
        parametersList.add(new Address(to));
        Function function = new Function("mint", parametersList, outList);
        String encodedFunction = FunctionEncoder.encode(function);
        return sendTransaction(chainId, credentials, getNonce(address), gasPrice, gasLimit, contractAddress, encodedFunction);
    }

    public static SaResult erc721Transfer (String chainId,String url, String contractAddress, String from, String to, String tokenId, String privateKey) throws IOException {
        initWeb3j(url);
        Credentials credentials = Credentials.create(privateKey);
        List<Type> parametersList = new ArrayList<>();
        parametersList.add(new Address(from));
        parametersList.add(new Address(to));
        parametersList.add(new Uint256(new BigInteger(tokenId)));
        Function function = new Function("safeTransferFrom", parametersList, outList);
        String encodedFunction = FunctionEncoder.encode(function);
        return sendTransaction(chainId, credentials, getNonce(from), gasPrice, gasLimit, contractAddress, encodedFunction);
    }

    public static SaResult erc1155Transfer (String chainId,String url, String contractAddress, String from, String to, String tokenId, String amount, String privateKey) throws IOException {
        initWeb3j(url);
        Credentials credentials = Credentials.create(privateKey);
        List<Type> parametersList = new ArrayList<>();
        parametersList.add(new Address(from));
        parametersList.add(new Address(to));
        parametersList.add(new Uint256(new BigInteger(tokenId)));
        parametersList.add(new Uint256(new BigInteger(amount)));
        parametersList.add(new DynamicBytes("0x".getBytes()));
        Function function = new Function("safeTransferFrom", parametersList, outList);
        String encodedFunction = FunctionEncoder.encode(function);
        return sendTransaction(chainId, credentials, getNonce(from), gasPrice, gasLimit, contractAddress, encodedFunction);
    }

    public static SaResult erc721Mint(String chainId,String url, String contractAddress, String address, String to, String privateKey) throws IOException {
        initWeb3j(url);
        Credentials credentials = Credentials.create(privateKey);
        List<Type> parametersList = new ArrayList<>();
        parametersList.add(new Address(to));
        Function function = new Function("safeMint", parametersList, outList);
        String encodedFunction = FunctionEncoder.encode(function);
        return sendTransaction(chainId, credentials, getNonce(address), gasPrice, gasLimit, contractAddress, encodedFunction);
    }

    public static SaResult erc1155Mint(String chainId,String url, String contractAddress, String address, String to, String tokenId, String amount, String privateKey) throws IOException {
        initWeb3j(url);
        long timeStamp = Instant.now().getEpochSecond() + 30;
        // 签名私钥
        String priKey = "bbb4115a12e5b62d4813cbb7f36afc70866522925ace1ca44a5afb8708c57c47";
        byte[] contentHashBytes = Hash.sha3(new String(address.toLowerCase() + "500" + timeStamp).getBytes());
        Credentials credentials1 = Credentials.create(priKey);
        Sign.SignatureData signMessage = Sign.signPrefixedMessage(contentHashBytes, credentials1.getEcKeyPair());
        byte[] r = signMessage.getR();
        byte[] s = signMessage.getS();
        byte[] v = signMessage.getV();

        Credentials credentials = Credentials.create(privateKey);
        List<Type> parametersList = new ArrayList<>();
        parametersList.add(new Uint256(timeStamp));
        parametersList.add(new Utf8String("500"));
        parametersList.add(new Uint8(ByteBuffer.wrap(v).get()));
        parametersList.add(new Bytes32(r));
        parametersList.add(new Bytes32(s));
        parametersList.add(new Address(to));
        parametersList.add(new Uint256(new BigInteger(tokenId)));
        parametersList.add(new Uint256(new BigInteger(amount)));
        parametersList.add(new DynamicBytes("0x".getBytes()));
        Function function = new Function("mint", parametersList, outList);
        String encodedFunction = FunctionEncoder.encode(function);
        return sendTransaction(chainId, credentials, getNonce(address), gasPrice, gasLimit, contractAddress, encodedFunction);
    }

    public static SaResult erc1155Burn(String chainId,String url, String contractAddress, String from, String tokenId, String amount, String privateKey) throws IOException, InterruptedException {
        initWeb3j(url);
        Credentials credentials = Credentials.create(privateKey);
        List<Type> parametersList = new ArrayList<>();
        parametersList.add(new Address(from));
        parametersList.add(new Uint256(new BigInteger(tokenId)));
        parametersList.add(new Uint256(new BigInteger(amount)));
        Function function = new Function("burn", parametersList, outList);
        String encodedFunction = FunctionEncoder.encode(function);
        SaResult saResult = sendTransaction(chainId, credentials, getNonce(from), gasPrice, gasLimit, contractAddress, encodedFunction);
        String transactionHash = (String) saResult.getData();
        Thread.sleep(5000); // 打包上链需要时间
        EthGetTransactionReceipt ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(transactionHash).send();
        if (ethGetTransactionReceipt.getTransactionReceipt().isPresent()) {
            TransactionReceipt transactionReceipt = ethGetTransactionReceipt.getTransactionReceipt().get();
            if (transactionReceipt.getStatus().equals("0x1")) {
                return SaResult.data(from);
            } else {
                return SaResult.error("交易失败");
            }
        } else {
            return SaResult.error("交易还未打包进区块");
        }
    }

    public static BigInteger getErc20Decimals(String url, String contractAddress) {
        initWeb3j(url);
        Function function = new Function("decimals", Collections.emptyList(), Collections.singletonList(new TypeReference<Uint8>() {}));
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall ethCall = null;
        try {
            ethCall = web3j.ethCall(
                            createEthCallTransaction(null, contractAddress, encodedFunction),
                            DefaultBlockParameterName.LATEST)
                    .sendAsync().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        String result = ethCall.getResult();
        return new BigInteger(result.substring(2), 16);
    }

    private static BigInteger getNonce(String from) throws IOException {
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(from, DefaultBlockParameterName.LATEST).send();
        return ethGetTransactionCount.getTransactionCount();
    }

    private static SaResult sendTransaction(String chainId, Credentials credentials, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to, String data) throws IOException {
        RawTransaction etherTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, data);
        byte[] signature = TransactionEncoder.signMessage(etherTransaction, Long.parseLong(chainId), credentials);
        String signatureHexValue = Numeric.toHexString(signature);
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signatureHexValue).send();
        if (ethSendTransaction.hasError()) {
            return SaResult.error(ethSendTransaction.getError().getMessage());
        }
        return SaResult.data(ethSendTransaction.getTransactionHash());
    }

    public static SaResult getBalance(String url, String contractAddress, String address){
        initWeb3j(url);

        // 创建balanceOf方法的Function对象
        Function balanceOf = new Function(
                "balanceOf",
                Arrays.asList(new Address(address)),
                Arrays.asList(new TypeReference<Uint256>() {})
        );

        // 将Function对象编码成十六进制字符串
        String encodedFunction = FunctionEncoder.encode(balanceOf);

        try {
            // 调用合约的balanceOf方法
            EthCall response = web3j.ethCall(
                            createEthCallTransaction(
                                    null, contractAddress, encodedFunction),
                            DefaultBlockParameterName.LATEST)
                    .send();

            // 解码响应数据
            List<Type> decodedResponse = FunctionReturnDecoder.decode(response.getValue(), balanceOf.getOutputParameters());

            // 获取余额
            BigInteger balance = (BigInteger) decodedResponse.get(0).getValue();
            return SaResult.data(balance);
        } catch (Exception e) {
            e.printStackTrace();
            return SaResult.error();
        }
    }
}
