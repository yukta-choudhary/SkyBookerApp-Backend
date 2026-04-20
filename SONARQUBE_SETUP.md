# SonarQube Configuration for SkyBooker Backend Services
# Run: mvn sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<YOUR_TOKEN>

# === AUTH SERVICE ===
# cd auth-service && mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<TOKEN>

# === PAYMENT SERVICE ===
# cd payment-service && mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<TOKEN>

# === ALL SERVICES (run from each service dir) ===
# mvn clean test jacoco:report sonar:sonar \
#     -Dsonar.host.url=http://localhost:9000 \
#     -Dsonar.token=<YOUR_TOKEN>

# Quality Gate Target: 80%+ line coverage
# Coverage report: target/site/jacoco/index.html

# SonarQube Docker setup (if not installed):
# docker run -d --name sonarqube -p 9000:9000 sonarqube:lts-community
# Default login: admin / admin

# Maven SonarQube plugin (add to pom.xml if needed):
# mvn dependency:resolve -Dartifact=org.sonarsource.scanner.maven:sonar-maven-plugin:3.10.0.2594
