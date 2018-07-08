package xyz.docbleach.cli;

import com.google.gson.Gson;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.bleach.DefaultBleach;
import xyz.docbleach.api.exception.BleachException;

@SuppressFBWarnings(value = "DM_EXIT", justification = "Used as an app, an exit code is expected")
public class Main {

  private static Logger LOGGER = null;
  private int verbosityLevel = 0;
  private InputStream inputStream;
  private OutputStream outputStream;
  private boolean jsonOutput;

  private Main() {
    // Prevent instantiation from the outside worlds
  }

  public static void main(String[] args) throws IOException, ParseException {
    // Set the security manager, preventing commands/network interactions.
    System.setSecurityManager(new UnsafeSecurityManager());
    // Hide macOS' Java icon in Dock
    System.setProperty("java.awt.headless", "true");

    Main main = new Main();

    main.parseArguments(args);

    try {
      main.sanitize();
    } catch (BleachException e) {
      exitError(e);
    }

    System.exit(0);
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
  private void sanitize() throws BleachException {
    BleachSession session = new BleachSession(new DefaultBleach());

    session.sanitize(inputStream, outputStream);

    if (jsonOutput) {
      Gson gson = new Gson();
      System.err.println(gson.toJson(session));
    } else {
      if (session.threatCount() == 0) {
        LOGGER.info("The file was already safe, so I've just copied it over.");
      } else {
        LOGGER.warn(
            "Sanitized file has been saved, {} potential threat(s) removed.",
            session.threatCount());
      }
    }
  }

  private ClassLoader getPluginClassLoader() {
    ClassLoader ownClassLoader = getClass().getClassLoader();

    File loc = new File("./plugins");
    if (!loc.exists()) {
      LOGGER.debug("./plugins/ directory does not exist");
      return ownClassLoader;
    }

    File[] flist = loc.listFiles(file -> file.getPath().toLowerCase().endsWith(".jar"));
    if (flist == null) {
      return ownClassLoader;
    }

    URL[] urls = new URL[flist.length];
    for (int i = 0; i < flist.length; i++) {
      try {
        urls[i] = flist[i].toURI().toURL();
      } catch (MalformedURLException e) {
        urls[i] = null;
      }
    }

    return new URLClassLoader(urls, ownClassLoader);
  }

  private void setupLogging() {
    if (verbosityLevel <= 0) {
      // Verbosity is INFO or OFF, we hide the thread and logger names
      System.setProperty(SimpleLogger.SHOW_THREAD_NAME_KEY, "false");
      System.setProperty(SimpleLogger.SHOW_LOG_NAME_KEY, "false");
    }

    String level;
    switch (verbosityLevel) {
      case -1:
        level = "OFF";
        break;
      case 1:
        level = "DEBUG";
        break;
      case 2:
        level = "TRACE";
        break;
      default:
        level = "INFO";
        break;
    }

    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, level);

    LOGGER = LoggerFactory.getLogger(Main.class);
    LOGGER.debug("Log Level: {}", level);
  }

  /**
   * Parse the command line arguments, and store them in variables
   */
  private void parseArguments(String[] args) throws ParseException, IOException {
    Options options = new Options();
    options.addOption(
        Option.builder("in")
            .desc("The file to be processed")
            .hasArg()
            .argName("FILE")
            .required()
            .build());
    options.addOption(
        Option.builder("out")
            .desc("Generated's file path")
            .hasArg()
            .argName("FILE")
            .required()
            .build());
    options.addOption("v", false, "enable verbose mode");
    options.addOption("vv", false, "enable debug mode");
    options.addOption("json", false, "enable json output mode");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd;
    try {
      cmd = parser.parse(options, args);
    } catch (MissingOptionException | UnrecognizedOptionException e) {
      // Logger is not yet setup here, we have to handle it the hard way
      System.err.println(e.getMessage());
      System.err.println();
      PrintWriter err = new PrintWriter(System.err);
      new HelpFormatter().printHelp(err, 100, "docbleach", "DocBleach", options, 0, 0, "", true);
      System.exit(1);
      return;
    }

    verbosityLevel = cmd.hasOption("vv") ? 2 : (cmd.hasOption("v") ? 1 : 0);

    jsonOutput = cmd.hasOption("json");
    if (cmd.hasOption("json")) {
      // If we output JSON, disable logging
      verbosityLevel = -1;
    }

    setupLogging();

    String inName = cmd.getOptionValue("in");
    makeInputStream(inName);

    String outName = cmd.getOptionValue("out");
    makeOutputStream(outName);
  }

  /**
   * Generates the inputStream for this "file", that may be a local path, a - for stdin, or an URI.
   */
  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "Wanted user input")
  private void makeInputStream(String inName) throws IOException {
    LOGGER.debug("Checking input name : {}", inName);
    if ("-".equalsIgnoreCase(inName)) {
      inputStream = new BufferedInputStream(System.in);
      return;
    }

    File inFile = new File(inName);
    inputStream = getFileInputStream(inFile);
  }

  /**
   * Checks the output file is valid: either a file path or "-" for stdout.
   */
  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "Wanted user input")
  private void makeOutputStream(String outName) throws IOException {
    LOGGER.debug("Checking output name : {}", outName);
    if ("-".equalsIgnoreCase(outName)) {
      outputStream = System.out;
      return;
    }

    File outFile = new File(outName);
    if (outFile.exists()) {
      throw new IOException("Output file already exists. Quitting");
    }
    if (!outFile.createNewFile()) {
      throw new IOException("Output file could not be written to. Quitting.");
    }

    outputStream = new FileOutputStream(outFile);
  }

  private InputStream getFileInputStream(File inFile) throws FileNotFoundException {
    if (!inFile.exists()) {
      throw new FileNotFoundException("Input file does not exist. Quitting.");
    }

    if (!inFile.canRead()) {
      throw new FileNotFoundException("I can't read the Input File. Quitting.");
    }

    if (!inFile.isFile()) {
      throw new FileNotFoundException("I can't read the Input File. Quitting.");
    }

    return new BufferedInputStream(new FileInputStream(inFile));
  }
}
