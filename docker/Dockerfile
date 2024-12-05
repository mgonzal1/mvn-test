FROM --platform=linux/amd64 adregistry.fnal.gov/external/almalinux:9

# Update the package manager and install Java 8 JDK
RUN dnf -y update && \
    dnf install -y java-21-openjdk-devel \
                    openssl-devel \
                    rsyslog \
                    gcc \
                    g++ gcc-c++ make tcpdump nc vim && \
    dnf clean all

# Set the JAVA_HOME environment variable
ENV JAVA_HOME /usr/lib/jvm/java-21-openjdk

ARG DPM_RELEASE='default'

ENV DPM_RELEASE=$DPM_RELEASE

# Add Java and GCC to the system PATH
ENV PATH $PATH:$JAVA_HOME/bin

# ====== Install ACNETd ======
COPY docker/acnetd/ /opt/acnetd
WORKDIR /opt/acnetd
RUN make DEBUG=1 NO_DAEMON=1 && cp /opt/acnetd/acnetd /usr/local/bin/

RUN mkdir -p /usr/local/dae/bin
COPY docker/scripts/ /usr/local/dae/bin/

COPY target/*.jar /usr/local/dae/jars/
COPY lib/*.jar /usr/local/dae/jars/

RUN useradd acnetd
USER acnetd

CMD ["/bin/bash", "-c", "/usr/local/dae/bin/start_dpm"]
