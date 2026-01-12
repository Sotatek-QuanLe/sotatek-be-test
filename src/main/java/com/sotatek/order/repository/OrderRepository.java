package com.sotatek.order.repository;

import com.sotatek.order.model.entity.Order;
import com.sotatek.order.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByMemberId(String memberId);

    Page<Order> findByMemberId(String memberId, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByMemberIdAndStatus(String memberId, OrderStatus status);
}
