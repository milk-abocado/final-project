package com.example.finalproject.domain.users.service;

import com.example.finalproject.domain.users.dto.UserDetailResponse;
import com.example.finalproject.domain.users.dto.UserProfileUpdateRequest;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import com.example.finalproject.domain.auth.exception.AuthApiException;
import com.example.finalproject.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsersService {

    private final UsersRepository usersRepository;

    @Transactional
    public UserDetailResponse updateProfileByEmail(String email, UserProfileUpdateRequest req) {
        if (email == null || email.isBlank()) {
            throw AuthApiException.of(AuthErrorCode.PARAMETER_MISSING);
        }

        Users user = usersRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> AuthApiException.of(AuthErrorCode.ACCOUNT_NOT_FOUND));

        return updateProfile(user.getId(), req);
    }

    @Transactional
    public UserDetailResponse updateProfile(Long id, UserProfileUpdateRequest req) {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> AuthApiException.of(AuthErrorCode.ACCOUNT_NOT_FOUND));

        if (hasText(req.getNickname())) {
            String v = req.getNickname().trim();
            if (!v.equals(user.getNickname())) user.changeNickname(v);
        }
        if (hasText(req.getPhoneNumber())) {
            String v = req.getPhoneNumber().trim();
            if (!v.equals(user.getPhoneNumber())) user.changePhoneNumber(v);
        }
        if (hasText(req.getAddress())) {
            String v = req.getAddress().trim();
            if (!v.equals(user.getAddress())) user.changeAddress(v);
        }
        if (hasText(req.getAddressDetail())) {
            String v = req.getAddressDetail().trim();
            if (!v.equals(user.getAddressDetail())) user.changeAddressDetail(v);
        }
        if (hasText(req.getZipCode())) {
            String v = req.getZipCode().trim();
            if (!v.equals(user.getZipCode())) user.changeZipCode(v);
        }

        // @Transactional + dirty checking
        return UserDetailResponse.from(user);
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
