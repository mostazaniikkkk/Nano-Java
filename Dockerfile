# Pstros NDS build environment — BlocksDS + Wonderful Toolchain
# Image: skylyrac/blocksds:dev-v1.20.0 (Docker Hub, public)
# Includes: arm-none-eabi toolchain, libnds, libfat, ndstool, picolibc
FROM skylyrac/blocksds:dev-v1.20.0

# Upgrade OS packages to patch known vulnerabilities in the base image,
# then install host tools needed for JCC and the preverifier.
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
        openjdk-8-jdk-headless \
        build-essential \
        ant \
    && rm -rf /var/lib/apt/lists/*

# Wonderful Toolchain is the root; BlocksDS lives inside it as a thirdparty package.
# These paths are set by the base image — we only pin them here for sub-processes
# (build.sh, make) that might not inherit the login-shell environment.
ENV BLOCKSDS=/opt/wonderful/thirdparty/blocksds/core
ENV BLOCKSDSEXT=/opt/wonderful/thirdparty/blocksds/external
ENV WONDERFUL_TOOLCHAIN=/opt/wonderful
ENV PATH=/opt/wonderful/toolchain/gcc-arm-none-eabi/bin:/opt/wonderful/bin:${PATH}

WORKDIR /project

CMD ["bash"]
