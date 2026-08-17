/**
 * 幂等 Repository 顶层 SPI 与共享记录模型。
 *
 * <p>Repository 是幂等正确性的核心边界；具体抢占、恢复、终态写入协议分别下沉到 acquire/recovery/write 子包。</p>
 */
package com.xjtu.iron.idempotent.api.repository;
