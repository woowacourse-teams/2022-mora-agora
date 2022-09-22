import styled from '@emotion/styled';
import Button from 'components/@shared/Button';

export const Layout = styled.div`
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0.75rem;
`;

export const Form = styled.form`
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
`;

export const FieldGroupBox = styled.div`
  display: flex;
  gap: 1rem;
`;

export const FieldBox = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
`;

export const Label = styled.label`
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
`;

export const AddMemberParagraph = styled.p`
  display: flex;
  justify-content: space-between;
`;

export const MeetingCreateButton = styled(Button)`
  margin-top: 32px;
`;
