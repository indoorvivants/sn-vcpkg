FROM eclipse-temurin:17-focal

RUN apt update && apt install -y curl && \
    curl -Lo /usr/local/bin/sbt https://raw.githubusercontent.com/sbt/sbt/1.8.x/sbt && \
    chmod +x /usr/local/bin/sbt && \
    curl -Lo llvm.sh https://apt.llvm.org/llvm.sh && \
    chmod +x llvm.sh && \
    apt install -y lsb-release wget software-properties-common gnupg && \
    ./llvm.sh 13 && \
    apt update && \
    apt install -y zip unzip tar make cmake autoconf pkg-config libclang-13-dev

COPY . /sources

ENV LLVM_BIN "/usr/lib/llvm-13/bin"
ENV CC "/usr/lib/llvm-13/bin/clang"
ENV PATH="${PATH}:/root/.local/share/coursier/bin"

ENV SBT_VCPKG_VERSION dev
RUN cd /sources && sbt publishLocal
RUN cd /sources/example && sbt run
