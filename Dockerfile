FROM adoptopenjdk/openjdk17:ubi

ADD target/portfolio-1.0-SNAPSHOT.jar webapp.jar
EXPOSE 8080
CMD ["java","-jar","webapp.jar"]