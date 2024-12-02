rm ./*.pdf
cd ./doc
latexmk -pdf 3P71_Assignment2.tex
cp *.pdf ../

