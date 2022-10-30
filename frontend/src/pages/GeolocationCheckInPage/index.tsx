import { useEffect, useRef, useState } from 'react';
import { getMeetingListApi } from 'apis/meetingApis';
import Spinner from 'components/@shared/Spinner';
import ErrorIcon from 'components/@shared/ErrorIcon';
import ReloadButton from 'components/@shared/ReloadButton';
import useQuery from 'hooks/useQuery';
import { MeetingListResponseBody } from 'types/meetingType';
import emptyInboxSVG from 'assets/empty-inbox.svg';
import useGeolocation from 'hooks/useGeolocation';
import useKakaoMap from 'hooks/useKakaoMap';
import CheckMeetingItem from 'components/CheckMeetingItem';
import CheckInButtonSection from './CheckInButtonSection';
import * as S from './GeolocationCheckInPage.styled';

const GeolocationCheckInPage = () => {
  const [currentMeeting, setCurrentMeeting] = useState<
    MeetingListResponseBody['meetings'][0] | null
  >(null);
  const { mapContainerRef, setControllable, panTo } = useKakaoMap();
  const { isLoading, currentPosition, permissionState } = useGeolocation({
    options: {
      enableHighAccuracy: true,
    },
    onWatchSuccess: (position) => {
      panTo(position.coords.latitude, position.coords.longitude);
    },
  });
  const mapOverlayRef = useRef<HTMLDivElement>(null);
  const mapUserMarkerRef = useRef<SVGSVGElement>(null);

  const meetingListQuery = useQuery(['meetingList'], getMeetingListApi(), {
    onSuccess: ({ data: { meetings } }) => {
      const activeMeeting = meetings.find((meeting) => meeting.isActive);

      if (activeMeeting) {
        setCurrentMeeting(activeMeeting);
      }
    },
    onError: () => {
      alert('모임 정보를 가져오는데 실패했습니다.');
    },
  });

  const handleMeetingItemClick = (
    meeting: MeetingListResponseBody['meetings'][0]
  ) => {
    setCurrentMeeting(meeting);
  };

  useEffect(() => {
    if (isLoading && mapOverlayRef.current && mapUserMarkerRef.current) {
      mapOverlayRef.current.classList.add('loading');
      mapUserMarkerRef.current.classList.add('loading');
    } else {
      mapOverlayRef.current?.classList.remove('loading');
      mapUserMarkerRef.current?.classList.remove('loading');

      if (currentPosition) {
        panTo(
          currentPosition.coords.latitude,
          currentPosition.coords.longitude
        );
      }
    }

    setControllable(false);
  }, [
    isLoading,
    mapOverlayRef.current,
    setControllable,
    panTo,
    currentPosition,
  ]);

  if (permissionState !== 'granted') {
    return (
      <S.Layout>
        <S.EmptyStateBox>
          <S.EmptyStateTitle>위치 정보를 사용할 수 없어요.</S.EmptyStateTitle>
          <S.EmptyStateParagraph>
            브라우저의 위치 정보 접근 권한을 확인해주세요.
          </S.EmptyStateParagraph>
        </S.EmptyStateBox>
      </S.Layout>
    );
  }

  if (meetingListQuery.isLoading) {
    return (
      <S.Layout>
        <S.SpinnerBox>
          <Spinner />
        </S.SpinnerBox>
      </S.Layout>
    );
  }

  if (meetingListQuery.isError) {
    return (
      <S.Layout>
        <S.ErrorBox>
          <ErrorIcon />
          <ReloadButton
            onClick={() => {
              meetingListQuery.refetch();
            }}
          />
        </S.ErrorBox>
      </S.Layout>
    );
  }

  if (!currentMeeting) {
    return (
      <S.Layout>
        <S.EmptyStateBox>
          <S.EmptyStateImage src={emptyInboxSVG} alt="empty inbox" />
          <S.EmptyStateTitle>출석 가능한 모임이 없어요.</S.EmptyStateTitle>
          <S.EmptyStateParagraph>
            출석 시간을 확인해주세요.
          </S.EmptyStateParagraph>
        </S.EmptyStateBox>
      </S.Layout>
    );
  }

  return (
    <S.Layout>
      <S.MapSection>
        <S.Map ref={mapContainerRef}>
          <S.MapOverlay ref={mapOverlayRef} className="loading">
            Loading...
          </S.MapOverlay>
          {currentPosition && (
            <S.GeolocationUpdatedTimeParagraph className="loading">
              <S.GeolocationUpdatedTimeSpan>
                {new Date(currentPosition.timestamp).toLocaleTimeString(
                  undefined,
                  {
                    hourCycle: 'h24',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit',
                  }
                )}
              </S.GeolocationUpdatedTimeSpan>
              에 업데이트 됨
            </S.GeolocationUpdatedTimeParagraph>
          )}
          <S.MapUserMarker
            ref={mapUserMarkerRef}
            width="2rem"
            height="2rem"
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 22"
            fill="currentColor"
            className="loading"
          >
            <path
              fillRule="evenodd"
              d="M11.54 22.351l.07.04.028.016a.76.76 0 00.723 0l.028-.015.071-.041a16.975 16.975 0 001.144-.742 19.58 19.58 0 002.683-2.282c1.944-1.99 3.963-4.98 3.963-8.827a8.25 8.25 0 00-16.5 0c0 3.846 2.02 6.837 3.963 8.827a19.58 19.58 0 002.682 2.282 16.975 16.975 0 001.145.742zM12 13.5a3 3 0 100-6 3 3 0 000 6z"
              clipRule="evenodd"
            />
          </S.MapUserMarker>
        </S.Map>
      </S.MapSection>
      <S.CheckTimeSection>
        <S.SectionTitle>출결 중인 모임</S.SectionTitle>
        <S.MeetingList>
          {meetingListQuery.data?.data.meetings
            .filter((meeting) => meeting.isActive)
            .map((meeting) => (
              <CheckMeetingItem
                key={meeting.id}
                onClick={handleMeetingItemClick}
                meeting={meeting}
                clicked={meeting.id === currentMeeting?.id}
              >
                {meeting.name}
              </CheckMeetingItem>
            ))}
        </S.MeetingList>
      </S.CheckTimeSection>
      <CheckInButtonSection
        meeting={currentMeeting}
        currentPosition={currentPosition}
        refetchMeetingList={meetingListQuery.refetch}
      />
    </S.Layout>
  );
};

export default GeolocationCheckInPage;
