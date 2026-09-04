package com.xjtu.iron.relational.spi;

/**
 * 关系型数据库方言扩展点。
 *
 * <p>V1 只建立契约，不在这里构建 SQL AST、分页解析器或 ORM。真正出现数据库差异时，
 * 再由具体 dialect 实现承担最小必要差异。</p>
 */
public interface RelationalDialect {

    String name();

    boolean supportsGeneratedKeys();
}
