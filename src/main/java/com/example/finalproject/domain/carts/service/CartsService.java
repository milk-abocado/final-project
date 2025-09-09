package com.example.finalproject.domain.carts.service;

import com.example.finalproject.domain.carts.dto.request.CartsItemRequest;
import com.example.finalproject.domain.carts.dto.request.CartsOptionRequest;
import com.example.finalproject.domain.carts.dto.response.CartsItemResponse;
import com.example.finalproject.domain.carts.dto.response.CartsOptionResponse;
import com.example.finalproject.domain.carts.dto.response.CartsResponse;
import com.example.finalproject.domain.carts.repository.CartsRepository;
import com.example.finalproject.domain.menus.entity.MenuOptionChoices;
import com.example.finalproject.domain.menus.entity.MenuOptions;
import com.example.finalproject.domain.menus.entity.Menus;
import com.example.finalproject.domain.menus.repository.MenuOptionChoicesRepository;
import com.example.finalproject.domain.menus.repository.MenuOptionsRepository;
import com.example.finalproject.domain.menus.repository.MenusRepository;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartsService {
    private final CartsRepository cartsRepository; // Redis
    private final StoresRepository storeRepository; // DB
    private final MenusRepository menusRepository; // DB
    private final MenuOptionChoicesRepository menuOptionChoicesRepository; // DB
    private final MenuOptionsRepository menuOptionsRepository;

    // 옵션 체크
    private List<CartsOptionResponse> validateAndBuildOptions(
            List<CartsOptionRequest> cartsOptionRequest,
            List<MenuOptions> optionGroups,
            MenuOptionChoicesRepository menuOptionChoicesRepository) {

        // 메뉴에 맞는 유효한 옵션 확인
        Map<Long, MenuOptionChoices> validChoices = new HashMap<>();
        for (MenuOptions group : optionGroups) {
            List<MenuOptionChoices> choices = menuOptionChoicesRepository.findByGroup_Id(group.getId());
            for (MenuOptionChoices choice : choices) {
                validChoices.put(choice.getId(), choice);
            }
        }

        // request 옵션 체크
        List<CartsOptionResponse> options = new ArrayList<>();
        if (cartsOptionRequest != null) {
            for (var optReq : cartsOptionRequest) {
                MenuOptionChoices choice = validChoices.get(optReq.getMenuOptionChoicesId());
                if (choice == null) {
                    throw new IllegalArgumentException("잘못된 옵션 선택입니다: " + optReq.getMenuOptionChoicesId());
                }
                options.add(new CartsOptionResponse(choice.getId(), choice.getChoiceName(), choice.getExtraPrice()));
            }
        }

        for (MenuOptions group : optionGroups) {
            long selectedCount = options.stream()
                    .filter(opt -> validChoices.get(opt.getMenuOptionChoicesId()).getGroup().getId().equals(group.getId()))
                    .count();

            // 필수 옵션 + 최소 선택 개수 체크
            if (Boolean.TRUE.equals(group.getIsRequired()) && selectedCount < (group.getMinSelect() != null ? group.getMinSelect() : 1)) {
                throw new IllegalArgumentException("필수 옵션 (" + group.getOptionsName() + ")을 최소" + group.getMinSelect() + "개 선택해주세요.");
            }

            // 최대 선택 체크
            if (group.getMaxSelect() != null && selectedCount > group.getMaxSelect()) {
                throw new IllegalArgumentException("옵션 (" + group.getOptionsName() + ")은 최대 " + group.getMaxSelect() + "개까지만 선택할 수 있습니다.");
            }
        }

        return options;
    }


    public CartsResponse getCart(Long userId){
        CartsResponse cart = cartsRepository.getCart(userId);

        if(cart == null){
            cart = new CartsResponse();
            cart.setUserId(userId);
            cart.setUpdatedAt(LocalDateTime.now());
            return cart;
        }

        // 장바구니에 아이템이 있고 storeId가 없으면 메뉴에서 가져오기
        if ((cart.getStoreId() == null || cart.getStoreName() == null) && !cart.getItems().isEmpty()) {
            Long firstMenuId = cart.getItems().get(0).getMenuId();
            var optionalMenu = menusRepository.findById(firstMenuId);
            if (optionalMenu.isPresent()) {
                Menus menu = optionalMenu.get();
                Stores store = menu.getStore();
                if (store != null) {
                    cart.setStoreId(store.getId());
                    cart.setStoreName(store.getName());
                }
            }
        }

        // 이미 storeId가 있으면 storeName 채우기
        else if (cart.getStoreId() != null && cart.getStoreName() == null) {
            var optionalStore = storeRepository.findById(cart.getStoreId());
            if (optionalStore.isPresent()) {
                Stores store = optionalStore.get();
                cart.setStoreName(store.getName());
            }
        }

        int cartTotalPrice = cart.getItems().stream()
                .mapToInt(CartsItemResponse::getTotalPrice)
                .sum();
        cart.setCartTotalPrice(cartTotalPrice);
        cart.setUpdatedAt(LocalDateTime.now());
        return cart;
    }

    public CartsItemResponse addCartItem(Long userId, CartsItemRequest cartsItemRequest){

        // 수량 체크
        if (cartsItemRequest.getAmount() <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }

        // 메뉴 조회
        Menus menu = menusRepository.findById(cartsItemRequest.getMenuId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다."));

        // 해당 메뉴로 storeId 가져오기
        Long storeId = menu.getStore().getId();
        String storeName = menu.getStore().getName();

        // 장바구니 조회
        CartsResponse cart = cartsRepository.getCart(userId);

        if (cart == null) {
            cart = new CartsResponse();
            cart.setUserId(userId);
            cart.setStoreId(storeId);
            cart.setStoreName(storeName);
            cart.setItems(new ArrayList<>());
        }

        // 다른 가게 메뉴가 이미 존재하면 예외 처리
        if(cart.getStoreId() != null && !cart.getStoreId().equals(storeId)){
            throw new RuntimeException("다른 가게의 메뉴가 존재합니다.");
        }

        // 해당 메뉴의 옵션 체크
        List<MenuOptions> optionGroups = menuOptionsRepository.findByMenu_Id(menu.getId());
        List<CartsOptionResponse> options = validateAndBuildOptions(
                cartsItemRequest.getOptions(),
                optionGroups,
                menuOptionChoicesRepository
        );

        // 동일 아이템 검사 (메뉴+옵션)
        Optional<CartsItemResponse> existingItem = cart.getItems().stream()
                .filter(item -> item.getMenuId().equals(menu.getId())
                        && item.getOptions().stream()
                        .map(CartsOptionResponse::getMenuOptionChoicesId)
                        .collect(Collectors.toSet())
                        .equals(
                                options.stream()
                                        .map(CartsOptionResponse::getMenuOptionChoicesId)
                                        .collect(Collectors.toSet())
                        )
                )
                .findFirst();

        if (existingItem.isPresent()) {
            // 동일 아이템 존재 -> amount 증가
            CartsItemResponse item = existingItem.get();
            item.setAmount(item.getAmount() + cartsItemRequest.getAmount());
            item.setTotalPrice(item.getPrice() * item.getAmount());
            item.setUpdatedAt(LocalDateTime.now());
            cartsRepository.saveCart(userId, cart);
            return item;
        }

        // 가격 계산
        int itemPrice = menu.getPrice() + options.stream().mapToInt(CartsOptionResponse::getExtraPrice).sum();
        int totalPrice = itemPrice * cartsItemRequest.getAmount();

        // response body 생성
        CartsItemResponse item = new CartsItemResponse();
        item.setCartItemId(UUID.randomUUID().toString());
        item.setMenuId(menu.getId());
        item.setMenuName(menu.getName());
        item.setAmount(cartsItemRequest.getAmount());
        item.setPrice(itemPrice);
        item.setTotalPrice(totalPrice);
        item.setOptions(options);
        item.setUpdatedAt(LocalDateTime.now());

        // 아이템 추가 후 Redis 저장
        cart.getItems().add(item);
        cartsRepository.saveCart(userId, cart);

        return item;
    }

    public CartsItemResponse updateCartItem(Long userId, String cartItemId, CartsItemRequest cartsItemRequest) {

        CartsResponse cart = getCart(userId);

        if (cart == null)
            throw new IllegalArgumentException("장바구니가 존재하지 않습니다.");

        CartsItemResponse item = cart.getItems().stream()
                .filter(i -> i.getCartItemId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 cartItemId입니다."));

        // 수량 체크
        if (cartsItemRequest.getAmount() <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }

        // 메뉴 조회
        Menus menu = menusRepository.findById(item.getMenuId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다."));

        // 해당 메뉴의 옵션 체크
        List<MenuOptions> optionGroups = menuOptionsRepository.findByMenu_Id(menu.getId());
        List<CartsOptionResponse> options = validateAndBuildOptions(
                cartsItemRequest.getOptions(),
                optionGroups,
                menuOptionChoicesRepository
        );

        // 가격 계산
        int itemPrice = menu.getPrice() + options.stream().mapToInt(CartsOptionResponse::getExtraPrice).sum();
        int totalPrice = itemPrice * cartsItemRequest.getAmount();

        // 값 갱신
        item.setAmount(cartsItemRequest.getAmount());
        item.setPrice(itemPrice);
        item.setTotalPrice(totalPrice);
        item.setOptions(options);
        item.setUpdatedAt(LocalDateTime.now());


        // 동일 메뉴+옵션이 이미 있는지 확인
        Optional<CartsItemResponse> duplicate = cart.getItems().stream()
                .filter(i -> !i.getCartItemId().equals(cartItemId)) // 자기 자신 제외
                .filter(i -> i.getMenuId().equals(menu.getId())
                        && i.getOptions().stream()
                        .map(CartsOptionResponse::getMenuOptionChoicesId)
                        .collect(Collectors.toSet())
                        .equals(options.stream()
                                .map(CartsOptionResponse::getMenuOptionChoicesId)
                                .collect(Collectors.toSet()))
                )
                .findFirst();

        if (duplicate.isPresent()) {
            // 중복 발견 -> 합치기
            CartsItemResponse dupItem = duplicate.get();
            dupItem.setAmount(dupItem.getAmount() + item.getAmount());
            dupItem.setTotalPrice(dupItem.getPrice() * dupItem.getAmount());
            dupItem.setUpdatedAt(LocalDateTime.now());

            // 기존 아이템 삭제
            cart.getItems().remove(item);

            cartsRepository.saveCart(userId, cart);
            return dupItem;
        }

        // 중복이 없다면 그대로 Redis 저장
        cartsRepository.saveCart(userId, cart);
        return item;
    }

    public void deleteCartItem(Long userId, String cartItemId) {
        CartsResponse cart = cartsRepository.getCart(userId);

        if (cart == null) return;

        cart.getItems().removeIf(i -> i.getCartItemId().equals(cartItemId));
        cartsRepository.saveCart(userId, cart);
    }

    // 장바구니 전체 삭제
    public void clearCart(Long userId) {
        cartsRepository.deleteCart(userId);
    }

}
