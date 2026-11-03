package com.gonglu.billboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gonglu.billboard.common.BizException;
import com.gonglu.billboard.entity.SysDict;
import com.gonglu.billboard.entity.SysDictItem;
import com.gonglu.billboard.mapper.SysDictItemMapper;
import com.gonglu.billboard.mapper.SysDictMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
public class DictService extends ServiceImpl<SysDictMapper, SysDict> {

    private static final Set<String> ARCHIVE_TYPES = Set.of("ownership", "landNature", "spec");

    private final SysDictItemMapper itemMapper;
    private final LogService logService;

    public DictService(SysDictItemMapper itemMapper, LogService logService) {
        this.itemMapper = itemMapper;
        this.logService = logService;
    }

    /** 全部字典（含字典项，按 sort 排序） */
    public List<SysDict> listAll() {
        List<SysDict> dicts = list(Wrappers.<SysDict>lambdaQuery().orderByAsc(SysDict::getId));
        List<SysDictItem> items = itemMapper.selectList(
                Wrappers.<SysDictItem>lambdaQuery().orderByAsc(SysDictItem::getSort).orderByAsc(SysDictItem::getId));
        for (SysDict dict : dicts) {
            dict.setItems(items.stream().filter(i -> i.getDictType().equals(dict.getType())).toList());
        }
        return dicts;
    }

    /** 档案员只看到与广告牌档案有关的选项，管理员可维护全部字典。 */
    public List<SysDict> listForRole(String role) {
        List<SysDict> dicts = listAll();
        if ("archivist".equals(role)) {
            return dicts.stream().filter(dict -> ARCHIVE_TYPES.contains(dict.getType())).toList();
        }
        return dicts;
    }

    public SysDictItem addItem(String type, String label, String role) {
        assertEditable(type, role);
        if (!StringUtils.hasText(label)) {
            throw new BizException("选项名称不能为空");
        }
        SysDict dict = getOne(Wrappers.<SysDict>lambdaQuery().eq(SysDict::getType, type));
        if (dict == null) {
            throw new BizException("字典类型不存在");
        }
        Long exists = itemMapper.selectCount(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getDictType, type).eq(SysDictItem::getLabel, label));
        if (exists > 0) {
            throw new BizException("选项「" + label + "」已存在");
        }
        Integer maxSort = itemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                        .eq(SysDictItem::getDictType, type).orderByDesc(SysDictItem::getSort).last("LIMIT 1"))
                .stream().findFirst().map(SysDictItem::getSort).orElse(0);
        SysDictItem item = new SysDictItem();
        item.setDictType(type);
        item.setLabel(label);
        item.setSort(maxSort + 1);
        itemMapper.insert(item);
        logService.add("新增", "新增数据字典项：" + dict.getName() + "/" + label);
        return item;
    }

    public void deleteItem(Long id, String role) {
        SysDictItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BizException("字典项不存在");
        }
        assertEditable(item.getDictType(), role);
        itemMapper.deleteById(id);
        logService.add("删除", "删除数据字典项：" + item.getDictType() + "/" + item.getLabel());
    }

    public SysDictItem updateItem(Long id, SysDictItem input) {
        SysDictItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BizException("字典项不存在");
        }
        if (!StringUtils.hasText(input.getLabel())) {
            throw new BizException("选项名称不能为空");
        }
        Long exists = itemMapper.selectCount(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getDictType, item.getDictType())
                .eq(SysDictItem::getLabel, input.getLabel())
                .ne(SysDictItem::getId, id));
        if (exists > 0) {
            throw new BizException("选项「" + input.getLabel() + "」已存在");
        }
        item.setLabel(input.getLabel().trim());
        item.setSort(input.getSort() != null ? input.getSort() : item.getSort());
        itemMapper.updateById(item);
        logService.add("修改", "修改数据字典项：" + item.getDictType() + "/" + item.getLabel());
        return item;
    }

    private void assertEditable(String type, String role) {
        if ("archivist".equals(role) && !ARCHIVE_TYPES.contains(type)) {
            throw new BizException("档案员只能维护产权、用地性质和规格选项");
        }
    }
}
