FROM public.ecr.aws/amazoncorretto/amazoncorretto:21
ARG JAR_FILE=./build/libs/*.jar
COPY ${JAR_FILE} DocMaestroEngine.jar
ENV JAVA_OPTS="-Xms2g -Xmx8g -Duser.timezone=America/New_York"
ENTRYPOINT ["java","-jar","/DocMaestroEngine.jar"]