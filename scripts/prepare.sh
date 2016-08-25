#!/usr/bin/env bash

##############################################################################
## prepare.sh GP_HOME DEST_DIR
##
##   GP_HOME - directory where binary distribution of GearPump is placed
##   DEST_DIR - directory where output files should be placed
##
## This script also expects 'dashboard.sh' and 'dashboard-manifest.yml'
## to be present in the directory it's ran from.
##############################################################################
##
## Copyright (c) 2015 Intel Corporation
##
## Licensed under the Apache License, Version 2.0 (the "License");
## you may not use this file except in compliance with the License.
## You may obtain a copy of the License at
##
## http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
##############################################################################
if [ "$#" -ne 2 ]; then
    echo "Illegal number of arguments. GP_HOME and DEST_DIR required (see source code)."
    echo "Example usage: ./prepare.sh gearpump-2.11-0.8.0 gearpump-dashboard"
    exit 1
fi

echo "Preparing the Gearpump dashboard tar.gz package..."

GP_HOME=$1
DEST_DIR=$2

#set -x

if [ ! -d "$DEST_DIR" ]; then
  mkdir $DEST_DIR
fi

mkdir $DEST_DIR/bin
mkdir $DEST_DIR/lib
mkdir $DEST_DIR/lib/daemon
mkdir $DEST_DIR/lib/services
mkdir $DEST_DIR/conf
mkdir $DEST_DIR/dashboard

# copy dependencies
cp $GP_HOME/conf/* $DEST_DIR/conf
cp -r $GP_HOME/dashboard/* $DEST_DIR/dashboard
cp $GP_HOME/lib/* $DEST_DIR/lib
cp $GP_HOME/lib/daemon/* $DEST_DIR/lib/daemon
cp $GP_HOME/lib/services/* $DEST_DIR/lib/services
cp $GP_HOME/VERSION $DEST_DIR/lib/
cp $GP_HOME/VERSION $DEST_DIR/

# prepare gearpump-dashboard overrides

TARGET_FOLDER=$(pwd)/../target/tap-gearpump-dashboard
TAP_DASHBOARD_JAVA=$(pwd)/../src/dashboard/java
CUSTOM_AUTHENTICATOR_CLASS=io/gearpump/services/security/oauth2/impl/CustomCloudFoundryUAAOAuth2Authenticator

rm -rf $TARGET_FOLDER
mkdir -p $TARGET_FOLDER

echo "Compiling tap-gearpump-dashboard classes"
javac -cp $DEST_DIR/lib/services/gearpump-services_2.11-0.8.0.jar:$DEST_DIR/lib/config-1.3.0.jar:$DEST_DIR/lib/scala-library-2.11.8.jar -d $TARGET_FOLDER $TAP_DASHBOARD_JAVA/$CUSTOM_AUTHENTICATOR_CLASS.java

echo "Creating tap-gearpump-dashboard.jar file"
jar cvf $DEST_DIR/lib/tap-gearpump-dashboard.jar -C $TARGET_FOLDER $CUSTOM_AUTHENTICATOR_CLASS.class

# compute classpath
CP_STRING=""
JAR_PREFIX=\$APP_HOME

CP_STRING+=$JAR_PREFIX/lib:$JAR_PREFIX/lib/*:$JAR_PREFIX/lib/services:$JAR_PREFIX/lib/services/*:$JAR_PREFIX/lib/daemon:$JAR_PREFIX/lib/daemon/*:
CP_STRING+=$JAR_PREFIX/dashboard/:$JAR_PREFIX/dashboard/*:$JAR_PREFIX/conf/:$JAR_PREFIX/conf/*

echo $CP_STRING

#copy starting script
cp dashboard.sh $DEST_DIR/bin/dashboard

#set execution permissions
chmod 766 $DEST_DIR/bin/dashboard

#change CLASSPATH
sed -i "s|CLASSPATH=TOCHANGE|CLASSPATH=${CP_STRING}|" "$DEST_DIR/bin/dashboard"

#copy manifest
cp dashboard-manifest.yml $DEST_DIR/manifest.yml

cd $DEST_DIR/
#make tar.gz in target directory
mkdir target/
tar -zcf target/gearpump-dashboard.tar.gz bin/ lib/ conf/ dashboard/ VERSION
