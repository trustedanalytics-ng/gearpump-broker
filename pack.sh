#
# Copyright (c) 2016 Intel Corporation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

VERSION=0.8.3
PROJECT_NAME=gearpump-broker
PACKAGE_NAME=$PROJECT_NAME-$VERSION
JAR_NAME="$PACKAGE_NAME.jar"
PACKAGE_CATALOG="target/$PACKAGE_NAME"

############################ GEARPUMP BROKER #############################

GEARPUMP_BROKER_FILE_ARCHIVE=$PACKAGE_NAME.tar.gz
GEARPUMP_BROKER_FILE_ARCHIVE_FULL_PATH=$PACKAGE_CATALOG/$PACKAGE_NAME.tar.gz

if [ -e $GEARPUMP_BROKER_FILE_ARCHIVE_FULL_PATH ] ; then
    echo "Package $GEARPUMP_BROKER_FILE_ARCHIVE already exists. Exiting."
    exit 0
fi

# download gearpump binaries
GEARPUMP_PACK_FULL_VER=$(cat src/cloudfoundry/manifest.yml | grep GEARPUMP_PACK_VERSION | cut -d ' ' -f 6- | sed 's/["]//g')
GEARPUMP_PACK_SHORT_VER=$(echo $GEARPUMP_PACK_FULL_VER | cut -d '-' -f 2-)
GEARPUMP_FOLDER=gearpump-$GEARPUMP_PACK_FULL_VER
GEARPUMP_FILE_ZIP=$GEARPUMP_FOLDER.zip

if [ -e $GEARPUMP_FILE_ZIP ] ; then
    echo "Package $GEARPUMP_FILE_ZIP already downloaded."
else
    echo "Downloading $GEARPUMP_FILE_ZIP..."
    curl --location --retry 3 --insecure https://github.com/gearpump/gearpump/releases/download/$GEARPUMP_PACK_SHORT_VER/$GEARPUMP_FILE_ZIP -o $GEARPUMP_FILE_ZIP
fi

# create tmp catalog
rm -rf $PACKAGE_CATALOG
mkdir $PACKAGE_CATALOG

# files to package
cp $GEARPUMP_FILE_ZIP $PACKAGE_CATALOG
cp scripts/run.sh $PACKAGE_CATALOG
cp src/cloudfoundry/manifest.yml $PACKAGE_CATALOG
cp target/$JAR_NAME $PACKAGE_CATALOG

# prepare build manifest
echo "commit_sha=$(git rev-parse HEAD)" > $PACKAGE_CATALOG/build_info.ini

# create zip package
cd $PACKAGE_CATALOG
tar -zcf ../$GEARPUMP_BROKER_FILE_ARCHIVE *
cd ..

# remove tmp catalog
rm -r $PACKAGE_NAME

echo "tar.gz package for $PROJECT_NAME project in version $VERSION has been prepared."


