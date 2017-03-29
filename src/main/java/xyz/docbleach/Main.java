package xyz.docbleach;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

@SuppressFBWarnings(value = "DM_EXIT", justification = "Used as an app, an exit code is expected")
public class Main {

  private static final String FILE_SCHEME = "file";
  private static final String HTTP_SCHEME = "http";
  private static final String HTTPS_SCHEME = "https";
  private static final String FTP_SCHEME = "ftp";
  private static Logger LOGGER = null;
  private boolean batchMode;
  private int verbosityLevel = 0;
  private InputStream inputStream;
  private OutputStream outputStream;

  private Main() {
    // Prevent instantiation from the outside worlds
  }

  public static void main(String[] args) throws Exception {
    Main main = new Main();

    try {
      main.parseArguments(args);
      main.sanitize();
      System.exit(0);
    } catch (IOException | BleachException e) {
      exitError(e);
    }
  }

  private static void exitError(Exception e) {
    if (LOGGER == null) {
      System.err.println("An error occured: " + e.getMessage());
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.error("An error occured", e);
      } else {
        LOGGER.error(e.getMessage());
      }
    }
    System.exit(1);
  }

  /**
   * Sanitizes the designated files
   */
  private void sanitize() throws IOException, BleachException {
    BleachSession session = new BleachSession(inputStream, outputStream, batchMode);
    session.findBleach();
    session.sanitize();

    if (session.threatCount() == 0) {
      LOGGER.info("The file was already safe, so I've just copied it over.");
    } else {
      LOGGER.warn("Sanitized file has been saved, {} potential threat(s) removed.",
          session.threatCount());
    }
  }

  private InputStream getDownloadChannel(String uri) throws IOException {
    URL website = new URL(uri);
    ReadableByteChannel rbc = Channels.newChannel(website.openStream());
    return new BufferedInputStream(Channels.newInputStream(rbc));
  }

  private void setupLogging() {
    String level = "INFO";
    switch (verbosityLevel) {
      case 1:
        level = "DEBUG";
        break;
      case 2:
        level = "TRACE";
        break;
      case 0:
      default:
        // By default, hide thread & class
        System.setProperty(SimpleLogger.SHOW_THREAD_NAME_KEY, "false");
        System.setProperty(SimpleLogger.SHOW_LOG_NAME_KEY, "false");
        break;
    }

    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, level);

    LOGGER = LoggerFactory.getLogger(Main.class);
    LOGGER.debug("Log Level: {}", level);
  }

  /**
   * Parse the command line arguments, and store them in variables
   */
  private void parseArguments(String[] args)
      throws ParseException, BleachException, IOException, URISyntaxException {
    Options options = new Options();
    options.addOption(Option.builder("in")
        .desc("The file to be processed")
        .hasArg()
        .argName("FILE")
        .required()
        .build());
    options.addOption(Option.builder("out")
        .desc("Generated's file path")
        .hasArg()
        .argName("FILE")
        .required()
        .build());
    options.addOption("batch", false,
        "enable batch mode, removing all interactions (password prompt, ...)");
    options.addOption("v", false, "enable verbose mode");
    options.addOption("vv", false, "enable debug mode");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd;
    try {
      cmd = parser.parse(options, args);
    } catch (MissingOptionException | UnrecognizedOptionException e) {
      // Logger is not yet setup here, we have to handle it the hard way
      System.err.println(e.getMessage());
      System.err.println();
      new HelpFormatter().printHelp("docbleach", "DocBleach", options, "", true);
      System.exit(1);
      return;
    }
    setBatchMode(cmd.hasOption("batch"));
    verbosityLevel = cmd.hasOption("vv") ? 2 : (cmd.hasOption("v") ? 1 : 0);
    setupLogging();

    String inName = cmd.getOptionValue("in");
    String outName = cmd.getOptionValue("out");
    makeOutputStream(outName);
    makeInputStream(inName);
  }

  /**
   * Generates the inputStream for this "file", that may be a local path, a -
   * for stdin, or an URI.
   */
  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "Wanted user input")
  private void makeInputStream(String inName)
      throws BleachException, IOException, URISyntaxException {
    LOGGER.debug("Checking input name : {}", inName);
    if ("-".equalsIgnoreCase(inName)) {
      inputStream = new BufferedInputStream(System.in);
      if (batchMode) {
        LOGGER.error("Batch mode is not available when reading from stdin");
      }
      batchMode = false;
      return;
    }

    URI inFileUri = new URI(inName); // Using {@link File#toURI} forces the file:// prefix

    if (inFileUri.getScheme() == null || FILE_SCHEME.equals(inFileUri.getScheme())) {
      File inFile = new File(inName);
      inputStream = getFileInputStream(inFile);
      return;
    }

    switch (inFileUri.getScheme()) {
      case HTTP_SCHEME:
      case HTTPS_SCHEME:
      case FTP_SCHEME:
        inputStream = getDownloadChannel(inName);
        break;
      default:
        throw new BleachException("Unknown scheme: " + inFileUri.getScheme());
    }
  }

  /**
   * Checks the output file is valid: either a file path or "-" for stdout.
   */
  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "Wanted user input")
  private void makeOutputStream(String outName) throws BleachException, IOException {
    LOGGER.debug("Checking output name : {}", outName);
    if ("-".equalsIgnoreCase(outName)) {
      outputStream = System.out;
      return;
    }

    File outFile = new File(outName);
    if (outFile.exists()) {
      throw new BleachException("Output file already exists. Quitting");
    }
    if (!outFile.createNewFile()) {
      throw new BleachException("Output file could not be written to. Quitting.");
    }

    outputStream = new BufferedOutputStream(new FileOutputStream(outFile));
  }

  private void setBatchMode(boolean batchMode) {
    this.batchMode = batchMode;
  }

  private InputStream getFileInputStream(File inFile)
      throws BleachException, FileNotFoundException {
    if (!inFile.exists()) {
      throw new BleachException("Input file does not exist. Quitting.");
    }

    if (!inFile.canRead()) {
      throw new BleachException("I can't read the Input File. Quitting.");
    }

    if (!inFile.isFile()) {
      throw new BleachException("I can't read the Input File. Quitting.");
    }
    return new BufferedInputStream(new FileInputStream(inFile));
  }
}