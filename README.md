# Easily make CEUR Compliant index.html

Generating a CEUR Compliant index.html file for your submission can take quite some time and can be tricky.
The java code in this repository is supposed to make this easier, but is still a work in progress.

There is no release yet so the first thing one needs to do is build a copy.

## Requirements for building the software

java-21+, see your OS documentation on how to easily install this.
[maven 3.9+](https://maven.apache.org/)
git, you are on github ;) hope you have this already.

``
git clone https://github.com/JervenBolleman/ceur-index-html-helper.git
cd ceur-index-html-helper
mvn package
``

## Using the ceur-index-html-helper

At the moment this is command line logic.

```
cd ceur-index-html-helper/target

# This will give all the options that are required. 
java -jar ceur-index-html-helper-1.0-SNAPSHOT-uber.jar -h

# An example
java -jar ceur-index-html-helper-1.0-SNAPSHOT-uber.jar \
	-i ~/swat4hcls-proceedings-2025/ \
	-o ~/swat4hcls-proceedings-2025-for-ceur/ \
	-y 2026 \
	-n 'Jerven Bolleman' \
	-p 'Mar 23-26' \
	-u 'https://www.swat4ls.org/workshops/amsterdam2026/' \
	-s 'SWAT4HCLS2026' \
	-l '17th International Semantic Web Applications and Tools for Health Care and Life Sciences Conference' \
	-e ~/swat4hcls-2025-editors-affiliations/
```

The file 'e/--editor-affiliations' is a new line separated file containing on each line
the affiliations of a conference chair/editor. e.g.

```
university 1, country b
company c, country d
```
This must be in the same order as the editors named in the preface.pdf.

## Preface

While CEUR is accepting of many ways to write the preface pdf. 
This tool needs the preface to be using the CEUR template.
The tool mines the metadata (names, orcids) etc. from the structure of the PDFs and this only works for CEUR templated PDFs.

## Coming

 * The output is now still a directory. We will add an option that creates the zip file.
 * We will add an option to automatically match scanned copyright forms with the papers.
 ** This is going to use tesseract4j for the OCR. So this might still need manual interventions. 
 ** It means we will get two more options for the directory with signed copyright forms and an output directory
 * bibtex/ris files for citing the papers in this conference. 
 * Checking if the pdfs use the CEUR template, have the right fonts embedded and that there is a decleration of AI use. 




