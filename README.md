<p align="center">
  <img src="logo.png"/>
</p>


DocBleach is an advanced Content Disarm and Reconstruction software.
Its objective is to remove misbehaving dynamic content from your Office
files, or everything that could be a threat to the safety of your computer.

[![Build Status][travis_img]][travis_link]

Let's assume your job involves working with files from external sources, for
instance reading resumes from unknown applicants. You receive for example a .doc
file, your anti-virus doesn't detect it as harmful, and you decide to open it
anyway. You get infected.
You can use DocBleach to sanitize this document: chances are you don't get
infected, because the dynamic content isn't run.

# Howto's
To build DocBleach, use Maven:
```bash
$ mvn clean package
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 10.696 s
[INFO] Finished at: 2016-12-19T17:36:10+01:00
[INFO] Final Memory: 29M/234M
[INFO] ------------------------------------------------------------------------
```
The final jar is stored in `cli/target/docbleach.jar`.


To use DocBleach, you may either use the [Web Interface][webI] or run it in CLI:
```bash
java -jar docbleach.jar -in unsafe_document.doc -out safe_doc.doc
```

The input file may be a relative/absolute path, an URI (think: http:// link),
or a dash (`-`).

The output file may be a relative/absolute path, or a dash (`-`).

If a dash is given, the input will be taken from stdin, and the output will be
sent to stdout.

DocBleach's information (removed threats, errors, ...) are sent to stderr.

[Advanced usage][wiki-adv-usage]

## Get the sources

```bash
    git clone https://github.com/docbleach/DocBleach.git
    cd DocBleach
    mvn install
    # Import it as a Maven project in your favorite IDE
```

You've developed a new cool feature ? Fixed an annoying bug ? We'd be happy
to hear from you !

## Run the tests
The tests run with JUnit 5, which is perfectly integrated in Maven.
To run tests, just run `mvn test`. You should get something similar to this:

```
[INFO] Scanning for projects...
...
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Dec 19, 2016 5:33:54 PM org.junit.platform.launcher.core.ServiceLoaderTestEngineRegistry loadTestEngines
INFO: Discovered TestEngines with IDs: [junit-jupiter]
Running org.docbleach.bleach.PdfBleachTest
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.246 sec - in org.docbleach.bleach.PdfBleachTest
Running org.docbleach.bleach.OLE2BleachTest

Results :

Tests run: 13, Failures: 0, Errors: 0, Skipped: 0

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 3.252 s
[INFO] Finished at: 2016-12-19T17:33:55+01:00
[INFO] Final Memory: 19M/211M
[INFO] ------------------------------------------------------------------------
```

BUILD SUCCESS confirms that all the tests were run successfuly.


# Related links

 * [:wrench: How to Contribute][contribute]
 * [:beetle: Reporting bugs][issues]
 * [:gem: Download latest version][release-page]
 * [:mag_right: Wiki][wiki]

# Releases
The releases are available as Windows executables that don't depend on Java, thanks
to the Excelsior Jet technology.

[![](https://i.imgur.com/vBE9rqk.png)](https://www.excelsiorjet.com/)

## License

See [LICENSE][license].


# Project Status

Don't expect the code base to change everyday, but feel free to contribute: new ideas are more than
welcome,  and threats evolve - so should we.

Some things would be awesome, though:
- Adding a way to configure bleaches
- Write tests!
- Writing more content to show and explain how the sanitation process works, why it works.
- Adding more stats!



[release-page]: https://github.com/docbleach/docbleach/releases
[webI]: https://github.com/docbleach/DocBleach-Web
[travis_img]: https://api.travis-ci.org/docbleach/DocBleach.svg?branch=master
[travis_link]: https://travis-ci.org/docbleach/DocBleach
[issues]: https://github.com/docbleach/DocBleach/issues
[contribute]: https://github.com/docbleach/DocBleach/blob/master/CONTRIBUTING.md
[license]: https://github.com/docbleach/DocBleach/blob/master/LICENSE
[wiki]: https://github.com/docbleach/DocBleach/wiki
[wiki-adv-usage]: https://github.com/docbleach/DocBleach/wiki/Advanced-usage
