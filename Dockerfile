FROM confluentinc/cp-kafka-connect:7.4.0

USER root

# Install Debezium MySQL Connector
RUN curl -L https://repo1.maven.org/maven2/io/debezium/debezium-connector-mysql/2.5.1.Final/debezium-connector-mysql-2.5.1.Final-plugin.tar.gz \
  | tar -xz -C /usr/share/java

# Copy custom SMT plugin
COPY target/company-smt-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/share/java/kafka-connect-plugins/
COPY target/company-smt-1.0-SNAPSHOT.jar /usr/share/java/kafka-connect-plugins/

USER appuser
