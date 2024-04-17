//  $Id: EventFactory.java,v 1.2 2023/10/04 19:38:05 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

import gov.fnal.controls.servers.dpm.events.DataEvent;

public final class EventFactory
{
    private EventFactory() {}

    public static DataEvent createEvent( DiscreteRequest req ) throws IllegalArgumentException 
	{
        return createEvent( req.getEvent());
    }

    public static DataEvent createEvent( Event evt ) throws IllegalArgumentException 
	{
        if (evt == null) {
            throw new NullPointerException();
        }
        if (evt instanceof DefaultEvent) {
            return createDefaultEvent();
        } else if (evt instanceof ImmediateEvent) {
            return createImmediateEvent();
        } else if (evt instanceof PeriodicEvent) {
            return createPeriodicEvent( (PeriodicEvent)evt );
        } else if (evt instanceof ClockEvent) {
            return createClockEvent( (ClockEvent)evt );
        } else if (evt instanceof StateEvent) {
            return createStateEvent( (StateEvent)evt );
		} else if (evt instanceof NeverEvent) {
			return createNeverEvent();
        } else {
            throw new IllegalArgumentException( "Unsupported event: " + evt.getClass().getName());
        }
    }

    private static DataEvent createDefaultEvent()
	{
        return new gov.fnal.controls.servers.dpm.events.DefaultDataEvent();
    }

    private static DataEvent createImmediateEvent()
	{
        return new gov.fnal.controls.servers.dpm.events.OnceImmediateEvent();
    }

	private static DataEvent createNeverEvent()
	{
		return new gov.fnal.controls.servers.dpm.events.NeverEvent();
	}

    private static DataEvent createPeriodicEvent( PeriodicEvent evt )
	{
		if (evt.inContinuous())
			return new gov.fnal.controls.servers.dpm.events.DeltaTimeEvent(
					evt.getPeriodValue().getTimeMillis(),
					evt.isImmediate()
			);
		else
			return new gov.fnal.controls.servers.dpm.events.MonitorChangeEvent(
					evt.getPeriodValue().getTimeMillis(),
					evt.isImmediate()
			);
    }

    private static DataEvent createClockEvent( ClockEvent evt )
	{
        ClockType type = evt.getClockType();
        return new gov.fnal.controls.servers.dpm.events.ClockEvent(
                evt.getEventNumber(),
                type == ClockType.HARDWARE || type == ClockType.EITHER,
                evt.getDelayValue().getTimeMillis(),
                type == ClockType.EITHER
        );
    }


    private static DataEvent createStateEvent( StateEvent evt )
	{
        String device = evt.getDevice();
        if (isInteger( device )) {
            return new gov.fnal.controls.servers.dpm.events.StateEvent(
                    Integer.parseInt( device ),
                    evt.getValue(),
                    evt.getDelayValue().getTimeMillis(),
                    convertExpression( evt.getExpression())
            );
        } else {
            return new gov.fnal.controls.servers.dpm.events.StateEvent(
                    device,
                    evt.getValue(),
                    evt.getDelayValue().getTimeMillis(),
                    convertExpression( evt.getExpression())
            );
        }

    }

    private static byte convertExpression( StateExpr exp )
	{
        switch (exp) {
            case EQUAL_EXPR :
                return gov.fnal.controls.servers.dpm.events.StateEvent.FLAG_EQUALS;
            case NOT_EQUAL_EXPR :
                return gov.fnal.controls.servers.dpm.events.StateEvent.FLAG_NOT_EQUALS;
            case GREATER_EXPR :
                return gov.fnal.controls.servers.dpm.events.StateEvent.FLAG_GREATER_THAN;
            case GREATER_OR_EQUAL_EXPR :
                return gov.fnal.controls.servers.dpm.events.StateEvent.FLAG_GREATER_THAN
                     | gov.fnal.controls.servers.dpm.events.StateEvent.FLAG_EQUALS;
            case LESS_EXPR :
                return gov.fnal.controls.servers.dpm.events.StateEvent.FLAG_LESS_THAN;
            case LESS_OR_EQUAL_EXPR :
                return gov.fnal.controls.servers.dpm.events.StateEvent.FLAG_LESS_THAN
                     | gov.fnal.controls.servers.dpm.events.StateEvent.FLAG_EQUALS;
            default :
                return gov.fnal.controls.servers.dpm.events.StateEvent.FLAG_ALL_VALUES;
        }
    }

    private static boolean isInteger( String str )
	{
        int n = str.length();
        if (n > 9) {
            return false;
        }
        for (int i = 0; i < n; ++i) {
            char c = str.charAt( i );
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
