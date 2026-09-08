package com.banking.service;

import com.banking.dto.CreateUserRequest;
import com.banking.dto.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    UserResponse getUserById(Long id);
    List<UserResponse> getAllUsers();
}

