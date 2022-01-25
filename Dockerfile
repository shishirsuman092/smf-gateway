FROM openjdk:8
MAINTAINER haridas <haridas.kakunje@tarento.com>

ADD zuul-0.0.1-SNAPSHOT.jar zuul-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "/zuul-0.0.1-SNAPSHOT.jar"]
EXPOSE 8081
