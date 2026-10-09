# Nano Java build environment: devkitARM (libnds/calico) for the DS port,
# the system gcc for the host build used in tests, and JDK 8 for the
# class library (JDK 8 is the last javac that emits J2ME-era class files).
FROM eclipse-temurin:8-jdk AS jdk

FROM devkitpro/devkitarm:latest
COPY --from=jdk /opt/java/openjdk /opt/jdk8
ENV PATH=/opt/jdk8/bin:$PATH
RUN apt-get update \
 && apt-get install -y --no-install-recommends make libc6-dev zip unzip \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /src
