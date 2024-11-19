set -e

rm -rf ./build
rm -f assign2.jar

mkdir ./build

javac -d ./build/ \
./src/*.java \
./src/data/*.java \
./src/ga/*.java \
./src/ga/functional/*.java \
./src/util/*.java

jar cvfe assign2.jar Main -C ./build .