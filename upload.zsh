#!/usr/bin/env zsh

# Can't store it in /tmp because safe deletes everything in /tmp for
# every run
if [ -f ~/gh-upload-1.0-standalone.jar ]; then
    cp ~/gh-upload-1.0-standalone.jar .
fi

if [ ! -f gh-upload-1.0-standalone.jar ]; then
    wget https://github.com/downloads/dakrone/gh-upload/gh-upload-1.0-standalone.jar
    cp gh-upload-1.0-standalone.jar ~/
fi

FILE=$(ls target/releases/*.zip)

echo "Uploading $FILE to $GHREPO..."
java -jar gh-upload-1.0-standalone.jar $FILE

