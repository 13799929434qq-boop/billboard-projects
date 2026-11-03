package com.gonglu.billboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gonglu.billboard.entity.Billboard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface BillboardMapper extends BaseMapper<Billboard> {

    /** 广告牌数量表：按路段分组 */
    @Select("SELECT s.name AS name, COUNT(b.id) AS count " +
            "FROM biz_section s LEFT JOIN biz_billboard b ON b.section_id = s.id " +
            "GROUP BY s.id, s.name ORDER BY s.id")
    List<Map<String, Object>> countBySection();

    @Select("SELECT IFNULL(ownership,'未填写') AS name, COUNT(*) AS count FROM biz_billboard GROUP BY ownership ORDER BY count DESC")
    List<Map<String, Object>> countByOwnership();

    @Select("SELECT IFNULL(land_nature,'未填写') AS name, COUNT(*) AS count FROM biz_billboard GROUP BY land_nature ORDER BY count DESC")
    List<Map<String, Object>> countByLandNature();

    @Select("SELECT status AS name, COUNT(*) AS count FROM biz_billboard GROUP BY status ORDER BY count DESC")
    List<Map<String, Object>> countByStatus();
}
