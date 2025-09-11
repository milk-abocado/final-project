package com.example.finalproject.domain.orders.repository;

import com.example.finalproject.domain.orders.entity.OrderItems;
import com.example.finalproject.domain.orders.entity.OrderOptions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderOptionsRepository extends JpaRepository<OrderOptions, Long> {
    List<OrderOptions> findByOrderItem(OrderItems orderItemId);
}
