FROM openjdk:8-slim
ARG PROXY_HOST
ARG PROXY_PORT
ARG PROXY_USER
ARG PROXY_PASS

COPY gradle.properties /root/.gradle/gradle.properties
RUN \
  sed -i /root/.gradle/gradle.properties -e "s/PROXY_HOST/$PROXY_HOST/g" &&\
  sed -i /root/.gradle/gradle.properties -e "s/PROXY_PORT/$PROXY_PORT/g" &&\
  sed -i /root/.gradle/gradle.properties -e "s/PROXY_USER/$PROXY_USER/g" &&\
  sed -i /root/.gradle/gradle.properties -e "s/PROXY_PASS/$PROXY_PASS/g"

WORKDIR /src
COPY project /src/
RUN \
  sed -i gradlew -e '1a JAVA_OPTS="-DproxyHost=$PROXY_HOST -DproxyPort=$PROXY_PORT -Dhttp.proxyUser=$PROXY_USER -Dhttp.proxyPassword=$PROXY_PASS -Dhttps.proxyUser=$PROXY_USER -Dhttps.proxyPassword=$PROXY_PASS"'
RUN cd export && bash export.sh

RUN \
  apt-get update &&\
  apt-get install -y libfontconfig libfreetype6 &&\
  apt-get clean &&\
  rm -rf /var/lib/apt/lists/*
