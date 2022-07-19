package com.woowacourse.moragora.service;

import com.woowacourse.moragora.dto.EmailCheckResponse;
import com.woowacourse.moragora.dto.SearchedUserResponse;
import com.woowacourse.moragora.dto.SearchedUsersResponse;
import com.woowacourse.moragora.dto.UserRequest;
import com.woowacourse.moragora.entity.user.EncodedPassword;
import com.woowacourse.moragora.entity.user.User;
import com.woowacourse.moragora.exception.NoKeywordException;
import com.woowacourse.moragora.exception.NoParameterException;
import com.woowacourse.moragora.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long create(final UserRequest userRequest) {
        final User user = new User(userRequest.getEmail(), EncodedPassword.fromRawValue(userRequest.getPassword()),
                userRequest.getNickname());
        final User savedUser = userRepository.save(user);
        return savedUser.getId();
    }

    public EmailCheckResponse isEmailExist(final String email) {
        if (email.isBlank()) {
            throw new NoParameterException();
        }
        final boolean isExist = userRepository.findByEmail(email).isPresent();
        return new EmailCheckResponse(isExist);
    }

    public SearchedUsersResponse searchByKeyword(final String keyword) {
        validateKeyword(keyword);
        final List<User> searchedUsers = userRepository.findByNicknameOrEmailContaining(keyword);
        final List<SearchedUserResponse> responses = searchedUsers.stream()
                .map(SearchedUserResponse::from)
                .collect(Collectors.toList());
        return new SearchedUsersResponse(responses);
    }

    private void validateKeyword(final String keyword) {
        if (keyword.isEmpty()) {
            throw new NoKeywordException();
        }
    }
}
