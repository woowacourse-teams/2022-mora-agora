import React, { useRef, useState } from 'react';

type Values = Record<HTMLInputElement['name'], HTMLInputElement['value']>;
type Errors = Record<HTMLInputElement['name'], string | null>;

const useForm = () => {
  const [values, setValues] = useState<Values>({});
  const [errors, setErrors] = useState<Errors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const inputElementList = useRef<HTMLInputElement[]>([]);
  const isValid =
    Object.keys(errors).length > 0 &&
    Object.values(errors).every((error) => !!error);

  const validate = (target: HTMLInputElement) => {
    target.checkValidity();

    setErrors((prev) => ({
      ...prev,
      [target.name]: target.validationMessage,
    }));
  };

  const handleChange =
    (
      onChange?: React.ChangeEventHandler<HTMLInputElement>
    ): React.ChangeEventHandler<HTMLInputElement> =>
    (e) => {
      const { target } = e;
      const { name, value } = target;

      validate(target);

      setValues((prev) => ({
        ...prev,
        [name]: value,
      }));

      if (onChange) {
        onChange(e);
      }
    };

  const handleBlur: React.FormEventHandler<HTMLInputElement> = (e) => {
    const { target } = e;

    validate(target as HTMLInputElement);
  };

  const handleSubmit =
    (
      onValid: React.FormEventHandler<HTMLFormElement>,
      onError?: React.FormEventHandler<HTMLFormElement>
    ): React.FormEventHandler<HTMLFormElement> =>
    (e) => {
      e.preventDefault();

      inputElementList.current.forEach((element) => {
        validate(element);
      });

      const isValidateComplete =
        Object.keys(errors).length === inputElementList.current.length;
      const isValid = Object.values(errors).every(
        (errorMessage) => errorMessage === ''
      );

      setIsSubmitting(true);

      try {
        if (isValidateComplete && isValid) {
          onValid(e);

          return;
        } else if (onError) {
          onError(e);
        }
      } catch (e) {
        console.error(e);
      } finally {
        setIsSubmitting(false);
      }
    };

  const onSubmit = (
    onValid: React.FormEventHandler<HTMLFormElement>,
    onError?: React.FormEventHandler<HTMLFormElement>
  ) => {
    return {
      onSubmit: handleSubmit(onValid, onError),
      noValidate: true,
    };
  };

  const bindInputElementToRef: React.RefCallback<HTMLInputElement> = (
    element
  ) => {
    if (element && !inputElementList.current.includes(element)) {
      inputElementList.current.push(element);
    }
  };

  const register = (
    name: HTMLInputElement['name'],
    {
      onChange,
      ...attributes
    }: Omit<React.InputHTMLAttributes<HTMLInputElement>, 'name'>
  ) => {
    return {
      name,
      ...attributes,
      ref: bindInputElementToRef,
      onChange: handleChange(onChange),
      onBlur: handleBlur,
    };
  };

  return { values, errors, isSubmitting, isValid, onSubmit, register };
};

export default useForm;
