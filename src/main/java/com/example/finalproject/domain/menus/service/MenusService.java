package com.example.finalproject.domain.menus.service;

import com.example.finalproject.domain.menus.dto.request.MenuOptionChoicesRequest;
import com.example.finalproject.domain.menus.dto.request.MenuOptionsRequest;
import com.example.finalproject.domain.menus.dto.request.MenusRequest;
import com.example.finalproject.domain.menus.dto.response.MenuOptionChoicesResponse;
import com.example.finalproject.domain.menus.dto.response.MenuOptionsResponse;
import com.example.finalproject.domain.menus.dto.response.MenusCategoriesResponse;
import com.example.finalproject.domain.menus.dto.response.MenusResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenusService {

    private final MenusRepository menusRepository;
    private final MenuCategoriesRepository categoriesRepository;
    private final MenuOptionsRepository optionsRepository;
    private final MenuOptionChoicesRepository choicesRepository;
    private final StoresRepository storesRepository;

    @Transactional
    public MenusResponse createMenu(Long storeId, MenusRequest request) {
        Stores store = storesRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 가게입니다."));

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
                option.setMinSelect(optReq.getMinSelect());
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
    public MenusResponse updateMenu(Long menuId, Long storeId, MenusRequest request) {

        Menus menu = menusRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 가게의 메뉴가 존재하지 않습니다."));

        menu.setName(request.getName() != null ? request.getName() : menu.getName());
        menu.setPrice(request.getPrice() != null ? request.getPrice() : menu.getPrice());
        if (request.getStatus() != null) menu.setStatus(Menus.MenuStatus.valueOf(request.getStatus()));
        menusRepository.save(menu);

        // 카테고리
        if (request.getCategories() != null) {
            categoriesRepository.deleteAll(categoriesRepository.findByMenuId(menuId));
            for (String cat : request.getCategories()) {
                MenuCategories category = new MenuCategories();
                category.setMenu(menu);
                category.setCategory(cat);
                categoriesRepository.save(category);
            }
        }

        // 옵션
        if (request.getOptions() != null) {
            List<MenuOptions> existingOptions = optionsRepository.findByMenuId(menuId);
            for (MenuOptions opt : existingOptions) {
                choicesRepository.deleteAll(choicesRepository.findByGroupId(opt.getId()));
            }
            optionsRepository.deleteAll(existingOptions);

            for (MenuOptionsRequest optReq : request.getOptions()) {
                MenuOptions option = new MenuOptions();
                option.setMenu(menu);
                option.setOptionsName(optReq.getOptionsName());
                option.setIsRequired(optReq.getIsRequired());
                option.setMinSelect(optReq.getMinSelect());
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
    public void deleteMenu(Long menuId, Long storeId) {
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

    public List<MenusResponse> getMenusByStore(Long storeId) {
        List<Menus> menus = menusRepository.findByStoreId(storeId);
        return menus.stream().map(menu -> {
            List<MenuCategories> categories = categoriesRepository.findByMenuId(menu.getId());
            List<MenusCategoriesResponse> categoryResponses = categories.stream()
                    .map(MenusCategoriesResponse::new).toList();

            List<MenuOptions> options = optionsRepository.findByMenuId(menu.getId());
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
        }).toList();
    }
}
