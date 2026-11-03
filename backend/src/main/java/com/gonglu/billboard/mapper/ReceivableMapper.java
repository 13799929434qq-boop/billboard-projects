package com.gonglu.billboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gonglu.billboard.entity.Receivable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReceivableMapper extends BaseMapper<Receivable> {

    /** 应收汇总 */
    @Select("SELECT IFNULL(SUM(amount),0) AS totalAmount, IFNULL(SUM(received),0) AS totalReceived, " +
            "IFNULL(SUM(amount - received),0) AS totalArrears FROM biz_receivable")
    Map<String, Object> sumAll();

    /** 应收款统计表：按商家汇总欠款 */
    @Select("SELECT m.id AS merchantId, m.name AS merchantName, m.contact, m.phone, " +
            "IFNULL(SUM(r.amount),0) AS amount, IFNULL(SUM(r.received),0) AS received, " +
            "IFNULL(SUM(r.amount - r.received),0) AS arrears " +
            "FROM biz_receivable r JOIN biz_merchant m ON m.id = r.merchant_id " +
            "GROUP BY m.id, m.name, m.contact, m.phone ORDER BY arrears DESC")
    List<Map<String, Object>> groupByMerchant();

    /** 催收清单：未收齐的应收明细 */
    @Select("SELECT r.id, r.year, r.amount, r.received, (r.amount - r.received) AS arrears, " +
            "m.name AS merchantName, m.contact, m.phone, c.code AS contractCode, b.code AS billboardCode, s.name AS sectionName " +
            "FROM biz_receivable r " +
            "JOIN biz_merchant m ON m.id = r.merchant_id " +
            "JOIN biz_contract c ON c.id = r.contract_id " +
            "JOIN biz_billboard b ON b.id = r.billboard_id " +
            "LEFT JOIN biz_section s ON s.id = b.section_id " +
            "WHERE r.amount > r.received ORDER BY r.year, m.id")
    List<Map<String, Object>> collectionNoticeList();

    /** 合同执行：合同应收/已收汇总 */
    @Select("SELECT contract_id AS contractId, IFNULL(SUM(amount),0) AS amount, IFNULL(SUM(received),0) AS received " +
            "FROM biz_receivable GROUP BY contract_id")
    List<Map<String, Object>> sumByContract();

    /** 行锁查询（收款冲账并发保护） */
    @Select("SELECT * FROM biz_receivable WHERE id = #{id} FOR UPDATE")
    Receivable selectByIdForUpdate(@Param("id") Long id);

    @Update("UPDATE biz_receivable SET received = received + #{delta}, " +
            "status = CASE WHEN received + #{delta} >= amount THEN '已收' ELSE '部分缴纳' END " +
            "WHERE id = #{id}")
    int addReceived(@Param("id") Long id, @Param("delta") java.math.BigDecimal delta);
}
