# GearPump Dashboard on TAP NG

This is a GearPump dashboard service on TAP NG.

## Requirements

Required software:
 
- Docker 1.6.2
- TAP NG CLI


## Push a Docker image to the image repository 

First we need push the GearPump dashboard Docker image to the TAP NG image repository.

Before we build the image we must set the following ENV:
 
```
export REPOSITORY_URL=tapimages.us.enableiot.com:8080
```

Now login to the Docker repository:
 
```
docker login $REPOSITORY_URL
```

and enter a user name and a secret password.



If we forward ports when developing a service for iLAB/SCLAB it must be different:

```
export REPOSITORY_URL=localhost:30000
```

and then we do not need to login.



Then execute the following command from CLI:

```
make clean build push_docker
```

## Create a service offering in TAP NG

First login to TAP NG:

```
tap $MACHINE_IP admin password
```

Now we can create a service offering:

```
tap create-offering co_gearpump_dashboard.json
```

Now we can even create a GearPump dashboard service:

```
tap create-service <service_offering_id> <plan_id> <custom_name>
```

To list available services run this:

```
tap catalog 
```

