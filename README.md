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

The input directory should contain a structure

```
~/swat4hcls-proceedings-2025/preface.pdf # The preface written by the editor
							/Short Paper/paper-1.pdf #names of the files don't matter
								  /our-submission.pdf
								  /why-do-authors-give-crappy-names-to-their-files.pdf
							/Long Paper/paper-sixty-pages.pdf
							     /ouff-ten-pages.pdf
							     /why-so-long.pdf
							/Demo/cool.pdf
							/Keynote/whow-what-a-career.pdf
							/Invited Talks/like-this-prof.pdf
```
Folders names should be written out as you want the Sessions to be headed in the index.html 

The file 'e/--editor-affiliations' is a new line separated file containing on each line
the affiliations of a conference chair/editor. e.g.

```
university 1, country b
company c, country d
```
This must be in the same order as the editors named in the preface.pdf.

## Checks for subset of CEUR requirements

The option `--run-checks` generates an index.html with inline comments in red for failing checks. These checks are not complete but a first step.

## Preface

While CEUR is accepting of many ways to write the preface pdf. 
This tool needs the preface to be using the CEUR template.
The tool mines the metadata (names, orcids) etc. from the structure of the PDFs and this only works for CEUR templated PDFs.

## Submitted PDFs are the source of truth

If an author forgets half the co-authors in the PDF, then they will not be added to the index.html.

