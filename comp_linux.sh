mkdir -p tmp/WEB_INF/classes
cp -r $1/bin/pack tmp/WEB_INF/classes/.
cp -r $1/lib/ tmp/WEB_INF/.
cp -r $1/src/webcontent/* tmp/.
jar cf $1.war -C tmp .
rm -rf tmp