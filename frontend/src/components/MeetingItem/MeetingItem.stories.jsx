import MeetingItem from '.';

export default {
  title: 'Components/MeetingItem',
  component: MeetingItem,
};

const Template = (args) => {
  return <MeetingItem {...args} />;
};

export const Default = Template.bind({});
Default.args = {
  meeting: {
    id: 1,
    name: '모임1',
    attendanceCount: 0,
    startDate: '2022-01-01',
    endDate: '2022-12-31',
    entranceTime: '10:00',
    leaveTime: '18:00',
    users: [
      {
        id: 1,
        email: 'gildong@gmail.com',
        nickname: 'KoreanThief',
        tardyCount: 3,
      },
      {
        id: 2,
        email: 'boki@gmail.com',
        nickname: 'boki',
        tardyCount: 2,
      },
    ],
  },
};
