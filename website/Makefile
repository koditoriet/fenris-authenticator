REGISTRY = registry.koditoriet.se
IMAGE = fenris-website
VERSION = 2
PLATFORMS = linux/amd64,linux/arm64/v8
MANIFEST = $(REGISTRY)/$(IMAGE):$(VERSION)

.PHONY: build
build: img/* index.html style.css
	podman manifest rm $(MANIFEST) || true
	podman manifest create $(MANIFEST)
	podman build --manifest $(MANIFEST) --platform $(PLATFORMS) .

.PHONY: push
push: build
	podman manifest push --all $(MANIFEST)
