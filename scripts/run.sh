#!/bin/bash

# Copyright (c) 2017 Intel Corporation
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

USER_DIR=$(echo ${HADOOP_USER_NAME} | sed 's|/|_|')
export KRB5CCNAME="/tmp/${USER_DIR}@CLOUDERA"
export YARN_CONF_DIR="/etc/hadoop"
export GEARPUMP_NAME="gearpump-${GEARPUMP_PACK_VERSION}"

export GEARPUMP_DASHBOARD_STATE_VALIDATOR_RETRY_ENABLED=true
export GEARPUMP_DASHBOARD_STATE_VALIDATOR_RETRY_INTERVAL=10
export GEARPUMP_DASHBOARD_STATE_VALIDATOR_RETRY_MAX_RETRIALS=6

env | sort

if [[ ! -e ${GEARPUMP_NAME}.zip ]]; then
    echo "Required ${GEARPUMP_NAME}.zip does not exist in this folder:" `pwd`
    exit 1
fi

echo "Unzip ./${GEARPUMP_NAME}.zip"
unzip ./${GEARPUMP_NAME}.zip

echo "Change mode ./${GEARPUMP_NAME}/bin/"
chmod -R +x ./${GEARPUMP_NAME}/bin/

echo "Copy ${YARN_CONF_DIR}/* to ./${GEARPUMP_NAME}/conf/yarnconf"
cp -RL ${YARN_CONF_DIR}/* ./${GEARPUMP_NAME}/conf/yarnconf

echo "Done! Run broker..."
exec java -jar gearpump-broker-${GEARPUMP_BROKER_VERSION}.jar
