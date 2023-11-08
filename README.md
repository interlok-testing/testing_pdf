# PDF testing

[![license](https://img.shields.io/github/license/interlok-testing/testing_pdf.svg)](https://github.com/interlok-testing/testing_pdf/blob/develop/LICENSE)
[![Actions Status](https://github.com/interlok-testing/testing_pdf/actions/workflows/gradle-build.yml/badge.svg)](https://github.com/interlok-testing/testing_pdf/actions/workflows/gradle-build.yml)

Project tests interlok-pdf features

## What it does

This project contains various workflows that each demonstrates an interlok-pdf service.

Each workflow is made up of:
* a fs-consumer polling a given directory every 5 seconds
* a service from the interlok-pdf component that will do a transform
* a fs-producer which outputs the transform result to file

## Getting started

* `./gradlew clean build`
* `(cd ./build/distribution && java -jar lib/interlok-boot.jar)`

## Example FO to PDF using fop-transform-service

* copy the `\pdf-testing\src\test\resources\example-input.fo` into the `./build/distribution/messages/create-pdf-data-in` folder
* fop-transform-service will process the document
* check for the result at `./build/distribution/messages/create-pdf-data-out`
 
## Example PDF to HTML using pdf-to-html-service

* copy the `\pdf-testing\src\test\resources\example-output.pdf` into the `./build/distribution/messages/create-html-data-in` folder
* pdf-to-html-service will process the document
* check for the result at `./build/distribution/messages/create-html-data-out`
 
## Example PDF to Text using pdf-to-text-service

* copy the `\pdf-testing\src\test\resources\example-output.pdf` into the `./build/distribution/messages/create-text-data-in` folder
* pdf-to-text-service will process the document
* check for the result at `./build/distribution/messages/create-text-data-out`
 
 

