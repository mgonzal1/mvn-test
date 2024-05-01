// $Id: Config.java,v 1.2 2022/05/04 19:26:31 kingc Exp $

package gov.fnal.controls.servers.dpm.protocols;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.lang.reflect.Constructor;
import java.util.logging.Level;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class Config implements Iterable<DPMProtocolHandler>
{
	private static final Map<Class<?>, Class<?>> primitives;

	static {
		primitives = new HashMap<Class<?>, Class<?>>(16);
		primitives.put(Integer.class, int.class);
		primitives.put(Byte.class, byte.class);
		primitives.put(Character.class, char.class);
		primitives.put(Boolean.class, boolean.class);
		primitives.put(Double.class, double.class);
		primitives.put(Float.class, float.class);
		primitives.put(Long.class, long.class);
		primitives.put(Short.class, short.class);
		primitives.put(Void.class, void.class);
	}

	private static Object[] p(Object...p) { return p; }

	static Object[][] handlers = 
	{
		{ "gov.fnal.controls.servers.dpm.protocols.acnet.DPMQueryHandlerAcnet",	 p("DPM") },
		{ "gov.fnal.controls.servers.dpm.protocols.acnet.pc.DPMProtocolHandlerPC", p("DPMD") },
		{ "gov.fnal.controls.servers.dpm.protocols.acnet.pc.ScalingProtocolHandlerPC", p("SCALE") },
		{ "gov.fnal.controls.servers.dpm.protocols.tcp.DPMProtocolHandlerTCP", p(6802) },
		{ "gov.fnal.controls.servers.dpm.protocols.grpc.DPMProtocolHandlerGRPC", p(50051) },
	};

	public Iterator<DPMProtocolHandler> iterator()
	{
		return new Iter();
	}

	private Class<?>[] parameterTypes(Object[] parameters)
	{
		final Class<?>[] types = new Class[parameters.length];
		
		for (int ii = 0; ii < types.length; ii++) {
			final Class<?> parameterClass = parameters[ii].getClass();
			final Class<?> primitiveClass = Config.primitives.get(parameterClass);

			if (primitiveClass != null)
				types[ii] = primitiveClass;
			else
				types[ii] = parameters[ii].getClass();
		}

		return types;
	}

	private DPMProtocolHandler getInstance(int ii) throws Exception
	{
		final Class<?> clazz = Class.forName((String) handlers[ii][0]);
		final Class<?>[] pTypes = parameterTypes((Object[]) handlers[ii][1]);

		for (Constructor constructor : clazz.getConstructors()) {
			if (Arrays.deepEquals(pTypes, constructor.getParameterTypes()))
				return (DPMProtocolHandler) constructor.newInstance((Object[]) handlers[ii][1]);
		}
		
		throw new NoSuchMethodException();
	}

	private class Iter implements Iterator<DPMProtocolHandler>
	{
		int index;
		DPMProtocolHandler next;

		public Iter()
		{
			this.index = 0;
			this.next = null;
		}

		private void setNext()
		{
			while (index < handlers.length) {
				try {
					next = getInstance(index++);
					return;
				} catch (Throwable e) {
					logger.log(Level.CONFIG, "Error creating protocol handler instance", e);
				}
			}
		}

		public boolean hasNext()
		{
			if (next == null)
				setNext();

			return next != null;
		}

		public DPMProtocolHandler next()
		{
			if (next == null)
				setNext();

			final DPMProtocolHandler h = next;

			next = null;
			return h;
		}

		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
}

