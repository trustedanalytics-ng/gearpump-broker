[![Dependency Status](https://www.versioneye.com/user/projects/57236753ba37ce0031fc1d55/badge.svg?style=flat)](https://www.versioneye.com/user/projects/57236753ba37ce0031fc1d55)

# gearpump-broker

Cloud Foundry broker for Apache Gearpump (see: [http://gearpump.apache.org/](http://gearpump.apache.org/)).

Apache Gearpump broker spawns Apache Gearpump UI (dashboard) on Cloud Foundry (using application-broker) and submits Apache Gearpump to YARN.
That's why it needs YARN service instance and gearpump-dashboard already prepared to work.

# How to use it?
To use gearpump-broker, you need to build it from sources, configure, deploy application, register the broker in TAP. Follow steps described below.

## Build
Broker uses Apache Gearpump binaries internally so, before building, you need to obtain them and put in ``src/main/resources/gearpump``.

Run command for compile and package:
```
mvn clean package
```

## Configure
For strict separation of config from code (twelve-factor principle), configuration must be placed in environment variables.

### Configuration parameters
Broker configuration params list (environment properties):

* obligatory:
  * USER_PASSWORD - password to interact with the broker
  * BASE_GUID - base id for catalog plan creation (default: gearpump)
  * CF_CATALOG_SERVICENAME - service name in cloud foundry catalog (default: gearpump)
  * CF_CATALOG_SERVICEID - service id in cloud foundry catalog (default: gearpump)
  * GEARPUMP_UI_ORG, GEARPUMP_UI_SPACE, GEARPUMP_UI_NAME - org, space and name of Apache Gearpump's dashboard to be used by application broker (make sure, that there’s application-broker up and running, and dashboard service available)
  * GEARPUMP_PACK_VERSION - the version of Apache Gearpump binaries to be used in the broker (define the version by following the pattern: if the binary is called ``gearpump-2.11-0.8.0.zip``, the version is: ``GEARPUMP_PACK_VERSION: "2.11-0.8.0"``)

### Services
The broker uses some the following TAP services;  

* sso  (user-provided-service) - to obtain credentials (for connecting to Application Broker)
* zookeeper - to store instance information
* kerberos - to be able to log in to kerberos (it will provide kerberos configuration, for REALM and KDC host)
* hdfs - to upload Apache Gearpump binary to HDFS
* yarn - to obtain YARN configuration  
```
Example CF CLI commands:     
 cf cs zookeeper shared zookeeper-gearpump  
 cf cs kerberos shared kerberos-service  
 cf cs hdfs shared hdfs-gearpump  
 cf cs yarn bare yarn-gearpump  
```

Check, if there are instances of the services above in your organization. If not, create them.

## Deploy
For your convenience a sample manifest file is created for you (``src/cloudfoundry/manifest.yml``) with some defaults.
Modify the parameters according to your needs and use the following to push the app to Cloud Foundry:
```
cf push
```

## Create service broker
Create new service broker:
```
cf create-service-broker gearpump-broker <user> <password> https://gearpump-broker.<platform_domain>
cf enable-service-access gearpump
```

Now you can create new Apache Gearpump cluster from marketplace ``cf cs gearpump "1 worker" <instance_name>`` or from console in section "marketplace".
