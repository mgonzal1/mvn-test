// $Id: DPMListAcnet.java,v 1.16 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.protocols.acnet;

import java.net.InetAddress;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequest;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.DPMList;
import gov.fnal.controls.servers.dpm.protocols.DPMProtocolHandler;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

abstract public class DPMListAcnet extends DPMList
{
	final protected AcnetRequest request;
	final protected AcnetConnection connection;

	private InetAddress requestHostAddress;
	private String requestNodeName;

	public DPMListAcnet(DPMProtocolHandler owner, AcnetRequest request) throws AcnetStatusException
	{
		super(owner);
	
		this.request = request;
		this.connection = request.connection();
		this.requestHostAddress = null;
		this.requestNodeName = null;
		this.owner.listCreated(this);
	}

	final AcnetRequest request()
	{
		return request;
	}

	public int server()
	{
		return request.server();
	}

	public int client()
	{
		return request.client();
	}

	@Override
	public String toString()
	{
		return "DPMListAcnet-" + id();
	}

	@Override
	public String clientHostName()
	{
		try {
			return fromNodeName();// + "@" + fromHostAddress().getHostName();
		} catch (Exception ignore) {
			ignore.printStackTrace();
		}

		return "";
	}

	String fromNodeName() throws AcnetStatusException
	{
		if (requestNodeName == null)
			requestNodeName = request.getName(request.client()).trim();

		return requestNodeName;
	}

	@Override
	public InetAddress fromHostAddress() throws AcnetStatusException
	{
		if (requestHostAddress == null)
			requestHostAddress = connection.remoteTaskAddress(request.client(), request.clientTaskId());
	
		return requestHostAddress;
	}

	@Override
	public boolean dispose(int status)
	{
		return super.dispose(status);
	}
}
