import {
  User,
  GetLoginUserDataResponseBody,
  UserCoffeeStatsResponseBody,
  UserLoginRequestBody,
  UserLoginResponseBody,
  UserRegisterRequestBody,
  UserUpdateNicknameRequestBody,
  UserUpdatePasswordRequestBody,
  GoogleLoginRequestBody,
} from 'types/userType';
import {
  AttendancesResponseBody,
  PostUserAttendanceRequestBody,
} from 'types/attendanceType';
import request from '../utils/request';

export const checkEmailApi = (email: User['email']) => () =>
  request<{ isExist: boolean }>(`/users/check-email?email=${email}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
  });

export const submitRegisterApi = async (payload: UserRegisterRequestBody) => {
  await request<{ accessToken: string }>('/users', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  const { nickname, ...loginRequestBody } = payload;

  return submitLoginApi(loginRequestBody);
};

export const submitLoginApi = async (payload: UserLoginRequestBody) => {
  const loginResponse = await request<UserLoginResponseBody>('/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!loginResponse.body.accessToken) {
    throw new Error('로그인 중 오류가 발생했습니다.');
  }

  const accessToken = loginResponse.body.accessToken;
  const loginUserResponse = await request<GetLoginUserDataResponseBody>(
    '/users/me',
    {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
    }
  );

  return { ...loginUserResponse, accessToken };
};

export const googleLoginApi = async ({ code }: GoogleLoginRequestBody) => {
  const googleLoginResponse = await request<UserLoginResponseBody>(
    `/login/oauth2/google?code=${code}`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    }
  );

  if (!googleLoginResponse.body.accessToken) {
    throw new Error('구글 로그인 중 오류가 발생했습니다.');
  }

  const accessToken = googleLoginResponse.body.accessToken;
  const loginUserResponse = await request<GetLoginUserDataResponseBody>(
    '/users/me',
    {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
    }
  );

  return { ...loginUserResponse, accessToken };
};

export const getAttendancesApi =
  (id: number | undefined, accessToken: User['accessToken']) => () => {
    if (!id || !accessToken) {
      throw new Error('출석 정보 요청 중 에러가 발생했습니다.');
    }

    return request<AttendancesResponseBody>(
      `/meetings/${id}/attendances/today`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${accessToken}`,
        },
      }
    );
  };

export const postUserAttendanceApi = async ({
  meetingId,
  userId,
  accessToken,
  isPresent,
}: PostUserAttendanceRequestBody) => {
  if (!accessToken) {
    throw new Error('미팅 정보를 불러오는 중 에러가 발생했습니다.');
  }

  return request<{}>(
    `/meetings/${meetingId}/users/${userId}/attendances/today`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({ isPresent }),
    }
  );
};

export const getLoginUserDataApi =
  (accessToken: User['accessToken']) => async () => {
    if (!accessToken) {
      throw new Error('내 정보를 가져오는 중 에러가 발생했습니다.');
    }

    return request<GetLoginUserDataResponseBody>('/users/me', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
    });
  };

export const getUserCoffeeStatsApi =
  (id: string | undefined, accessToken: User['accessToken']) => async () => {
    if (!accessToken) {
      throw new Error('유저별 커피정보를 불러오는 중 에러가 발생했습니다.');
    }

    return request<UserCoffeeStatsResponseBody>(`/meetings/${id}/coffees/use`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
    });
  };

export const updateNicknameApi =
  (accessToken: User['accessToken']) =>
  async (payload: UserUpdateNicknameRequestBody) => {
    if (!accessToken) {
      throw new Error('닉네임 변경 중 에러가 발생했습니다.');
    }

    return request('/users/me/nickname', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify(payload),
    });
  };

export const updatePasswordApi =
  (accessToken: User['accessToken']) =>
  async (payload: UserUpdatePasswordRequestBody) => {
    if (!accessToken) {
      throw new Error('비밀번호 변경 중 에러가 발생했습니다.');
    }

    return request('/users/me/password', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify(payload),
    });
  };

export const unregisterApi =
  (accessToken: User['accessToken']) =>
  async (payload: { password: User['password'] }) => {
    return request('/users/me', {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify(payload),
    });
  };
