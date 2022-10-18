import React, { useContext, useState } from 'react';
import * as S from './RegisterPage.styled';
import Input from 'components/@shared/Input';
import InputHint from 'components/@shared/InputHint';
import Button from 'components/@shared/Button';
import { userContext, UserContextValues } from 'contexts/userContext';
import useForm from 'hooks/useForm';
import useMutation from 'hooks/useMutation';
import { UserRegisterRequestBody } from 'types/userType';
import { postEmailSendApi, submitRegisterApi } from 'apis/userApis';
import ModalPortal from 'components/ModalPortal';
import EmailConfirmModal from 'components/EmailConfirmModal';

const RegisterPage = () => {
  const { login } = useContext(userContext) as UserContextValues;
  const { values, errors, isSubmitting, onSubmit, register } = useForm();
  const [isModalOpened, setIsModalOpened] = useState(false);
  const [expiredTimestamp, setExpiredTimestamp] = useState(0);
  const [isEmailVerified, setIsEmailVerified] = useState(false);

  const emailSendMutation = useMutation(postEmailSendApi, {
    onSuccess: ({ body: { expiredTime } }) => {
      setExpiredTimestamp(expiredTime);
      setIsModalOpened(true);
    },
    onError: ({ message }) => {
      const status = message.split(': ')[0];

      switch (status) {
        case '409': {
          alert('이미 존재하는 이메일 입니다.');
          break;
        }
        case '500': {
          alert('이메일 발송에 실패했습니다.');
          break;
        }
        default: {
          alert('이메일 인증하기를 실패했습니다.');
        }
      }
    },
  });

  const registerMutation = useMutation(submitRegisterApi, {
    onSuccess: ({ body: userData, accessToken }) => {
      login(userData, accessToken);
    },
    onError: () => {
      alert('회원가입을 실패했습니다.');
    },
  });

  const handleModalClose = () => {
    setIsModalOpened(false);
  };

  const handleEmailConfirmSuccess = () => {
    setIsEmailVerified(true);
    setIsModalOpened(false);
  };

  const handleEmailCheckButtonClick: React.MouseEventHandler<
    HTMLButtonElement
  > = () => {
    if (typeof values['email'] === 'string') {
      emailSendMutation.mutate({ email: values['email'] });
    }
  };

  const handleSubmit: React.FormEventHandler<HTMLFormElement> = async (e) => {
    if (!isEmailVerified) {
      throw e;
    }

    const target = e.target as HTMLFormElement;
    const formData = new FormData(target);
    formData.delete('passwordConfirm');
    const formDataObject = Object.fromEntries(
      formData.entries()
    ) as UserRegisterRequestBody;

    await registerMutation.mutate(formDataObject);
  };

  return (
    <>
      {isModalOpened && (
        <ModalPortal closePortal={handleModalClose}>
          <EmailConfirmModal
            email={values['email'] as string}
            expiredTimestamp={expiredTimestamp}
            onSuccess={handleEmailConfirmSuccess}
            onDismiss={handleModalClose}
            refetch={emailSendMutation.mutate}
          />
        </ModalPortal>
      )}
      <S.Layout>
        <S.Form id="register-form" {...onSubmit(handleSubmit)}>
          <S.FieldBox>
            <S.Label>
              이메일
              <S.EmailBox>
                <S.EmailInput
                  type="email"
                  {...register('email', {
                    required: true,
                    maxLength: 50,
                    watch: true,
                    onChange: () => {
                      setIsEmailVerified(false);
                    },
                    readOnly: isEmailVerified,
                  })}
                  placeholder="이메일을 입력해주세요."
                />
                <S.EmailCheckButton
                  type="button"
                  variant="confirm"
                  onClick={handleEmailCheckButtonClick}
                  disabled={
                    !values['email'] ||
                    errors['email'] !== '' ||
                    isEmailVerified
                  }
                >
                  {isEmailVerified ? '인증완료' : '인증하기'}
                </S.EmailCheckButton>
              </S.EmailBox>
            </S.Label>
            <InputHint
              isShow={Boolean(errors['email']) && errors['email'] !== ''}
              message={errors['email']}
            />
          </S.FieldBox>
          <S.FieldBox>
            <S.Label>
              비밀번호
              <Input
                type="password"
                {...register('password', {
                  pattern:
                    '(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,30}',
                  patternValidationMessage:
                    '8에서 30자리 이하의 영어, 숫자, 특수문자로 입력해주세요.',
                  onChange: (e, inputController) => {
                    inputController['passwordConfirm'].checkValidity();
                  },
                  minLength: 8,
                  maxLength: 30,
                  required: true,
                })}
                placeholder="8에서 30자리 이하의 영어, 숫자, 특수문자로 입력해주세요."
              />
            </S.Label>
            <InputHint
              isShow={Boolean(errors['password']) && errors['password'] !== ''}
              message={errors['password']}
            />
          </S.FieldBox>
          <S.FieldBox>
            <S.Label>
              비밀번호 확인
              <Input
                type="password"
                {...register('passwordConfirm', {
                  customValidations: [
                    {
                      validate: (value, inputController) =>
                        inputController['password'] &&
                        inputController['passwordConfirm'] &&
                        inputController['password'].element.value ===
                          inputController['passwordConfirm'].element.value,
                      validationMessage: '비밀번호가 다릅니다.',
                    },
                  ],
                  required: true,
                })}
              />
            </S.Label>
            <InputHint
              isShow={
                Boolean(errors['passwordConfirm']) &&
                errors['passwordConfirm'] !== ''
              }
              message={errors['passwordConfirm']}
            />
          </S.FieldBox>
          <S.FieldBox>
            <S.Label>
              닉네임
              <Input
                type="text"
                {...register('nickname', {
                  maxLength: 15,
                  pattern: '([a-zA-Z0-9가-힣]){1,15}',
                  patternValidationMessage:
                    '15자 이하의 영어, 한글, 숫자 조합으로 입력해주세요.',
                  required: true,
                })}
                placeholder="15자 이하의 영어, 한글, 숫자 조합으로 입력해주세요."
              />
            </S.Label>
            <InputHint
              isShow={Boolean(errors['nickname']) && errors['nickname'] !== ''}
              message={errors['nickname']}
            />
          </S.FieldBox>
        </S.Form>
        <S.ButtonBox>
          <Button type="submit" form="register-form" disabled={isSubmitting}>
            회원가입
          </Button>
          <S.LoginHintParagraph>
            이미 가입된 계정이 있으신가요?
            <S.LoginLink to="/login">로그인</S.LoginLink>
          </S.LoginHintParagraph>
        </S.ButtonBox>
      </S.Layout>
    </>
  );
};

export default RegisterPage;
