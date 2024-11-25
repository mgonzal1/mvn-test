//package gov.fnal.controls.servers.dpm.events;
//
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//
//import java.text.ParseException;
//
//public class DataEventFactoryTest {
//
//    @Test
//    public void test_stringToEvent_StateEvent() throws ParseException {
//        final DataEvent dataEvent = DataEventFactory.stringToEvent("sest,urbern,12345,50000,!=");
//        Assertions.assertInstanceOf(StateEvent.class,dataEvent);
//    }
//    @Test
//    public void test_stringToEvent_DefaultDataEvent() throws ParseException {
//        final DataEvent dataEvent = DataEventFactory.stringToEvent("Uest,urbern,12345,50000,!=");
//        Assertions.assertInstanceOf(DefaultDataEvent.class,dataEvent);
//    }
//    @Test
//    public void test_stringToEvent_AbsoluteTimeEvent() throws ParseException {
//        final DataEvent dataEvent = DataEventFactory.stringToEvent("Aest,Thu Mar 29 20:15:30 UTC 2022,65000,50000,!=");
//        Assertions.assertInstanceOf(AbsoluteTimeEvent.class,dataEvent);
//    }
//
//    @Test
//    public void test_stringToEvent_DataLoggerClientLoggingDataEvent() throws ParseException {
//        final DataEvent dataEvent = DataEventFactory.stringToEvent("client,Thu Mar 29 20:15:30 UTC 2022,65000,50000,!=");
//        Assertions.assertInstanceOf(DataLoggerClientLoggingDataEvent.class,dataEvent);
//    }
//    @Test
//    public void test_stringToEvent_DeltaTimeEvent() throws ParseException {
//        final DataEvent dataEvent = DataEventFactory.stringToEvent("Dclient,Thu Mar 29 20:15:30 UTC 2022,Thu Mar 29 20:15:30 UTC 2022,65000,50000,!=");
//        Assertions.assertInstanceOf(DeltaTimeEvent.class,dataEvent);
//    }
//}
