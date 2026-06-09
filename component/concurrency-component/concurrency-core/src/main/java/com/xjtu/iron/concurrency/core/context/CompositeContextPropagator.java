package com.xjtu.iron.concurrency.core.context;

import com.xjtu.iron.concurrency.api.context.ContextPropagator;
import com.xjtu.iron.concurrency.api.context.ContextScope;
import com.xjtu.iron.concurrency.api.context.ContextSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CompositeContextPropagator implements ContextPropagator {

    private final List<ContextPropagator> propagators;

    public CompositeContextPropagator(List<ContextPropagator> propagators) {
        this.propagators = propagators == null ? List.of() : List.copyOf(propagators);
    }

    @Override
    public ContextSnapshot capture() {
        List<ContextSnapshot> snapshots = propagators.stream().map(ContextPropagator::capture).toList();

        return () -> {
            List<ContextScope> scopes = new ArrayList<>();

            for (ContextSnapshot snapshot : snapshots) {
                scopes.add(snapshot.restore());
            }

            return () -> {
                for (int i = scopes.size() - 1; i >= 0; i--) {
                    scopes.get(i).close();
                }
            };
        };
    }
}