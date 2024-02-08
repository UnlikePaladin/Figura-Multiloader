# Sets up the GitPod shell using the Java 8 workspace which then
# also sets SDKMAN for GitPod as finalization.

FROM gitpod/workspace-java-8
USER gitpod

SHELL ["/bin/bash", "-c"]

RUN source ~/.sdkman/bin/sdkman-init.sh && sdk install 8.0.402-tem && sdk use java 8.0.402-tem
