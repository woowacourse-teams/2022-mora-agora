import styled from '@emotion/styled';

export const Label = styled.label`
  display: flex;
  align-items: center;
  gap: 0.5em;
`;

export const Input = styled.input`
  -webkit-appearance: none;
  appearance: none;
  background-color: #fff;
  margin: 0;

  font: inherit;
  color: ${({ theme: { colors } }) => colors['subtle-light']};
  width: 2.5rem;
  height: 2.5rem;
  border: 1px solid currentColor;
  border-radius: 50%;
  transform: translateY(-0.075em);

  display: grid;
  place-content: center;

  :checked {
    color: ${({ theme: { colors } }) => colors['primary']};
    background-color: currentColor;
  }

  ::before {
    content: '';
    clip-path: polygon(26% 49%, 42% 64%, 77% 32%, 82% 36%, 42% 73%, 22% 53%);
    position: absolute;
    inset: 0.75rem;
    background-color: currentColor;
  }

  :checked::before {
    inset: 0.75rem;
    color: ${({ theme: { colors } }) => colors['background']};
    background-color: ;
  }

  :disabled {
    color: ${({ theme: { colors } }) => colors['subtle-light']};
    background-color: ${({ theme: { colors } }) => colors['background']};
  }

  :disabled:checked {
    background-color: ${({ theme: { colors } }) => colors['subtle-light']};
  }
`;
