import React, { useState } from 'react';
import * as S from './MemberAddInput.styled';
import useQuerySelectItems from 'hooks/useQuerySelectItems';
import Input from 'components/@shared/Input';
import { UserQueryWithKeywordResponse } from 'types/userType';

const MemberAddInput: React.FC<
  Omit<React.InputHTMLAttributes<HTMLInputElement>, 'type'> &
    Omit<
      ReturnType<typeof useQuerySelectItems<UserQueryWithKeywordResponse>>,
      'loading'
    >
> = ({
  queryResult,
  selectedItems,
  queryWithKeyword,
  selectItem,
  unselectItem,
  clearQueryResult,
  ...props
}) => {
  const [isDropdownOpened, setIsDropdownOpened] = useState(false);

  const openDropdown = () => {
    setIsDropdownOpened(true);
  };

  const closeDropdown = () => {
    setIsDropdownOpened(false);
  };
  const [value, setValue] = useState('');

  const handleSearchUserInputChange: React.FormEventHandler<
    HTMLInputElement
  > = (e) => {
    const { value } = e.target as HTMLInputElement;

    setValue(value);

    if (value === '') {
      clearQueryResult();
      closeDropdown();

      return;
    }

    queryWithKeyword(value);
    openDropdown();
  };

  return (
    <S.Layout>
      <Input
        type="search"
        value={value}
        onChange={handleSearchUserInputChange}
        onFocus={() => {
          openDropdown();
        }}
        onBlur={() => {
          closeDropdown();
        }}
        {...props}
      />
      {isDropdownOpened && queryResult.length > 0 && (
        <S.QueryResultList>
          {queryResult.map((user) => (
            <S.QueryResultListItem
              key={user.id}
              onMouseDown={(e) => {
                e.preventDefault();
              }}
              onClick={(e) => {
                setValue('');
                closeDropdown();
                selectItem(user);
                clearQueryResult();
              }}
            >
              {user.nickname}
            </S.QueryResultListItem>
          ))}
        </S.QueryResultList>
      )}
      <S.AddedMembersList>
        {selectedItems.map((user) => (
          <S.AddedMembersListItem key={user.id} caption={user.email}>
            {user.nickname}
            <S.RemoveButton
              type="button"
              onClick={() => {
                unselectItem(user.id);
              }}
            >
              <svg
                width="0.75rem"
                height="0.75rem"
                viewBox="0 0 16 16"
                fill="currentColor"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  fillRule="evenodd"
                  clipRule="evenodd"
                  d="M8 16C10.1217 16 12.1566 15.1571 13.6569 13.6569C15.1571 12.1566 16 10.1217 16 8C16 5.87827 15.1571 3.84344 13.6569 2.34315C12.1566 0.842855 10.1217 0 8 0C5.87827 0 3.84344 0.842855 2.34315 2.34315C0.842855 3.84344 0 5.87827 0 8C0 10.1217 0.842855 12.1566 2.34315 13.6569C3.84344 15.1571 5.87827 16 8 16V16ZM5 7C4.73478 7 4.48043 7.10536 4.29289 7.29289C4.10536 7.48043 4 7.73478 4 8C4 8.26522 4.10536 8.51957 4.29289 8.70711C4.48043 8.89464 4.73478 9 5 9H11C11.2652 9 11.5196 8.89464 11.7071 8.70711C11.8946 8.51957 12 8.26522 12 8C12 7.73478 11.8946 7.48043 11.7071 7.29289C11.5196 7.10536 11.2652 7 11 7H5Z"
                />
              </svg>
            </S.RemoveButton>
          </S.AddedMembersListItem>
        ))}
      </S.AddedMembersList>
    </S.Layout>
  );
};

export default MemberAddInput;
