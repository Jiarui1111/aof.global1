package cyber.dealer.sys.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;

public class UnSafeUtil {
    private static Unsafe unSafe;

    static {
        Field f;
        try {
            f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unSafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <T> T allocateInstance(Class<T> clazz) {
        T t = null;
        try {
            t = (T) unSafe.allocateInstance(clazz);
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return t;
    }

    public static <T> T clone(Object object, Class<T> clazz) {
        T res = null;
        try {
            // unsafe实例化对象
            res = allocateInstance(clazz);
            if (Objects.isNull(object)) {
                return res;
            }
            // 获取目标类中的所有字段
            Field[] fields = res.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                // 字段在当前类
                if (fieldInTarget(fieldName, object.getClass())) {
                    // 当前类的属性
                    Field declaredField = object.getClass().getDeclaredField(fieldName);
                    declaredField.setAccessible(true);
                    // 当前类的属性值
                    Object fieldValue = unSafe.getObject(object, unSafe.objectFieldOffset(declaredField));
                    if (Objects.nonNull(fieldValue)) {
                        // 赋值到目标类
                        unSafe.putObject(res, unSafe.objectFieldOffset(field), fieldValue);
                    }
                }
            }
        } catch (
                Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static boolean fieldInTarget(String field, Class clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        boolean flag = false;
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);
            String name = declaredField.getName();
            if (name.equals(field)) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public static <T> T mapClone(Map map, Class<T> clazz) {
        T res = null;
        try {
            // unsafe实例化对象
            res = (T) unSafe.allocateInstance(clazz);
            // 获取类中的所有字段
            Field[] fields = res.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String name = field.getName();
                if (fieldInTarget(name, clazz)) {
                    // 当前map的属性值
                    Object fieldValue = map.get(name);
                    // 赋值到目标类
                    unSafe.putObject(res, unSafe.objectFieldOffset(field), fieldValue);
                }
            }
        } catch (
                Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static Object getFieldValue(Object obj, String fieldName) {
        Class<?> objClass = obj.getClass();
        try {
            Field field = objClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return unSafe.getObject(obj, unSafe.objectFieldOffset(field));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void setFieldValue(Object obj, String fieldName, Object fieldValue) {
        Class<?> objClass = obj.getClass();
        try {
            Field field = objClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            unSafe.putObject(obj, unSafe.objectFieldOffset(field), fieldValue);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static Field[] getAllFields(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<>();
        while (clazz != null) {
            fieldList.addAll(new ArrayList<>(Arrays.asList(clazz.getDeclaredFields())));
            clazz = clazz.getSuperclass();
        }
        Field[] fields = new Field[fieldList.size()];
        return fieldList.toArray(fields);
    }
}
