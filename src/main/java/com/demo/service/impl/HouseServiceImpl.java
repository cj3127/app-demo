package com.demo.service.impl;

import com.demo.entity.House;
import com.demo.repository.HouseRepository;
import com.demo.service.HouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * House 业务实现（含 Redis 缓存）
 */
@Service
@RequiredArgsConstructor
public class HouseServiceImpl implements HouseService {

    private final HouseRepository houseRepository;

    @Override
    @Transactional
    @CacheEvict(value = "houseList", allEntries = true) // 新增后清空列表缓存
    public House save(House house) {
        return houseRepository.save(house);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"house", "houseList"}, allEntries = true) // 清空相关缓存
    public void deleteById(Long id) {
        if (!houseRepository.existsById(id)) {
            throw new RuntimeException("房屋不存在：ID=" + id);
        }
        houseRepository.deleteById(id);
    }

    @Override
    @Transactional
    @CachePut(value = "house", key = "#house.id") // 更新缓存
    @CacheEvict(value = "houseList", allEntries = true) // 清空列表缓存
    public House update(House house) {
        if (!houseRepository.existsById(house.getId())) {
            throw new RuntimeException("房屋不存在：ID=" + house.getId());
        }
        return houseRepository.save(house);
    }

    @Override
    @Cacheable(value = "house", key = "#id", unless = "#result == null") // 缓存单个房屋
    public Optional<House> getById(Long id) {
        return houseRepository.findById(id);
    }

    @Override
    @Cacheable(value = "houseList", key = "'all'", unless = "#result.isEmpty()") // 缓存房屋列表
    public List<House> listAll() {
        return houseRepository.findAll();
    }
}
