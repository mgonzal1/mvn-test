// $Id: DPMProtocolHandler.java,v 1.7 2023/11/02 16:36:15 kingc Exp $
package gov.fnal.controls.servers.dpm.protocols;

import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.ListId;
import gov.fnal.controls.servers.dpm.DPMList;
import gov.fnal.controls.servers.dpm.Scope;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

abstract public class DPMProtocolHandler implements AcnetErrors
{
	private static final ArrayList<DPMProtocolHandler> handlers = new ArrayList<>();

	private final Map<ListId, DPMList> activeLists = new ConcurrentHashMap<>();
	private final LinkedBlockingQueue<DPMList> disposedLists = new LinkedBlockingQueue<>(150);

	abstract public HandlerType type();
	abstract public Protocol protocol();
	abstract public void logStart();

	public Collection<DPMList> activeLists()
	{
		return activeLists.values();
	}

	public Collection<DPMList> disposedLists()
	{
		return disposedLists;
	}

	public boolean restartable()
	{
		for (DPMList list : activeLists.values()) {
			if (!list.restartable())
				return false;
		}

		return true;
	}

	public void shutdown()
	{
		for (DPMList list : activeLists.values()) {
			list.dispose(ACNET_DISCONNECTED);
		}
	}

	public DPMList list(int listId) throws AcnetStatusException
	{
		final DPMList list = activeLists.get(new ListId(listId));

		if (list != null)
			return list;
		
		throw new AcnetStatusException(DPM_NO_SUCH_LIST);
	}

	public void listCreated(DPMList list)
	{
		activeLists.put(list.id(), list);
		logger.finer("list create - active size: " + activeLists.size());
		Scope.listOpened(list);
	}

	public void listDisposed(DPMList list)
	{
		if (activeLists.remove(list.id()) != null) {
			logger.finer("list disposed - active size: " + activeLists.size());
			if (!disposedLists.offer(list)) {
				disposedLists.poll();
				disposedLists.offer(list);
			}
			Scope.listDisposed(list);
		}
	}

	public static Collection<DPMProtocolHandler> allHandlers()
	{
		return handlers;
	}

	public static void init() throws AcnetStatusException
	{
		for (DPMProtocolHandler h : new Config()) {
			h.logStart();
			handlers.add(h);
		}
	}
}
