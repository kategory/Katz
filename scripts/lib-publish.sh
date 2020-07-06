#!/bin/bash

set -e
. $BASEDIR/arrow/scripts/commons4gradle.sh

ARROW_LIB=$1

echo "Check and prepare the environment ..."
for lib in $(cat $BASEDIR/arrow/lists/libs.txt); do
    checkAndDownloadViaHTTPS $lib master
done

replaceOSSbyLocalRepository $BASEDIR/arrow/generic-conf.gradle
for lib in $(cat $BASEDIR/arrow/lists/libs.txt); do
    replaceGlobalPropertiesbyLocalConf $BASEDIR/$lib/gradle.properties
    $BASEDIR/arrow/scripts/project-install.sh $lib
done
$BASEDIR/arrow/scripts/project-publish.sh $ARROW_LIB