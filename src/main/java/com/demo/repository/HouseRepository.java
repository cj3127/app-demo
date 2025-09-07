package com.demo.repository;

import com.demo.entity.House;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * House 数据访问层（JPA 自动实现 CRUD 方法）
 */
@Repository
public interface HouseRepository extends JpaRepository<House, Long> {
    // 可根据需求添加自定义查询方法，例如：
    // List<House> findByDistrict(String district); // 按行政区查询
}
