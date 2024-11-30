./build.sh

./run_default.sh ./data/t1
mkdir ./doc/t1
rm ./doc/t1/*.tex
cp ./runs/*.tex ./doc/t1/

./run_default.sh ./data/t2
mkdir ./doc/t2
rm ./doc/t2/*.tex
cp ./runs/*.tex ./doc/t2
