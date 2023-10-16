package cyber.dealer.sys.domain.vo;

import cyber.dealer.sys.util.UnSafeUtil;

import java.lang.reflect.Field;

/**
 * @author Ming Qiu
 * @date Created in 2020/11/1
 * ModifiedBy Ming Qiu 2020/11/7
 **/
public interface VoObject {

    /**
     * 创建Vo对象
     *
     * @return Vo对象
     */
    public Object createVo();

    /**
     * 创建简单Vo对象
     *
     * @return 简单Vo对象
     */
    public Object createSimpleVo();

    default String string() {
        Field[] fields = UnSafeUtil.getAllFields(this.getClass());
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Field field : fields) {
            String name = field.getName();
            Object value = UnSafeUtil.getFieldValue(this, name);
            sb.append(String.format("%s: ", name)).append(value).append(", ");
        }
        sb = new StringBuilder(sb.substring(0, sb.length() - 2));
        sb.append("}");
        return sb.toString();
    }
}
