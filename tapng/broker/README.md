# GearPump Broker on TAP NG

This is a GearPump service broker on TAP NG.

## Requirements

Required software:
 
- Docker 1.6.2


## Push a Docker image to the image repository 

First we need push the GearPump broker Docker image to the TAP NG image repository.

Before we build the image we must set the following ENV:
 
```
export REPOSITORY_URL=tapimages.us.enableiot.com:8080
```

Now login to the Docker repository:
 
```
docker login $REPOSITORY_URL
```

and enter a user name and a secret password.


Then execute the following command from CLI:

```
make push_docker
```

