PROFILE=-Prelease


all: build docs

build:
	export MAVEN_OPTS="-Xmx1024m -Xms512m"
	mvn clean
	mvn package site $(PROFILE)
	mvn deploy $(PROFILE)

docs:
	mkdir -p docs/javadocs
	cp -r target/apidocs/* docs/javadocs/ 
	find docs -exec svn add {} \; 2>/dev/null
	find docs -name '*.html' -exec svn pset svn:mime-type text/html {} \;
	find docs -name '*.txt' -exec svn pset svn:mime-type text/plain {} \;
	find docs -name '*.css' -exec svn pset svn:mime-type text/css {} \;
	find docs -name '*.js' -exec svn pset svn:mime-type application/x-javascript {} \;
	find docs -name '*.png' -exec svn pset svn:mime-type image/png {} \;
	find docs -name '*.gif' -exec svn pset svn:mime-type image/gif {} \;
	find docs -name '*.jpeg' -exec svn pset svn:mime-type image/jpeg {} \;
