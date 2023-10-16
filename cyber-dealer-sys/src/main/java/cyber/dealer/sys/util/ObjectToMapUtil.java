package cyber.dealer.sys.util;

/**
 * @author lfy
 * @Date 2022/4/25 15:59
 */

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;

public class ObjectToMapUtil {
    public static Map<String, String> convert(Object object) {
        Map<String, String> map = new HashMap<>();
        Class<?> clazz = object.getClass();
        try {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                String value = field.get(object) != null ? field.get(object).toString() : "";
                map.put(field.getName(), value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}
