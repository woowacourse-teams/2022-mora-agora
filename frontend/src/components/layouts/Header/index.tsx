import React from 'react';
import * as S from './Header.styled';
import ChevronLeftIconSVG from '../../../assets/chevron-left.svg';
import { Route, Routes, useNavigate } from 'react-router-dom';
import Avatar from '../../@shared/Avatar';

const Header = () => {
  const navigate = useNavigate();

  const handleClick = () => {
    navigate(-1);
  };

  return (
    <S.Layout>
      <Routes>
        <Route
          path="/meeting"
          element={
            <S.AvatarMessageBox>
              <S.Box>
                <Avatar />
              </S.Box>
              <S.WelcomeMessageBox>
                <p>반갑습니다.</p>
                <S.NicknameParagraph>쿤</S.NicknameParagraph>
              </S.WelcomeMessageBox>
            </S.AvatarMessageBox>
          }
        />
        <Route
          path="*"
          element={
            <S.Box>
              <S.BackwardButton type="button" onClick={handleClick}>
                <S.ChevronLeftImage src={ChevronLeftIconSVG} />
              </S.BackwardButton>
            </S.Box>
          }
        />
      </Routes>
      <S.Box />
    </S.Layout>
  );
};

export default Header;
