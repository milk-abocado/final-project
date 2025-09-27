package com.example.finalproject.domain.orders.service;

import com.example.finalproject.domain.carts.dto.response.CartsItemResponse;
import com.example.finalproject.domain.carts.dto.response.CartsResponse;
import com.example.finalproject.domain.carts.exception.AccessDeniedException;
import com.example.finalproject.domain.carts.service.CartsService;
import com.example.finalproject.domain.coupons.entity.CouponType;
import com.example.finalproject.domain.coupons.entity.Coupons;
import com.example.finalproject.domain.coupons.repository.CouponsRepository;
import com.example.finalproject.domain.coupons.service.CouponsService;
import com.example.finalproject.domain.menus.entity.MenuOptionChoices;
import com.example.finalproject.domain.menus.entity.Menus;
import com.example.finalproject.domain.menus.repository.MenuOptionChoicesRepository;
import com.example.finalproject.domain.menus.repository.MenusRepository;
import com.example.finalproject.domain.orders.dto.request.OrdersRequest;
import com.example.finalproject.domain.orders.dto.response.*;
import com.example.finalproject.domain.orders.entity.OrderItems;
import com.example.finalproject.domain.orders.entity.OrderOptions;
import com.example.finalproject.domain.orders.entity.Orders;
import com.example.finalproject.domain.orders.repository.OrderItemsRepository;
import com.example.finalproject.domain.orders.repository.OrderOptionsRepository;
import com.example.finalproject.domain.orders.repository.OrdersRepository;
import com.example.finalproject.domain.points.dto.PointsDtos;
import com.example.finalproject.domain.points.service.PointsService;
import com.example.finalproject.domain.slack.service.SlackService;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import com.example.finalproject.domain.orders.util.OrderSlackMessage;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
    private final SlackService slackService;
    private final CouponsRepository couponsRepository;
    private final CouponsService couponsService;
    private final PointsService pointsService;

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

        // 가게 영업 여부(active)
        if (!store.isActive()) {
            throw new IllegalArgumentException("현재 주문할 수 없는 가게입니다.");
        }

        // 영업 시간 확인
        LocalTime now = LocalTime.now();
        LocalTime opensAt = store.getOpensAt();
        LocalTime closesAt = store.getClosesAt();
        boolean isOpen;
        if (opensAt.isBefore(closesAt)) {
            isOpen = !now.isBefore(opensAt) && !now.isAfter(closesAt);
        }
        else {
            // 영업 시간이 자정 넘어가는 경우
            isOpen = !now.isBefore(opensAt) || !now.isAfter(closesAt);
        }

        if (!isOpen) {
            throw new IllegalArgumentException("현재 영업 시간이 아닙니다.");
        }

        // 기본 가격
        int totalPrice = cart.getCartTotalPrice();

        // 최소 주문 금액 확인
        if (totalPrice < store.getMinOrderPrice()) {
            throw new IllegalArgumentException("최소 주문 금액(" + store.getMinOrderPrice() + "원) 이상 주문해야 합니다.");
        }

        // Orders 엔티티 생성
        Orders order = new Orders();
        order.setUser(user);
        order.setStore(store);
        order.setStatus(Orders.Status.WAITING);
        order.setTotalPrice(totalPrice);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        ordersRepository.save(order);

        // 쿠폰 적용
        if (request.getUsedCouponId() != null) {
            Coupons coupon = couponsRepository.findById(request.getUsedCouponId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

            int discount = 0;
            if (coupon.getType() == CouponType.RATE) {
                discount = (int) (totalPrice * (coupon.getDiscountValue() / 100.0));
                if (coupon.getMaxDiscount() != null) {
                    discount = Math.min(discount, coupon.getMaxDiscount());
                }
            } else if (coupon.getType() == CouponType.AMOUNT) {
                discount = coupon.getDiscountValue();
            }

            totalPrice -= discount;
            if (totalPrice < 0) totalPrice = 0;

            // 쿠폰 사용 처리
            couponsService.useCoupon(userId, coupon.getCode(), order.getId());
            order.setAppliedCoupon(coupon);
        }

        // 포인트 사용
        if (request.getUsedPoints() != null && request.getUsedPoints() > 0) {
            PointsDtos.UseRequest useRequest = new PointsDtos.UseRequest();
            useRequest.setUserId(userId);
            useRequest.setOrderId(order.getId());
            useRequest.setAmount(request.getUsedPoints());

            pointsService.usePoints(user, useRequest);

            totalPrice -= request.getUsedPoints();
            if (totalPrice < 0) totalPrice = 0;

            order.setUsedPoints(request.getUsedPoints());
        }

        // 배달비 추가
        totalPrice += store.getDeliveryFee();

        // 최종 가격 업데이트
        order.setTotalPrice(totalPrice);
        ordersRepository.save(order);

        // OrderItems, OrderOptions 생성
        for (CartsItemResponse cartItem : cart.getItems()) {
            Menus menu = menusRepository.findById(cartItem.getMenuId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다."));

            // 메뉴 상태 체크
            if (menu.getStatus() != Menus.MenuStatus.ACTIVE) {
                throw new IllegalStateException("해당 메뉴("+menu.getName()+")는 주문할 수 없습니다.");
            }

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

        // 사장님 채널로 새 주문 알림
        slackService.sendOwnerMessage("[사장님 알림] 새 주문이 들어왔습니다.️");
        // 사용자 채널로 주문 대기 알림
        slackService.sendUserMessage(OrderSlackMessage.of("WAITING"));

        return buildOrderResponse(order);
    }

    @Transactional
    public OrderStatusResponse updateOrderStatus(Authentication authentication, Long orderId, String statusStr) {

        // 로그인한 사용자의 userId 가져오기
        Long userId = Long.valueOf(
                ((Map<String, Object>) authentication.getDetails()).get("uid").toString()
        );

        // 주문 확인
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 상태 확인
        Orders.Status status = Arrays.stream(Orders.Status.values())
                .filter(s -> s.name().equalsIgnoreCase(statusStr))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 주문 상태입니다."));

        if (!order.getStatus().canTransitionTo(status)) {
            throw new IllegalArgumentException("주문 상태 '" + order.getStatus() + "' → '" + status + "' 전환은 허용되지 않습니다.");
        }

        // OWNER 권한 여부 확인
        boolean isOwner = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .anyMatch(r -> r.equalsIgnoreCase("OWNER"));

        // USER 권한 여부 확인
        boolean isUser = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .anyMatch(r -> r.equalsIgnoreCase("USER"));

        // OWNER: 가게 주인인지 확인
        if (isOwner) {
            Long storeOwnerId = order.getStore().getOwner().getId(); // Stores 엔티티에 owner 필요
            if (!storeOwnerId.equals(userId)) {
                throw new AccessDeniedException("이 가게의 OWNER만 접근할 수 있습니다.");
            }
            if (status.equals(Orders.Status.CANCELED)) {
                throw new AccessDeniedException("주문 상태 'CANCELED'는 OWNER가 직접 변경할 수 없습니다. 고객센터에 문의하세요.");
            }
        }
        // USER: 주문자 본인인지 확인
        else if (isUser) {
            if (!order.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("본인 주문만 접근할 수 있습니다.");
            }
            if (status.equals(Orders.Status.CANCELED)) {
                throw new AccessDeniedException("주문 상태 'CANCELED'는 USER가 직접 변경할 수 없습니다. 고객센터에 문의하세요.");
            }
            else if (!status.equals(Orders.Status.REJECTED)) {
                throw new AccessDeniedException("주문 상태 '"+status+"'는 USER가 변경할 수 없습니다.");
            }
        }

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        ordersRepository.save(order);

        // 사용자 채널로 상태별 알림
        String slackMsg = OrderSlackMessage.of(status.name());
        slackService.sendUserMessage(slackMsg);

        // 상태 : COMPLETED(배달 완료)시 사장님도 배달 완료 알림 받도록 구현
        if (status == Orders.Status.COMPLETED) {

            // 쿠폰을 적용하지 않은 주문만 포인트 적립이 되도록 설정
            if (order.getAppliedCoupon() == null) {
                Users user = order.getUser();

                // 포인트 적립 (주문 금액의 5%)
                int earnedPoints = (int) (order.getTotalPrice() * 0.05);

                PointsDtos.EarnRequest earnRequest = new PointsDtos.EarnRequest();
                earnRequest.setAmount(earnedPoints);
                earnRequest.setReason("주문 완료 포인트 적립");

                pointsService.earnPoints(user, earnRequest);
            }
            // 사장님 알림
            slackService.sendOwnerMessage("[사장님 알림] 배달이 완료되었습니다.");
        }

        OrderStatusResponse resp = new OrderStatusResponse();
        resp.setOrderId(order.getId());
        resp.setStoreId(order.getStore().getId());
        resp.setStatus(order.getStatus().name());
        resp.setUpdatedAt(order.getUpdatedAt());
        resp.setMessage("주문 상태가 변경되었습니다.");
        return resp;
    }

    @Transactional(readOnly = true)
    public OrdersResponse getOrder(Authentication authentication, Long orderId) {

        // 로그인한 사용자의 userId 가져오기
        Long userId = Long.valueOf(
                ((Map<String, Object>) authentication.getDetails()).get("uid").toString()
        );

        // 주문 확인
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // OWNER 권한 여부 확인
        boolean isOwner = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .anyMatch(r -> r.equalsIgnoreCase("OWNER"));

        // USER 권한 여부 확인
        boolean isUser = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .anyMatch(r -> r.equalsIgnoreCase("USER"));

        // OWNER: 가게 주인인지 확인
        if (isOwner) {
            Long storeOwnerId = order.getStore().getOwner().getId(); // Stores 엔티티에 owner 필요
            if (!storeOwnerId.equals(userId)) {
                throw new AccessDeniedException("이 가게의 OWNER만 접근할 수 있습니다.");
            }
        }
        // USER: 주문자 본인인지 확인
        else if (isUser) {
            if (!order.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("본인 주문만 접근할 수 있습니다.");
            }
        }

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

        // 쿠폰, 포인트 적용 전 가격
        int totalPrice = items.stream().mapToInt(OrderItemsResponse::getTotalPrice).sum();
        response.setOrderTotalPrice(totalPrice);

        // 쿠폰, 포인트
        response.setUsedPoints(order.getUsedPoints() != null ? order.getUsedPoints() : 0);
        if (order.getAppliedCoupon() != null) {
            Coupons coupon = order.getAppliedCoupon();
            OrderCouponsResponse couponResp = new OrderCouponsResponse();
            couponResp.setCouponId(coupon.getId());
            couponResp.setCode(coupon.getCode());

            // 할인 금액
            int discount = 0;
            if (coupon.getType() == CouponType.RATE) {
                discount = (int) (response.getOrderTotalPrice() * (coupon.getDiscountValue() / 100.0));
                if (coupon.getMaxDiscount() != null) {
                    discount = Math.min(discount, coupon.getMaxDiscount());
                }
            } else if (coupon.getType() == CouponType.AMOUNT) {
                discount = coupon.getDiscountValue();
            }

            couponResp.setDiscountAmount(discount); // 조회용 할인 금액 세팅
            response.setAppliedCoupon(couponResp);
        }
        else {
            response.setAppliedCoupon(null);
        }

        // 쿠폰, 포인트 적용 후 가격
        int finalTotal = order.getTotalPrice();
        response.setTotalPrice(finalTotal);

        return response;
    }
}
