package com.xjtu.iron.idempotent.core;
import com.xjtu.iron.idempotent.api.*;import java.util.Objects;
/** Starter 传给 Core 的默认策略快照。 */
public final class IdempotencyDefaults{private final IdempotencyMode defaultMode;private final IdempotencyOptions shortTerm,durable;public IdempotencyDefaults(IdempotencyMode defaultMode,IdempotencyOptions shortTerm,IdempotencyOptions durable){this.defaultMode=Objects.requireNonNull(defaultMode);this.shortTerm=Objects.requireNonNull(shortTerm);this.durable=Objects.requireNonNull(durable);}public IdempotencyOptions defaultOptions(){return defaultMode==IdempotencyMode.SHORT_TERM?shortTerm:durable;}}
