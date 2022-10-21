import React, { useContext, useEffect } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { userContext, UserContextValues } from 'contexts/userContext';

type AuthProps = { shouldLogin: boolean };

const Auth: React.FC<AuthProps> = ({ shouldLogin }) => {
  const navigate = useNavigate();
  const userState = useContext(userContext) as UserContextValues;

  useEffect(() => {
    if (shouldLogin && !userState.accessToken) {
      navigate('/login');
    }

    if (!shouldLogin && userState.accessToken) {
      navigate('/');
    }
  }, [navigate, userState]);

  return <Outlet />;
};

export default Auth;
