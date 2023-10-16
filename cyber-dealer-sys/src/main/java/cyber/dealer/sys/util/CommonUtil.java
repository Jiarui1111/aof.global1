package cyber.dealer.sys.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cyber.dealer.sys.constant.InternalReturnObject;
import cyber.dealer.sys.constant.ReturnNo;
import cyber.dealer.sys.constant.ReturnObject;
import cyber.dealer.sys.domain.vo.VoObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * 通用工具类
 *
 * @author Ming Qiu
 **/
@Slf4j
public class CommonUtil {

    /**
     * 处理BindingResult的错误
     *
     * @param bindingResult
     * @return
     */
    public static Object processFieldErrors(BindingResult bindingResult, HttpServletResponse response) {
        Object retObj = null;
        if (bindingResult.hasErrors()) {
            StringBuffer msg = new StringBuffer();
            //解析原错误信息，封装后返回，此处返回非法的字段名称，原始值，错误信息
            for (FieldError error : bindingResult.getFieldErrors()) {
                msg.append(error.getDefaultMessage());
                msg.append(";");
            }
            log.debug("processFieldErrors: msg = " + msg);
            retObj = ResponseUtil.fail(ReturnNo.FIELD_NOTVALID, msg.toString());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        }
        return retObj;
    }

    /**
     * 处理返回对象
     *
     * @param returnObject 返回的对象
     * @return
     */
    public static ReturnObject getRetObject(ReturnObject<VoObject> returnObject) {
        ReturnNo code = returnObject.getCode();
        switch (code) {
            case OK:
            case RESOURCE_FALSIFY:
                VoObject data = returnObject.getData();
                if (data != null) {
                    Object voObj = data.createVo();
                    return new ReturnObject(voObj);
                } else {
                    return new ReturnObject();
                }
            default:
                return new ReturnObject(returnObject.getCode(), returnObject.getErrmsg());
        }
    }

    /**
     * @param returnObject
     * @param voClass
     * @return
     * @author xucangbai
     */
    public static ReturnObject getRetVo(ReturnObject<Object> returnObject, Class voClass) {
        ReturnNo code = returnObject.getCode();
        switch (code) {
            case OK:
            case RESOURCE_FALSIFY:
                Object data = returnObject.getData();
                if (data != null) {
                    Object voObj = cloneVo(data, voClass);
                    return new ReturnObject(voObj);
                } else {
                    return new ReturnObject();
                }
            default:
                return new ReturnObject(returnObject.getCode(), returnObject.getErrmsg());
        }
    }

    /**
     * 处理返回对象
     *
     * @param returnObject 返回的对象
     */

    public static ReturnObject getListRetObject(ReturnObject<List> returnObject) {
        ReturnNo code = returnObject.getCode();
        switch (code) {
            case OK:
            case RESOURCE_FALSIFY:
                List objs = returnObject.getData();
                if (objs != null) {
                    List<Object> ret = new ArrayList<>(objs.size());
                    for (Object data : objs) {
                        if (data instanceof VoObject) {
                            ret.add(((VoObject) data).createVo());
                        }
                    }
                    return new ReturnObject(ret);
                } else {
                    return new ReturnObject();
                }
            default:
                return new ReturnObject(returnObject.getCode(), returnObject.getErrmsg());
        }
    }

    /**
     * @param returnObject
     * @param voClass
     * @return
     * @author xucangbai
     */
    public static ReturnObject getListRetVo(ReturnObject<List> returnObject, Class voClass) {
        ReturnNo code = returnObject.getCode();
        switch (code) {
            case OK:
            case RESOURCE_FALSIFY:
                List objs = returnObject.getData();
                if (objs != null) {
                    List<Object> ret = new ArrayList<>(objs.size());
                    for (Object data : objs) {
                        if (data instanceof Object) {
                            ret.add(cloneVo(data, voClass));
                        }
                    }
                    return new ReturnObject(ret);
                } else {
                    return new ReturnObject();
                }
            default:
                return new ReturnObject(returnObject.getCode(), returnObject.getErrmsg());
        }
    }


    /**
     * 根据 errCode 修饰 API 返回对象的 HTTP Status
     *
     * @param returnObject 原返回 Object
     * @return 修饰后的返回 Object
     */
    public static Object decorateReturnObject(ReturnObject<Object> returnObject) {
        ReturnNo returnNo = returnObject.getCode();
        if ("失败".equals(returnObject.getErrmsg()) || !Common.isOk(returnObject)) {
            log.error("{}:{}", returnObject.getErrmsg(), returnObject.getData());
        }
        if ("JWT过期".equals(returnObject.getData())) {
            returnNo = ReturnNo.AUTH_JWT_EXPIRED;
        }
        switch (returnNo) {
            case RESOURCE_ID_NOT_EXIST:
                // 404：资源不存在
                return new ResponseEntity<>(
                        ResponseUtil.fail(returnObject.getCode(), returnObject.getErrmsg()),
                        HttpStatus.NOT_FOUND);

            case AUTH_NEED_LOGIN:
            case AUTH_INVALID_JWT:
            case AUTH_JWT_EXPIRED:
                // 401
                return new ResponseEntity<>(
                        ResponseUtil.fail(returnObject.getCode(), returnObject.getErrmsg()),
                        HttpStatus.UNAUTHORIZED);

            case SERVER_ERROR:
            case GLOBE_ERROR:
            case SPRINGBOOT_ERROR:
            case INTERNAL_SERVER_ERR:
                // 500：数据库或其他严重错误
                return ResponseUtil.fail(returnObject.getCode());

            case FIELD_NOTVALID:
            case IMG_FORMAT_ERROR:
            case IMG_SIZE_EXCEED:
                // 400
                return new ResponseEntity<>(
                        ResponseUtil.fail(returnObject.getCode(), returnObject.getErrmsg()),
                        HttpStatus.BAD_REQUEST);

            case RESOURCE_ID_OUTSCOPE:
            case FILE_NO_WRITE_PERMISSION:
                // 403
                return new ResponseEntity<>(
                        ResponseUtil.fail(returnObject.getCode(), returnObject.getErrmsg()),
                        HttpStatus.FORBIDDEN);

            case OK:
                // 200: 无错误
                Object data = returnObject.getData();
                if (data != null) {
                    return ResponseUtil.ok(data);
                } else {
                    return ResponseUtil.ok();
                }

            default:
                data = returnObject.getData();
                if (data != null) {
                    return ResponseUtil.fail(returnObject.getCode(), returnObject.getErrmsg(), returnObject.getData());
                } else {
                    return ResponseUtil.fail(returnObject.getCode());
                }

        }

    }

    /**
     * 转换List的时间类型并返回秒数
     *
     * @param list          待转换 list
     * @param dateFieldName 日期名称
     * @param clazz         转换对象
     * @return
     */
    @SuppressWarnings("all")
    public static Object convertListSecond(List list, String dateFieldName, Class clazz) {
        Object records = list.stream().map(row -> {
            Object clone = UnSafeUtil.clone(row, clazz);
            Object timeObj = UnSafeUtil.getFieldValue(row, dateFieldName);
            if (timeObj instanceof LocalDate) {
                LocalDate rowPaymentDate = (LocalDate) timeObj;
                long epochSecond = rowPaymentDate.atStartOfDay(ZoneOffset.ofHours(8)).toInstant().getEpochSecond();
                UnSafeUtil.setFieldValue(clone, dateFieldName, epochSecond);
            }
            if (timeObj instanceof LocalDateTime) {
                LocalDateTime rowPaymentDate = (LocalDateTime) timeObj;
                long epochSecond = rowPaymentDate.toEpochSecond(ZoneOffset.ofHours(8));
                UnSafeUtil.setFieldValue(clone, dateFieldName, epochSecond);
            }
            return clone;
        }).collect(Collectors.toList());
        return records;
    }

    /**
     * 转换List的时间类型并返回毫秒数
     *
     * @param list          待转换 list
     * @param dateFieldName 日期名称
     * @param clazz         转换对象
     * @return
     */
    @SuppressWarnings("all")
    public static Object convertListMilli(List list, String dateFieldName, Class clazz) {
        Object records = list.stream().map(row -> {
            Object clone = UnSafeUtil.clone(row, clazz);
            Object timeObj = UnSafeUtil.getFieldValue(row, dateFieldName);
            if (timeObj instanceof LocalDate) {
                LocalDate rowPaymentDate = (LocalDate) timeObj;
                long epochSecond = rowPaymentDate.atStartOfDay(ZoneOffset.ofHours(8)).toInstant().getEpochSecond();
                UnSafeUtil.setFieldValue(clone, dateFieldName, epochSecond * 1000);
            }
            if (timeObj instanceof LocalDateTime) {
                LocalDateTime rowPaymentDate = (LocalDateTime) timeObj;
                long epochSecond = rowPaymentDate.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
                UnSafeUtil.setFieldValue(clone, dateFieldName, epochSecond);
            }
            return clone;
        }).collect(Collectors.toList());
        return records;
    }

    @SuppressWarnings("all")
    public static <T> Supplier<Stream<T>> convertStream(List obj, Class<T> clazz) {
        return () -> obj.parallelStream()
                .filter(Objects::nonNull)
                .map(JSONObject::toJSONString)
                .map(row -> JSON.parseObject((String) row, clazz));
    }

    /**
     * @param bo      business object
     * @param voClass vo对象类型
     * @return 浅克隆的vo对象
     * @author xucangbai
     * @date 2021/11/13
     * 根据clazz实例化一个对象，并深度克隆bo中对应属性到这个新对象
     * 其中会自动实现modifiedBy和createdBy两字段的类型转换
     */
    public static <T> T cloneVo(Object bo, Class<T> voClass) {
        Class boClass = bo.getClass();
        T newVo = null;
        try {
            //默认voClass有无参构造函数
            newVo = voClass.getDeclaredConstructor().newInstance();
            Field[] voFields = voClass.getDeclaredFields();
            Field[] boFields = boClass.getDeclaredFields();
            for (Field voField : voFields) {
                //静态和Final不能拷贝
                int mod = voField.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isFinal(mod)) {
                    continue;
                }
                voField.setAccessible(true);
                Field boField = null;
                try {
                    boField = boClass.getDeclaredField(voField.getName());
                    boField.setAccessible(true);
                }
                //bo中查找不到对应的属性，那就有可能为特殊情况xxx，需要由xxxId与xxxName组装
                catch (NoSuchFieldException e) {
                    //提取头部
                    String head = voField.getName();
                    Field boxxxNameField = null;
                    Field boxxxIdField = null;
                    for (Field bof : boFields) {
                        if (bof.getName().matches(head + "Name")) {
                            boxxxNameField = bof;
                        } else if (bof.getName().matches(head + "Id")) {
                            boxxxIdField = bof;
                        }
                    }
                    //找不到xxxName或者找不到xxxId
                    if (boxxxNameField == null || boxxxIdField == null) {
                        voField.set(newVo, null);
                        continue;
                    }

                    Object newSimpleRetVo = voField.getType().getDeclaredConstructor().newInstance();
                    Field newSimpleRetVoIdField = newSimpleRetVo.getClass().getDeclaredField("id");
                    Field newSimpleRetVoNameField = newSimpleRetVo.getClass().getDeclaredField("name");
                    newSimpleRetVoIdField.setAccessible(true);
                    newSimpleRetVoNameField.setAccessible(true);

                    //bo的xxxId和xxxName组装为SimpleRetVo的id,name
                    boxxxIdField.setAccessible(true);
                    boxxxNameField.setAccessible(true);
                    Object boxxxId = boxxxIdField.get(bo);
                    Object boxxxName = boxxxNameField.get(bo);

                    newSimpleRetVoIdField.set(newSimpleRetVo, boxxxId);
                    newSimpleRetVoNameField.set(newSimpleRetVo, boxxxName);

                    voField.set(newVo, newSimpleRetVo);
                    continue;
                }
                Class<?> boFieldType = boField.getType();
                //属性名相同，类型相同，直接克隆
                if (voField.getType().equals(boFieldType)) {
                    boField.setAccessible(true);
                    Object newObject = boField.get(bo);
                    voField.set(newVo, newObject);
                }
                //属性名相同，类型不同
                else {
                    boolean boFieldIsIntegerOrByteAndVoFieldIsEnum = ("Integer".equals(boFieldType.getSimpleName()) || "Byte".equals(boFieldType.getSimpleName())) && voField.getType().isEnum();
                    boolean voFieldIsIntegerOrByteAndBoFieldIsEnum = ("Integer".equals(voField.getType().getSimpleName()) || "Byte".equals(voField.getType().getSimpleName())) && boFieldType.isEnum();
                    boolean voFieldIsLocalDateTimeAndAndBoFieldIsZonedDateTime = ("LocalDateTime".equals(voField.getType().getSimpleName()) && "ZonedDateTime".equals(boField.getType().getSimpleName()));
                    boolean voFieldIsZonedDateTimeAndBoFieldIsLocalDateTime = ("ZonedDateTime".equals(voField.getType().getSimpleName()) && "LocalDateTime".equals(boField.getType().getSimpleName()));

                    try {
                        //整形或Byte转枚举
                        if (boFieldIsIntegerOrByteAndVoFieldIsEnum) {
                            Object newObj = boField.get(bo);
                            if ("Byte".equals(boFieldType.getSimpleName())) {
                                newObj = ((Byte) newObj).intValue();
                            }
                            Object[] enumer = voField.getType().getEnumConstants();
                            voField.set(newVo, enumer[(int) newObj]);
                        }
                        //枚举转整形或Byte
                        else if (voFieldIsIntegerOrByteAndBoFieldIsEnum) {
                            Object value = ((Enum) boField.get(bo)).ordinal();
                            if ("Byte".equals(voField.getType().getSimpleName())) {
                                value = ((Integer) value).byteValue();
                            }
                            voField.set(newVo, value);
                        }
                        //ZonedDateTime转LocalDateTime
                        else if (voFieldIsLocalDateTimeAndAndBoFieldIsZonedDateTime) {
                            ZonedDateTime newObj = (ZonedDateTime) boField.get(bo);
                            LocalDateTime localDateTime = newObj.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                            voField.set(newVo, localDateTime);
                        }
                        //LocalDateTime转ZonedDateTime
                        else if (voFieldIsZonedDateTimeAndBoFieldIsLocalDateTime) {
                            LocalDateTime newObj = (LocalDateTime) boField.get(bo);
                            ZonedDateTime zdt = newObj.atZone(ZoneId.systemDefault());
                            voField.set(newVo, zdt);
                        } else {
                            voField.set(newVo, null);
                        }
                    }
                    //如果为空字段则不复制
                    catch (Exception e) {
                        voField.set(newVo, null);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return newVo;
    }

    public static boolean isOk(ReturnObject<Object> returnObject) {
        return ReturnNo.OK.equals(returnObject.getCode());
    }

    public static boolean isOk(InternalReturnObject<Object> returnObject) {
        return ReturnNo.OK.getCode() == returnObject.getErrno();
    }

    public static <T> List<T> enumerationToList(Enumeration<T> enumeration) {
        List<T> list = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            list.add(enumeration.nextElement());
        }
        return list;
    }

    public static String unit(Double data) {
        List<String> unitNames = Arrays.asList("MH/s", "GH/s", "TH/s", "PH/s", "EH/s", "ZH/s");
        int num = (data.longValue() + "").length() - 1;
        double pow = Math.pow(10, num);
        int index = Double.valueOf(Math.floor(Math.log10(pow) / 3) + "").intValue();
        String unit = index <= 1 ? "H/s" : unitNames.get(index - 2);
        return String.format("%s %s", data / Math.pow(10, (index == 0 ? 1 : index) * 3), unit);
    }

    public static void cutNumberByStream(long number, long size, Map<Long, List<Long>> res) {
        AtomicBoolean flag = new AtomicBoolean(true);
        LongStream.range(number, number + 1).filter(n -> n >= size).peek(n -> flag.set(false)).forEach(n -> res.put(n, Arrays.asList(n - size, n)));
        if (flag.get()) {
            res.put(number, Arrays.asList(0L, number));
            return;
        }
        cutNumberByStream(number - size, size, res);
    }

    public static String unitDate(Long date) {
        int num = (date + "").length() - 1;
        if (num <= 2) {
            return String.format("%s %s", date, "毫秒");
        }
        date /= 1000;
        Double index = Math.floor(log(60, date.doubleValue()));
        ArrayList<String> resList = new ArrayList<>();
        unitDate(date, index.intValue(), resList);
        return String.join("", resList);
    }

    private static void unitDate(Long date, int index, List<String> resList) {
        List<String> unitNames = Arrays.asList("秒", "分钟", "小时", "天", "周", "月", "年");
        List<Long> unitConvert = Arrays.asList(1L, 60L, 60L, 24L, 7L, 4L, 12L);
        if (index == 0) {
            resList.add(String.format("%s%s", date, unitNames.get(0)));
            return;
        }
        Long currentUnitSecond = recursionList(unitConvert, index);
        int convertFirst = Double.valueOf(date / currentUnitSecond).intValue();
        resList.add(String.format("%s%s", convertFirst, unitNames.get(index)));
        unitDate(date - convertFirst * currentUnitSecond, index - 1, resList);
    }

    public static double log(double x, double y) {
        return Math.log(y) / Math.log(x);
    }

    public static Long recursionList(List<Long> list, int index) {
        if (index == 0) {
            return list.get(index);
        }
        return recursionList(list, index - 1) * list.get(index);
    }

    public static String genSeqNum(int platform) {
        int maxNum = 36;
        int i;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmssS");
        LocalDateTime localDateTime = LocalDateTime.now();
        String strDate = localDateTime.format(dtf);
        StringBuffer sb = new StringBuffer(strDate);

        int count = 0;
        char[] str = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
                'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
                'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        Random r = new Random();
        while (count < 2) {
            i = Math.abs(r.nextInt(maxNum));
            if (i >= 0 && i < str.length) {
                sb.append(str[i]);
                count++;
            }
        }
        if (platform > 36) {
            platform = 36;
        } else if (platform < 1) {
            platform = 1;
        }

        sb.append(str[platform - 1]);
        return sb.toString();
    }

    public static int randomInt(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}
