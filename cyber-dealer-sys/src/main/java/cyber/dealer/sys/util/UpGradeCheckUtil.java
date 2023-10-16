package cyber.dealer.sys.util;

/**
 * Author hw
 * Date 2023/4/14 10:56
 */
public class UpGradeCheckUtil {
    public static String errorMessage;

    public static boolean isUpGrade(String invCode,Integer level,Integer subLevel,Integer upLevel,Integer upSubLevel){
        int length = invCode.length();
        if (level==4){
            errorMessage = "此用户不能再继续升级";
            return false;
        }else if (level==3){
            if (subLevel==1){
                errorMessage = "此用户不能再继续升级";
                return false;
            }else {
                if (length!=8){
                    errorMessage = "七位邀请码不能进行区域级账号升级";
                    return false;
                }else {
                    if (subLevel==2){
                        if (upLevel==4){
                            return true;
                        }
                        errorMessage = "该邀请码对应的经销商权限不够";
                        return false;
                    }else if (subLevel==3){
                        if (upLevel==4){
                            return true;
                        }else if (upLevel==3 && upSubLevel==1){
                            return true;
                        }else {
                            errorMessage = "该邀请码对应的经销商权限不够";
                            return false;
                        }
                    }else if (subLevel==4){
                        if (upLevel==4 || (upLevel==3 && upSubLevel==1) || (upLevel==3 && upSubLevel==2)){
                            return true;
                        }
                        errorMessage = "该邀请码对应的经销商权限不够";
                        return false;
                    }else if (subLevel==5){
                        if (upLevel==4 || (upLevel==3 && upSubLevel==1) || (upLevel==3 && upSubLevel==2) || (upLevel==3 && upSubLevel==3)){
                            return true;
                        }
                        errorMessage = "该邀请码对应的经销商权限不够";
                        return false;
                    }else if (subLevel==6){
                        if (upLevel==4 || (upLevel==3 && upSubLevel==1) || (upLevel==3 && upSubLevel==2) || (upLevel==3 && upSubLevel==3) || (upLevel==3 && upSubLevel==4)){
                            return true;
                        }
                        errorMessage = "该邀请码对应的经销商权限不够";
                        return false;
                    }else if (subLevel==7){
                        if (upLevel==4 || (upLevel==3 && upSubLevel==1) || (upLevel==3 && upSubLevel==2) || (upLevel==3 && upSubLevel==3) || (upLevel==3 && upSubLevel==4) || (upLevel==3 && upSubLevel==5)){
                            return true;
                        }
                        errorMessage = "该邀请码对应的经销商权限不够";
                        return false;
                    }else {
                        if (upLevel==4 || (upLevel==3 && upSubLevel==1) || (upLevel==3 && upSubLevel==2) || (upLevel==3 && upSubLevel==3) || (upLevel==3 && upSubLevel==4) || (upLevel==3 && upSubLevel==5) || (upLevel==3 && upSubLevel==6)){
                            return true;
                        }
                        errorMessage = "该邀请码对应的经销商权限不够";
                        return false;
                    }
                }
            }
        }else if (level==2){
            if (length!=8){
                errorMessage = "七位邀请码不能进行伙伴级账号升级";
                return false;
            }else {
                return true;
            }
        }else {
            return true;
        }
    }
}
