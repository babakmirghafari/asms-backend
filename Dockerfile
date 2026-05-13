FROM eclipse-temurin:25-jre-alpine AS runtime

# Add non-root user for security
RUN addgroup -S asms && adduser -S asms -G asms

WORKDIR /app

# Copy the fat JAR
COPY target/asms-backend-*.jar app.jar

# Set ownership
RUN chown -R asms:asms /app

USER asms

EXPOSE 8080

# Virtual threads + 75% heap of container memory
ENTRYPOINT ["java", \
  "-XX:+UseVirtualThreads", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
