import styled from '@emotion/styled';

export const Layout = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.75rem 0;
  gap: 1rem;
  width: 100%;
  border: 1px solid ${({ theme: { colors } }) => colors['subtle-light']};
  border-radius: 1rem;

  :hover {
    background-color: ${({ theme: { colors } }) => colors['background']};
  }
`;
