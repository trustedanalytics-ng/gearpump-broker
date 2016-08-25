set -e

VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\[' | tail -1)
PROJECT_NAME=$(basename $(pwd))
PACKAGE_CATALOG=$PROJECT_NAME-$VERSION
JAR_NAME="$PACKAGE_CATALOG.jar"

############################ GEARPUMP BROKER #############################

GEARPUMP_BROKER_FILE_ARCHIVE=$PACKAGE_CATALOG.tar.gz

if [ -e $GEARPUMP_BROKER_FILE_ARCHIVE ] ; then
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

# build project
mvn clean install

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
rm -r $PACKAGE_CATALOG

echo "tar.gz package for $PROJECT_NAME project in version $VERSION has been prepared."

############################ GEARPUMP DASHBOARD #############################
TMP_CATALOG=/tmp/gearpump-binaries

rm -rf $TMP_CATALOG
mkdir -p $TMP_CATALOG

unzip $GEARPUMP_FILE_ZIP -d $TMP_CATALOG

cd scripts
./prepare.sh $TMP_CATALOG/$GEARPUMP_FOLDER $TMP_CATALOG

cd ..

# prepare files to archive
mv $TMP_CATALOG/target/gearpump-dashboard.tar.gz gearpump-dashboard-${VERSION}.tar.gz
echo "commit_sha=$(git rev-parse HEAD)" > build_info.ini

# clean temporary data
rm -r $TMP_CATALOG

echo "tar.gz package for gearpump-dashboard project in version $VERSION has been prepared."
