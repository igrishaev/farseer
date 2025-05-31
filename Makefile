
all: test install

install:
	lein sub install

.PHONY: test
test:
	lein sub test :all

toc-install:
	npm install --save markdown-toc

toc-build:
	node_modules/.bin/markdown-toc -i README.md

release:
	lein release
