package com.demo.service;

import com.demo.entity.House;

import java.util.List;
import java.util.Optional;

/**
 * House 业务接口
 */
public interface HouseService {

    // 新增房屋
    House save(House house);

    // 根据 ID 删除房屋
    void deleteById(Long id);

    // 根据 ID 更新房屋
    House update(House house);

    // 根据 ID 查询房屋（缓存）
    Optional<House> getById(Long id);

    // 查询所有房屋（缓存）
    List<House> listAll();
}
