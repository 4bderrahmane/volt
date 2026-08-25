package com.volt.catalog.infrastructure.adapter.out.persistence.repository;

import com.volt.catalog.infrastructure.adapter.out.persistence.entity.ProductJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository
        extends JpaRepository<ProductJpaEntity, Long>, JpaSpecificationExecutor<ProductJpaEntity> {

    Optional<ProductJpaEntity> findByReference(String reference);

    List<ProductJpaEntity> findAllByIdIn(Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from ProductJpaEntity product "
            + "where product.id in :ids order by product.id")
    List<ProductJpaEntity> lockAllByIds(@Param("ids") Collection<Long> ids);
}
