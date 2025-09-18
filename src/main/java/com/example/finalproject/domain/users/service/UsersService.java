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

    @Transactional(readOnly = true)
    public UserDetailResponse getDetail(Long id) {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."));
        return UserDetailResponse.from(user);
    }

    @Transactional
    public UserDetailResponse updateProfile(Long id, UserProfileUpdateRequest req) {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."));

        // null 아닌 값만 반영 (부분 업데이트)
        if (req.getNickname() != null)      user.setNickname(req.getNickname().trim());
        if (req.getPhoneNumber() != null)   user.setPhoneNumber(req.getPhoneNumber());
        if (req.getAddress() != null)       user.setAddress(req.getAddress());
        if (req.getAddressDetail() != null) user.setAddressDetail(req.getAddressDetail());
        if (req.getZipCode() != null)       user.setZipCode(req.getZipCode());

        return UserDetailResponse.from(user); // JPA dirty checking
    }
}