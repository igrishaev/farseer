
sub-install:
	lein sub install

sub-test:
	lein sub test :all

toc-install:
	npm install --save markdown-toc

toc-build:
	node_modules/.bin/markdown-toc -i README.md
