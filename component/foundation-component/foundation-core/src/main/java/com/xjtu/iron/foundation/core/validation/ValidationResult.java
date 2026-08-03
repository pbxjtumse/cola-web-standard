package com.xjtu.iron.foundation.core.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合多个结构性校验结果。
 */
public final class ValidationResult {

    /** 本次校验发现的全部结构性问题。 */
    private final List<ValidationViolation> violations;

    private ValidationResult(List<ValidationViolation> violations) {
        this.violations = List.copyOf(violations);
    }

    public static ValidationResult valid() {
        return new ValidationResult(List.of());
    }

    public static ValidationResult of(List<ValidationViolation> violations) {
        return new ValidationResult(violations == null ? List.of() : violations);
    }

    public boolean isValid() {
        return violations.isEmpty();
    }

    public List<ValidationViolation> getViolations() {
        return violations;
    }

    /**
     * 合并多个校验结果。
     */
    public ValidationResult merge(ValidationResult other) {
        if (other == null || other.isValid()) {
            return this;
        }
        List<ValidationViolation> merged = new ArrayList<>(violations);
        merged.addAll(other.violations);
        return new ValidationResult(merged);
    }
}
