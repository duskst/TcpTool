package org.duskst.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * @author duskst
 */
public class Util {
    /**
     * 幂运算
     * @param basic 底数
     * @param powerNum 幂数
     * @return 结果
     */
    public static int power(int basic, int powerNum) {
        int res = 1;
        for (int i = 0; i < powerNum; i++) {
            res *= basic;
        }
        return res;
    }


    /**
     *
     * 运行命令，不关注结果
     *
     * @param cmd window 命令行命令
     * @param envp 执行上下文环境变量
     * @param file 执行目录
     *
     */
    @SuppressWarnings("unused")
    public static void runCommandLine(String cmd, String[] envp, File file) {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process p = runtime.exec(cmd, envp, file);
            int exitVal = p.waitFor();
            System.out.println(exitVal == 0 ? "command exec Successfully!" : "command exec Fail");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将输入对象转为String ,可用于异常时将相关数据打印，以便分析
     * @param obj 源对象
     * @return String 对象转为的String
     */
    public static String obj2String(Object obj) {
        return new ObjectAnalyzer().toString(obj, 0);
    }

    /**
     * 静态内部类，用于实现对象转string，主要是为了访问记录，不将访问记录当做参数传入是考虑封装。
     */
    private static class ObjectAnalyzer {
        /**
         * 访问记录
         */
        private final ArrayList<Object> visited = new ArrayList<>();

        private String getIndentStr(int indentCount) {
            StringBuilder indentStr = new StringBuilder();
            for (int i = 0; i < indentCount; i++) {
                indentStr.append(' ');
            }
            return indentStr.toString();
        }

        /**
         * 真正的toString 方法
         * @param obj 对象
         * @return String String
         */
        @SuppressWarnings("WeakerAccess")
        public String toString(Object obj, int indent) {
            String indentStr;

            if (obj == null) {
                return "null";
            }
            if (visited.contains(obj)) {
                return "...";
            }
            visited.add(obj);

            Class cl = obj.getClass();

            /* 输入对象是String */
            if (cl == String.class) {
                return (String) obj;
            }

            /* 输入对象是数组 */
            if (cl.isArray()) {
                StringBuilder sb = new StringBuilder();
                sb.append(cl.getComponentType()).append("[]{");
                indent += sb.length();
                indentStr = getIndentStr(indent);
                for (int i = 0; i < Array.getLength(obj); i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append('\n').append(indentStr);

                    Object val = Array.get(obj, i);
                    if (cl.getComponentType().isPrimitive()) {
                        sb.append(val);
                    } else {
                        sb.append(toString(val, indent));
                    }
                }
                sb.append("}");
                return sb.toString();
            }

            /* 输入对象是普通java类 */
            StringBuilder res = new StringBuilder();
            res.append(cl.getName());
            indent += res.length();
            indentStr = getIndentStr(indent);
            do {
                res.append("{");
                // 对象本身声明的属性（不是从父类继承的所有可见性的属性）
                Field[] fields = cl.getDeclaredFields();
                // 可见性设为可见（实际是跳过访问范围校验，属性的可见性并没有改变）
                AccessibleObject.setAccessible(fields, true);
                // 获取属性名及对应值
                for (Field f : fields) {
                    if (!Modifier.isStatic(f.getModifiers())) {
                        if (res.charAt(res.length() - 1) != '{') {
                            res.append(',');
                        }
                        res.append("\n").append(indentStr);
                        res.append(f.getName()).append('=');
                        try {
                            Class t = f.getType();
                            Object val = f.get(obj);
                            if (t.isPrimitive()) {
                                res.append(val);
                            } else {
                                // 属性也是对象，则递归调用toString
                                res.append(toString(val, indent));
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
                res.append("}");
                cl = cl.getSuperclass();
            } while (cl != null && cl != Object.class);
            return res.toString();
        }
    }

    public static void selectSort() {
        int[] intArray = {3, 1, 5, 8, 99, 2, 46, 4, 9, 10};
        int index = 0;
        for (int i = 0; i < intArray.length; i++) {
            int tmpValue = intArray[i];
            index = i;
            for (int j = 0; j < intArray.length; j++) {
                int value = intArray[j];
                if (tmpValue > value) {
                    index = j;
                    tmpValue = value;
                }
            }
        }
    }

    /**
     * 查看对象是否为null.
     * 若是集合还会判读元素数量是否为0
     * @param obj obj which want to check
     * @return <tt>true</tt> if obj is null or
     *      <p> string:trim size is zero</p>
     *      <p> Map or Collection:size is zero</p>
     *      <tt>false</tt> otherwise
     * @date 2022/03/21 14:19
     * @author duskst
     **/
    public static boolean isNullOrEmpty(Object obj) {
        if (null == obj) {
            return true;
        } else if (obj instanceof String) {
            return isNullOrEmpty((String) obj);
        } else if (obj instanceof Map) {
            return isNullOrEmpty((Map) obj);
        } else if (obj instanceof Collection) {
            return isNullOrEmpty((Collection) obj);
        }
        return false;
    }

    public static boolean isNullOrEmpty(String obj) {
        return obj == null || obj.trim().length() == 0;
    }

    public static boolean isNullOrEmpty(Map obj) {
        return obj == null || obj.size() == 0;
    }

    public static boolean isNullOrEmpty(Collection obj) {
        return obj == null || obj.size() == 0;
    }

    /**
     * 传入参数是否全为空或全非空.
     * @param objs 至少两个参数（可变数量参数，实际会被包装成对象数组）
     * @return -1:传入的参数，空值非空值均有  0:所有传入对象均为空   1:所有传入对象都非空
     */
    private static int nullOrNotSimilar(Object... objs) {

        if (objs == null || objs.length < 2) {
            throw new IllegalArgumentException("need at least 2 parameters");
        }

        int expectFlag = Util.isNullOrEmpty(objs[0]) ? 0 : 1;
        for (int i = 1; i < objs.length; i++) {
            int tmpFlag = Util.isNullOrEmpty(objs[i]) ? 0 : 1;
            if (expectFlag != tmpFlag) {
                return -1;
            }
        }
        return expectFlag;
    }

}
