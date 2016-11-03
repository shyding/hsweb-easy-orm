package org.hsweb.ezorm.rdb.render.support.simple;

import org.hsweb.commons.DateTimeUtils;
import org.hsweb.commons.StringUtils;
import org.hsweb.ezorm.rdb.meta.RDBTableMetaData;
import org.hsweb.ezorm.rdb.meta.Correlation;
import org.hsweb.ezorm.rdb.meta.RDBColumnMetaData;
import org.hsweb.ezorm.core.param.Term;
import org.hsweb.ezorm.core.param.TermType;
import org.hsweb.ezorm.rdb.render.dialect.Dialect;
import org.hsweb.ezorm.rdb.render.SqlAppender;

import java.sql.JDBCType;
import java.util.Date;
import java.util.List;
import java.util.Set;

public abstract class SimpleWhereSqlBuilder {

    protected String getTableAlias(RDBTableMetaData metaData, String field) {
        if (field.contains("."))
            field = field.split("[.]")[0];
        else return metaData.getAlias();
        Correlation correlation = metaData.getCorrelation(field);
        if (correlation != null) return correlation.getAlias();
        return metaData.getAlias();
    }

    public void buildWhere(RDBTableMetaData metaData, String prefix,
                           List<Term> terms, SqlAppender appender,
                           Set<String> needSelectTable) {
        if (terms == null || terms.isEmpty()) return;
        int index = -1;
        String prefixTmp = StringUtils.concat(prefix, StringUtils.isNullOrEmpty(prefix) ? "" : ".");
        for (Term term : terms) {
            index++;
            boolean nullTerm = StringUtils.isNullOrEmpty(term.getColumn());
            RDBColumnMetaData field = metaData.findColumn(term.getColumn());
            //不是空条件 也不是可选字段
            if (!nullTerm && field == null && term.getTermType() != TermType.func) continue;
            //不是空条件，值为空
            if (!nullTerm && StringUtils.isNullOrEmpty(term.getValue())) continue;
            //是空条件，但是无嵌套
            if (nullTerm && term.getTerms().isEmpty()) continue;
            String tableAlias = null;
            if (field != null) {
                tableAlias = getTableAlias(metaData, term.getColumn());
                needSelectTable.add(tableAlias);
                //转换参数的值
                term.setValue(transformationValue(field.getJdbcType(), term.getValue(), term.getTermType()));
            }
            //用于sql预编译的参数名
            prefix = StringUtils.concat(prefixTmp, "terms[", index, "]");
            //添加类型，and 或者 or
            appender.add(StringUtils.concat(" ", term.getType().toString().toUpperCase(), " "));
            if (!term.getTerms().isEmpty()) {
                //构建嵌套的条件
                SqlAppender nest = new SqlAppender();
                buildWhere(metaData, prefix, term.getTerms(), nest, needSelectTable);
                //如果嵌套结果为空,
                if (nest.isEmpty()) {
                    appender.removeLast();//删除最后一个（and 或者 or）
                    continue;
                }
                if (nullTerm) {
                    //删除 第一个（and 或者 or）
                    nest.removeFirst();
                }
                appender.add("(");
                if (!nullTerm)
                    appender.add(getDialect().buildCondition(prefix, term, field, tableAlias));
                appender.addAll(nest);
                appender.add(")");
            } else {
                if (!nullTerm)
                    appender.add(getDialect().buildCondition(prefix, term, field, tableAlias));
            }
        }
    }

    protected Object transformationValue(JDBCType type, Object value, String termType) {
        if (type == null) return value;
        switch (type) {
            case INTEGER:
            case NUMERIC:
                if (StringUtils.isInt(type)) return StringUtils.toInt(value);
                if (StringUtils.isDouble(type)) return StringUtils.toDouble(value);
                break;
            case TIMESTAMP:
            case TIME:
            case DATE:
                if (!(value instanceof Date)) {
                    String strValue = String.valueOf(value);
                    Date date = DateTimeUtils.formatUnknownString2Date(strValue);
                    if (date != null) return date;
                }
                break;
        }
        return value;
    }

    public abstract Dialect getDialect();
}
