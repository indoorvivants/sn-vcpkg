FROM ubuntu:22.04 as dev

RUN apt-get update && apt-get install -y openjdk-17-jdk \
    clang zip unzip tar make cmake autoconf ninja-build \
    pkg-config git libtool curl

# Coursier
RUN curl -fL "https://github.com/coursier/launchers/raw/master/cs-$(uname -m)-pc-linux.gz" | gzip -d > /usr/bin/cs && \
    chmod +x /usr/bin/cs
# Sbt
RUN curl -Lo /usr/bin/sbt https://raw.githubusercontent.com/sbt/sbt/1.9.x/sbt && chmod +x /usr/bin/sbt

WORKDIR /workdir/source
COPY . .
RUN sbt publishLocal versionDump
RUN export SN_VCPKG_VERSION=$(cat version)
RUN cs bootstrap "com.indoorvivants.vcpkg:sn-vcpkg_3:$(cat version)" -f -o /usr/bin/sn-vcpkg

WORKDIR /workdir 
RUN rm -rf source
