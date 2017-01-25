[![Dependency Status](https://www.versioneye.com/user/projects/57236753ba37ce0031fc1d55/badge.svg?style=flat)](https://www.versioneye.com/user/projects/57236753ba37ce0031fc1d55)

# gearpump-broker

TAP broker for Apache Gearpump (see: [http://gearpump.apache.org/](http://gearpump.apache.org/)).

Apache Gearpump broker spawns Apache Gearpump UI (dashboard) on TAP NG (using application-broker) and submits Apache Gearpump to YARN.
That's why it needs YARN service instance and gearpump-dashboard already prepared to work.

# How to use it?
To use gearpump-broker, you need to build it from sources, push its image to the docker image repository, prepare Gearpump dashboard and broker offerings, register them in TAP. 
Follow steps described below.

## Build
Broker uses Apache Gearpump binaries internally so, before building, you need to obtain them and put in ``src/main/resources/gearpump``.

Run command for compile and package:
```
mvn clean package
```

## Prepare docker images

After the broker has been built you must push the image to the docker repository.
The default repository is localhost:30000.

But first we need to prepare broker and dashboard packages that will be used to create Docker images.
 
### Dashboard image

Go to folder tapng/broker and run the following:

```
make clean build push_docker
```

Before that increase a version of a docker image in Makefile.

For other commands look at Makefile.


### Broker image

Go to folder tapng/dashboard and run the following:
```
make clean build push_docker
```

Before that increase a version of a docker image in Makefile.


## Prepare offerings

### Gearpump dashboard offering

Steps to add a dashboard offering to the catalog:

- create a dedicated folder for gearpump dashboard in TAP-{version}/roles/tap-api-catalog/templates
- place a dashboard offering there 
- add an entry to  TAP-{version}/group_vars/all/default_offerings.yml

### Gearpump broker offering

Steps to add a dashboard offering to the catalog:

- create a dedicated folder for gearpump broker in TAP-{version}/roles/tap-api-catalog/templates
- place a broker offering there 
- add an entry to TAP-{version}/group_vars/all/default_offerings.yml

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
