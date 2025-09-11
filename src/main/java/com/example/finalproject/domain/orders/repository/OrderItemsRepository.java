package com.example.finalproject.domain.orders.repository;

import com.example.finalproject.domain.orders.entity.OrderItems;
import com.example.finalproject.domain.orders.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {
    List<OrderItems> findByOrder(Orders order);
}
