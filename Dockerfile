FROM openjdk:17-slim

COPY dir/SaulGoodmanBot-1.0-SNAPSHOT.jar /application.jar
COPY dir/config.json /config.json
COPY dir/docs/agreement.jpg /docs/agreement.jpg
COPY dir/strings.json /strings.json

EXPOSE 8080

ENTRYPOINT ["java","-jar","-Xmx512M","-Xms128M","/application.jar"]