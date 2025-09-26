package com.example.finalproject.domain.users.service;

import com.example.finalproject.domain.users.dto.UserDetailResponse;
import com.example.finalproject.domain.users.dto.UserProfileUpdateRequest;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UsersService {

    private final UsersRepository usersRepository;

    @Transactional
    public UserDetailResponse updateProfileByEmail(String email, UserProfileUpdateRequest req) {
        Users user = usersRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."));

        // 기존 id 기반 메서드 재사용
        return updateProfile(user.getId(), req);
    }

    @Transactional
    public UserDetailResponse updateProfile(Long id, UserProfileUpdateRequest req) {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."));

        // null/공백 제외 + 변경된 값만 반영 (선택 개선)
        if (org.springframework.util.StringUtils.hasText(req.getNickname())) {
            String v = req.getNickname().trim();
            if (!v.equals(user.getNickname())) user.changeNickname(v);
        }
        if (org.springframework.util.StringUtils.hasText(req.getPhoneNumber())) {
            String v = req.getPhoneNumber().trim();
            if (!v.equals(user.getPhoneNumber())) user.changePhoneNumber(v);
        }
        if (org.springframework.util.StringUtils.hasText(req.getAddress())) {
            String v = req.getAddress().trim();
            if (!v.equals(user.getAddress())) user.changeAddress(v);
        }
        if (org.springframework.util.StringUtils.hasText(req.getAddressDetail())) {
            String v = req.getAddressDetail().trim();
            if (!v.equals(user.getAddressDetail())) user.changeAddressDetail(v);
        }
        if (org.springframework.util.StringUtils.hasText(req.getZipCode())) {
            String v = req.getZipCode().trim();
            if (!v.equals(user.getZipCode())) user.changeZipCode(v);
        }

        return UserDetailResponse.from(user); // @Transactional + dirty checking
    }
}