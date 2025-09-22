package com.example.finalproject.domain.menus.service;

import com.example.finalproject.domain.carts.exception.AccessDeniedException;
import com.example.finalproject.domain.menus.dto.request.MenuOptionChoicesRequest;
import com.example.finalproject.domain.menus.dto.request.MenuOptionsRequest;
import com.example.finalproject.domain.menus.dto.request.MenusRequest;
import com.example.finalproject.domain.menus.dto.response.*;
import com.example.finalproject.domain.menus.entity.MenuCategories;
import com.example.finalproject.domain.menus.entity.MenuOptionChoices;
import com.example.finalproject.domain.menus.entity.MenuOptions;
import com.example.finalproject.domain.menus.entity.Menus;
import com.example.finalproject.domain.menus.repository.MenuCategoriesRepository;
import com.example.finalproject.domain.menus.repository.MenuOptionChoicesRepository;
import com.example.finalproject.domain.menus.repository.MenuOptionsRepository;
import com.example.finalproject.domain.menus.repository.MenusRepository;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MenusService {

    private final MenusRepository menusRepository;
    private final MenuCategoriesRepository categoriesRepository;
    private final MenuOptionsRepository optionsRepository;
    private final MenuOptionChoicesRepository choicesRepository;
    private final StoresRepository storesRepository;


    private Stores verifiedUser(Authentication authentication, Long storeId) {

        // 로그인한 사용자의 userId 가져오기
        Long userId = Long.valueOf(
                ((Map<String, Object>) authentication.getDetails()).get("uid").toString()
        );

        // OWNER 권한 접근 방지 (대소문자 무시)
        if (authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r) // ROLE_ 제거
                .anyMatch(r -> r.equalsIgnoreCase("USER"))) {
            throw new AccessDeniedException("USER는 접근할 수 없습니다.");
        }

        Stores store = storesRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 가게입니다."));

        if (!store.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("본인 가게만 접근할 수 있습니다.");
        }

        return store;

    }



    @Transactional
    public MenusResponse createMenu(Authentication authentication, Long storeId, MenusRequest request) {

        // 권한 체크
        Stores store = verifiedUser(authentication, storeId);

        Menus menu = new Menus();
        menu.setStore(store);
        menu.setName(request.getName());
        menu.setPrice(request.getPrice());
        menu.setStatus(Menus.MenuStatus.valueOf(request.getStatus()));
        menusRepository.save(menu);

        // 카테고리 저장
        if (request.getCategories() != null) {
            for (String cat : request.getCategories()) {
                MenuCategories category = new MenuCategories();
                category.setMenu(menu);
                category.setCategory(cat);
                categoriesRepository.save(category);
            }
        }

        // 옵션 + 선택지 저장
        if (request.getOptions() != null) {
            for (MenuOptionsRequest optReq : request.getOptions()) {

                MenuOptions option = new MenuOptions();
                option.setMenu(menu);
                option.setOptionsName(optReq.getOptionsName());
                option.setIsRequired(optReq.getIsRequired());
                if (Boolean.FALSE.equals(optReq.getIsRequired())) {
                    option.setMinSelect(0);
                } else {
                    if (optReq.getMinSelect() == null || optReq.getMinSelect() < 1) {
                        throw new IllegalArgumentException("필수 옵션("+optReq.getOptionsName()+")의 최소 선택 수는 1 이상이어야 합니다.");
                    }
                    option.setMinSelect(optReq.getMinSelect());
                }

                if (optReq.getMaxSelect() != null && optReq.getMaxSelect() < option.getMinSelect()) {
                    throw new IllegalArgumentException("옵션("+optReq.getOptionsName()+")의 최대 선택 수는 최소 선택 수 이상이어야 합니다.");
                }

                option.setMaxSelect(optReq.getMaxSelect());
                optionsRepository.save(option);

                if (optReq.getChoices() != null) {
                    for (MenuOptionChoicesRequest choiceReq : optReq.getChoices()) {
                        MenuOptionChoices choice = new MenuOptionChoices();
                        choice.setGroup(option);
                        choice.setChoiceName(choiceReq.getChoiceName());
                        choice.setExtraPrice(choiceReq.getExtraPrice());
                        choicesRepository.save(choice);
                    }
                }
            }
        }

        return getMenu(menu.getId(), storeId);
    }

    @Transactional
    public MenusResponse updateMenu(Authentication authentication, Long menuId, Long storeId, MenusRequest request) {

        // 권한 체크
        verifiedUser(authentication, storeId);

        Menus menu = menusRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 가게의 메뉴가 존재하지 않습니다."));

        menu.setName(request.getName() != null ? request.getName() : menu.getName());
        menu.setPrice(request.getPrice() != null ? request.getPrice() : menu.getPrice());
        if (request.getStatus() != null) menu.setStatus(Menus.MenuStatus.valueOf(request.getStatus()));
        menusRepository.save(menu);

        return getMenu(menu.getId(), storeId);
    }

    @Transactional
    public void deleteMenu(Authentication authentication, Long menuId, Long storeId) {

        // 권한 체크
        verifiedUser(authentication, storeId);

        Menus menu = menusRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 가게의 메뉴가 존재하지 않습니다."));

        List<MenuOptions> options = optionsRepository.findByMenuId(menuId);

        // 세부 옵션 삭제
        for (MenuOptions opt : options) {
            choicesRepository.deleteAll(choicesRepository.findByGroupId(opt.getId()));
        }
        optionsRepository.deleteAll(options);
        // 옵션 그룹 삭제
        categoriesRepository.deleteAll(categoriesRepository.findByMenuId(menuId));
        // 메뉴삭제
        menusRepository.delete(menu);
    }


    @Transactional(readOnly = true)
    public MenusResponse getMenu(Long menuId, Long storeId) {
        Menus menu = menusRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 가게의 메뉴가 존재하지 않습니다."));

        List<MenuCategories> categories = categoriesRepository.findByMenuId(menuId);
        List<MenusCategoriesResponse> categoryResponses = categories.stream()
                .map(MenusCategoriesResponse::new).toList();

        List<MenuOptions> options = optionsRepository.findByMenuId(menuId);
        List<MenuOptionsResponse> optionResponses = options.stream().map(opt -> {
            List<MenuOptionChoices> choices = choicesRepository.findByGroupId(opt.getId());
            List<MenuOptionChoicesResponse> choiceResponses = choices.stream()
                    .map(MenuOptionChoicesResponse::new).toList();
            return new MenuOptionsResponse(opt, choiceResponses);
        }).toList();

        return new MenusResponse(
                menu.getId(),
                menu.getStore().getId(),
                menu.getName(),
                menu.getPrice(),
                menu.getStatus().name(),
                categoryResponses,
                optionResponses
        );
    }

    public List<MenusSimpleResponse> getMenusByStore(Long storeId) {

        // 가게 존재 여부 확인
        storesRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 가게입니다."));

        List<Menus> menus = menusRepository.findByStoreId(storeId);
        return menus.stream().map(menu -> {
            List<MenuCategories> categories = categoriesRepository.findByMenuId(menu.getId());
            List<MenusCategoriesResponse> categoryResponses = categories.stream()
                    .map(MenusCategoriesResponse::new).toList();

            return new MenusSimpleResponse(
                    menu.getId(),
                    menu.getStore().getId(),
                    menu.getName(),
                    menu.getPrice(),
                    menu.getStatus().name(),
                    categoryResponses
            );
        }).toList();
    }

    @Transactional
    public void deleteCategory(Long menuId, Long categoryId, Authentication authentication, Long storeId) {
        // 권한 체크
        verifiedUser(authentication, storeId);

        MenuCategories category = categoriesRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        if (!category.getMenu().getId().equals(menuId)) {
            throw new AccessDeniedException("해당 메뉴의 카테고리가 아닙니다.");
        }

        categoriesRepository.delete(category);
    }

    @Transactional
    public void deleteAllCategories(Long menuId, Authentication authentication, Long storeId) {
        // 권한 체크
        verifiedUser(authentication, storeId);

        List<MenuCategories> categories = categoriesRepository.findByMenuId(menuId);
        categoriesRepository.deleteAll(categories);
    }

    @Transactional
    public void deleteOptionGroup(Long menuId, Long optionGroupId, Authentication authentication, Long storeId) {
        // 권한 체크
        verifiedUser(authentication, storeId);

        MenuOptions optionGroup = optionsRepository.findById(optionGroupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옵션 그룹입니다."));

        if (!optionGroup.getMenu().getId().equals(menuId)) {
            throw new AccessDeniedException("해당 메뉴의 옵션 그룹이 아닙니다.");
        }

        // 그룹 내 선택지 먼저 삭제
        choicesRepository.deleteAll(choicesRepository.findByGroupId(optionGroupId));
        optionsRepository.delete(optionGroup);
    }

    @Transactional
    public void deleteAllOptionGroups(Long menuId, Authentication authentication, Long storeId) {
        // 권한 체크
        verifiedUser(authentication, storeId);

        List<MenuOptions> optionGroups = optionsRepository.findByMenuId(menuId);
        for (MenuOptions opt : optionGroups) {
            choicesRepository.deleteAll(choicesRepository.findByGroupId(opt.getId()));
        }
        optionsRepository.deleteAll(optionGroups);
    }

}
