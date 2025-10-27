package com.loopers.infrastructure.user;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.example.ExampleModel;
import com.loopers.domain.user.UserEntity;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {}
