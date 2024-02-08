# Sets up the GitPod shell using gitpod's base workspace which then
# also sets up JDK 8 using SDKMAN for GitPod as finalization.

FROM gitpod/workspace-base
USER gitpod

SHELL ["/bin/bash", "-c"]

RUN curl -s "https://get.sdkman.io" | bash && source ~/.sdkman/bin/sdkman-init.sh && sdk install java 8.0.402-tem && sdk use java 8.0.402-tem