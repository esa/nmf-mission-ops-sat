#!/bin/sh

# NMF_LIB can be provided by the parent app (i.e. supervisor) or set locally
if [ -z "$NMF_LIB" ] ; then
    NMF_LIB=@NMF_LIB@
fi

# Replaced with the main class name
MAIN_CLASS_NAME=@MAIN_CLASS_NAME@

exec java $JAVA_OPTS \
  -classpath "$NMF_LIB:lib/*:/usr/lib/java/*" \
  "$MAIN_CLASS_NAME" \
  "$@"

