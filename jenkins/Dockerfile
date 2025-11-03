FROM jenkins/jenkins:lts-jdk11

# Switch to root to install packages
USER root

# Install Docker CLI
RUN apt-get update && \
    apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release && \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && \
    apt-get install -y docker-ce-cli && \
    rm -rf /var/lib/apt/lists/*

# Install kubectl
RUN curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" && \
    install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl && \
    rm kubectl

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Install curl for health checks
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Switch back to jenkins user
USER jenkins

# Install Jenkins plugins
RUN jenkins-plugin-cli --plugins \
    blueocean \
    docker-workflow \
    kubernetes \
    maven-plugin \
    junit \
    htmlpublisher \
    performance \
    build-timeout \
    credentials-binding \
    timestamper \
    ws-cleanup \
    ant \
    gradle \
    workflow-aggregator \
    git \
    github \
    github-branch-source \
    pipeline-stage-view \
    pipeline-github-lib \
    pipeline-stage-tags-metadata \
    pipeline-graph-analysis \
    pipeline-milestone-step \
    pipeline-input-step \
    pipeline-stage-step \
    pipeline-rest-api \
    pipeline-build-step \
    pipeline-utility-steps \
    plain-credentials \
    ssh-credentials \
    matrix-auth \
    pam-auth \
    ldap \
    email-ext \
    mailer \
    slack \
    telegram-notifications \
    discord-notifier \
    matrix-project \
    resource-disposer \
    ssh-slaves \
    matrix-auth \
    pam-auth \
    ldap \
    email-ext \
    mailer

# Copy Jenkins configuration
COPY jenkins.yaml /usr/share/jenkins/ref/casc.yaml

# Copy job configurations
COPY jobs/ /usr/share/jenkins/ref/jobs/

# Set environment variables
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
ENV CASC_JENKINS_CONFIG=/usr/share/jenkins/ref/casc.yaml
