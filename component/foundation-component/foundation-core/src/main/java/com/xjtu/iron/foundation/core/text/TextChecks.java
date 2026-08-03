package com.xjtu.iron.foundation.core.text;

import org.apache.commons.lang3.StringUtils;

/**
 * 提供字符串存在性和内容状态检查。
 *
 * <p>该类只统一组件工程中反复出现的空值语义，不复制 Apache Commons Lang 的全部能力。</p>
 */
public final class TextChecks {

    private TextChecks() {
    }

    /**
     * 判断文本是否为 {@code null}、空字符串或者仅包含空白字符。
     *
     * @param value 待检查文本
     * @return 文本为空白时返回 {@code true}
     */
    public static boolean isBlank(CharSequence value) {
        return StringUtils.isBlank(value);
    }

    /**
     * 判断文本是否至少包含一个非空白字符。
     *
     * @param value 待检查文本
     * @return 文本有效时返回 {@code true}
     */
    public static boolean isNotBlank(CharSequence value) {
        return StringUtils.isNotBlank(value);
    }

    /**
     * 判断多个文本是否全部有效。
     *
     * @param values 待检查文本集合
     * @return 所有文本均非空白时返回 {@code true}
     */
    public static boolean allNotBlank(CharSequence... values) {
        if (values == null || values.length == 0) {
            return false;
        }
        for (CharSequence value : values) {
            if (isBlank(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断多个文本中是否至少有一个为空白。
     *
     * @param values 待检查文本集合
     * @return 存在空白值时返回 {@code true}
     */
    public static boolean anyBlank(CharSequence... values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (CharSequence value : values) {
            if (isBlank(value)) {
                return true;
            }
        }
        return false;
    }
}
