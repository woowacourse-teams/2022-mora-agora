import { Participant } from './userType';

export type Meeting = {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  entranceTime: string;
  closingTime: string;
  isActive: boolean;
  userIds: number[];
  leaveTime: string;
  attendanceCount: number;
};

export type MeetingWithTardyCount = Omit<
  Meeting,
  'leaveTime' | 'attendanceCount' | 'userIds'
> & {
  tardyCount: number;
};

export type MeetingWithMasterAndCoffeeTime = MeetingWithTardyCount & {
  isMaster: boolean;
  isCoffeeTime: boolean;
};

export type MeetingCreateRequestBody = Pick<
  Meeting,
  'name' | 'startDate' | 'endDate' | 'entranceTime' | 'leaveTime' | 'userIds'
>;

export type MeetingListResponseBody = {
  meetings: MeetingWithMasterAndCoffeeTime[];
};

export type MeetingResponseBody = Omit<
  Meeting,
  'userIds' | 'closingTime' | 'isActive'
> & {
  users: Participant[];
  attendanceCount: number;
  isMaster: boolean;
  isCoffeeTime: boolean;
};
