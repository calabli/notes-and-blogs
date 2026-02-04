package com.tutor.springjourney.datavalidation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ValidationUtils {
    /**
     * 判断对象是否为空
     * 支持：String, Collection, Map, Array
     */
    public boolean empty(Object obj) {
        if (obj == null) return true;
        if (obj instanceof String) return ((String) obj).trim().isEmpty();
        if (obj instanceof Collection) return ((Collection<?>) obj).isEmpty();
        if (obj instanceof Map) return ((Map<?, ?>) obj).isEmpty();
        if (obj.getClass().isArray()) return java.lang.reflect.Array.getLength(obj) == 0;
        return false;
    }

    // 顺便做一个非空判断，让表达式更好写
    public boolean notEmpty(Object obj) {
        return !empty(obj);
    }


    // 检查字符串是否包含黑名单中的任意字符
    public boolean safe(String input, String blacklist) {
        if (input == null) return true;
        for (char c : blacklist.toCharArray()) {
            if (input.indexOf(c) >= 0) return false;
        }
        return true;
    }

    // 递归检查。blacklist 如 "@#$"
    public boolean isDeepSafe(Object node, String blacklist) {
        if (node == null) return true;

        if (node instanceof String) {
            String val = (String) node;
            for (char c : blacklist.toCharArray()) {
                if (val.indexOf(c) >= 0) return false;
            }
        } else if (node instanceof Map) {
            for (Object v : ((Map<?, ?>) node).values()) {
                if (!isDeepSafe(v, blacklist)) return false;
            }
        } else if (node instanceof List) {
            for (Object v : (List<?>) node) {
                if (!isDeepSafe(v, blacklist)) return false;
            }
        }
        return true;
    }
}