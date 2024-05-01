// $Id: DPMProtocolHandlerAcnet.java,v 1.9 2023/12/13 17:04:49 kingc Exp $

package gov.fnal.controls.servers.dpm.protocols.acnet;

import java.util.Map;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.ReplyId;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetCancel;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequest;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequestHandler;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.DPMList;
import gov.fnal.controls.servers.dpm.protocols.DPMProtocolHandler;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

abstract public class DPMProtocolHandlerAcnet extends DPMProtocolHandler implements AcnetRequestHandler
{
	protected AcnetRequest request; 

	protected final AcnetConnection connection;
	protected final String localNodeName;
	private final Map<ReplyId, DPMList> listsByRequest = new ConcurrentHashMap<>();


	protected DPMProtocolHandlerAcnet(String name) throws AcnetStatusException
	{
		this.connection = AcnetInterface.open(name);
		this.localNodeName = this.connection.getName(connection.getLocalNode());
	}

	protected void handleRequests(AcnetRequestHandler handler) throws AcnetStatusException
	{
		connection.handleRequests(handler);
	}

	@Override
	public void logStart()
	{  
        logger.info("'" + connection.connectedName().trim() + "' ACNET " + type() + 
						" (" + protocol() + ")" + " now available on " + 
						localNodeName + "(" + AcnetInterface.localhost() + ")");
	}

	@Override
	public void handle(AcnetCancel c)
	{
		final DPMList list = listsByRequest.remove(c.replyId());

		if (list != null) {
			logger.fine(String.format("%s Cancelled", list.id()));
			list.dispose(DPM_OK);
		}
	}

	@Override 
	public void listCreated(DPMList list)
	{
		super.listCreated(list);
		listsByRequest.put(((DPMListAcnet) list).request.replyId(), list);
	}

	@Override
	public void listDisposed(DPMList list)
	{
		super.listDisposed(list);

		listsByRequest.remove(((DPMListAcnet) list).request.replyId());
	}

	private final String clientName() throws AcnetStatusException
	{
		return String.format("0x%x", request.client());
	}

	private final DPMListAcnet getActiveList(int listId) throws AcnetStatusException
	{
		final DPMList list = list(listId);
		
		if (list instanceof DPMListAcnet)
			return (DPMListAcnet) list;
		
		throw new AcnetStatusException(DPM_NO_SUCH_LIST);
	}

	private final void handleException(AcnetStatusException e)
	{
		if (e.status != ACNET_NO_SUCH)
			logger.log(Level.WARNING, "exception with status: ", e);
	}
}

