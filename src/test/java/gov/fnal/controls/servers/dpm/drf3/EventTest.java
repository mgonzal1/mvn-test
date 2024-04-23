package gov.fnal.controls.servers.dpm.drf3;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EventTest {

    //DefaultEvent test cases
    @Test
    public void test_ValidDefaultEvent() throws EventFormatException {

        String validInput = "u";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertInstanceOf(DefaultEvent.class, result);
    }

    @Test
    public void test_InvalidDefaultEvent() throws EventFormatException {

        String validInput = "uUtest";
        assertThrows(EventFormatException.class, () -> {
            Event.parse(validInput);
        });
        //        assertNotNull(result);
//        assertInstanceOf(DefaultEvent.class, result);
    }

    //Immediate Event test cases
    @Test
    public void test_ValidImmediateEvent() throws EventFormatException {

        String validInput = "I";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertInstanceOf(ImmediateEvent.class, result);
    }

    @Test
    public void test_InvalidImmediateEvent() throws EventFormatException {

        String validInput = "Illinois";
        assertThrows(EventFormatException.class, () -> {
            Event.parse(validInput);
        });
//        Event result = Event.parse(validInput);
//        assertNotNull(result);
//        assertInstanceOf(ImmediateEvent.class, result);
    }

    @Test
    public void test_ImmediateEventWhenInputIsNotMatchAnyEvent() throws EventFormatException {
        assertThrows(EventFormatException.class, () -> {
            Event.parse("O");
        });
    }

    //periodic event test cases
    @Test
    public void test_PeriodicEventForP() throws EventFormatException {

        String validInput = "P";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertInstanceOf(PeriodicEvent.class, result);
    }

    @Test
    public void test_PeriodicEventWithTimeFrequency() throws EventFormatException {

        String validInput = "P,90000";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertInstanceOf(PeriodicEvent.class, result);
    }

    @Test
    public void test_PeriodicEventWithTimeFrequencyAndImmediate() throws EventFormatException {

        String validInput = "P,90000,T";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertInstanceOf(PeriodicEvent.class, result);
    }

    @Test
    public void test_PeriodicEventForRegularExpIsNotMatched() throws EventFormatException {

        String validInput = "p4343555,o7oyi7ti7t,yiutiu";
        assertThrows(EventFormatException.class, () -> {
            Event.parse(validInput);
        });

    }

    @Test
    public void test_PeriodicEventForQ() throws EventFormatException {

        String validInput = "Q";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertInstanceOf(PeriodicEvent.class, result);
    }

    //ClockEvent test cases
    @Test
    public void test_ClockEvent() throws EventFormatException {

        String validInput = "E,FF,E,90000";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertInstanceOf(ClockEvent.class, result);
    }

    //StateEvent test cases
    @Test
    public void test_StateEvent() throws EventFormatException {

        String validInput = "S,DeviceName,64534,90000,*";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertInstanceOf(StateEvent.class, result);
    }

    //NeverEvent test cases
    @Test
    public void test_NeverEvent() throws EventFormatException {

        String validInput = "N";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertInstanceOf(NeverEvent.class, result);
    }

    //when given string input is null
    @Test
    public void test_DefaultEventWhenInputIsNull() throws EventFormatException {
        assertThrows(NullPointerException.class, () -> {
            Event.parse(null);
        });
    }

    //when given string character at 0 position doesn't contain any
    @Test
    public void test_DefaultEventWhenInputIsNotMatchAnyEvent() throws EventFormatException {
        assertThrows(EventFormatException.class, () -> {
            Event.parse("X");
        });
    }
}
