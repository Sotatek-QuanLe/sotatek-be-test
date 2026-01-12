package com.sotatek.order.repository;

import com.sotatek.order.model.entity.Order;
import com.sotatek.order.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByMemberId(String memberId);

    Page<Order> findByMemberId(String memberId, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByMemberIdAndStatus(String memberId, OrderStatus status);

    /**
     * Find order by ID with pessimistic write lock to prevent concurrent
     * modifications.
     * Use this method when you need to ensure exclusive access to the order.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    java.util.Optional<Order> findByIdWithLock(@Param("id") Long id);
}
