package com.woowacourse.moragora.dto;

import com.woowacourse.moragora.entity.Attendance;
import com.woowacourse.moragora.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class UserResponse {

    private Long id;
    private String email;
    private String nickName;
    private int tardyCount;

    public UserResponse(final Long id,
                        final String email,
                        final String nickName,
                        final int tardyCount) {
        this.id = id;
        this.email = email;
        this.nickName = nickName;
        this.tardyCount = tardyCount;
    }

    public static UserResponse from(final Attendance attendance) {
        final User user = attendance.getUser();
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                attendance.getTardyCount()
        );
    }
}
