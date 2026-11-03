package com.gonglu.billboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gonglu.billboard.entity.FundTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface FundTransactionMapper extends BaseMapper<FundTransaction> {

    /** 收款情况表：合计（仅收入） */
    @Select("<script>" +
            "SELECT COUNT(*) AS totalCount, IFNULL(SUM(amount),0) AS totalAmount FROM biz_transaction " +
            "WHERE direction = '收入' " +
            "<if test='from != null'> AND tx_date &gt;= #{from} </if>" +
            "<if test='to != null'> AND tx_date &lt;= #{to} </if>" +
            "</script>")
    Map<String, Object> sumIncome(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 收款情况表：按商家分布 */
    @Select("<script>" +
            "SELECT IFNULL(m.name, '未指定商家') AS name, COUNT(*) AS count, IFNULL(SUM(t.amount),0) AS amount " +
            "FROM biz_transaction t LEFT JOIN biz_merchant m ON m.id = t.merchant_id " +
            "WHERE t.direction = '收入' " +
            "<if test='from != null'> AND t.tx_date &gt;= #{from} </if>" +
            "<if test='to != null'> AND t.tx_date &lt;= #{to} </if>" +
            "GROUP BY m.name ORDER BY amount DESC" +
            "</script>")
    List<Map<String, Object>> incomeByMerchant(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 收款情况表：按类别分布 */
    @Select("<script>" +
            "SELECT category AS name, COUNT(*) AS count, IFNULL(SUM(amount),0) AS amount " +
            "FROM biz_transaction t WHERE t.direction = '收入' " +
            "<if test='from != null'> AND t.tx_date &gt;= #{from} </if>" +
            "<if test='to != null'> AND t.tx_date &lt;= #{to} </if>" +
            "GROUP BY category ORDER BY amount DESC" +
            "</script>")
    List<Map<String, Object>> incomeByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 收款情况表：按合同分布 */
    @Select("<script>" +
            "SELECT IFNULL(c.code, '未关联合同') AS name, COUNT(*) AS count, IFNULL(SUM(t.amount),0) AS amount " +
            "FROM biz_transaction t LEFT JOIN biz_contract c ON c.id = t.contract_id " +
            "WHERE t.direction = '收入' " +
            "<if test='from != null'> AND t.tx_date &gt;= #{from} </if>" +
            "<if test='to != null'> AND t.tx_date &lt;= #{to} </if>" +
            "GROUP BY c.code ORDER BY amount DESC" +
            "</script>")
    List<Map<String, Object>> incomeByContract(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
