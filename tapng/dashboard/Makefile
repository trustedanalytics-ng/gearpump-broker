
IMAGE_REPOSITORY_URL="127.0.0.1:30000"
IMAGE_NAME="gearpump-dashboard"
IMAGE_VERSION="0.1.0"
IMAGE_TAG=$(IMAGE_REPOSITORY_URL)/$(IMAGE_NAME):$(IMAGE_VERSION)

docker_build:
	docker build -t $(IMAGE_TAG) .

push_docker: docker_build
	docker push $(IMAGE_TAG)

build:
	cd ../..; ./pack.sh
	cp ../../gearpump-dashboard-*.tar.gz .

clean:
	rm -f gearpump-dashboard-*.tar.gz
