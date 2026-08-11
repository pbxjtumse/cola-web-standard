package com.xjtu.iron.idempotent.core;
import com.xjtu.iron.idempotent.api.IdempotencyMode;import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;import java.util.*;
/** 按名称注册 Repository，并为 SHORT_TERM/DURABLE 分别维护默认 Provider。 */
public final class DefaultIdempotencyRepositoryRegistry implements IdempotencyRepositoryRegistry{
 private final Map<String,IdempotencyRepository> repositories=new LinkedHashMap<>();private final Map<IdempotencyMode,String> defaults=new EnumMap<>(IdempotencyMode.class);
 public DefaultIdempotencyRepositoryRegistry(List<IdempotencyRepository> list,String shortTerm,String durable){if(list!=null)for(IdempotencyRepository r:list){IdempotencyRepository old=repositories.put(r.providerName(),r);if(old!=null)throw new IllegalArgumentException("duplicate idempotency repository: "+r.providerName());}defaults.put(IdempotencyMode.SHORT_TERM,shortTerm);defaults.put(IdempotencyMode.DURABLE,durable);}
 public IdempotencyRepository resolve(IdempotencyMode mode,String requested){String name=requested==null||requested.isBlank()?defaults.get(mode):requested;if(name==null||name.isBlank())throw new IllegalArgumentException("no default repository for mode: "+mode);IdempotencyRepository repo=repositories.get(name);if(repo==null)throw new IllegalArgumentException("idempotency repository not found: "+name);if(!repo.supports(mode))throw new IllegalArgumentException("repository "+name+" does not support "+mode);return repo;}
}
