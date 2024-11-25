// $Id: SnapRequest.java,v 1.8 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.List;
import java.util.LinkedList;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;

class SnapRequest
{
	WhatDaq device;
	String name;
	ClassCode plotClass;
	Trigger trigger;
	SnapScope scope;
	ReArm reArm;
	boolean delete;
	int error;
	transient int completionParameterId;

	ReceiveData callback;

    SnapRequest(WhatDaq device, ClassCode classCode, Trigger userTrigger, SnapScope userScope, ReceiveData callback, ReArm again)
    {
		setCallback(callback);

        this.device = device;
        this.plotClass = classCode;
        this.trigger = userTrigger;
        this.scope = userScope;
        this.reArm = again;
        this.delete = false;

        if (this.device != null)
			this.name = device.name();
    }

	@Override
    public String toString()
    {
        return ("SnapRequest: " + name + " ");
    }

	/**
	 * Set a callback for this request.
	 * @param userCallback the callback.
	 */
	void setCallback(ReceiveData callback)
	{
		this.callback = callback;
	}

    String dumpString()
    {
        StringBuffer returnBuffer = new StringBuffer();
        returnBuffer.append("SnapRequest: " + device + " " + trigger + " " + scope + " ");

        return returnBuffer.toString();
    }

    public void setCompletionParameterId(int id)
    {
        completionParameterId = id;
        delete = false;
    }

    int getCompletionParameterId()
    {
        return completionParameterId;
    }

    WhatDaq getDevice()
    {
        return device;
    }

    int getSnapShotClassCode()
    {
        return plotClass.snap();
    }

    ClassCode getClassCode()
    {
        return plotClass;
    }

    void setClassCode(ClassCode code)
    {
        plotClass = code;
    }

    Trigger getTrigger()
    {
        return trigger;
    }

    ClockTrigger getClockTrigger()
    {
        if (trigger instanceof ClockTrigger) return (ClockTrigger) trigger;
        else 
			return null;
    }

    StateTransitionTrigger getStateTransitionTrigger()
    {
        if (trigger instanceof StateTransitionTrigger) return (StateTransitionTrigger) trigger;
        else return null;
    }

    ExternalTrigger getExternalTrigger()
    {
        if (trigger instanceof ExternalTrigger) 
			return (ExternalTrigger) trigger;
        else 
			return null;
    }

    void delete()
    {
        delete = true;
    }

    boolean isDeleted()
    {
        return delete;
    }

    SnapScope getScope()
    {
        return scope;
    }

    ReArm getReArm()
    {
        return reArm;
    }

    int getError()
    {
        return error;
    }

    List<SnapRequest> getPlotRequests()
    {
        List<SnapRequest> snapRequests = new LinkedList<>();

        snapRequests.add(this);

        return snapRequests;
    }

	void pushCallbackInfo(SnapRequest req)
	{
		req.callback = this.callback;
	}

    //String getReconstructionString()
    //{
	//	return "";
    //}
}
