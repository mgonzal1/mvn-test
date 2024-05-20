//package gov.fnal.controls.servers.dpm.events;
//
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//public class ClockEventTest_Events {
//
//    private static final int MAX_NUMBER_EVENTS = 256;
//    private static final String[] names = new String[MAX_NUMBER_EVENTS];
//
//    @Test
//    public void ClockEventTest1() {
//        ClockEvent clockEvent = new ClockEvent(8900, false, 2000L, true);
//
//    }
//    @Test
//    public void ClockEventTest2() {
//        ClockEvent clockEvent = new ClockEvent(8900, true, 2000L, true);
//
//    }
//
//    @Test
//    public void ClockEventTest3() {
//
//        ClockEvent clockEvent = new ClockEvent(8900, false, 2000L, false);
//    }
//
//    @Test
//    public void ClockEventTest4() {
//
//        ClockEvent clockEvent = new ClockEvent(8900, true, 2000L, false);
//    }
//
//    @Test
//    public void ClockTest_ftd(){
//
//        ClockEvent clockEvent = new ClockEvent(2000, false, 0L);
//        assertEquals(34768,clockEvent.ftd());
//    }
//
//    @Test
//    public void ClockTest_ClockEventNumberToNameWhenClockEventIsGreaterThan256(){
//
//        ClockEvent clockEvent = new ClockEvent(2000);
//        assertEquals("invalid clock event", ClockEvent.clockEventNumberToName(350));
//    }
//
//
//    //clockEvent < 0 || clockEvent >= MAX_NUMBER_EVENTS
//    @Test
//    public void ClockTest_WhenClockEventIsGreaterThanZeroAndLessThan256(){
//
//        ClockEvent clockEvent = new ClockEvent(2000, false);
//        assertEquals(1212, ClockEvent.clockEventNumberToName(101));
//    }
//}
