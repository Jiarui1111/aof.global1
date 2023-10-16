package cyber.dealer.sys.util;

/**
 * Author hw
 * Date 2023/4/10 16:26
 */
import org.springframework.stereotype.Component;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class SendMailUtil  {

    /**
     * 判断邮箱格式是否正确
     * @param email
     * @return false或者true
     */
    public static boolean isEmail(String email) {
        if (email == null || email.length() < 1 || email.length() > 256) {
            return false;
        }
        Pattern pattern = Pattern.compile("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$");
        return pattern.matcher(email).matches();
    }

    /**
     * 发送邮件(参数自己根据自己的需求来修改，发送短信验证码可以直接套用这个模板)
     * @param receiver		接收人的邮箱
     * @param code			验证码
     * @return
     */
    public static int sendEmail(String receiver, String code){
        String sender = "info@moba.network";
        String pwd = "business@2023";
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.host", "smtpout.secureserver.net");
        props.setProperty("mail.smtp.auth", "true");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        Session session = Session.getInstance(props);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender,"AOF Games"));
            message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(receiver,"用户","utf-8"));		//设置收件人
            message.setSubject("Your verification code this time is "+code,"utf-8");
            message.setSentDate(new Date());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String str = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body><p style='font-size: 20px;font-weight:bold;'>Dear ："+receiver+" ,hello</p>"
                    + "<p style='text-indent:2em; font-size: 20px;'>Your verification code this time is "
                    + "<span style='font-size:30px;font-weight:bold;color:red'>" + code + "</span>,Valid within 90 SECONDS, please use as soon as possible! If not operated by yourself, please ignore it!</p>"
                    + "<p style='text-align:right; padding-right: 20px;'"
                    + "<a style='font-size: 18px'>aof labs</a></p>"
                    + "<span style='font-size: 18px; float:right; margin-right: 60px;'>" + sdf.format(new Date()) + "</span></body></html>";

            Multipart mul=new MimeMultipart();  //新建一个MimeMultipart对象来存放多个BodyPart对象
            BodyPart mdp=new MimeBodyPart();  //新建一个存放信件内容的BodyPart对象
            mdp.setContent(str, "text/html;charset=utf-8");
            mul.addBodyPart(mdp);  //将含有信件内容的BodyPart加入到MimeMultipart对象中
            message.setContent(mul); //把mul作为消息内容


            message.saveChanges();

            //创建一个传输对象
            Transport transport=session.getTransport("smtp");


            //建立与服务器的链接  465端口是 SSL传输
            transport.connect("smtpout.secureserver.net", 465, sender, pwd);

            //发送邮件
            transport.sendMessage(message,message.getAllRecipients());

            //关闭邮件传输
            transport.close();
            return 1;

        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return 0;
    }
    /**生成随机的六位验证码*/
    public static StringBuilder CreateCode() {
        String dates = "0123456789";
        StringBuilder code = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 6; i++) {
            int index = r.nextInt(dates.length());
            char c = dates.charAt(index);
            code.append(c);
        }
        return code;
    }

    /**
     * 使用mailgun方式发送验证码
     *
     * @return
     */
    public static int EmailWithMailgun(String to, String code) throws Exception {
        String apiKey = "364200ded432843f764b73145fc08d34-af778b4b-c0d302a2"; // Mailgun API Key
        String domain = "email.arenaoffaith.online"; // Mailgun Domain Name
        String from = "aof@aof.games"; // 发件人邮箱地址
        String subject = "VerifyCode"; // 邮件主题
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String str = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body><p style='font-size: 20px;font-weight:bold;'>Hey: " + to + "</p>"
                + "<p style='text-indent:2em; font-size: 20px;'>Your verification code this time is "
                + "<span style='font-size:30px;font-weight:bold;color:#3f51b5'>" + code + "</span>,Valid within 90 SECONDS,"
                + " please use as soon as possible! If not operated by yourself, please ignore it!</p>"
                + "<p style='text-align:right; padding-right: 20px;'"
                + "<a style='font-size: 18px'>aof labs</a></p>"
                + "<span style='font-size: 18px; float:right; margin-right: 60px;'>"
                + sdf.format(new Date()) + "</span></body></html>";
        // 创建URL对象
        URL url = new URL("https://api.mailgun.net/v3/" + domain + "/messages");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        // 设置请求头部信息
        String userCredentials = "api:" + apiKey;
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));
        conn.setRequestProperty("Authorization", basicAuth);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // 设置请求参数
        String postData = "from=" + URLEncoder.encode(from, "UTF-8") +
                "&to=" + URLEncoder.encode(to, "UTF-8") +
                "&subject=" + URLEncoder.encode(subject, "UTF-8") +
                "&html=" + URLEncoder.encode(str,"UTF-8");

        // 发送请求
        conn.getOutputStream().write(postData.getBytes("UTF-8"));
        return conn.getResponseCode();
    }
}

