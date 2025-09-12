//package com.example.finalproject.domain.security;
//
//import com.example.finalproject.domain.users.entity.Users;
//import com.example.finalproject.domain.users.repository.UsersRepository;
//import com.example.finalproject.domain.security.CustomUserDetails;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//@Service
//public class CustomUserDetailsService implements UserDetailsService {
//
//    private final UsersRepository usersRepository; // users 테이블과 연결된 JPA Repository
//
//    public CustomUserDetailsService(UsersRepository usersRepository) {
//        this.usersRepository = usersRepository;
//    }
//
//    @Override
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        return null;
//    }
//
//    // 추 후 수 정 필 요
//    // @Override
//    // public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//    //    Users users = usersRepository.findByUsername(username)
//    //            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
//
//    //    return new CustomUserDetails(
//    //            users.getUserId(),
//    //            users.getUsername(),
//    //            users.getPassword()
//    //    );
//    // }
//}
