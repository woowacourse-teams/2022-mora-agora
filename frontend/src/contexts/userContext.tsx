import React, { createContext, useState } from 'react';
import { getLoginUserDataApi } from 'apis/userApis';
import useQuery from 'hooks/useQuery';
import { User } from 'types/userType';

type UserContextData = Omit<User, 'password'>;

export type UserContextValues = {
  user: UserContextData | null;
  accessToken: User['accessToken'];
  getLoginUserData: () => Promise<void>;
  login: (
    user: NonNullable<Omit<UserContextData, 'accessToken'>>,
    accessToken: NonNullable<UserContextData['accessToken']>
  ) => void;
  logout: () => void;
};

const userContext = createContext<UserContextValues | null>(null);

const UserContextProvider: React.FC<React.PropsWithChildren> = ({
  children,
}) => {
  const [user, setUser] = useState<UserContextValues['user']>(null);
  const [accessToken, setAccessToken] = useState<
    UserContextData['accessToken']
  >(localStorage.getItem('accessToken'));

  const login: UserContextValues['login'] = (user, accessToken) => {
    localStorage.setItem('accessToken', accessToken);
    setUser({ ...user, accessToken });
    setAccessToken(localStorage.getItem('accessToken'));
  };

  const logout = () => {
    localStorage.removeItem('accessToken');
    setUser(null);
    setAccessToken(null);
  };

  const getUserDataQuery = useQuery(
    ['loginUserData'],
    getLoginUserDataApi(accessToken),
    {
      enabled: !!accessToken,
      refetchOnMount: false,
      onSuccess: ({ body }) => {
        if (accessToken) {
          login(body, accessToken);
        }
      },
    }
  );

  return (
    <userContext.Provider
      value={{
        user,
        accessToken,
        getLoginUserData: getUserDataQuery.refetch,
        login,
        logout,
      }}
    >
      {children}
    </userContext.Provider>
  );
};

export { userContext, UserContextProvider };
