package com.demo.controller;

import com.demo.entity.House;
import com.demo.service.HouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 房屋管理接口
 * 访问地址：http://ip:8080/api/houses
 */
@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
@Tag(name = "房屋管理接口", description = "提供房屋 CRUD 操作")
public class HouseController {

    private final HouseService houseService;

    /**
     * 新增房屋
     */
    @PostMapping
    @Operation(summary = "新增房屋", description = "传入房屋信息，创建新记录")
    public ResponseEntity<House> save(
            @Parameter(description = "房屋信息（id 无需传入）") @RequestBody House house) {
        House savedHouse = houseService.save(house);
        return new ResponseEntity<>(savedHouse, HttpStatus.CREATED);
    }

    /**
     * 根据 ID 删除房屋
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除房屋", description = "根据 ID 删除房屋记录")
    public ResponseEntity<Void> deleteById(
            @Parameter(description = "房屋 ID") @PathVariable Long id) {
        houseService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 更新房屋信息
     */
    @PutMapping
    @Operation(summary = "更新房屋", description = "传入完整房屋信息（含 id）")
    public ResponseEntity<House> update(
            @Parameter(description = "房屋信息（id 必须传入）") @RequestBody House house) {
        House updatedHouse = houseService.update(house);
        return ResponseEntity.ok(updatedHouse);
    }

    /**
     * 根据 ID 查询房屋
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询单个房屋", description = "根据 ID 查询房屋详情")
    public ResponseEntity<House> getById(
            @Parameter(description = "房屋 ID") @PathVariable Long id) {
        Optional<House> house = houseService.getById(id);
        return house.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 查询所有房屋
     */
    @GetMapping
    @Operation(summary = "查询所有房屋", description = "返回所有房屋列表（带缓存）")
    public ResponseEntity<List<House>> listAll() {
        List<House> houses = houseService.listAll();
        return ResponseEntity.ok(houses);
    }
}
