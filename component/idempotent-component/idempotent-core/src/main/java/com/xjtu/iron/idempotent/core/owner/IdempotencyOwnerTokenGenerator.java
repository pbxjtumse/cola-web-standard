package com.xjtu.iron.idempotent.core.owner;

/**
 * PROCESSING ownerToken 生成策略。
 *
 * <p>ownerToken 用来识别“当前是谁拥有这一代执行权”，并与 version 一起参与
 * markSuccess/markFailed 的条件写。它不是用户身份，也不应作为业务主键。</p>
 */
@FunctionalInterface
public interface IdempotencyOwnerTokenGenerator {

    String generate(String namespace, String key);
}
