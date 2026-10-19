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
java -jar target/ceur-index-html-helper-1.0-SNAPSHOT-uber.jar make-index -h

# An example
java -jar target/ceur-index-html-helper-1.0-SNAPSHOT-uber.jar make-index \
	-i ~/swat4hcls-proceedings-2025/ \
	-o ~/swat4hcls-proceedings-2025-for-ceur/ \
	-y 2026 \
	-n 'Jerven Bolleman' \
	-p 'Mar 23-26' \
	-u 'https://www.swat4ls.org/workshops/amsterdam2026/' \
	-s 'SWAT4HCLS2026' \
	-l '17th International Semantic Web Applications and Tools for Health Care and Life Sciences Conference'
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

## Browser version (WebAssembly)

The same `make-index` logic can run entirely in the browser, compiled to WebAssembly with GraalVM Web Image.
It is published at https://www.jerven.eu/ceur-index-html-helper/ (built and deployed from `main` by
`.github/workflows/pages.yml`).
The page lets you pick or drop the proceedings folder, fill in the volume details, and download the
`index.html` or a zip with the renamed PDFs. No files leave your computer; with "Run checks" the
ORCID records are looked up from the browser.

Building it requires [Oracle GraalVM 25+](https://www.graalvm.org/) (which includes the `svm-wasm` tool) with its
`native-image` on the PATH (or pass `-Dnative-image=/path/to/native-image`), and binaryen's `wasm-opt`.

```
mvn -Pwasm package
cd target/web
python3 -m http.server 8000
# open http://localhost:8000/
```

`target/web` is a static site (index.html, a web worker, and the .js/.wasm pair) that can be put on any web server,
e.g. GitHub Pages. It needs to be served over http(s), browsers don't run it from a `file://` URL.
The `-g` git integration is not available in the browser.

## Output modes

By default (`-m RDFA`) the index.html is semantic HTML5 with RDFa annotations.
With `-m CEUR_TEMPLATE` the index.html follows the plain [CEUR-WS template](https://ceur-ws.org/Vol-XXX/index.html),
using only the `CEUR*` class tags and schema.org `itemid` attributes for ORCIDs.
In this mode, use `--submitted-papers N` to fill in the number of submitted papers in the preface summary.

## Checks for subset of CEUR requirements

The option `--run-checks` generates an index.html with inline comments in red for failing checks. These checks are not yet complete but a first step. We also show warnings in orange.

## Preface

While CEUR is accepting of many ways to write the preface pdf. 
This tool needs the preface to be using the CEUR template.
The tool mines the metadata (names, orcids, affiliations) from the structure of the PDFs and this only works for PDFs using the CEUR templates.

## Submitted PDFs are the source of truth

If an author forgets half the co-authors in the PDF, then they will not be added to the index.html.
It is key that as submissions editors accurately and actively engage with the authors that they have correct submissions.


