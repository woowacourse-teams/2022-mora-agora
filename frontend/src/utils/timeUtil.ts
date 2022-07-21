// minute이 60을 넘어갈 때 hour + 1
// 24시를 넘어갈 때 0으로 수정
const reconcileTime = (time: string) => {
  const timeList = time.split(':');
  let hour = parseInt(timeList[0]);
  let minute = parseInt(timeList[1]);

  if (minute >= 60) {
    hour += 1;
    minute -= 60;
  }

  return `${('0' + (hour % 24)).slice(-2)}:${('0' + minute).slice(-2)}`;
};

// 분에 minute에 해당하는만큼 추가
export const addMinute = (startTime: string, minute: number) => {
  const timeList = startTime.split(':');
  let startHour = timeList[0];
  let startMinute = parseInt(timeList[1]);

  const addedTime = `${startHour}:${('0' + (startMinute + minute)).slice(-2)}`;

  return reconcileTime(addedTime);
};

export const dateToFormattedString = (date: Date) => {
  const year = date.getFullYear().toString();
  const month = (1 + date.getMonth()).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');

  return `${year}-${month}-${day}`;
};
