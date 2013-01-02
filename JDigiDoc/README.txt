DigiDoc Java library
--------------------

JDigiDoc is a primary library for manipulating Estonian DDOC and BDOC
digital signature container files.

It offers the functionality for creating digitally signed  files in
DIGIDOC-XML 1.3, 1.2, 1.1 and BDOC 1.0 formats, adding new signatures,
verifying signatures, timestamps and adding confirmations in OCSP format.

DigiDoc documents are XML files based on the international standards XML-DSIG
and ETSI TS 101 903. DigiDoc documents and the JDigiDoc library implement a
subset of XML-DSIG and ETSI TS 101 903.

Building
--------
1. Get Maven2 from http://maven.apache.org
2. Run
     mvn install

Generating API documentation
----------------------------
Run mvn javadoc:jar

Full documentation
----------------------------

For documentation please see in doc folder SK-JDD-PRG-GUIDE