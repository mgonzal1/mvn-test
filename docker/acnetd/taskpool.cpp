#ifndef NO_REPORT
#include <sys/time.h>
#include <sys/resource.h>
#include <sys/time.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <fstream>
#include <iomanip>
#include <cstring>
#include <signal.h>
#include <unistd.h>
#endif
#include "server.h"
#include "lcltask.h"
#include "remtask.h"
#include "mctask.h"

const taskid_t AcnetTaskId(0);

TaskPool::TaskPool(trunknode_t node, nodename_t nodeName) : node_(node), nodeName_(nodeName)
{
    taskStatTimeBase = now();

    for (int ii = 0; ii < MAX_TASKS; ii++)
	tasks_[ii] = 0;

    tasks_[AcnetTaskId.raw()] = new AcnetTask(*this, AcnetTaskId);
    active.insert(TaskHandleMap::value_type(taskhandle_t(ator("ACNET")), tasks_[AcnetTaskId.raw()]));
    active.insert(TaskHandleMap::value_type(taskhandle_t(ator("ACNAUX")), tasks_[AcnetTaskId.raw()]));
}

taskid_t TaskPool::nextFreeTaskId(ConnectCommand const* const cmd)
{
    bool ext = (CommandList::cmdConnectExt == cmd->cmd() || CommandList::cmdTcpConnectExt == cmd->cmd());

    const uint16_t max = ext ? MAX_TASKS : 256;
    const uint16_t min = ext ? 256 : 0;

    for (uint16_t ii = min; ii < max; ii++)
	if (!tasks_[ii])
	    return taskid_t(ii);

    throw ACNET_NLM;
}

void TaskPool::removeInactiveTasks()
{
    // First remove all active tasks that are no longer alive

    for (int ii = 0; ii < MAX_TASKS; ii++)
	if (tasks_[ii] && !tasks_[ii]->stillAlive())
        removeTask(tasks_[ii]);

    // Then delete all removed TaskInfo objects and clear the removed container

    for (auto iter = removed.begin(); iter != removed.end(); iter++)
	delete *iter;

    removed.clear();
}

size_t TaskPool::activeCount() const
{
    size_t count = 0;

    for (int ii = 0; ii < MAX_TASKS; ii++)
	if (tasks_[ii])
	    ++count;

    return count;
}

size_t TaskPool::rumHandleCount() const
{
    size_t total = 0;

    for (int ii = 0; ii < MAX_TASKS; ii++)
	if (tasks_[ii] && tasks_[ii]->isReceiving())
	    ++total;

    return total;
}

size_t TaskPool::requestCount() const
{
    size_t total = 0;

    for (int ii = 0; ii < MAX_TASKS; ii++)
	if (tasks_[ii])
	    total += tasks_[ii]->requestCount();

    return total;
}

size_t TaskPool::replyCount() const
{
    size_t total = 0;

    for (int ii = 0; ii < MAX_TASKS; ii++)
	if (tasks_[ii])
	    total += tasks_[ii]->replyCount();

    return total;
}

TaskInfo *TaskPool::getTask(taskid_t id) const
{
    return tasks_[id.raw()];
}

TaskRangeIterator TaskPool::tasks(taskhandle_t th) const
{
    return active.equal_range(th);
}

bool TaskPool::taskExists(taskhandle_t name) const
{
    auto const ii = active.equal_range(name);

    return ii.first != ii.second;
}

// Searches the TaskInfo objects for one that has the given task name and command port.

TaskInfo* TaskPool::getTask(taskhandle_t th, uint16_t cmdPort) const
{
    auto ii = active.equal_range(th);

    while (ii.first != ii.second) {
	ExternalTask const* const o = dynamic_cast<ExternalTask const*>(ii.first->second);

	if (o && o->commandPort() == cmdPort)
	    return ii.first->second;
	++ii.first;
    }

    return 0;
}

bool TaskPool::isPromiscuous(taskhandle_t name) const
{
    auto ii = tasks(name);

    if (ii.first != ii.second) {
	TaskInfo *task = ii.first->second;

	if (++ii.first == ii.second)
	    return task->isPromiscuous();
    }

    return false;
}

// This function handles incoming Connect commands

void TaskPool::handleConnect(sockaddr_in const& in, ConnectCommand const* const cmd, size_t len)
{
    AckConnect ack;
    AckConnectExt ackExt;
    taskhandle_t clientName = cmd->clientName();
    uint16_t cmdPort = ntohs(in.sin_port);
    uint16_t dataPort = cmd->dataPort();

    // (TP-4)

    try {
	if (dataPort != INADDR_ANY) {
	    removeInactiveTasks();

	    // Convert a blank task name to %dataPort to get
	    // a unique anonymous connection.

	    if (clientName.isBlank()) {
		char buf[8];
		sprintf(buf, "%%%05d", dataPort);
		clientName = taskhandle_t(ator(buf));
	    }

	    // Check to see if we are already connected and if we are, just
	    // return our task id

	    TaskInfo *task = getTask(clientName, cmdPort);

	    if (!task) {
		taskid_t taskId = nextFreeTaskId(cmd);

		// Create a new task based on the connection parameters

		ipaddr_t addr;

		if (nameLookup(nodename_t(clientName), addr) && addr.isMulticast())
		     task = new MulticastTask(*this, clientName, taskId, cmd->pid(),
						cmdPort, dataPort, addr);
		else {
		    if (taskExists(clientName))
			throw ACNET_NAME_IN_USE;

		    if (len == sizeof(TcpConnectCommand))
			task = new RemoteTask(*this, clientName, taskId, cmd->pid(), cmdPort, dataPort,
						((TcpConnectCommand const*) cmd)->remoteAddr());
		    else
			task = new LocalTask(*this, clientName, taskId, cmd->pid(), cmdPort, dataPort);
		}

		active.insert(TaskHandleMap::value_type(task->handle(), task));
		tasks_[taskId.raw()] = task;
	    }

	    ack.setTaskId(task->id());
	    ack.setClientName(clientName);

	    ackExt.setTaskId(task->id());
	    ackExt.setClientName(clientName);
	} else
	    throw ACNET_INVARG;

    } catch (status_t err) {
	ack.setStatus(err);
	ackExt.setStatus(err);
	syslog(LOG_ERR, "failed connect for %s - %d", clientName.str(), err.raw());
    } catch (...) {
	ack.setStatus(ACNET_NLM);
	ackExt.setStatus(ACNET_NLM);
	syslog(LOG_ERR, "failed connect for %s", clientName.str());
    }

//#ifdef DEBUG
    {
	char cBuf[16], nBuf[16];
	syslog(LOG_INFO, "connect: port:%d task:'%s' node:'%s' err:%d",
	       dataPort, clientName.str(cBuf), cmd->virtualNodeName().str(nBuf), ackExt.status().raw());
    }
//#endif

    // Send ack back to the client

    if (cmd->cmd() == CommandList::cmdConnectExt || cmd->cmd() == CommandList::cmdTcpConnectExt) {
	(void) sendto(sClient, &ackExt, sizeof(ackExt), 0, (sockaddr*) &in, sizeof(sockaddr_in));
	syslog(LOG_WARNING, "send extended connect ack");
    } else
	(void) sendto(sClient, &ack, sizeof(ack), 0, (sockaddr*) &in, sizeof(sockaddr_in));
}

size_t TaskPool::fillBufferWithTaskInfo(uint8_t subType, uint16_t rep[])
{
    switch (subType) {
     case 0:
	removeInactiveTasks();
#if (__GNUC__ >= 11)
	[[fallthrough]];
#endif
     case 2:
	{
	    size_t const count = activeCount();
	    uint32_t* ptr = (uint32_t*) (rep + 1);
	    uint8_t* ptr2 = (uint8_t*) (rep + 1 + 2 * count);

	    rep[0] = htoas(count);

	    for (int ii = 0; ii < MAX_TASKS; ii++) {
		if (tasks_[ii]) {
		    *ptr++ = htoal(subType ? tasks_[ii]->pid() : tasks_[ii]->handle().raw());
		    *ptr2++ = (uint8_t) tasks_[ii]->id().raw();
		}
	    }
	    return sizeof(uint16_t) + sizeof(uint32_t) * count + ((count + 1) & ~1);
	}
	break;

     case 1:
	{
	    uint16_t taskHandleCount = 0;
	    uint32_t* ptr = (uint32_t*) (rep + 1);

	    for (int ii = 0; ii < MAX_TASKS; ii++) {
		if (tasks_[ii] && tasks_[ii]->isReceiving()) {
		    *ptr++ = htoal(tasks_[ii]->handle().raw());
		    taskHandleCount++;
		}
	    }

	    size_t idx = 0;
	    for (int ii = 0; ii < MAX_TASKS; ii++) {
		if (tasks_[ii] && tasks_[ii]->isReceiving()) {
		    uint16_t* const tmp = rep + (1 + 2 * taskHandleCount);

		    if (idx & 1)
			tmp[idx / 2] = htoas((tasks_[ii]->id().raw() << 8) | atohs(tmp[idx / 2]));
		    else
			tmp[idx / 2] = htoas(tasks_[ii]->id().raw());
		    ++idx;
		}
	    }
	    rep[0] = htoas(taskHandleCount);
	    return sizeof(uint16_t) + sizeof(uint32_t) * taskHandleCount + ((taskHandleCount + 1) & ~1);
	}
	break;

     case 3:
	removeInactiveTasks();

	// Return task id, handle and status in one shot
	//
	{
	    uint16_t taskHandleCount = 0;
	    typedef struct {
		uint16_t taskId;
		uint8_t flags;
		uint32_t hTask;
		uint32_t pid;
	    } __attribute__((packed)) TaskListEntry;
	    TaskListEntry *ptr = (TaskListEntry *) (rep + 1);

	    for (int ii = 0; ii < MAX_TASKS; ii++) {
		if (tasks_[ii]) {
		    ptr->taskId = htoas(tasks_[ii]->id().raw());
		    ptr->flags = 0;
		    if (tasks_[ii]->isReceiving())
			ptr->flags |= 0x01;
		    ptr->hTask = htoal(tasks_[ii]->handle().raw());
		    ptr->pid = htoal(tasks_[ii]->pid());
		    taskHandleCount++;
		    ptr++;
		}
	    }
	    rep[0] = htoas(taskHandleCount);
	    return sizeof(uint16_t) + (taskHandleCount * sizeof(TaskListEntry));
	}
	break;
    }
    return 0;
}

bool TaskPool::rename(TaskInfo* task, taskhandle_t th)
{
    // If the handle isn't a multi-client handle (i.e. it isn't associated with a multicast address), then only one client
    // can have it at a time. This section checks to see if the handle is owned by an active client.

    if (!isMulticastHandle(th) && !task->isPromiscuous()) {
	{
	    auto ii = active.find(th);

	    if (ii != active.end()) {
		if (ii->second->stillAlive())
		    return false;

		removeTask(ii->second);
	    }
	}

	// Remove this task from the map and insert it with the new task name

	std::pair<TaskHandleMap::iterator, TaskHandleMap::iterator> ii =
	    active.equal_range(task->handle());

	while (ii.first != ii.second)
	    if (ii.first->second->equals(task)) {
		active.erase(ii.first);
		task->setHandle(th);
		active.insert(TaskHandleMap::value_type(th, task));
		return true;
	    } else
		++ii.first;
    }

    return false;
}

size_t TaskPool::fillBufferWithTaskStats(uint8_t subType, void* buf)
{
    typedef struct {
	uint16_t taskId;
	uint32_t hTask;
	uint16_t statCnts[6];
    } __attribute__((packed)) TaskStats;

    typedef struct {
	time48_t statsTime;
	uint16_t taskCount;
	TaskStats stats[];

    } __attribute__((packed)) TaskStatsReply;

    removeInactiveTasks();

    TaskStatsReply* const rpy = (TaskStatsReply*) buf;
    size_t const count = activeCount();

    toTime48(now() - taskStatTimeBase, &rpy->statsTime);

    // Task count plus type code of task stats

    rpy->taskCount = htoas(0x900 + count);

    TaskStats* ts = rpy->stats;

    for (int ii = 0; ii < MAX_TASKS; ii++) {
	TaskInfo *task = tasks_[ii];

	if (task) {
	    ts->taskId = htoas(task->id().raw());
	    ts->hTask = htoal(task->handle().raw());
	    ts->statCnts[0] = htoas((uint16_t) task->stats.usmXmt);
	    ts->statCnts[1] = htoas((uint16_t) task->stats.reqXmt);
	    ts->statCnts[2] = htoas((uint16_t) task->stats.rpyXmt);
	    ts->statCnts[3] = htoas((uint16_t) task->stats.usmRcv);
	    ts->statCnts[4] = htoas((uint16_t) task->stats.reqRcv);
	    ts->statCnts[5] = htoas((uint16_t) task->stats.rpyRcv);

	    if (subType & 1) {
		taskStatTimeBase = now();
		task->stats.usmXmt.reset();
		task->stats.reqXmt.reset();
		task->stats.rpyXmt.reset();
		task->stats.usmRcv.reset();
		task->stats.reqRcv.reset();
		task->stats.rpyRcv.reset();
	    }

	    ts++;
	}
    }

    return sizeof(TaskStatsReply) + sizeof(TaskStats) * count;
}

// Removes all tasks from the active list. This has the side effect of sending EMRs and CANCELs.

void TaskPool::removeAllTasks()
{
    for (int ii = 0; ii < MAX_TASKS; ii++)
	if (tasks_[ii])
	    removeOnlyThisTask(tasks_[ii], ACNET_NODE_DOWN, true);
}

// Removes an entry from the table.

void TaskPool::removeOnlyThisTask(TaskInfo* const task, status_t status, bool sendLastReply)
{
    assert(tasks_[task->id().raw()]);

    tasks_[task->id().raw()] = 0;

    // Find and remove it from the active map.

    auto ii = active.equal_range(task->handle());

    while (ii.first != ii.second)
	if (ii.first->second->equals(task)) {
	    active.erase(ii.first);
	    break;
	} else if (++ii.first == ii.second) {
	    syslog(LOG_ERR, "removeOnlyThisTask: task %s not found", task->handle().str());
	    abort();
	}

    // Now we can free up the resources associated with the task.

    while (task->requests.begin() != task->requests.end())
        reqPool.cancelReqId(*task->requests.begin(), true, sendLastReply);
    while (task->replies.begin() != task->replies.end())
        rpyPool.endRpyId(*task->replies.begin(), status);

#ifdef DEBUG
    syslog(LOG_DEBUG, "removing task '%s' (pid = %d)", task->handle().str(), task->pid());
#endif

    removed.push_back(task);
}

void TaskPool::removeTask(TaskInfo* const task)
{
    pid_t const pid = task->pid();

    if (0 == pid)

	// If the task pid is 0 then only remove the given task

	removeOnlyThisTask(task);
    else {

	// Otherwise, loop through all the active tasks removing everthing
	// with the given pid

	for (int ii = 0; ii < MAX_TASKS; ii++)
	    if (tasks_[ii] && pid == tasks_[ii]->pid())
		removeOnlyThisTask(tasks_[ii]);
    }
}

#ifndef NO_REPORT
void TaskPool::generateNodeDataReport(std::ostream& os)
{
    os << "\t\t<div class=\"section\">\n\t\t<h1>Global Statistics</h1>\n";

    os << "\t\t<table class=\"dump\">\n"
	"\t\t\t<colgroup>\n"
	"\t\t\t\t<col class=\"label\"/>\n"
	"\t\t\t\t<col/>\n"
	"\t\t\t</colgroup>\n"
	"\t\t\t<tbody>\n";

    {
	static struct {
	    char const* descr;
	    StatCounter const* count;
	} const data[] = {
	    { "Received USMs", &stats.usmRcv },
	    { "Received Requests", &stats.reqRcv },
	    { "Received Replies", &stats.rpyRcv },
	    { "Transmitted USMs", &stats.usmXmt },
	    { "Transmitted Requests", &stats.reqXmt },
	    { "Transmitted Replies", &stats.rpyXmt },
	};

	for (size_t ii = 0; ii < sizeof(data) / sizeof(*data); ++ii)
	    os << "\t\t\t\t<tr" << (ii % 2 ? "" : " class=\"even\"") << ">"
		"<td class=\"label\">" << data[ii].descr << "</td>"
		"<td>'" << std::setw(13) << std::setfill(' ') << (uint32_t) *data[ii].count << "'</td></tr>\n";
    }
    os << "\t\t\t</tbody>\n"
	"\t\t</table>\n"
	"\t\t</div>\n";
}

void TaskPool::generateTaskReport(std::ostream& os) const
{
    os << "\t\t<div class=\"section\">\n\t\t<h1>Connected Tasks Report</h1>\n";

    for (int ii = 0; ii < MAX_TASKS; ii++)
	if (tasks_[ii])
	    tasks_[ii]->report(os);

    os << "\t\t</div>\n" << std::endl;
}


// Main entry point for generating a report of the internal state of acnetd.

void TaskPool::generateReport()
{
#ifndef NO_REPORT
    pid_t pidReport = -1;

    // Fork the process. This does several things: 1) It lets the parent process get back to servicing acnet packets as soon
    // as possible. 2) It gives the child process a snapshot of the internal data structures. Since Unix follows
    // copy-on-write semantics, any data the parent changes will get copied into a new memory page and the child is free to
    // take its time in running through the data structures.


    if (!(pidReport = fork())) {

	// Lower our priority so the parent can do its work.

	setpriority(PRIO_PROCESS, 0, 1);
	signal(SIGCHLD, SIG_DFL);

	char file[256];

	strcpy(file, "/tmp/acnet_");
	strcat(file, nodeName().str());
	strcat(file, ".html");

	syslog(LOG_INFO, "report process started to file '%s'", file);

	{
	    std::ofstream os(file);

	    os << "Subject: ACNET Report\n"
		"Content-type: text/html; charset=us-ascii\n\n"
		"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" "
		"\"http://www.w3.org/TR/html4/loose.dtd\">\n"
		"<html>\n"
		"\t<head>\n"
		"\t\t<title>Acnet Report</title>\n"
		"\t\t<style type=\"text/css\">\n"
		"body {\n"
		"  font: 10pt Verdana,Arial,Helvectica,san-serif;\n"
		"}\n\n"
		"h1 {\n"
		"  font-size: 12pt;\n"
		"}\n\n"
		"div.section {\n"
		"  padding: 10pt;\n"
		"}\n\n"
		".label:after {\n"
		"  content: \":\";\n"
		"}\n\n"
		".label {\n"
		"  text-align: right;\n"
		"  padding-right: 1em;\n"
		"}\n\n"
		"thead {\n"
		"  text-align: left;\n"
		"  background: gray;\n"
		"  color: white;\n"
		"}\n\n"
		"table.dump {\n"
		"  width: 45em;\n"
		"  margin-top: 12pt;\n"
		"}\n\n"
		"tr.even {\n"
		"  background: #e0ffe0;\n"
		"}\n"
		"\t\t</style>\n"
		"\t</head>\n"
		"\t<body>\n";

	    os << "\t\t<div class=\"section\">\n\t\t\t<h1>Report for ACNET Node " << nodeName().str() << "</h1>\n\t\t</div>\n";

	    // Call the various report functions.

	    generateNodeDataReport(os);
	    generateTaskReport(os);
	    reqPool.generateReqReport(os);
	    rpyPool.generateRpyReport(os);
	    generateIpReport(os);

	    os << "\t</body>\n"
		"</html>\n";
	}

	chmod(file, DEFFILEMODE);

	char command[512];

	strcpy(command, "/usr/sbin/sendmail neswold@fnal.gov kingc@fnal.gov < ");
	strcat(command, file);

	exit(system(command));
    }
#endif
}

#endif
