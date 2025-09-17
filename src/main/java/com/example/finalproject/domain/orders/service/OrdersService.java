package com.example.finalproject.domain.orders.service;

import com.example.finalproject.domain.carts.dto.response.CartsItemResponse;
import com.example.finalproject.domain.carts.dto.response.CartsResponse;
import com.example.finalproject.domain.carts.exception.AccessDeniedException;
import com.example.finalproject.domain.carts.service.CartsService;
import com.example.finalproject.domain.menus.entity.MenuOptionChoices;
import com.example.finalproject.domain.menus.entity.Menus;
import com.example.finalproject.domain.menus.repository.MenuOptionChoicesRepository;
import com.example.finalproject.domain.menus.repository.MenusRepository;
import com.example.finalproject.domain.orders.dto.request.OrdersRequest;
import com.example.finalproject.domain.orders.dto.response.OrderItemsResponse;
import com.example.finalproject.domain.orders.dto.response.OrderOptionsResponse;
import com.example.finalproject.domain.orders.dto.response.OrderStatusResponse;
import com.example.finalproject.domain.orders.dto.response.OrdersResponse;
import com.example.finalproject.domain.orders.entity.OrderItems;
import com.example.finalproject.domain.orders.entity.OrderOptions;
import com.example.finalproject.domain.orders.entity.Orders;
import com.example.finalproject.domain.orders.repository.OrderItemsRepository;
import com.example.finalproject.domain.orders.repository.OrderOptionsRepository;
import com.example.finalproject.domain.orders.repository.OrdersRepository;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdersService {

    private final UsersRepository usersRepository;
    private final StoresRepository storesRepository;
    private final MenusRepository menusRepository;
    private final MenuOptionChoicesRepository menuOptionChoicesRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final OrderOptionsRepository orderOptionsRepository;
    private final CartsService cartsService; // Redis에서 장바구니 조회

    @Transactional
    public OrdersResponse createOrder(Long userId, OrdersRequest request) {

        // User 조회
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // Cart 조회
        CartsResponse cart = cartsService.getCart(userId);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("장바구니가 비어있습니다.");
        }

        // Store 조회
        Stores store = storesRepository.findById(cart.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 가게입니다."));


        // Orders 엔티티 생성
        Orders order = new Orders();
        order.setUser(user);
        order.setStore(store);
        order.setStatus(Orders.Status.WAITING);
        order.setTotalPrice(cart.getCartTotalPrice());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        ordersRepository.save(order);

        // OrderItems, OrderOptions 생성
        for (CartsItemResponse cartItem : cart.getItems()) {
            Menus menu = menusRepository.findById(cartItem.getMenuId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다."));

            OrderItems orderItem = new OrderItems();
            orderItem.setOrder(order);
            orderItem.setMenu(menu);
            orderItem.setQuantity(cartItem.getAmount());
            orderItemsRepository.save(orderItem);

            if (cartItem.getOptions() != null && !cartItem.getOptions().isEmpty()) {
                List<OrderOptions> options = cartItem.getOptions().stream().map(opt -> {
                    MenuOptionChoices choice = menuOptionChoicesRepository.findById(opt.getMenuOptionChoicesId())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옵션 선택입니다."));

                    OrderOptions orderOption = new OrderOptions();
                    orderOption.setOrderItem(orderItem);
                    orderOption.setOptionGroupName(choice.getGroup().getOptionsName());
                    orderOption.setChoiceName(choice.getChoiceName());
                    orderOption.setExtraPrice(choice.getExtraPrice());
                    return orderOption;
                }).collect(Collectors.toList());

                orderOptionsRepository.saveAll(options);
            }
        }
        // 주문 생성 후 장바구니 비우기
        cartsService.clearCart(userId);

        return buildOrderResponse(order);
    }

    @Transactional
    public OrderStatusResponse updateOrderStatus(Long orderId, String statusStr) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        Orders.Status status;
        try {
            status = Orders.Status.valueOf(statusStr.toUpperCase()); // 대소문자 둘다 OK
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 주문 상태입니다: " + statusStr);
        }

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        ordersRepository.save(order);

        OrderStatusResponse resp = new OrderStatusResponse();
        resp.setOrderId(order.getId());
        resp.setStoreId(order.getStore().getId());
        resp.setStatus(order.getStatus().name());
        resp.setUpdatedAt(order.getUpdatedAt());
        resp.setMessage("주문 상태가 변경되었습니다.");
        return resp;
    }

    @Transactional(readOnly = true)
    public OrdersResponse getOrder(Long orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        return buildOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrdersResponse> getOrdersByUser(Long userId, Pageable pageable) {
        List<Orders> orders = ordersRepository.findByUser_Id(userId, pageable);
        return orders.stream().map(this::buildOrderResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrdersResponse> getOrdersByStore(Long storeId, Pageable pageable) {
        List<Orders> orders = ordersRepository.findByStore_Id(storeId, pageable);
        return orders.stream().map(this::buildOrderResponse).collect(Collectors.toList());
    }

    // 주문 삭제
    public void deleteOrder(Long userId, Long orderId) {

        // 주문 조회
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 주문자와 해당 요청을 한 사용자가 일치하는지 확인
        if (!order.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("본인의 주문만 삭제할 수 있습니다.");
        }

        List<OrderItems> items = orderItemsRepository.findByOrder(order);
        // 옵션 삭제
        for (OrderItems item : items) {
            orderOptionsRepository.deleteAll(orderOptionsRepository.findByOrderItem(item));
        }
        // 아이템 삭제
        orderItemsRepository.deleteAll(items);
        // 주문 삭제
        ordersRepository.delete(order);
    }

    // response 조합
    private OrdersResponse buildOrderResponse(Orders order) {
        OrdersResponse response = new OrdersResponse();
        response.setOrderId(order.getId());
        response.setUserId(order.getUser().getId());
        response.setAddress(order.getUser().getAddress());
        response.setStoreId(order.getStore().getId());
        response.setStoreName(order.getStore().getName());
        response.setStatus(order.getStatus().name());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        // 주문 아이템과 옵션 매핑
        List<OrderItemsResponse> items = orderItemsRepository.findByOrder(order).stream()
                .map(item -> {
                    OrderItemsResponse itemResp = new OrderItemsResponse();
                    itemResp.setOrderItemId(item.getId());
                    itemResp.setMenuId(item.getMenu().getId());
                    itemResp.setMenuName(item.getMenu().getName());
                    itemResp.setAmount(item.getQuantity());
                    itemResp.setPrice(item.getMenu().getPrice());

                    List<OrderOptionsResponse> opts = orderOptionsRepository.findByOrderItem(item).stream()
                            .map(opt -> {
                                OrderOptionsResponse optResp = new OrderOptionsResponse();
                                optResp.setOptionGroupName(opt.getOptionGroupName());
                                optResp.setChoiceName(opt.getChoiceName());
                                optResp.setExtraPrice(opt.getExtraPrice());
                                return optResp;
                            }).collect(Collectors.toList());

                    itemResp.setOptions(opts);

                    int optionsPrice = opts.stream()
                            .mapToInt(o -> o.getExtraPrice() != null ? o.getExtraPrice() : 0)
                            .sum();
                    int totalPrice = (item.getMenu().getPrice() + optionsPrice) * item.getQuantity();
                    itemResp.setTotalPrice(totalPrice);

                    return itemResp;
                }).collect(Collectors.toList());

        response.setItems(items);

        // 현재는 쿠폰, 포인트 미사용 <- 나중에 연결 예정
        response.setUsedPoints(0);
        response.setAppliedCoupon(null);

        // 총 금액 계산
        int totalPrice = items.stream().mapToInt(OrderItemsResponse::getTotalPrice).sum();
        response.setOrderTotalPrice(totalPrice);
        response.setTotalPrice(totalPrice);

        return response;
    }

}
