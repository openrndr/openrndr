#!/usr/bin/env bash

cd `dirname $0`

# dokka places style.css one directory above the root directory of the docs (where index.html is)
# this is a problem, because gh-pages needs index.html at the root of the repo and the '../style.css' link won't work
# we copy style.css to the root directory of the docs
cp ./style.css ../build/docs/openrndr/style.css

cp ./CNAME ../build/docs/openrndr/CNAME
cd ../build/docs/openrndr

# we need to relink style.css in all html files
find . -name "*.html" -exec sed -i -e 's/\.\.\/style.css/style.css/g' {} \;
