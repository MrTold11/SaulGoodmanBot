FROM openjdk:17-slim

COPY dir/SaulGoodmanBot-1.0-SNAPSHOT.jar /application.jar
COPY dir/config.json /config.json
COPY dir/docs/ /docs/
COPY dir/strings.json /strings.json

RUN apt-get update && apt-get install -y libfontconfig1 fontconfig
COPY dir/fonts /usr/share/fonts

EXPOSE 8080

ENTRYPOINT ["java","-jar","-Xmx512M","-Xms128M","/application.jar"]