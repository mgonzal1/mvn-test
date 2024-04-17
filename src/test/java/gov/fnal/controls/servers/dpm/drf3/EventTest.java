package gov.fnal.controls.servers.dpm.drf3;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class EventTest {

    //DefaultEvent test cases
    @Test
    public void test_ValidDefaultEvent() throws EventFormatException{

        String validInput = "u";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertTrue(result instanceof DefaultEvent);
    }

    @Test (expected = EventFormatException.class)
    public void test_InvalidDefaultEvent() throws EventFormatException{

        String validInput = "uUtest";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertTrue(result instanceof DefaultEvent);
    }

    //Immediate Event test cases
    @Test
    public void test_ValidImmediateEvent() throws EventFormatException {

        String validInput = "I";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertTrue(result instanceof ImmediateEvent);
    }

    @Test (expected = EventFormatException.class)
    public void test_InvalidImmediateEvent() throws EventFormatException {

        String validInput = "Illinois";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertTrue(result instanceof ImmediateEvent);
    }

    @Test (expected = EventFormatException.class)
    public void test_ImmediateEventWhenInputIsNotMatchAnyEvent() throws EventFormatException {
        Event.parse("O");
    }

    //periodic event test cases
    @Test
    public void test_PeriodicEventForP() throws EventFormatException {

        String validInput = "P";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertTrue(result instanceof PeriodicEvent);
    }

    @Test
    public void test_PeriodicEventWithTimeFrequency() throws EventFormatException {

        String validInput = "P,90000";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertTrue(result instanceof PeriodicEvent);
    }

    @Test
    public void test_PeriodicEventWithTimeFrequencyAndImmediate() throws EventFormatException {

        String validInput = "P,90000,T";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertTrue(result instanceof PeriodicEvent);
    }

    @Test(expected = EventFormatException.class)
    public void test_PeriodicEventForRegularExpIsNotMatched() throws EventFormatException {

        String validInput = "p4343555,o7oyi7ti7t,yiutiu";
        Event.parse(validInput);
    }

    @Test
    public void test_PeriodicEventForQ() throws EventFormatException {

        String validInput = "Q";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertTrue(result instanceof PeriodicEvent);
    }

   //ClockEvent test cases
    @Test
    public void test_ClockEvent() throws EventFormatException {

        String validInput = "E,FF,E,90000";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertTrue(result instanceof ClockEvent);
    }

    //StateEvent test cases
    @Test
    public void test_StateEvent() throws EventFormatException {

        String validInput = "S,DeviceName,64534,90000,*";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertTrue(result instanceof StateEvent);
    }

    //NeverEvent test cases
    @Test
    public void test_NeverEvent() throws EventFormatException {

        String validInput = "N";
        Event result = Event.parse(validInput);
        assertNotNull(result);
        assertTrue(result instanceof NeverEvent);
    }

    //when given string input is null
    @Test(expected = NullPointerException.class)
    public void test_DefaultEventWhenInputIsNull() throws EventFormatException {
        Event.parse(null);
    }

    //when given string character at 0 position doesn't contain any
    @Test (expected = EventFormatException.class)
    public void test_DefaultEventWhenInputIsNotMatchAnyEvent() throws EventFormatException {
        Event.parse("X");
    }

}
