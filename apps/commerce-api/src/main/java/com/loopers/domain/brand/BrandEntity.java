package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * @author hyunjikoh
 * @since 2025. 11. 8.
 */
@Entity
@Table(name = "brands", indexes = {
        @Index(name = "idx_brand_name", columnList = "name")
})
@Getter
@NoArgsConstructor
public class BrandEntity extends BaseEntity {
    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;


}
