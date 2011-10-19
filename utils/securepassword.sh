#!/bin/sh
JETTY_VERSION=7.4.5.v20110725
GRADLE_CACHE=~/.gradle/cache/org.eclipse.jetty
java -cp $GRADLE_CACHE/jetty-util/jars/jetty-util-$JETTY_VERSION.jar:$GRADLE_CACHE/jetty-http/jars/jetty-http-$JETTY_VERSION.jar org.eclipse.jetty.http.security.Password "$@"
