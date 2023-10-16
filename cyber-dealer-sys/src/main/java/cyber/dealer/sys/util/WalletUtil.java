package cyber.dealer.sys.util;
import cn.dev33.satoken.util.SaResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.crypto.*;
import java.io.*;
import java.math.BigInteger;

@Slf4j
@Component
public class WalletUtil{

    private static final String password = "Lux:oE6-xz_Z";

    private static final File uniqueAddressDirectory = new File("/home/ec2-user/data/java/aof/MintRoleDir/");

    private static final File walletDirectory = new File("/home/ec2-user/data/java/aof/PrivateKeyDir/");

//    private static final File uniqueAddressDirectory = new File("D:\\wallet-SecretKey");

//    private static final File walletDirectory = new File("D:\\wallet-SecretKey");

    private static Credentials credentials = null;

    public static String generateWalletFile() throws Exception {

        // 创建新的EC密钥对
        ECKeyPair ecKeyPair = Keys.createEcKeyPair();

        // 将EC密钥对转换为钱包文件，并将文件保存到指定的目录中
        return WalletUtils.generateWalletFile(password, ecKeyPair, walletDirectory, true);
    }

    public static String getAddress(String fileName){
        // 使用钱包文件和密码创建凭证
        try {
            credentials = WalletUtils.loadCredentials(password, new File(walletDirectory, fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CipherException e) {
            throw new RuntimeException(e);
        }
        return credentials.getAddress();
    }

    public static String getSecretKey(String fileName){
        try {
            credentials = WalletUtils.loadCredentials(password, new File(walletDirectory, fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CipherException e) {
            throw new RuntimeException(e);
        }
        return credentials.getEcKeyPair().getPrivateKey().toString(16);
    }

    public static SaResult setUniqueAddress(String privateKey) throws Exception {

        // 创建新的EC密钥对
        BigInteger bigInteger = new BigInteger(privateKey, 16);
        ECKeyPair ecKeyPair = ECKeyPair.create(bigInteger);
        WalletFile walletFile = Wallet.createStandard(password, ecKeyPair);
        File destination = new File(uniqueAddressDirectory, "MintRole.json");
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.writeValue(destination, walletFile);
        return SaResult.ok();
    }

    public static Credentials getUniqueAddress(){
        try {
            credentials = WalletUtils.loadCredentials(password, new File(uniqueAddressDirectory, "MintRole.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CipherException e) {
            throw new RuntimeException(e);
        }
        return credentials;
    }
}