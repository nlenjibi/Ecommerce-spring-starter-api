package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.service.impl;//package com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.service.impl;
//
//import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.entity.User;
//import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.repository.UserRepository;
//import lombok.AllArgsConstructor;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.stereotype.Service;
//
//import java.util.Collections;
//
//@AllArgsConstructor
//@Service
//public class UserDetailsServiceImpl implements UserDetailsService {
//    private final UserRepository userRepository;
//
//    @Override
//    @Cacheable(value = "users", key = "#email")
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        var user = userRepository.findByEmail(email).orElseThrow(
//                () -> new UsernameNotFoundException("User not found"));
//
//        return new User(
//            user.getEmail(),
//            user.getPassword(),
//            Collections.emptyList()
//        );
//    }
//}
