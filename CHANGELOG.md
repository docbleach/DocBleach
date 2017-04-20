# Change Log
All notable changes to this project will be documented in this file.


## [0.0.4] - 2017-04-20

### :sparkles: New features
- Total rewrite of the DocBleach API, with a **threat model** (cf #2)
- **-json** option for the command line tool (cf #3)
- Office Open XML: Main part's content type is rewritten (cf #6)
- A **SecurityManager** is set when using the command line tool, preventing some exploits
- New wiki page: **[Advanced Usage](https://github.com/docbleach/DocBleach/wiki/Advanced-usage)**

### :bug: Bug fixes
- Fixed typos in the readme

### :fire: Backward incompatible changes
- Whole **API** is rewritten.

[:link: Full list of changes](https://github.com/docbleach/DocBleach/compare/v0.0.3...v0.0.4)

## [0.0.3] - 2017-04-19
#### :bug: Bug fixes
A regression was introduced in commit 271f1e60, preventing OOXML files from being sanitized.

That's all.

[:link: Full list of changes](https://github.com/docbleach/DocBleach/compare/v0.0.2...v0.0.3)

## [0.0.2] - 2017-04-19
### :sparkles: New features
- Code is now split into independent Maven modules: api, cli and the bleaches  (see #2)
- Improved logging of the exceptions (see #3)
- Automatic **SonarQube** and **SourceClear** scans
- Updated dependencies
- Artifacts are pushed on Maven to *[Central](http://central.sonatype.org/)*
- :tada: *OOXML Bleach rewritten* - now filters using relations and content types

### :bug: Bug fixes
- A bug corrupting .docm files has been fixed — #5 
- Fixed typos in the readme

### :fire: Backward incompatible changes
- *Batch mode* has been removed
- `-in` does not handle network files anymore, because of the added code maintenance required (SSL checks, ...)

[:link: Full list of changes](https://github.com/docbleach/DocBleach/compare/v0.0.1...v0.0.2)

## [0.0.1] - 2017-03-29
:tada: This is the first release of the *DocBleach* project. May there be plenty!

### Supported formats:
- Office Open XML
- OLE2
- PDF
- RTF