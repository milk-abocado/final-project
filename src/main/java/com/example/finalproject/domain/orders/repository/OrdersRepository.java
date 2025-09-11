package com.example.finalproject.domain.orders.repository;

import com.example.finalproject.domain.orders.entity.Orders;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> {
    List<Orders> findByUser_Id(Long userId, Pageable pageable);
    List<Orders> findByStore_Id(Long storeId, Pageable pageable);
}
