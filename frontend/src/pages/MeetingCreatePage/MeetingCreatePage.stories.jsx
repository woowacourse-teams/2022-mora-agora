import { rest } from 'msw';
import MeetingCreatePage from '.';
import MobileLayout from 'components/layouts/MobileLayout';
import Header from 'components/layouts/Header';

export default {
  title: 'Pages/MeetingCreatePage',
  component: MeetingCreatePage,
  parameters: {
    reactRouter: {
      routePath: '/meeting/create',
    },
  },
};

const Template = (args) => {
  return (
    <>
      <MobileLayout>
        <Header />
        <MeetingCreatePage {...args} />
      </MobileLayout>
    </>
  );
};

export const Default = Template.bind({});

export const Failure = Template.bind({});
Failure.parameters = {
  msw: {
    handlers: [
      rest.get('*', (req, res, ctx) => {
        return res(ctx.status(400), ctx.delay(700));
      }),
    ],
  },
};
