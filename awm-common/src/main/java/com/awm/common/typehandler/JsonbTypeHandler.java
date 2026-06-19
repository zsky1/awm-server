package com.awm.common.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.*;

/**
 * PostgreSQL JSONB 类型处理器。
 * 将 Java String（JSON 字符串）与 PG JSONB 列互相转换。
 *
 * 用法：
 * 1. 在 Entity 字段上添加 @TableField(typeHandler = JsonbTypeHandler.class)
 * 2. 在 @TableName 注解上添加 autoResultMap = true
 */
public class JsonbTypeHandler extends BaseTypeHandler<String> {

    private static final String TYPE_JSONB = "jsonb";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        PGobject pGobject = new PGobject();
        pGobject.setType(TYPE_JSONB);
        pGobject.setValue(parameter);
        ps.setObject(i, pGobject);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }
}
