#!/bin/bash

# Build back with Maven
echo "Building back..."
cd back/facade
chmod +x ./mvnw
./mvnw clean package
cd ../../

# Build front
echo "Building front..."
mkdir -p tmp/WEB-INF/classes
cp -r front/bin/pack tmp/WEB-INF/classes/.
cp -r front/lib/ tmp/WEB-INF/.
cp -r front/src/webcontent/* tmp/.
jar cf front.war -C tmp .
rm -rf tmp

# Move wars to Tomcat webapps
echo "Moving wars to Tomcat webapps..."
TOMCAT_DIR="apache-tomcat-11.0.1"
if [ -d "$TOMCAT_DIR" ]; then
    cp back/facade/target/back.war "$TOMCAT_DIR/webapps/back.war"
    mv front.war "$TOMCAT_DIR/webapps/front.war"
    echo "Wars moved to $TOMCAT_DIR/webapps/"
else
    echo "Error: $TOMCAT_DIR not found. Wars not moved."
fi

echo "Build and deployment complete."