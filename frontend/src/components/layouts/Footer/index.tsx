import React from 'react';
import * as S from './Footer.styled';

const Footer: React.FC<React.HTMLAttributes<HTMLElement>> = () => {
  return (
    <S.Nav>
      <S.MenuNavLink to="/meeting">
        <S.Figure>
          <svg
            width="1.5rem"
            height="1.5rem"
            viewBox="0 0 24 24"
            fill="currentColor"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M6.25 3.5C3.5 3.5 3.5 3.708 3.5 6.25V6.275C3.5 7.382 3.5 8.182 3.771 8.52C4.036 8.848 4.823 9 6.25 9C7.677 9 8.464 8.847 8.729 8.519C9 8.182 9 7.382 9 6.274C9 3.708 9 3.5 6.25 3.5ZM6.25 10.5C4.564 10.5 3.299 10.323 2.604 9.46C2 8.711 2 7.689 2 6.275L2.75 6.25H2C2 3.38 2.181 2 6.25 2C10.319 2 10.5 3.38 10.5 6.25C10.5 7.688 10.5 8.711 9.896 9.46C9.201 10.323 7.936 10.5 6.25 10.5Z"
            />
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M17.25 3.5C14.5 3.5 14.5 3.708 14.5 6.25V6.275C14.5 7.382 14.5 8.182 14.771 8.52C15.036 8.848 15.823 9 17.25 9C18.677 9 19.464 8.847 19.729 8.519C20 8.182 20 7.382 20 6.274C20 3.708 20 3.5 17.25 3.5ZM17.25 10.5C15.564 10.5 14.299 10.323 13.604 9.46C13 8.711 13 7.689 13 6.275L13.75 6.25H13C13 3.38 13.181 2 17.25 2C21.319 2 21.5 3.38 21.5 6.25C21.5 7.688 21.5 8.711 20.896 9.46C20.201 10.323 18.936 10.5 17.25 10.5Z"
            />
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M6.25 14.5C3.5 14.5 3.5 14.708 3.5 17.25V17.275C3.5 18.382 3.5 19.182 3.771 19.52C4.036 19.848 4.823 20 6.25 20C7.677 20 8.464 19.847 8.729 19.519C9 19.182 9 18.382 9 17.274C9 14.708 9 14.5 6.25 14.5ZM6.25 21.5C4.564 21.5 3.299 21.323 2.604 20.46C2 19.711 2 18.689 2 17.275L2.75 17.25H2C2 14.38 2.181 13 6.25 13C10.319 13 10.5 14.38 10.5 17.25C10.5 18.688 10.5 19.711 9.896 20.46C9.201 21.323 7.936 21.5 6.25 21.5Z"
            />
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M17.25 14.5C14.5 14.5 14.5 14.708 14.5 17.25V17.275C14.5 18.382 14.5 19.182 14.771 19.52C15.036 19.848 15.823 20 17.25 20C18.677 20 19.464 19.847 19.729 19.519C20 19.182 20 18.382 20 17.274C20 14.708 20 14.5 17.25 14.5ZM17.25 21.5C15.564 21.5 14.299 21.323 13.604 20.46C13 19.711 13 18.689 13 17.275L13.75 17.25H13C13 14.38 13.181 13 17.25 13C21.319 13 21.5 14.38 21.5 17.25C21.5 18.688 21.5 19.711 20.896 20.46C20.201 21.323 18.936 21.5 17.25 21.5Z"
            />
          </svg>
          <S.Figcaption>모임</S.Figcaption>
        </S.Figure>
      </S.MenuNavLink>
      <S.MenuNavLink to="/check-in">
        <S.Figure>
          <svg
            width="1.5rem"
            height="1.5rem"
            viewBox="0 0 24 24"
            fill="currentColor"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M7.665 3.5C5.135 3.5 3.5 5.233 3.5 7.916V16.084C3.5 18.767 5.135 20.5 7.665 20.5H16.333C18.864 20.5 20.5 18.767 20.5 16.084V7.916C20.5 5.233 18.864 3.5 16.334 3.5H7.665ZM16.333 22H7.665C4.276 22 2 19.622 2 16.084V7.916C2 4.378 4.276 2 7.665 2H16.334C19.723 2 22 4.378 22 7.916V16.084C22 19.622 19.723 22 16.333 22Z"
              fill="currentFill"
            />
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M10.8134 15.123C10.6224 15.123 10.4294 15.05 10.2834 14.903L7.90945 12.53C7.61645 12.237 7.61645 11.763 7.90945 11.47C8.20245 11.177 8.67645 11.177 8.96945 11.47L10.8134 13.312L15.0294 9.09701C15.3224 8.80401 15.7964 8.80401 16.0894 9.09701C16.3824 9.39001 16.3824 9.86401 16.0894 10.157L11.3434 14.903C11.1974 15.05 11.0054 15.123 10.8134 15.123Z"
              fill="currentFill"
            />
          </svg>

          <S.Figcaption>출첵</S.Figcaption>
        </S.Figure>
      </S.MenuNavLink>
      <S.MenuNavLink to="/settings">
        <S.Figure>
          <svg
            width="1.5rem"
            height="1.5rem"
            viewBox="0 0 24 24"
            fill="currentColor"
            xmlns="http://www.w3.org/2000/svg"
          >
            <mask
              id="mask0_4_2965"
              style={{ maskType: 'alpha' }}
              maskUnits="userSpaceOnUse"
              x="2"
              y="2"
              width="20"
              height="21"
            >
              <path
                fillRule="evenodd"
                clipRule="evenodd"
                d="M2 2.00049H21.2849V22.7215H2V2.00049Z"
                fill="white"
              />
            </mask>
            <g mask="url(#mask0_4_2965)">
              <path
                fillRule="evenodd"
                clipRule="evenodd"
                d="M7.2022 17.4405C7.4312 17.4405 7.6602 17.4695 7.8842 17.5295C8.5602 17.7115 9.1472 18.1635 9.4952 18.7705C9.7212 19.1515 9.8462 19.5965 9.8502 20.0505C9.8502 20.7005 10.3722 21.2215 11.0142 21.2215H12.2672C12.9062 21.2215 13.4282 20.7035 13.4312 20.0645C13.4272 19.3585 13.7032 18.6875 14.2082 18.1825C14.7062 17.6845 15.4022 17.3855 16.0982 17.4055C16.5542 17.4165 16.9932 17.5395 17.3802 17.7595C17.9372 18.0785 18.6482 17.8885 18.9702 17.3385L19.6342 16.2315C19.7822 15.9765 19.8252 15.6565 19.7462 15.3615C19.6682 15.0665 19.4722 14.8105 19.2082 14.6595C18.5902 14.3035 18.1492 13.7295 17.9662 13.0415C17.7852 12.3665 17.8842 11.6295 18.2372 11.0225C18.4672 10.6225 18.8042 10.2855 19.2082 10.0535C19.7502 9.73649 19.9402 9.02749 19.6252 8.47549C19.6122 8.45349 19.6002 8.43049 19.5902 8.40649L19.0042 7.39049C18.6852 6.83549 17.9752 6.64449 17.4182 6.96149C16.8162 7.31749 16.1002 7.41949 15.4122 7.23849C14.7252 7.06049 14.1492 6.62549 13.7902 6.01149C13.5602 5.62749 13.4352 5.18049 13.4312 4.72549C13.4402 4.38349 13.3202 4.07649 13.1022 3.85149C12.8852 3.62749 12.5802 3.50049 12.2672 3.50049H11.0142C10.7042 3.50049 10.4142 3.62149 10.1952 3.83949C9.9772 4.05849 9.8582 4.34949 9.8602 4.65949C9.8392 6.12149 8.6442 7.29849 7.1972 7.29849C6.7332 7.29349 6.2862 7.16849 5.8982 6.93649C5.3532 6.62649 4.6412 6.81749 4.3222 7.37249L3.6452 8.48549C3.3352 9.02349 3.5252 9.73449 4.0772 10.0555C4.8962 10.5295 5.4072 11.4135 5.4072 12.3615C5.4072 13.3095 4.8962 14.1925 4.0752 14.6675C3.5262 14.9855 3.3362 15.6925 3.6542 16.2425L4.2852 17.3305C4.4412 17.6115 4.6962 17.8145 4.9912 17.8975C5.2852 17.9795 5.6092 17.9445 5.8792 17.7945C6.2762 17.5615 6.7382 17.4405 7.2022 17.4405ZM12.2672 22.7215H11.0142C9.5452 22.7215 8.3502 21.5275 8.3502 20.0585C8.3482 19.8775 8.2962 19.6895 8.1992 19.5265C8.0422 19.2525 7.7882 19.0565 7.4952 18.9785C7.2042 18.9005 6.8852 18.9435 6.6232 19.0955C5.9952 19.4455 5.2562 19.5305 4.5802 19.3405C3.9052 19.1495 3.3222 18.6855 2.9802 18.0705L2.3552 16.9935C1.6242 15.7255 2.0592 14.1005 3.3252 13.3685C3.6842 13.1615 3.9072 12.7755 3.9072 12.3615C3.9072 11.9475 3.6842 11.5605 3.3252 11.3535C2.0582 10.6175 1.6242 8.98849 2.3542 7.72049L3.0322 6.60749C3.7532 5.35349 5.3832 4.91149 6.6542 5.64149C6.8272 5.74449 7.0152 5.79649 7.2062 5.79849C7.8292 5.79849 8.3502 5.28449 8.3602 4.65249C8.3562 3.95549 8.6312 3.28649 9.1322 2.78149C9.6352 2.27749 10.3032 2.00049 11.0142 2.00049H12.2672C12.9832 2.00049 13.6792 2.29449 14.1782 2.80549C14.6762 3.31949 14.9512 4.02449 14.9302 4.73949C14.9322 4.90049 14.9852 5.08649 15.0812 5.24949C15.2402 5.51949 15.4912 5.70949 15.7892 5.78749C16.0872 5.86149 16.3992 5.82149 16.6642 5.66449C17.9442 4.93349 19.5732 5.37149 20.3042 6.64149L20.9272 7.72049C20.9432 7.74949 20.9572 7.77749 20.9692 7.80649C21.6312 9.05749 21.1892 10.6325 19.9592 11.3515C19.7802 11.4545 19.6352 11.5985 19.5352 11.7725C19.3802 12.0415 19.3372 12.3615 19.4152 12.6555C19.4952 12.9555 19.6862 13.2045 19.9552 13.3585C20.5622 13.7075 21.0152 14.2955 21.1962 14.9745C21.3772 15.6525 21.2782 16.3885 20.9252 16.9955L20.2612 18.1015C19.5302 19.3575 17.9012 19.7925 16.6342 19.0605C16.4652 18.9635 16.2702 18.9105 16.0762 18.9055H16.0702C15.7812 18.9055 15.4842 19.0285 15.2682 19.2435C15.0492 19.4625 14.9292 19.7545 14.9312 20.0645C14.9242 21.5335 13.7292 22.7215 12.2672 22.7215Z"
                fill="currentFill"
              />
            </g>
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M11.6452 10.4746C10.6052 10.4746 9.75916 11.3216 9.75916 12.3616C9.75916 13.4016 10.6052 14.2466 11.6452 14.2466C12.6852 14.2466 13.5312 13.4016 13.5312 12.3616C13.5312 11.3216 12.6852 10.4746 11.6452 10.4746ZM11.6452 15.7466C9.77816 15.7466 8.25916 14.2286 8.25916 12.3616C8.25916 10.4946 9.77816 8.97458 11.6452 8.97458C13.5122 8.97458 15.0312 10.4946 15.0312 12.3616C15.0312 14.2286 13.5122 15.7466 11.6452 15.7466Z"
              fill="currentFill"
            />
          </svg>
          <S.Figcaption>설정</S.Figcaption>
        </S.Figure>
      </S.MenuNavLink>
    </S.Nav>
  );
};

export default Footer;
