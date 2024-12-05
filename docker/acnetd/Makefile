# You can define several symbols to alter the final executable that gets
# built. Simply define the symbol(s) on the command line like so:
#
#	$ make SYMBOL=1 [...]
#
# Symbol	Description
#
# DEBUG		The project is, by default, built without debugging
#		features enabled. Not defining this symbol does several
#		things: assert() macros aren't compiled and compiler
#		optimization levels are increased.
#
# NO_REPORT	If this symbol is defined, none of the report
#		generating code is compiled into the executable.
#
# PROFILE	Enable profiling code to be added. Also invokes
#		NO_DAEMON.
#
# NO_DAEMON	Builds an ACNETD that doesn't become a background
#		task.
#
# NO_SWAP	Doesn't swap bytes when packets are sent or received
#		over the network. All ACNET nodes on a network need to
#		be built the same way with respect to this option.
#
# PINGER	Compile in the internal task that occasionally
#		pings nodes to which it's replying.
#
# KEEP_ALIVE	Enables the request timeout code. Requests now require
#		timely replies to avoid being timed-out. If a reply
#		isn't sent, acnetd will send an ACNET_PEND to keep the
#		request alive.
#

THIS_PLATFORM:=	$(shell uname -s)
THIS_ARCH:=	$(shell uname -p)

ACNETD=		acnetd
ACNETD_OBJS=	main.o taskinfo.o inttask.o exttask.o mctask.o lcltask.o remtask.o \
		taskpool.o ipaddr.o network.o acnaux.o reqinfo.o rpyinfo.o \
		mcast.o global.o rad50.o node.o timesensitive.o tcpclient.o \
		rawhandler.o wshandler.o

VALIDATOR=	validator
VALIDATOR_OBJS=	regression.o global.o rad50.o

TARGETS=	${ACNETD}
#-I../../uls/ul_acnetd -L../../uls/ul_acnetd
CFLAGS+=	-pipe -W -Wall  -Werror -I/usr/include/openssl -fno-strict-aliasing\
		-DTHIS_PLATFORM=\"${THIS_PLATFORM}\" -DTHIS_ARCH=\"${THIS_ARCH}\" \
		-DTHIS_TARGET=${THIS_PLATFORM}_Target -Wno-deprecated-declarations \
		-std=c++0x

ifdef NO_REPORT
CFLAGS+=	-DNO_REPORT
endif

ifdef PINGER
CFLAGS+=	-DPINGER
endif

ifdef DEBUG
CFLAGS+=	-DDEBUG -g -O0
else
CFLAGS+=	-O2 -DNDEBUG
endif

ifdef PROFILE
CFLAGS+=	-pg -DPROFILE -DNO_DAEMON -fprofile
else
ifdef NO_DAEMON
CFLAGS+=	-DNO_DAEMON
endif
endif

ifdef NO_SWAP
CFLAGS+=	-DNO_SWAP
endif

ifdef KEEP_ALIVE
CFLAGS+=	-DKEEP_ALIVE
endif

CXXFLAGS+=	${CFLAGS} ${APPEND_CFLAGS}
LDFLAGS=	-lcrypto

ifeq (${THIS_PLATFORM}, SunOS)
LDFLAGS+= 	-lresolv -lsocket -lnsl
endif

ifeq (${THIS_PLATFORM}, NetBSD)
LDFLAGS+= 	-lutil
endif

CXX?=		g++
RANLIB?=	ranlib
ARCHIVER?=	ar

# Target list...

all : ${TARGETS}

TAGS : $(wildcard *.cpp) $(wildcard *.h)
	etags -o $@ $^

${ACNETD} : ${ACNETD_OBJS}
	${CXX} ${CXXFLAGS} ${LDFLAGS} -o $@ $^

${VALIDATOR} : ${VALIDATOR_OBJS}
	${CXX} ${CXXFLAGS} ${LDFLAGS} -o $@ $^

${ACNETD_OBJS} : server.h node.h trunknode.h timesensitive.h idpool.h

.PHONY : clean

clean :
	@rm -f ${TARGETS} *.o ${VALIDATOR_OBJS} *~

# Local Variables:
# mode:makefile
# End:
