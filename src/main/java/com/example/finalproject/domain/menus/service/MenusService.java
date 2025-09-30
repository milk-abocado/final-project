package com.example.finalproject.domain.menus.service;

import com.example.finalproject.domain.carts.exception.AccessDeniedException;
import com.example.finalproject.domain.menus.dto.request.MenuCategoryRequest;
import com.example.finalproject.domain.menus.dto.request.MenuOptionChoicesRequest;
import com.example.finalproject.domain.menus.dto.request.MenuOptionsRequest;
import com.example.finalproject.domain.menus.dto.request.MenusRequest;
import com.example.finalproject.domain.menus.dto.response.*;
import com.example.finalproject.domain.menus.entity.MenuCategories;
import com.example.finalproject.domain.menus.entity.MenuOptionChoices;
import com.example.finalproject.domain.menus.entity.MenuOptions;
import com.example.finalproject.domain.menus.entity.Menus;
import com.example.finalproject.domain.menus.exception.ErrorCode;
import com.example.finalproject.domain.menus.exception.MenusException;
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

        // USER 권한 접근 방지 (대소문자 무시)
        if (authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r) // ROLE_ 제거
                .anyMatch(r -> r.equalsIgnoreCase("USER"))) {
            throw new AccessDeniedException("USER는 접근할 수 없습니다.");
        }

        Stores store = storesRepository.findById(storeId)
                .orElseThrow(() -> new MenusException(ErrorCode.STORE_NOT_FOUND, "존재하지 않는 가게입니다."));

        // 가게 주인 확인
        if (!store.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("본인 가게만 접근할 수 있습니다.");
        }

        // 폐업 가게 체크
        if (!store.isActive()) {
            throw new MenusException(ErrorCode.GONE, "폐업한 가게에는 메뉴를 등록/수정할 수 없습니다.");
        }

        return store;

    }

    private MenuOptions createMenuOption(Menus menu, MenuOptionsRequest optReq) {
        MenuOptions option = new MenuOptions();
        option.setMenu(menu);
        option.setOptionsName(optReq.getOptionsName());
        option.setIsRequired(optReq.getIsRequired());

        if (Boolean.FALSE.equals(optReq.getIsRequired())) {
            option.setMinSelect(0);
        } else {
            if (optReq.getMinSelect() == null || optReq.getMinSelect() < 1) {
                throw new MenusException(ErrorCode.OPTION_INVALID_SELECTION, "필수 옵션("+optReq.getOptionsName()+")의 최소 선택 수는 1 이상이어야 합니다.");
            }
            option.setMinSelect(optReq.getMinSelect());
        }

        if (optReq.getMaxSelect() != null && optReq.getMaxSelect() < option.getMinSelect()) {
            throw new MenusException(ErrorCode.OPTION_INVALID_SELECTION, "옵션("+optReq.getOptionsName()+")의 최대 선택 수는 최소 선택 수 이상이어야 합니다.");
        }

        option.setMaxSelect(optReq.getMaxSelect());
        return option;
    }

    private MenuCategories createMenuCategory(Menus menu, String categoryName) {
        MenuCategories category = new MenuCategories();
        category.setMenu(menu);
        category.setCategory(categoryName != null ? categoryName.trim() : null);
        return category;
    }

    private MenuOptionChoices createMenuOptionChoice(MenuOptions option, MenuOptionChoicesRequest req) {
        MenuOptionChoices choice = new MenuOptionChoices();
        choice.setGroup(option);
        choice.setChoiceName(req.getChoiceName());
        choice.setExtraPrice(req.getExtraPrice());
        return choice;
    }

    // 메뉴 생성 (옵션 + 카테고리 포함)
    @Transactional
    public MenusResponse createMenu(Authentication authentication, Long storeId, MenusRequest request) {

        if (menusRepository.findByStoreIdAndNameAndStatus(storeId, request.getName(), Menus.MenuStatus.ACTIVE).isPresent()
                || menusRepository.findByStoreIdAndNameAndStatus(storeId, request.getName(), Menus.MenuStatus.SOLD_OUT).isPresent()) {
            throw new MenusException(ErrorCode.MENU_DUPLICATE_NAME, "동일한 이름의 메뉴가 이미 존재합니다.");
        }

        // 권한 체크
        Stores store = verifiedUser(authentication, storeId);

        // 가격 체크
        if (request.getPrice() == null || request.getPrice() <= 0) {
            throw new MenusException(ErrorCode.MENU_PRICE_INVALID, "메뉴 가격은 0원 초과여야 합니다.");
        }

        // status 체크
        Menus.MenuStatus status;
        try {
            status = Menus.MenuStatus.valueOf(request.getStatus());
        } catch (MenusException e) {
            throw new MenusException(ErrorCode.MENU_PRICE_INVALID, "유효하지 않은 메뉴 상태입니다.");
        }

        // 생성 시 DELETED 상태는 금지
        if (status == Menus.MenuStatus.DELETED) {
            throw new MenusException(ErrorCode.MENU_DELETE_NOT_ALLOWED, "생성 시 메뉴 상태를 DELETED로 지정할 수 없습니다.");
        }

        // 삭제된 메뉴 있으면 복구, 없으면 신규 생성
        Menus menu = menusRepository.findByStoreIdAndNameAndStatus(storeId, request.getName(), Menus.MenuStatus.DELETED)
                .orElseGet(Menus::new);
        menu.setStore(store);
        menu.setName(request.getName());
        menu.setPrice(request.getPrice());
        menu.setStatus(status);
        menusRepository.save(menu);

        // 카테고리 저장
        if (request.getCategories() != null) {
            for (String categoryName : request.getCategories()) {
                MenuCategories category = createMenuCategory(menu, categoryName);
                categoriesRepository.save(category);
            }
        }

        // 옵션 그룹 + 선택지 저장
        if (request.getOptions() != null) {
            for (MenuOptionsRequest optReq : request.getOptions()) {

                MenuOptions option = createMenuOption(menu, optReq);
                optionsRepository.save(option);

                if (optReq.getChoices() != null) {
                    for (MenuOptionChoicesRequest choiceReq : optReq.getChoices()) {
                        MenuOptionChoices choice = createMenuOptionChoice(option, choiceReq);
                        choicesRepository.save(choice);
                    }
                }
            }
        }

        return getMenu(menu.getId(), storeId);
    }

    // 메뉴 복구
    @Transactional
    public MenusResponse restoreMenu(Authentication authentication, Long menuId, Long storeId) {
        verifiedUser(authentication, storeId);

        Menus menu = menusRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new MenusException(ErrorCode.MENU_NOT_FOUND, "해당 가게의 메뉴가 존재하지 않습니다."));

        if (menu.getStatus() != Menus.MenuStatus.DELETED) {
            throw new MenusException(ErrorCode.MENU_RESTORE_NOT_ALLOWED, "삭제된 메뉴만 복구할 수 있습니다.");
        }

        // 같은 이름으로 ACTIVE/SOLD_OUT 메뉴가 있으면 복구 불가
        if (menusRepository.findByStoreIdAndNameAndStatus(storeId, menu.getName(), Menus.MenuStatus.ACTIVE).isPresent()
                || menusRepository.findByStoreIdAndNameAndStatus(storeId, menu.getName(), Menus.MenuStatus.SOLD_OUT).isPresent()) {
            throw new MenusException(ErrorCode.MENU_DUPLICATE_NAME, "동일한 이름의 메뉴가 이미 존재합니다.");
        }

        menu.setStatus(Menus.MenuStatus.ACTIVE);
        menusRepository.save(menu);

        return getMenu(menu.getId(), storeId);
    }

    // 메뉴 수정
    @Transactional
    public MenusResponse updateMenu(Authentication authentication, Long menuId, Long storeId, MenusRequest request) {

        // 권한 체크
        verifiedUser(authentication, storeId);

        Menus menu = menusRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new MenusException(ErrorCode.MENU_NOT_FOUND, "해당 가게의 메뉴가 존재하지 않습니다."));

        Menus.MenuStatus status;
        try {
            status = Menus.MenuStatus.valueOf(request.getStatus());
        } catch (MenusException e) {
            throw new MenusException(ErrorCode.MENU_INVALID_STATUS, "유효하지 않은 메뉴 상태입니다.");
        }

        if (status == Menus.MenuStatus.DELETED) {
            throw new MenusException(ErrorCode.MENU_DELETE_NOT_ALLOWED, "메뉴 삭제는 이곳에서 할 수 없습니다.");
        }

        if (request.getName() != null) {
            // 가게 + 메뉴 이름 중복 체크
            if (menusRepository.existsByStoreIdAndName(storeId, request.getName()) &&
                    !menu.getName().equals(request.getName())) {
                throw new MenusException(ErrorCode.DUPLICATE_MENU_NAME, "해당 가게에 이미 존재하는 메뉴 이름입니다.");
            }
            menu.setName(request.getName());
        }

        if (request.getPrice() != null) {
            // 가격 체크
            if (request.getPrice() <= 0) {
                throw new MenusException(ErrorCode.MENU_PRICE_INVALID, "메뉴 가격은 0원 초과여야 합니다.");
            }
            menu.setPrice(request.getPrice());
        }

        if (request.getStatus() != null) {
            try {
                // status 체크
                menu.setStatus(Menus.MenuStatus.valueOf(request.getStatus()));
            } catch (MenusException e) {
                throw new MenusException(ErrorCode.MENU_INVALID_STATUS, "유효하지 않은 메뉴 상태입니다.");
            }
        }

        menusRepository.save(menu);

        return getMenu(menu.getId(), storeId);
    }

    @Transactional
    public void deleteMenu(Authentication authentication, Long menuId, Long storeId) {

        // 권한 체크
        verifiedUser(authentication, storeId);

        Menus menu = menusRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new MenusException(ErrorCode.MENU_NOT_FOUND, "해당 가게의 메뉴가 존재하지 않습니다."));

        // DELETED 상태 메뉴 확인
        verifiedMenuIsActive(menu);

        List<MenuOptions> options = optionsRepository.findByMenuId(menuId);

        // 세부 옵션 삭제
        for (MenuOptions opt : options) {
            choicesRepository.deleteAll(choicesRepository.findByGroupId(opt.getId()));
        }
        // 옵션 그룹 삭제
        optionsRepository.deleteAll(options);
        // 카테고리 삭제
        categoriesRepository.deleteAll(categoriesRepository.findByMenuId(menuId));

        // 메뉴 soft 삭제
        menu.setStatus(Menus.MenuStatus.DELETED);
        menusRepository.save(menu);
    }

    // 옵션/카테고리 추가 시 DELETED 메뉴인지 체크
    private void verifiedMenuIsActive(Menus menu) {
        if (menu.getStatus() == Menus.MenuStatus.DELETED) {
            throw new MenusException(ErrorCode.MENU_DELETED, "삭제된 메뉴에는 옵션/카테고리를 추가할 수 없습니다.");
        }
    }

    // 메뉴 단건 조회
    @Transactional(readOnly = true)
    public MenusResponse getMenu(Long menuId, Long storeId) {
        Menus menu = menusRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new MenusException(ErrorCode.MENU_NOT_FOUND, "해당 가게의 메뉴가 존재하지 않습니다."));


        // 삭제된 메뉴 체크
        if (menu.getStatus() == Menus.MenuStatus.DELETED) {
            throw new MenusException(ErrorCode.MENU_DELETED, "삭제된 메뉴는 조회할 수 없습니다.");
        }

        // 가게 존재 여부 확인
        Stores store = storesRepository.findById(storeId)
                .orElseThrow(() -> new MenusException(ErrorCode.STORE_NOT_FOUND, "존재하지 않는 가게입니다."));

        // 폐업 가게 체크
        if (!store.isActive()) {
            throw new MenusException(ErrorCode.GONE, "폐업한 가게입니다.");
        }

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

    // 가게별 메뉴 조회
    public List<MenusSimpleResponse> getMenusByStore(Long storeId) {

        // 가게 존재 여부 확인
        Stores store = storesRepository.findById(storeId)
                .orElseThrow(() -> new MenusException(ErrorCode.STORE_NOT_FOUND, "존재하지 않는 가게입니다."));

        // 폐업 가게 체크
        if (!store.isActive()) {
            throw new MenusException(ErrorCode.GONE, "폐업한 가게입니다.");
        }

        List<Menus> menus = menusRepository.findByStoreId(storeId);

        // DELETED 메뉴 필터링
        menus = menus.stream()
                .filter(menu -> menu.getStatus() != Menus.MenuStatus.DELETED)
                .toList();

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


    // 카테고리 단건 추가
    @Transactional
    public MenusResponse addCategory(Long menuId, MenuCategoryRequest categoryName, Authentication authentication, Long storeId) {
        verifiedUser(authentication, storeId);

        Menus menu = menusRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new MenusException(ErrorCode.MENU_NOT_FOUND, "해당 가게의 메뉴가 존재하지 않습니다."));

        verifiedMenuIsActive(menu);

        MenuCategories category = createMenuCategory(menu, categoryName.getCategoryName());

        categoriesRepository.save(category);

        return getMenu(menu.getId(), storeId);
    }

    // 카테고리 단건 수정
    @Transactional
    public MenusResponse updateCategory(Long menuId, Long categoryId, MenuCategoryRequest newCategoryName, Authentication authentication, Long storeId) {

        // 권한 체크
        verifiedUser(authentication, storeId);

        // 카테고리 조회
        MenuCategories category = categoriesRepository.findById(categoryId)
                .orElseThrow(() -> new MenusException(ErrorCode.CATEGORY_NOT_FOUND, "존재하지 않는 카테고리입니다."));

        // 해당 가게에 속해있는지 확인
        Menus menu = category.getMenu();

        if (!menu.getStore().getId().equals(storeId)) {
            throw new MenusException(ErrorCode.MENU_NOT_FOUND, "해당 가게의 메뉴가 존재하지 않습니다.");
        }

        // DELETED 상태 메뉴 확인
        verifiedMenuIsActive(menu);

        // 해당 메뉴의 카테고리인지 확인
        if (!category.getMenu().getId().equals(menuId)) {
            throw new MenusException(ErrorCode.CATEGORY_NOT_BELONG_TO_MENU, "해당 메뉴의 카테고리가 아닙니다.");
        }

        if (newCategoryName != null) {
            category.setCategory(newCategoryName.getCategoryName());
        }

        categoriesRepository.save(category);

        return getMenu(menu.getId(), storeId);
    }

    // 카테고리 단건 삭제
    @Transactional
    public void deleteCategory(Long menuId, Long categoryId, Authentication authentication, Long storeId) {
        // 권한 체크
        verifiedUser(authentication, storeId);

        MenuCategories category = categoriesRepository.findById(categoryId)
                .orElseThrow(() -> new MenusException(ErrorCode.CATEGORY_NOT_FOUND, "존재하지 않는 카테고리입니다."));

        // 해당 가게에 속해있는지 확인
        Menus menu = category.getMenu();
        if (!menu.getStore().getId().equals(storeId)) {
            throw new MenusException(ErrorCode.MENU_NOT_FOUND, "해당 가게의 메뉴가 존재하지 않습니다.");
        }

        if (!category.getMenu().getId().equals(menuId)) {
            throw new MenusException(ErrorCode.CATEGORY_NOT_BELONG_TO_MENU, "해당 메뉴의 카테고리가 아닙니다.");
        }

        categoriesRepository.delete(category);
    }

    // 카테고리 전체 삭제
    @Transactional
    public void deleteAllCategories(Long menuId, Authentication authentication, Long storeId) {
        // 권한 체크
        verifiedUser(authentication, storeId);

        menusRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new MenusException(ErrorCode.MENU_NOT_FOUND, "해당 가게의 메뉴가 존재하지 않습니다."));

        List<MenuCategories> categories = categoriesRepository.findByMenuId(menuId);
        categoriesRepository.deleteAll(categories);
    }


    // 옵션 그룹 생성
    @Transactional
    public MenusResponse createOptionGroup(Long menuId, MenuOptionsRequest request, Authentication authentication, Long storeId) {

        // 권한 체크
        verifiedUser(authentication, storeId);

        Menus menu = menusRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new MenusException(ErrorCode.MENU_NOT_FOUND, "해당 가게의 메뉴가 존재하지 않습니다."));

        // DELETED 상태 메뉴 확인
        verifiedMenuIsActive(menu);

        MenuOptions option = createMenuOption(menu, request);
        optionsRepository.save(option);

        // 메뉴 선택지 저장
        if (request.getChoices() != null) {
            for (MenuOptionChoicesRequest choiceReq : request.getChoices()) {
                MenuOptionChoices choice = createMenuOptionChoice(option, choiceReq);
                choicesRepository.save(choice);
            }
        }

        return getMenu(menu.getId(), storeId);
    }

    // 옵션 그룹 단건 삭제
    @Transactional
    public void deleteOptionGroup(Long menuId, Long optionGroupId, Authentication authentication, Long storeId) {
        // 권한 체크
        verifiedUser(authentication, storeId);

        menusRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new MenusException(ErrorCode.MENU_NOT_FOUND, "해당 가게의 메뉴가 존재하지 않습니다."));

        MenuOptions optionGroup = optionsRepository.findById(optionGroupId)
                .orElseThrow(() -> new MenusException(ErrorCode.OPTION_GROUP_NOT_FOUND, "존재하지 않는 옵션 그룹입니다."));

        if (!optionGroup.getMenu().getId().equals(menuId)) {
            throw new MenusException(ErrorCode.OPTION_GROUP_NOT_BELONG_TO_MENU, "해당 메뉴의 옵션 그룹이 아닙니다.");
        }

        // 그룹 내 선택지 먼저 삭제
        choicesRepository.deleteAll(choicesRepository.findByGroupId(optionGroupId));
        optionsRepository.delete(optionGroup);
    }

    // 옵션 그룹 전체 삭제
    @Transactional
    public void deleteAllOptionGroups(Long menuId, Authentication authentication, Long storeId) {
        // 권한 체크
        verifiedUser(authentication, storeId);

        menusRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new MenusException(ErrorCode.MENU_NOT_FOUND, "해당 가게의 메뉴가 존재하지 않습니다."));

        List<MenuOptions> optionGroups = optionsRepository.findByMenuId(menuId);
        for (MenuOptions opt : optionGroups) {
            choicesRepository.deleteAll(choicesRepository.findByGroupId(opt.getId()));
        }
        optionsRepository.deleteAll(optionGroups);
    }


    // 옵션 선택지 단건 추가
    @Transactional
    public MenusResponse addOptionChoice(Long menuId, Long optionGroupId, MenuOptionChoicesRequest choiceReq, Authentication authentication, Long storeId) {
        // 권한 체크
        verifiedUser(authentication, storeId);

        MenuOptions option = optionsRepository.findById(optionGroupId)
                .orElseThrow(() -> new MenusException(ErrorCode.OPTION_GROUP_NOT_FOUND, "존재하지 않는 옵션 그룹입니다."));

        Menus menu = option.getMenu();
        if (!menu.getStore().getId().equals(storeId)) {
            throw new MenusException(ErrorCode.MENU_NOT_FOUND, "해당 가게의 메뉴가 존재하지 않습니다.");
        }

        verifiedMenuIsActive(menu);

        if (!option.getMenu().getId().equals(menuId)) {
            throw new MenusException(ErrorCode.OPTION_GROUP_NOT_BELONG_TO_MENU, "해당 메뉴의 옵션 그룹이 아닙니다.");
        }

        MenuOptionChoices choice = createMenuOptionChoice(option, choiceReq);
        choicesRepository.save(choice);
        return getMenu(menu.getId(), storeId);
    }

    // 옵션 선택지 단건 수정
    @Transactional
    public MenusResponse updateOptionChoice(Long menuId, Long optionGroupId, Long choiceId, MenuOptionChoicesRequest request, Authentication authentication, Long storeId) {
        // 권한 체크
        verifiedUser(authentication, storeId);

        MenuOptionChoices choice = choicesRepository.findById(choiceId)
                .orElseThrow(() -> new MenusException(ErrorCode.OPTION_CHOICE_NOT_FOUND, "존재하지 않는 옵션 선택지입니다."));

        Menus menu = choice.getGroup().getMenu();
        if (!menu.getStore().getId().equals(storeId)) {
            throw new MenusException(ErrorCode.MENU_NOT_FOUND, "해당 가게의 메뉴가 존재하지 않습니다.");
        }

        verifiedMenuIsActive(menu);

        if (!choice.getGroup().getMenu().getId().equals(menuId)) {
            throw new MenusException(ErrorCode.OPTION_GROUP_NOT_BELONG_TO_MENU, "해당 메뉴의 옵션 그룹이 아닙니다.");
        }

        if (!choice.getGroup().getId().equals(optionGroupId)) {
            throw new MenusException(ErrorCode.OPTION_CHOICE_NOT_BELONG_TO_GROUP, "해당 메뉴의 선택지가 아닙니다.");
        }

        if (request.getChoiceName() != null) choice.setChoiceName(request.getChoiceName());
        if (request.getExtraPrice() != null) choice.setExtraPrice(request.getExtraPrice());

        return getMenu(menu.getId(), storeId);
    }

    // 옵션 선택지 단건 삭제
    @Transactional
    public void deleteOptionChoice(Long menuId, Long optionGroupId, Long choiceId, Authentication authentication, Long storeId) {
        // 권한 체크
        verifiedUser(authentication, storeId);

        MenuOptionChoices choice = choicesRepository.findById(choiceId)
                .orElseThrow(() -> new MenusException(ErrorCode.OPTION_CHOICE_NOT_FOUND, "존재하지 않는 옵션 선택지입니다."));

        Menus menu = choice.getGroup().getMenu();
        if (!menu.getStore().getId().equals(storeId)) {
            throw new MenusException(ErrorCode.MENU_NOT_FOUND, "해당 가게의 메뉴가 존재하지 않습니다.");
        }

        if (!choice.getGroup().getMenu().getId().equals(menuId)) {
            throw new MenusException(ErrorCode.OPTION_GROUP_NOT_BELONG_TO_MENU, "해당 메뉴의 옵션 그룹이 아닙니다.");
        }

        if (!choice.getGroup().getId().equals(optionGroupId)) {
        throw new MenusException(ErrorCode.OPTION_CHOICE_NOT_BELONG_TO_GROUP, "해당 메뉴의 선택지가 아닙니다.");
        }

        choicesRepository.delete(choice);
    }
}
