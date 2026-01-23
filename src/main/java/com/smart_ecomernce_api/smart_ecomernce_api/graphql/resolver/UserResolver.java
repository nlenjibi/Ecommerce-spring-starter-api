package com.smart_ecomernce_api.smart_ecomernce_api.graphql.resolver;

import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.PageInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.SortDirection;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.UserCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.UserDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.UserUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.AdminDashboardDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.service.AdminService;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserResolver {

    private final UserService userService;
    private final AdminService adminService;

    @QueryMapping
    @Cacheable(value = "users", key = "#id")
    public UserDto user(@Argument Long id) {
        log.info("GraphQL Query: user(id: {})", id);
        return userService.getUserById(id).orElse(null);
    }

    @QueryMapping
    @Cacheable(value = "users", key = "'list_' + (#pagination==null?0:#pagination.page) + '_' + (#pagination==null?20:#pagination.size)")
    public Page<UserDto> users(@Argument PageInput pagination) {
        log.info("GraphQL Query: users");
        Pageable pageable = createPageable(pagination);
        return userService.getAllUsers(pageable);
    }

    @QueryMapping
    @Cacheable(value = "users", key = "'current_' + #userId")
    public UserDto currentUser(@ContextValue Long userId) {
        log.info("GraphQL Query: currentUser for user {}", userId);
        return userService.getUserById(userId).orElse(null);
    }

    @QueryMapping
    @Cacheable(value = "users", key = "'admin_dashboard'")
    public AdminDashboardDto adminDashboard() {
        log.info("GraphQL Query: adminDashboard");
        return adminService.getDashboardStats();
    }

    @MutationMapping
    @CacheEvict(value = "users", allEntries = true)
    public UserDto createUser(@Argument UserCreateRequest input) {
        log.info("GraphQL Mutation: createUser");
        return userService.createUser(input);
    }

    @MutationMapping
    @CacheEvict(value = "users", allEntries = true)
    public UserDto updateUser(@Argument Long id, @Argument UserUpdateRequest input) {
        log.info("GraphQL Mutation: updateUser(id: {})", id);
        return userService.updateUser(id, input);
    }

    @MutationMapping
    @CacheEvict(value = "users", allEntries = true)
    public Boolean deleteUser(@Argument Long id) {
        log.info("GraphQL Mutation: deleteUser(id: {})", id);
        userService.deleteUser(id);
        return true;
    }

    private Pageable createPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 20, Sort.by("id"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }
}