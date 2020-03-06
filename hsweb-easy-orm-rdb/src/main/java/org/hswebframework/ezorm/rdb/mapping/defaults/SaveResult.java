package org.hswebframework.ezorm.rdb.mapping.defaults;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * upsert保存结果.
 * <p>
 * 注意: added和updated的值并不一定准确,因为有的数据库可直接执行upsert SQL,无法获取是被新增还是被修改了.
 *
 * @author zhouhao
 * @version 4.0
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
public class SaveResult {
    private int added;

    private int updated;

    public int getTotal() {
        return added + updated;
    }

    public SaveResult merge(SaveResult result) {
        SaveResult res = SaveResult.of(added, updated);

        res.added += result.getAdded();
        res.updated += result.getUpdated();

        return res;
    }

    @Override
    public String toString() {
        return "added " + added + ",updated " + updated + ",total " + getTotal();
    }
}
