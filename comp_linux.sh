#!/bin/bash

# Build back with Maven
echo "Building back..."
cd back/facade
chmod +x ./mvnw
./mvnw clean package
cd ../../

# Build front
echo "Building front..."
mkdir -p tmp/WEB_INF/classes
cp -r front/bin/pack tmp/WEB_INF/classes/.
cp -r front/lib/ tmp/WEB_INF/.
cp -r front/src/webcontent/* tmp/.
jar cf front.war -C tmp .
rm -rf tmp

# Move wars to Tomcat webapps
echo "Moving wars to Tomcat webapps..."
cp back/facade/target/back.war apache-tomcat-11.0.1/webapps/back.war
mv front.war apache-tomcat-11.0.1/webapps/

echo "Build and deployment complete."