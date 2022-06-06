/*
  Redate Photo File #3 - Change File Dates, Names for JPEG Photos
  Written by: Keith Fenske, http://kwfenske.github.io/
  Wednesday, 6 September 2017
  Java class name: RedatePhotoFile3
  Copyright (c) 2017 by Keith Fenske.  Apache License or GNU GPL.

  This is a Java 1.4 application to change file names or the "last modified"
  date in the system file directory for JPEG photo files, using an embedded
  date and time found within most JPEG files.  The contents of the files are
  not changed.  The oldest date in a JPEG file is usually the original image
  creation date.  A newer date is often from editing, such as rotating a photo
  to be upright.

  On Windows 7 (and possibly the earlier Windows Vista), you have two choices
  when copying photo files from your digital camera.  First is the "import"
  feature, which sets the correct date and time, but changes files from what's
  on the camera by adding possibly empty Exif data.  Second is to copy files
  directly from the camera as a USB mass storage device.  This keeps camera
  data intact but uses the system clock for the modification date.  Windows
  Explorer cleverly shows you the embedded date and time for photo files, so
  you may not notice that the time stamp is wrong in the file directory.  (Use
  a DIR command in a "Command Prompt" window to see the difference.)

  One word of caution: there is no "undo" feature.  Once you change a file date
  or name, the only way to restore the original date or name is to change the
  file date or name again.  Practice on copies of your files before you blindly
  apply this program to large folders.  You may also turn on the "debug mode"
  option to see what would be changed, without actually making the changes.

  Apache License or GNU General Public License
  --------------------------------------------
  RedatePhotoFile3 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.

  Graphical Versus Console Application
  ------------------------------------
  The Java command line may contain options or file and folder names.  If no
  file or folder names are given on the command line, then this program runs as
  a graphical or "GUI" application with the usual dialog boxes and windows.
  See the "-?" option for a help summary:

      java  RedatePhotoFile3  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u16 or -u18 is recommended because the default
  Java font is too small.  If file or folder names are given on the command
  line, then this program runs as a console application without a graphical
  interface.  A generated report is written on standard output, and may be
  redirected with the ">" or "1>" operators.  (Standard error may be redirected
  with the "2>" operator.)  An example command line is:

      java  RedatePhotoFile3  -s  d:\temp  >report.txt

  The console application will return an exit status equal to the number of
  files that have been successfully changed, -1 for failure, and 0 for unknown.
  The graphical interface can be very slow when the output text area gets too
  big, which will happen if thousands of files are reported.

  Restrictions and Limitations
  ----------------------------
  Exif data is not parsed in the correct or official manner.  Dates are numeric
  using the Gregorian calendar (an ISO standard).  This program may fail if
  your locale has a different calendar, either regional or religious.  Many
  digital cameras have the wrong date or time.  See:

      http://en.wikipedia.org/wiki/Exif
      http://en.wikipedia.org/wiki/Calendar
      http://en.wikipedia.org/wiki/ISO_8601

  Daylight saving time (DST) may not be properly accounted for when setting
  times in a period of the year opposite to the current DST rules.  Java should
  have the correct time, but Windows 2000/XP/Vista/7 can sometimes be too
  helpful in adjusting the clock, and the effect varies with the underlying
  file system (FAT32, NTFS, etc).

  Suggestions for New Features
  ----------------------------
  (1) The following comment was copied from the FileDateName1 Java application.
  (2) Microsoft Windows 7 (among others) doesn't consistently display the date
      and time for files in regions that use daylight saving time.  To see
      this, find two files, one dated in January and one dated in July.  Copy
      them to the same folder.  Run Windows Explorer and observe the times.
      Now run a command prompt and use the DIR command to display the same
      files.  The file from the opposite daylight saving period will be
      different.  It doesn't matter if the file system is FAT32 or NTFS.
      Windows XP and earlier didn't have this problem.  It is not possible for
      FileDateName1 to agree with Windows 7 if Windows 7 can't agree with
      itself.  The time shown in the command prompt has the same behavior as
      previous versions of Windows, and is considered more "correct" by this
      program.  KF, 2014-04-21.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.text.*;               // number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support
import javax.swing.border.*;      // decorative borders

public class RedatePhotoFile3
{
  /* constants */

  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2017 by Keith Fenske.  Apache License or GNU GPL.";
  static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
                                  // standard date, time format
  static final String DATE_LOWER = "1980-01-02"; // minimum (initial substring)
  static final String DATE_UPPER = "2099-12-30"; // maximum (initial substring)
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final String EMPTY_STATUS = ""; // message when no status to display
  static final int EXIT_FAILURE = -1; // incorrect request or errors found
  static final int EXIT_SUCCESS = 1; // request completed successfully
  static final int EXIT_UNKNOWN = 0; // don't know or nothing really done
  static final String[] FONT_SIZES = {"10", "12", "14", "16", "18", "20", "24",
    "30"};                        // point sizes for text in output text area
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final String PROGRAM_TITLE =
    "Change File Dates, Names for JPEG Photos - by: Keith Fenske";
  static final String RENAME_FORMAT = "yyyy-MM-dd HH-mm-ss ";
                                  // date format as prefix when renaming files
  static final String[] SHOW_CHOICES = {"show all files", "changes only",
    "changes, errors", "errors only"};
  static final String SYSTEM_FONT = "Dialog"; // this font is always available
  static final int TIMER_DELAY = 1000; // 1.000 seconds between status updates

  /* All file systems have limits on how accurately they store dates and times.
  Don't change file dates when the millisecond difference is too small.  This
  must be at least 2000 ms (2 seconds) for MS-DOS FAT16/FAT32 file systems. */

  static final long MILLI_FUZZ = 2000; // ignore time changes smaller than this

  /* We don't know exactly where date and time strings will be in a JPEG file,
  but we do know they are near the beginning.  Set an upper limit on the number
  of bytes read per file.  Most original camera files have dates within the
  first 1 KB, and modified files within the first 8 KB. */

  static final long READ_LIMIT = 0x10000; // 64 KB

  /* class variables */

  static Calendar adjustCalendar; // for changing hours, minutes, seconds, etc
  static JCheckBox adjustCheckbox; // option to adjust dates and times
  static JTextField adjustFieldYear, adjustFieldMonth, adjustFieldDay,
    adjustFieldHour, adjustFieldMinute, adjustFieldSecond; // from big to small
  static JLabel adjustLabelYear, adjustLabelMonth, adjustLabelDay,
    adjustLabelHour, adjustLabelMinute, adjustLabelSecond;
  static int adjustValueYear, adjustValueMonth, adjustValueDay,
    adjustValueHour, adjustValueMinute, adjustValueSecond;
  static JButton cancelButton;    // graphical button for <cancelFlag>
  static boolean cancelFlag;      // our signal from user to stop processing
  static JCheckBox debugCheckbox; // graphical option for <debugFlag>
  static boolean debugFlag;       // true if we show debug information
  static JButton exitButton;      // "Exit" button for ending this application
  static JFileChooser fileChooser; // asks for input and output file names
  static String fontName;         // font name for text in output text area
  static JComboBox fontNameDialog; // graphical option for <fontName>
  static int fontSize;            // point size for text in output text area
  static JComboBox fontSizeDialog; // graphical option for <fontSize>
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static boolean hiddenFlag;      // true if we process hidden files or folders
  static JFrame mainFrame;        // this application's GUI window
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static JCheckBox newDateCheckbox; // graphical option for <oldDateFlag>
  static JCheckBox oldDateCheckbox; // graphical option for <oldDateFlag>
  static boolean oldDateFlag;     // true if we find oldest (original) dates
  static JButton openButton;      // "Open" button for files or folders
  static File[] openFileList;     // list of files selected by user
  static Thread openFilesThread;  // separate thread for doOpenButton() method
  static SimpleDateFormat ourDateFormat; // format, parse standard date, time
  static TimeZone ourTimeZone;    // our local time zone
  static JTextArea outputText;    // generated report while opening files
  static JCheckBox recurseCheckbox; // graphical option for <recurseFlag>
  static boolean recurseFlag;     // true if we search folders and subfolders
  static JCheckBox redateCheckbox; // graphical option for <redateFlag>
  static boolean redateFlag;      // true if we change file modification date
  static JCheckBox renameCheckbox; // graphical option for <renameFlag>
  static SimpleDateFormat renameDateFormat; // date, time prefix for file names
  static String renameDateString; // date, time format for renaming files
  static boolean renameFlag;      // true if we rename file with date prefix
  static JButton renameFormatButton; // button for editing rename date format
  static JPanel renameFormatDialog; // entire dialog panel for editing format
  static JTextField renamePatternText; // user edits text to change pattern
  static JTextField renameResultText; // we show user result after format
  static JButton renameTestButton; // button for testing date, time format
  static JButton saveButton;      // "Save" button for writing output text
  static JComboBox showDialog;    // graphical choice for message selection
  static int showIndex;           // index of current message selection
  static JLabel statusDialog;     // status message during extended processing
  static String statusPending;    // will become <statusDialog> after delay
  static javax.swing.Timer statusTimer; // timer for updating status message
  static long totalChange;        // number of files with successful changes
  static long totalCorrect;       // number of files that were already correct
  static long totalError;         // number of files with some type of error
  static long totalFiles;         // total number of files, all conditions
  static long totalFolders;       // total number of folders or subfolders
  static long totalNoData;        // number of files without date and time

/*
  main() method

  If we are running as a GUI application, set the window layout and then let
  the graphical interface run the show.
*/
  public static void main(String[] args)
  {
    ActionListener action;        // our shared action listener
    Font buttonFont;              // font for buttons, labels, status, etc
    boolean consoleFlag;          // true if running as a console application
    Border emptyBorder;           // remove borders around text areas
    int i;                        // index variable
    Insets inputMargins;          // margins on input text areas
    boolean maximizeFlag;         // true if we maximize our main window
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line

    /* Initialize variables used by both console and GUI applications. */

    adjustCalendar = Calendar.getInstance(); // for correcting dates and times
    adjustValueYear = adjustValueMonth = adjustValueDay = adjustValueHour
      = adjustValueMinute = adjustValueSecond = 0; // do not adjust dates
    buttonFont = null;            // by default, don't use customized font
    cancelFlag = false;           // don't cancel unless user complains
    consoleFlag = false;          // assume no files or folders on command line
    debugFlag = false;            // by default, don't show debug information
    fontName = "Verdana";         // preferred font name for output text area
    fontSize = 16;                // default point size for output text area
    hiddenFlag = false;           // by default, don't process hidden files
    mainFrame = null;             // during setup, there is no GUI window
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    oldDateFlag = true;           // by default, find oldest (original) dates
    recurseFlag = false;          // by default, don't search subfolders
    redateFlag = true;            // by default, change file modification date
    renameDateString = RENAME_FORMAT; // default date format to rename files
    renameFlag = false;           // by default, don't rename file with date
    showIndex = 0;                // by default, show messages for all files
    statusPending = EMPTY_STATUS; // begin with no text for <statusDialog>
    totalChange = totalCorrect = totalError = totalFiles = totalFolders
      = totalNoData = 0;          // no files found yet
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;

    /* Initialize number formatting styles. */

    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups

    /* Initialize formatting for dates and times in the local time zone. */

    ourDateFormat = new SimpleDateFormat(DATE_FORMAT); // date, time format
    ourTimeZone = ourDateFormat.getTimeZone(); // get local time zone
    renameDateFormat = new SimpleDateFormat(renameDateString);
                                  // assume correct, don't catch errors

    /* Check command-line parameters for options. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore empty parameters, which are more common than you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
        || word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(EXIT_UNKNOWN); // exit application after printing help
      }

      else if (word.startsWith("-a") || (mswinFlag && word.startsWith("/a")))
      {
        /* This option is followed by a number of seconds that will be added to
        each date and time found.  Use this to correct for time zones or errors
        in camera settings.  Only seconds are supported from the command line,
        and there is no range checking. */

        try                       // try to parse remainder as signed integer
        {
          adjustValueSecond = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          System.err.println("Invalid number of seconds: " + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      else if (word.equals("-d") || (mswinFlag && word.equals("/d")))
      {
        debugFlag = true;         // show debug information
        System.err.println("main args.length = " + args.length);
        for (int k = 0; k < args.length; k ++)
          System.err.println("main args[" + k + "] = <" + args[k] + ">");
      }

      else if (word.equals("-f0") || (mswinFlag && word.equals("/f0")))
        { redateFlag = false; renameFlag = false; }
      else if (word.equals("-f1") || (mswinFlag && word.equals("/f1")))
        { redateFlag = true; renameFlag = false; }
      else if (word.equals("-f2") || (mswinFlag && word.equals("/f2")))
        { redateFlag = false; renameFlag = true; }
      else if (word.equals("-f3") || (mswinFlag && word.equals("/f3")))
        { redateFlag = true; renameFlag = true; }

      else if (word.startsWith("-m") || (mswinFlag && word.startsWith("/m")))
      {
        /* This option is followed by an index number into our list of choices
        for which files to report.  We don't assign any meaning to the numbers
        here, only check that they are within range. */

        try                       // try to parse remainder as unsigned integer
        {
          showIndex = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          showIndex = -1;         // set result to an illegal value
        }
        if ((showIndex < 0) || (showIndex >= SHOW_CHOICES.length))
        {
          System.err.println("Message index must be from 0 to "
            + (SHOW_CHOICES.length - 1) + ": " + args[i]); // somewhat cryptic
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      else if (word.startsWith("-p") || (mswinFlag && word.startsWith("/p")))
      {
        /* This option is followed by a SimpleDateFormat pattern (string) used
        as a prefix when renaming files to have the embedded date and time. */

        renameDateString = args[i].substring(2);
        try { renameDateFormat = new SimpleDateFormat(renameDateString); }
        catch (IllegalArgumentException iae) // pattern was not accepted
        {
          System.err.println("Invalid SimpleDateFormat pattern: " + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        renameFlag = (renameDateString.length() > 0); // pattern enables option
      }

      else if (word.equals("-s") || (mswinFlag && word.equals("/s"))
        || word.equals("-s1") || (mswinFlag && word.equals("/s1")))
      {
        recurseFlag = true;       // start doing subfolders
      }
      else if (word.equals("-s0") || (mswinFlag && word.equals("/s0")))
        recurseFlag = false;      // stop doing subfolders

      else if (word.equals("-t") || (mswinFlag && word.equals("/t"))
        || word.equals("-t1") || (mswinFlag && word.equals("/t1")))
      {
        oldDateFlag = false;      // find newest (modified) date and time
      }
      else if (word.equals("-t0") || (mswinFlag && word.equals("/t0")))
        oldDateFlag = true;       // find oldest (original) date and time

      else if (word.startsWith("-u") || (mswinFlag && word.startsWith("/u")))
      {
        /* This option is followed by a font point size that will be used for
        buttons, dialogs, labels, etc. */

        int size = -1;            // default value for font point size
        try                       // try to parse remainder as unsigned integer
        {
          size = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          size = -1;              // set result to an illegal value
        }
        if ((size < 10) || (size > 99))
        {
          System.err.println("Dialog font size must be from 10 to 99: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        buttonFont = new Font(SYSTEM_FONT, Font.PLAIN, size); // for big sizes
//      buttonFont = new Font(SYSTEM_FONT, Font.BOLD, size); // for small sizes
        fontSize = size;          // use same point size for output text font
      }

      else if (word.startsWith("-w") || (mswinFlag && word.startsWith("/w")))
      {
        /* This option is followed by a list of four numbers for the initial
        window position and size.  All values are accepted, but small heights
        or widths will later force the minimum packed size for the layout. */

        Pattern pattern = Pattern.compile(
          "\\s*\\(\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*\\)\\s*");
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // if option has proper syntax
        {
          windowLeft = Integer.parseInt(matcher.group(1));
          windowTop = Integer.parseInt(matcher.group(2));
          windowWidth = Integer.parseInt(matcher.group(3));
          windowHeight = Integer.parseInt(matcher.group(4));
        }
        else                      // bad syntax or too many digits
        {
          System.err.println("Invalid window position or size: " + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      else if (word.equals("-x") || (mswinFlag && word.equals("/x")))
        maximizeFlag = true;      // true if we maximize our main window

      else if (word.startsWith("-") || (mswinFlag && word.startsWith("/")))
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }

      else
      {
        /* Parameter does not look like an option.  Assume this is a file or
        folder name. */

        consoleFlag = true;       // don't allow GUI methods to be called
        processFileOrFolder(new File(args[i]));
        if (cancelFlag) break;    // exit <for> loop if cancel or fatal error
      }
    }

    /* If running as a console application, print a summary of what we found
    and/or changed.  Exit to the system with an integer status. */

    if (consoleFlag)              // was at least one file/folder given?
    {
      printSummary();             // what we found and what was changed
      if (totalError > 0)         // were there any errors?
        System.exit(EXIT_FAILURE);
      else if (totalChange > 0)   // were any files successfully changed?
        System.exit((int) Math.min(totalChange, (long) Integer.MAX_VALUE));
      else                        // if there were no files at all
        System.exit(EXIT_UNKNOWN);
    }

    /* There were no file or folder names on the command line.  Open the
    graphical user interface (GUI).  We don't need to be inside an if-then-else
    construct here because the console application called System.exit() above.
    The standard Java interface style is the most reliable, but you can switch
    to something closer to the local system, if you want. */

//  try
//  {
//    UIManager.setLookAndFeel(
//      UIManager.getCrossPlatformLookAndFeelClassName());
////    UIManager.getSystemLookAndFeelClassName());
//  }
//  catch (Exception ulafe)
//  {
//    System.err.println("Unsupported Java look-and-feel: " + ulafe);
//  }

    /* Initialize shared graphical objects. */

    action = new RedatePhotoFile3User(); // create our shared action listener
    emptyBorder = BorderFactory.createEmptyBorder(); // for removing borders
    fileChooser = new JFileChooser(); // create our shared file chooser
    inputMargins = new Insets(1, 3, 2, 3); // top, left, bottom, right margins
    statusTimer = new javax.swing.Timer(TIMER_DELAY, action);
                                  // update status message on clock ticks only

    /* If our preferred font is not available for the output text area, then
    use the boring default font for the local system. */

    if (fontName.equals((new Font(fontName, Font.PLAIN, fontSize)).getFamily())
      == false)                   // create font, read back created name
    {
      fontName = SYSTEM_FONT;     // must replace with standard system font
    }

    /* Create the graphical interface as a series of little panels inside
    bigger panels.  The intermediate panel names are of no lasting importance
    and hence are only numbered (panel01, panel02, etc). */

    /* Create a vertical box to stack buttons and options. */

    JPanel panel01 = new JPanel();
    panel01.setLayout(new BoxLayout(panel01, BoxLayout.Y_AXIS));

    /* Create a horizontal panel for the action buttons. */

    JPanel panel11 = new JPanel(new BorderLayout(0, 0));

    openButton = new JButton("Open Files...");
    openButton.addActionListener(action);
    if (buttonFont != null) openButton.setFont(buttonFont);
    openButton.setMnemonic(KeyEvent.VK_O);
    openButton.setToolTipText("Start finding/opening files.");
    panel11.add(openButton, BorderLayout.WEST);

    JPanel panel12 = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));

    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(action);
    cancelButton.setEnabled(false);
    if (buttonFont != null) cancelButton.setFont(buttonFont);
    cancelButton.setMnemonic(KeyEvent.VK_C);
    cancelButton.setToolTipText("Stop finding/opening files.");
    panel12.add(cancelButton);

    saveButton = new JButton("Save Output...");
    saveButton.addActionListener(action);
    if (buttonFont != null) saveButton.setFont(buttonFont);
    saveButton.setMnemonic(KeyEvent.VK_S);
    saveButton.setToolTipText("Copy output text to a file.");
    panel12.add(saveButton);

    panel11.add(panel12, BorderLayout.CENTER);

    exitButton = new JButton("Exit");
    exitButton.addActionListener(action);
    if (buttonFont != null) exitButton.setFont(buttonFont);
    exitButton.setMnemonic(KeyEvent.VK_X);
    exitButton.setToolTipText("Close this program.");
    panel11.add(exitButton, BorderLayout.EAST);

    panel01.add(panel11);
    panel01.add(Box.createVerticalStrut(10)); // space between panels

    /* Options for changing which dates and times we search for. */

    JPanel panel21 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    oldDateCheckbox = new JCheckBox(
      "Search files for oldest date and time (original), or else ",
      oldDateFlag);
    if (buttonFont != null) oldDateCheckbox.setFont(buttonFont);
    oldDateCheckbox.addActionListener(action); // do last so don't fire early
    panel21.add(oldDateCheckbox);
    panel21.add(Box.createHorizontalStrut(1));

    newDateCheckbox = new JCheckBox("newest (modified) date.", (! oldDateFlag));
    if (buttonFont != null) newDateCheckbox.setFont(buttonFont);
    newDateCheckbox.addActionListener(action); // do last so don't fire early
    panel21.add(newDateCheckbox);

    panel01.add(panel21);
    panel01.add(Box.createVerticalStrut(6)); // space between panels

    /* Options for adjusting dates and times by seconds, minutes, etc. */

    JPanel panel23 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    adjustCheckbox = new JCheckBox(
      "Adjust date and time found by adding to each as follows (or subtracting if negative):",
      ((adjustValueYear != 0) || (adjustValueMonth != 0)
        || (adjustValueDay != 0) || (adjustValueHour != 0)
        || (adjustValueMinute != 0) || (adjustValueSecond != 0)));
    if (buttonFont != null) adjustCheckbox.setFont(buttonFont);
    panel23.add(adjustCheckbox);
    panel01.add(panel23);
    panel01.add(Box.createVerticalStrut(7)); // space between panels

    JPanel panel24 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    panel24.add(Box.createHorizontalStrut(22));

    adjustFieldYear = new JTextField(String.valueOf(adjustValueYear), 3);
    if (buttonFont != null) adjustFieldYear.setFont(buttonFont);
    adjustFieldYear.setHorizontalAlignment(JTextField.RIGHT);
    adjustFieldYear.setMargin(inputMargins);
    adjustFieldYear.setOpaque(false);
    panel24.add(adjustFieldYear);
    adjustLabelYear = new JLabel(" years, ");
    if (buttonFont != null) adjustLabelYear.setFont(buttonFont);
    panel24.add(adjustLabelYear);

    adjustFieldMonth = new JTextField(String.valueOf(adjustValueMonth), 3);
    if (buttonFont != null) adjustFieldMonth.setFont(buttonFont);
    adjustFieldMonth.setHorizontalAlignment(JTextField.RIGHT);
    adjustFieldMonth.setMargin(inputMargins);
    adjustFieldMonth.setOpaque(false);
    panel24.add(adjustFieldMonth);
    adjustLabelMonth = new JLabel(" months, ");
    if (buttonFont != null) adjustLabelMonth.setFont(buttonFont);
    panel24.add(adjustLabelMonth);

    adjustFieldDay = new JTextField(String.valueOf(adjustValueDay), 3);
    if (buttonFont != null) adjustFieldDay.setFont(buttonFont);
    adjustFieldDay.setHorizontalAlignment(JTextField.RIGHT);
    adjustFieldDay.setMargin(inputMargins);
    adjustFieldDay.setOpaque(false);
    panel24.add(adjustFieldDay);
    adjustLabelDay = new JLabel(" days, ");
    if (buttonFont != null) adjustLabelDay.setFont(buttonFont);
    panel24.add(adjustLabelDay);

    adjustFieldHour = new JTextField(String.valueOf(adjustValueHour), 3);
    if (buttonFont != null) adjustFieldHour.setFont(buttonFont);
    adjustFieldHour.setHorizontalAlignment(JTextField.RIGHT);
    adjustFieldHour.setMargin(inputMargins);
    adjustFieldHour.setOpaque(false);
    panel24.add(adjustFieldHour);
    adjustLabelHour = new JLabel(" hours, ");
    if (buttonFont != null) adjustLabelHour.setFont(buttonFont);
    panel24.add(adjustLabelHour);

    adjustFieldMinute = new JTextField(String.valueOf(adjustValueMinute), 3);
    if (buttonFont != null) adjustFieldMinute.setFont(buttonFont);
    adjustFieldMinute.setHorizontalAlignment(JTextField.RIGHT);
    adjustFieldMinute.setMargin(inputMargins);
    adjustFieldMinute.setOpaque(false);
    panel24.add(adjustFieldMinute);
    adjustLabelMinute = new JLabel(" minutes, ");
    if (buttonFont != null) adjustLabelMinute.setFont(buttonFont);
    panel24.add(adjustLabelMinute);

    adjustFieldSecond = new JTextField(String.valueOf(adjustValueSecond), 3);
    if (buttonFont != null) adjustFieldSecond.setFont(buttonFont);
    adjustFieldSecond.setHorizontalAlignment(JTextField.RIGHT);
    adjustFieldSecond.setMargin(inputMargins);
    adjustFieldSecond.setOpaque(false);
    panel24.add(adjustFieldSecond);
    adjustLabelSecond = new JLabel(" seconds.");
    if (buttonFont != null) adjustLabelSecond.setFont(buttonFont);
    panel24.add(adjustLabelSecond);

    panel01.add(panel24);
    panel01.add(Box.createVerticalStrut(6)); // space between panels

    /* Options for changing the file modification date. */

    JPanel panel25 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    redateCheckbox = new JCheckBox(
      "Change date in system file directory using date and time as found above or adjusted.",
      redateFlag);
    if (buttonFont != null) redateCheckbox.setFont(buttonFont);
    redateCheckbox.addActionListener(action); // do last so don't fire early
    panel25.add(redateCheckbox);

    panel01.add(panel25);
    panel01.add(Box.createVerticalStrut(5)); // space between panels

    /* Options for changing the file directory name. */

    JPanel panel27 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    renameCheckbox = new JCheckBox(
      ("Rename files with a date and time prefix, similar to: "
      + RENAME_FORMAT), renameFlag);
    if (buttonFont != null) renameCheckbox.setFont(buttonFont);
    renameCheckbox.addActionListener(action); // do last so don't fire early
    panel27.add(renameCheckbox);
    panel27.add(Box.createHorizontalStrut(5));

    renameFormatButton = new JButton("Set Format...");
    renameFormatButton.addActionListener(action);
    if (buttonFont != null) renameFormatButton.setFont(buttonFont);
    renameFormatButton.setMnemonic(KeyEvent.VK_F);
    renameFormatButton.setToolTipText("Edit date, time format for renaming.");
    panel27.add(renameFormatButton);

    panel01.add(panel27);
    panel01.add(Box.createVerticalStrut(10)); // space between panels

    /* Miscellaneous options. */

    JPanel panel31 = new JPanel(new BorderLayout(10, 0));

    JPanel panel32 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    fontNameDialog = new JComboBox(GraphicsEnvironment
      .getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    if (buttonFont != null) fontNameDialog.setFont(buttonFont);
    fontNameDialog.setSelectedItem(fontName); // select default font name
    fontNameDialog.setToolTipText("Font name for output text.");
    fontNameDialog.addActionListener(action); // do last so don't fire early
    panel32.add(fontNameDialog);
    panel32.add(Box.createHorizontalStrut(5));

    TreeSet sizelist = new TreeSet(); // collect font sizes 10 to 99 in order
    word = String.valueOf(fontSize); // convert number to a string we can use
    sizelist.add(word);           // add default or user's chosen font size
    for (i = 0; i < FONT_SIZES.length; i ++) // add our preferred size list
      sizelist.add(FONT_SIZES[i]); // assume sizes are all two digits (10-99)
    fontSizeDialog = new JComboBox(sizelist.toArray()); // give user nice list
    if (buttonFont != null) fontSizeDialog.setFont(buttonFont);
    fontSizeDialog.setSelectedItem(word); // selected item is our default size
    fontSizeDialog.setToolTipText("Point size for output text.");
    fontSizeDialog.addActionListener(action); // do last so don't fire early
    panel32.add(fontSizeDialog);

    panel31.add(panel32, BorderLayout.WEST);

    JPanel panel33 = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));

    debugCheckbox = new JCheckBox("debug mode", debugFlag);
    if (buttonFont != null) debugCheckbox.setFont(buttonFont);
    debugCheckbox.setToolTipText("Verbose output, don't change real files.");
    debugCheckbox.addActionListener(action); // do last so don't fire early
    panel33.add(debugCheckbox);

    recurseCheckbox = new JCheckBox("subfolders", recurseFlag);
    if (buttonFont != null) recurseCheckbox.setFont(buttonFont);
    recurseCheckbox.setToolTipText("Select to search folders and subfolders.");
    recurseCheckbox.addActionListener(action); // do last so don't fire early
    panel33.add(recurseCheckbox);

    panel31.add(panel33, BorderLayout.CENTER);

    showDialog = new JComboBox(SHOW_CHOICES);
    if (buttonFont != null) showDialog.setFont(buttonFont);
    showDialog.setSelectedIndex(showIndex);
    showDialog.setToolTipText("Select which files to report.");
    showDialog.addActionListener(action); // do last so don't fire early
    panel31.add(showDialog, BorderLayout.EAST);

    panel01.add(panel31);
    panel01.add(Box.createVerticalStrut(15)); // space between panels

    /* Bind all of the buttons and options above into a single panel so that
    the layout does not change when the window changes. */

    JPanel panel41 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
    panel41.add(panel01);

    /* Create a scrolling text area for the generated output. */

    outputText = new JTextArea(20, 40);
    outputText.setEditable(false); // user can't change this text area
    outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    outputText.setLineWrap(false); // don't wrap text lines
    outputText.setMargin(new Insets(5, 6, 5, 6)); // top, left, bottom, right
    outputText.setText(
      "\nChange file names or the \"last modified\" date for JPEG photo files,"
      + "\nusing an embedded date and time found in most JPEG files.  The"
      + "\ncontents of the files are not changed."
      + "\n\nChoose your options; then open files or folders to search.  There"
      + "\nis no \"undo\" feature.  If in doubt, select the \"debug mode\" option."
      + "\n\nBe careful when renaming files.  A new prefix is added each time"
      + "\nthere is a difference.  Old prefixes are not replaced or removed."
      + "\n\nCopyright (c) 2017 by Keith Fenske.  By using this program, you"
      + "\nagree to terms and conditions of the Apache License and/or GNU"
      + "\nGeneral Public License.\n\n");

    JScrollPane panel51 = new JScrollPane(outputText);
    panel51.setBorder(emptyBorder); // no border necessary here

    /* Create an entire panel just for the status message.  Set margins with a
    BorderLayout, because a few pixels higher or lower can make a difference in
    whether the position of the status text looks correct. */

    statusDialog = new JLabel(statusPending, JLabel.RIGHT);
    if (buttonFont != null) statusDialog.setFont(buttonFont);

    JPanel panel61 = new JPanel(new BorderLayout(0, 0));
    panel61.add(Box.createVerticalStrut(7), BorderLayout.NORTH);
    panel61.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    panel61.add(statusDialog, BorderLayout.CENTER);
    panel61.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
//  panel61.add(Box.createVerticalStrut(5), BorderLayout.SOUTH);

    /* Combine buttons and options with output text.  The text area expands and
    contracts with the window size.  Put our status message at the bottom. */

    JPanel panel71 = new JPanel(new BorderLayout(0, 0));
    panel71.add(panel41, BorderLayout.NORTH); // buttons and options
    panel71.add(panel51, BorderLayout.CENTER); // text area
    panel71.add(panel61, BorderLayout.SOUTH); // status message

    /* Create the main window frame for this application.  We supply our own
    margins using the edges of the frame's border layout. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    Container panel72 = mainFrame.getContentPane(); // where content meets frame
    panel72.setLayout(new BorderLayout(0, 0));
    panel72.add(Box.createVerticalStrut(15), BorderLayout.NORTH); // top margin
    panel72.add(Box.createHorizontalStrut(5), BorderLayout.WEST); // left
    panel72.add(panel71, BorderLayout.CENTER); // actual content in center
    panel72.add(Box.createHorizontalStrut(5), BorderLayout.EAST); // right
    panel72.add(Box.createVerticalStrut(5), BorderLayout.SOUTH); // bottom

    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setLocation(windowLeft, windowTop); // normal top-left corner
    if ((windowHeight < MIN_FRAME) || (windowWidth < MIN_FRAME))
      mainFrame.pack();           // do component layout with minimum size
    else                          // the user has given us a window size
      mainFrame.setSize(windowWidth, windowHeight); // size of normal window
    if (maximizeFlag) mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    mainFrame.validate();         // recheck application window layout
    mainFrame.setVisible(true);   // and then show application window

    /* We don't need this dialog box yet, but create a panel that will be used
    to edit the date and time format (pattern) for renaming files. */

    renameFormatDialog = new JPanel();
    renameFormatDialog.setLayout(new BoxLayout(renameFormatDialog,
      BoxLayout.Y_AXIS));
    renameFormatDialog.add(Box.createVerticalStrut(5));

    JTextArea text81 = new JTextArea(8, 30);
    if (buttonFont != null) text81.setFont(buttonFont);
    text81.setLineWrap(true);
    text81.setMargin(new Insets(1, 3, 2, 3)); // top, left, bottom, right
    text81.setOpaque(false);
    text81.setWrapStyleWord(true);
    text81.setText("Create or edit a SimpleDateFormat pattern to be used as a"
      + " prefix when renaming files, where: yyyy = 4-digit year, MM = 2-digit"
      + " month, dd = 2-digit day, HH = 2-digit hour (24-hour clock), mm ="
      + " 2-digit minute, ss = 2-digit second. Click the Test button to check"
      + " your pattern. Avoid the characters \" * / : < > ? \\ | and don't"
      + " start with . or a space.");
    renameFormatDialog.add(text81);
    renameFormatDialog.add(Box.createVerticalStrut(5));

    renamePatternText = new JTextField(30);
    if (buttonFont != null) renamePatternText.setFont(buttonFont);
    renamePatternText.setMargin(new Insets(1, 4, 2, 4));
    renameFormatDialog.add(renamePatternText);
    renameFormatDialog.add(Box.createVerticalStrut(8));

    JPanel panel82 = new JPanel(new BorderLayout(10, 0));

    renameTestButton = new JButton("Test");
    renameTestButton.addActionListener(action);
//  if (buttonFont != null) renameTestButton.setFont(buttonFont);
    renameTestButton.setMnemonic(KeyEvent.VK_T);
    renameTestButton.setToolTipText("Format current date, time.");
    panel82.add(renameTestButton, BorderLayout.WEST);

    renameResultText = new JTextField(20);
    if (buttonFont != null) renameResultText.setFont(buttonFont);
    renameResultText.setMargin(new Insets(1, 4, 3, 4));
    renameResultText.setOpaque(false);
    panel82.add(renameResultText, BorderLayout.CENTER);

    renameFormatDialog.add(panel82);
    renameFormatDialog.add(Box.createVerticalStrut(15));

    /* Let the graphical interface run the application now. */

    openButton.requestFocusInWindow(); // give keyboard focus to "Open" button

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  doCancelButton() method

  This method is called while we are opening files or folders if the user wants
  to end the processing early, perhaps because it is taking too long.  We must
  cleanly terminate any secondary threads.  Leave whatever output has already
  been generated in the output text area.
*/
  static void doCancelButton()
  {
    cancelFlag = true;            // tell other threads that all work stops now
    putOutput("Cancelled by user."); // print message and scroll
  }


/*
  doFormatButton() method

  Edit and test a new SimpleDateFormat pattern, which will be used when files
  are renamed with a date and time prefix.
*/
  static void doFormatButton()
  {
    renamePatternText.setText(renameDateString); // current pattern string
    doFormatTest();               // attempt to format with current pattern
    if (JOptionPane.showConfirmDialog(mainFrame, renameFormatDialog,
      "Edit Date, Time Format", JOptionPane.OK_CANCEL_OPTION,
      JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
    {
      return;                     // user cancelled, ignore changes
    }
    try                           // is this date, time pattern valid?
    {
      SimpleDateFormat sdf = new SimpleDateFormat(renamePatternText.getText());
    }
    catch (IllegalArgumentException iae) // pattern was not accepted
    {
      JOptionPane.showMessageDialog(mainFrame,
        "Sorry, that date and time pattern is not valid.\nUsing the previous pattern.");
      return;
    }
    renameDateString = renamePatternText.getText(); // pattern is valid
    renameDateFormat = new SimpleDateFormat(renameDateString);
    renameFlag = (renameDateString.length() > 0); // pattern enables option
    renameCheckbox.setSelected(renameFlag); // adjust GUI element to match

  } // end of doFormatButton() method


/*
  doFormatTest() method

  Check the SimpleDateFormat pattern by attempting to format the current date
  and time.  Only the creation of a SimpleDateFormat object may generate an
  exception; the formatting never does.
*/
  static void doFormatTest()
  {
    try
    {
      renameResultText.setText(new SimpleDateFormat(
        renamePatternText.getText()).format(new Date()));
    }
    catch (IllegalArgumentException iae) // pattern was not accepted
    {
      renameResultText.setText("Date and time pattern is not valid.");
    }
  }


/*
  doOpenButton() method

  Allow the user to select one or more files or folders for processing.
*/
  static void doOpenButton()
  {
    int i;                        // index variable

    /* The user's options for adjusting dates and times are easier to check
    here than with GUI listeners. */

    if (adjustCheckbox.isSelected()) // does the user want this option?
    {
      if ((adjustValueYear = doOpenNumber("years", adjustFieldYear, -99, 99))
        == Integer.MAX_VALUE) return;
      if ((adjustValueMonth = doOpenNumber("months", adjustFieldMonth, -999,
        999)) == Integer.MAX_VALUE) return;
      if ((adjustValueDay = doOpenNumber("days", adjustFieldDay, -9999, 9999))
        == Integer.MAX_VALUE) return;
      if ((adjustValueHour = doOpenNumber("hours", adjustFieldHour, -99999,
        99999)) == Integer.MAX_VALUE) return;
      if ((adjustValueMinute = doOpenNumber("minutes", adjustFieldMinute,
        -999999, 999999)) == Integer.MAX_VALUE) return;
      if ((adjustValueSecond = doOpenNumber("seconds", adjustFieldSecond,
        -9999999, 9999999)) == Integer.MAX_VALUE) return;
    }
    else                          // this option has not been selected
      adjustValueYear = adjustValueMonth = adjustValueDay = adjustValueHour
        = adjustValueMinute = adjustValueSecond = 0; // do not adjust dates

    /* Ask the user for input files or folders. */

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Open Files or Folders...");
    fileChooser.setFileHidingEnabled(! hiddenFlag); // may show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fileChooser.setMultiSelectionEnabled(true); // allow more than one file
    if (fileChooser.showOpenDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
    openFileList = sortFileList(fileChooser.getSelectedFiles());
                                  // get list of files selected by user

    /* We have a list of files or folders.  Disable the "Open" button until we
    are done, and enable a "Cancel" button in case our secondary thread runs
    for a long time and the user panics. */

    adjustCheckbox.setEnabled(false); // can't change this while running
    adjustFieldYear.setEnabled(false); // from big to small
    adjustFieldMonth.setEnabled(false);
    adjustFieldDay.setEnabled(false);
    adjustFieldHour.setEnabled(false);
    adjustFieldMinute.setEnabled(false);
    adjustFieldSecond.setEnabled(false);
    adjustLabelYear.setEnabled(false); // from big to small
    adjustLabelMonth.setEnabled(false);
    adjustLabelDay.setEnabled(false);
    adjustLabelHour.setEnabled(false);
    adjustLabelMinute.setEnabled(false);
    adjustLabelSecond.setEnabled(false);
    cancelButton.setEnabled(true); // enable button to cancel this processing
    cancelFlag = false;           // but don't cancel unless user complains
    newDateCheckbox.setEnabled(false);
    oldDateCheckbox.setEnabled(false);
    openButton.setEnabled(false); // suspend "Open" button until we are done
    outputText.setText("");       // clear output text area
    redateCheckbox.setEnabled(false);
    renameCheckbox.setEnabled(false);
    renameFormatButton.setEnabled(false);
    totalChange = totalCorrect = totalError = totalFiles = totalFolders
      = totalNoData = 0;          // no files found yet

    setStatusMessage(EMPTY_STATUS); // clear text in status message
    statusTimer.start();          // start updating status on clock ticks

    openFilesThread = new Thread(new RedatePhotoFile3User(), "doOpenRunner");
    openFilesThread.setPriority(Thread.MIN_PRIORITY);
                                  // use low priority for heavy-duty workers
    openFilesThread.start();      // run separate thread to open files, report

  } // end of doOpenButton() method


/*
  doOpenNumber() method

  This is a helper method for doOpenButton() to parse an integer field, check
  the range, and return the result or Integer.MAX_VALUE for an error.
*/
  static int doOpenNumber(String tag, JTextField field, int lower, int upper)
  {
    int result;                   // our result, as an integer or otherwise
    String text;                  // text from caller's field

    text = field.getText();       // may be empty, a number, or bad syntax
    if (text.length() > 0)        // any non-empty string should be a number
    {
      try { result = Integer.parseInt(text); } // parse as signed integer
      catch (NumberFormatException nfe) { result = Integer.MAX_VALUE; }
    }
    else
      result = 0;                 // assume empty string means integer zero

    if ((result < lower) || (result > upper))
    {
      JOptionPane.showMessageDialog(mainFrame,
        ("A date or time adjustment in " + tag + "\nmust be from " + lower
        + " to " + upper + "."));
      result = Integer.MAX_VALUE;
    }
    return(result);               // give caller whatever we could find

  } // end of doOpenNumber() method


/*
  doOpenRunner() method

  This method is called inside a separate thread by the runnable interface of
  our "user" class to process the user's selected files in the context of the
  "main" class.  By doing all the heavy-duty work in a separate thread, we
  won't stall the main thread that runs the graphical interface, and we allow
  the user to cancel the processing if it takes too long.
*/
  static void doOpenRunner()
  {
    int i;                        // index variable

    /* Loop once for each file name selected.  Don't assume that these are all
    valid file names. */

    for (i = 0; i < openFileList.length; i ++)
    {
      if (cancelFlag) break;      // exit <for> loop if cancel or fatal error
      processFileOrFolder(openFileList[i]); // process this file or folder
    }

    /* Print a summary and scroll the output, even if we were cancelled. */

    printSummary();               // what we found and what was changed

    /* We are done.  Turn off the "Cancel" button and allow the user to click
    the "Start" button again. */

    adjustCheckbox.setEnabled(true); // allow changes once again
    adjustFieldYear.setEnabled(true); // from big to small
    adjustFieldMonth.setEnabled(true);
    adjustFieldDay.setEnabled(true);
    adjustFieldHour.setEnabled(true);
    adjustFieldMinute.setEnabled(true);
    adjustFieldSecond.setEnabled(true);
    adjustLabelYear.setEnabled(true); // from big to small
    adjustLabelMonth.setEnabled(true);
    adjustLabelDay.setEnabled(true);
    adjustLabelHour.setEnabled(true);
    adjustLabelMinute.setEnabled(true);
    adjustLabelSecond.setEnabled(true);
    cancelButton.setEnabled(false); // disable "Cancel" button
    newDateCheckbox.setEnabled(true);
    oldDateCheckbox.setEnabled(true);
    openButton.setEnabled(true);  // enable "Open" button
    redateCheckbox.setEnabled(true);
    renameCheckbox.setEnabled(true);
    renameFormatButton.setEnabled(true);

    statusTimer.stop();           // stop updating status message by timer
    setStatusMessage(EMPTY_STATUS); // and clear any previous status message

  } // end of doOpenRunner() method


/*
  doSaveButton() method

  Ask the user for an output file name, create or replace that file, and copy
  the contents of our output text area to that file.  The output file will be
  in the default character set for the system, so if there are special Unicode
  characters in the displayed text (Arabic, Chinese, Eastern European, etc),
  then you are better off copying and pasting the output text directly into a
  Unicode-aware application like Microsoft Word.
*/
  static void doSaveButton()
  {
    FileWriter output;            // output file stream
    File userFile;                // file chosen by the user

    /* Ask the user for an output file name. */

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Save Output as Text File...");
    fileChooser.setFileHidingEnabled(true); // don't show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showSaveDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
    userFile = fileChooser.getSelectedFile();

    /* See if we can write to the user's chosen file. */

    if (userFile.isDirectory())   // can't write to directories or folders
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is a directory or folder.\nPlease select a normal file."));
      return;
    }
    else if (userFile.isHidden()) // won't write to hidden (protected) files
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is a hidden or protected file.\nPlease select a normal file."));
      return;
    }
    else if (userFile.isFile() == false) // if file doesn't exist
    {
      /* Maybe we can create a new file by this name.  Do nothing here. */
    }
    else if (userFile.canWrite() == false) // file exists, but is read-only
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is locked or write protected.\nCan't write to this file."));
      return;
    }
    else if (JOptionPane.showConfirmDialog(mainFrame, (userFile.getName()
      + " already exists.\nDo you want to replace this with a new file?"))
      != JOptionPane.YES_OPTION)
    {
      return;                     // user cancelled file replacement dialog
    }

    /* Write lines to output file. */

    try                           // catch file I/O errors
    {
      output = new FileWriter(userFile); // try to open output file
      outputText.write(output);   // couldn't be much easier for writing!
      output.close();             // try to close output file
    }
    catch (IOException ioe)
    {
      putOutput("Can't write to text file: " + ioe.getMessage());
    }
  } // end of doSaveButton() method


/*
  printSummary() method

  Tell the user what we found and what was changed.
*/
  static void printSummary()
  {
    putOutput("Found " + formatComma.format(totalFiles)
      + ((totalFiles == 1) ? " file" : " files") + " in "
      + formatComma.format(totalFolders)
      + ((totalFolders == 1) ? " folder" : " folders") + ": "
      + formatComma.format(totalCorrect) + " correct, "
      + formatComma.format(totalChange) + " changed, "
      + formatComma.format(totalError)
      + ((totalError == 1) ? " error" : " errors") + ", "
      + formatComma.format(totalNoData) + " no data.");
  }


/*
  processFileOrFolder() method

  The caller gives us a Java File object that may be a file, a folder, or just
  random garbage.  Search all files.  Get folder contents and process each file
  found, doing subfolders only if the <recurseFlag> is true.

  Do not call GUI methods or reference GUI objects here, because we may be
  running from the command line as a "console" application.
*/
  static void processFileOrFolder(File givenFile)
  {
    File canon;                   // full directory resolution of <givenFile>
    char ch;                      // one input character (from byte)
    File[] contents;              // contents if <givenFile> is a folder
    boolean fileChangeFlag;       // true if this file has successful changes
    boolean fileCorrectFlag;      // true if this file has correct date, prefix
    boolean fileErrorFlag;        // true if this file has failures to change
    char found[] = {'1', '9', '9', '9', '-', '1', '2', '-', '3', '1', ' ', '2',
      '3', ':', '5', '9', ':', '5', '9'}; // matching characters found in file
    String foundBestDate;         // best date, time found so far, or null
    String givenName;             // caller's file name only, without path
    String givenPath;             // name of caller's file, including path
    int i;                        // index variable
    BufferedInputStream input;    // for reading a file as bytes
    File newFile;                 // renamed File object for <givenFile>
    long newMillis;               // new Java date in milliseconds for file
    String newName;               // new file or folder name
    File next;                    // next File object from <contents>
    long oldMillis;               // old Java date in milliseconds for file
    String prefix;                // date, time prefix for file name
    long readCount;               // total number of bytes read from file
    int state;                    // current state number of parsing machine
    String userNewDate, userOldDate; // dates and times that we show user

    if (cancelFlag) return;       // stop if user cancel or fatal error

    /* Decide what kind of File object this is, if it's even real!  We process
    all files/folders given to us, no matter whether they are hidden or not.
    It's only when we look at subfolders that we pay attention to <hiddenFlag>
    and <recurseFlag>. */

    try { canon = givenFile.getCanonicalFile(); } // full directory search
    catch (IOException ioe) { canon = givenFile; } // accept abstract file
    givenName = canon.getName();  // get the file name only
    givenPath = canon.getPath();  // get file name with path
    setStatusMessage(givenPath);  // use name with path for status text

    /* Most of the work is done later in this method.  Search through folders
    and subfolders here, eliminate File objects that don't exist, and leave
    only real files for later. */

    if (canon.isDirectory())      // is this a folder?
    {
      totalFolders ++;            // one more folder or subfolder found
      putOutput("Searching folder " + givenPath);
//    putOutComment("Searching folder " + givenPath);
      contents = sortFileList(canon.listFiles()); // sorted, no filter
      for (i = 0; i < contents.length; i ++) // for each file in order
      {
        if (cancelFlag) return;   // stop if user cancel or fatal error
        next = contents[i];       // get next File object from <contents>
        if (next.isHidden() && (hiddenFlag == false))
        {
          putOutComment(next.getName()
            + " - ignoring hidden file or subfolder");
        }
        else if (next.isDirectory()) // is this a subfolder (in the folder)?
        {
          if (recurseFlag)        // should we look at subfolders?
            processFileOrFolder(next); // yes, search this subfolder
          else
            putOutComment(next.getName() + " - ignoring subfolder");
        }
        else if (next.isFile())   // is this a file (in the folder)?
        {
          processFileOrFolder(next); // yes, call ourself to do this file
        }
        else
        {
          /* File or folder does not exist.  Ignore without comment. */
        }
      }
      return;                     // folder is complete
    }
    else if (canon.isFile() == false) // most likely does not exist
    {
      putOutput(givenName + " - not a file or folder");
//    cancelFlag = true;          // don't do anything more
      totalError ++;              // count as error, even if don't know reason
      return;
    }
    totalFiles ++;                // one more file found, may be JPEG

    /* Read through the file, picking out strings formatted as Exif dates and
    times, including the null terminating byte.  There can be garbage bytes
    before the string, some of which may look like a partial date or time.
    This ugly code below is still easier and less error prone than parsing the
    official Exif file format.  (Big endian data versus little endian, offset
    pointers, range checking at every step, etc.)

    As an exercise to the reader, calculate the probability that a sequence of
    data bytes has the same format as an Exif date, assuming a uniform random
    distribution of values for the bytes.  (Answer: 6.84e-35.) */

    foundBestDate = null;         // no valid date, time found yet
    try                           // catch I/O errors (file not found, etc)
    {
      input = new BufferedInputStream(new FileInputStream(canon));
                                  // read file bytes as characters
      readCount = 0;              // no bytes (characters) read from file yet
      state = 0;                  // also number of characters found in string
      while ((readCount < READ_LIMIT) && ((i = input.read()) >= 0))
                                  // for each byte (character) in file
      {
        readCount ++;             // one more byte (character) read from file
        ch = (char) i;            // convert byte integer to character
        switch (state)            // helps that state is also parsed length
        {
          case (0):               // nothing found yet
          case (1):               // found: 1?
          case (2):               // found: 19?
          case (3):               // found: 199?
          case (5):               // found: 1999:?
          case (6):               // found: 1999:1?
          case (8):               // found: 1999:12:?
          case (9):               // found: 1999:12:3?
          case (11):              // found: 1999:12:31 ?
          case (12):              // found: 1999:12:31 2?
          case (14):              // found: 1999:12:31 23:?
          case (15):              // found: 1999:12:31 23:5?
          case (17):              // found: 1999:12:31 23:59:?
          case (18):              // found: 1999:12:31 23:59:5?
            if (Character.isDigit(ch)) // expecting digit (number)
            {
              found[state] = ch;  // save this digit
              state ++;           // advance to next state
            }
            else state = 0;       // unexpected input, start from beginning
            break;

          case (4):               // found: 1999?
            if (ch == ':') { state ++; } // expecting colon (:)
            else if (Character.isDigit(ch)) // too many digits for year
            {
              found[0] = found[1]; // drop first digit, shift others left
              found[1] = found[2]; found[2] = found[3]; found[3] = ch;
              /* and remain in state 4 */
            }
            else state = 0;
            break;

          case (7):               // found: 1999:12?
          case (13):              // found: 1999:12:31 23?
          case (16):              // found: 1999:12:31 23:59?
            if (ch == ':') { state ++; } // expecting colon (:)
            else if (Character.isDigit(ch)) // too many digits, restart year
            {
              found[0] = found[state - 2]; found[1] = found[state - 1];
              found[2] = ch; state = 3;
            }
            else state = 0;
            break;

          case (10):              // found: 1999:12:31?
            if (ch == ' ') { state ++; } // expecting blank space
            else if (Character.isDigit(ch)) // too many digits, restart year
            {
              found[0] = found[state - 2]; found[1] = found[state - 1];
              found[2] = ch; state = 3;
            }
            else state = 0;
            break;

          case (19):              // found: 1999:12:31 23:59:59?
            if (ch == 0x00)       // is string terminated by a null byte?
            {
              /* Found a date and time with valid syntax.  Is it within range
              (not "0000-00-00", etc) and better than what we already have? */

              String foundThisDate = new String(found);
                                  // convert character array to real string
              putOutDebug(givenName + " - found date and time "
                + foundThisDate);
              if ((foundThisDate.compareTo(DATE_LOWER) < 0)
                || (foundThisDate.compareTo(DATE_UPPER) > 0))
              {
                putOutDebug(givenName + " - not within limits " + DATE_LOWER
                  + " to " + DATE_UPPER);
              }
              else if ((foundBestDate == null) // better than what we have?
                || ((oldDateFlag == false) && (foundBestDate.compareTo(foundThisDate) < 0))
                || ((oldDateFlag == true)  && (foundBestDate.compareTo(foundThisDate) > 0)))
              {
                foundBestDate = foundThisDate; // save the better date, time
              }
              state = 0;          // this string finished, look for next string
            }
            else if (Character.isDigit(ch)) // too many digits, restart year
            {
              found[0] = found[state - 2]; found[1] = found[state - 1];
              found[2] = ch; state = 3;
            }
            else state = 0;
            break;

          default:
            System.err.println("Error in checkDateTime(): unknown state = "
              + state);           // should never happen, so write on console
            state = 0;
            break;
        }
      }
      input.close();              // close (and unlock) user's file
    }
    catch (IOException ioe)       // file may be locked, invalid, etc
    {
      putOutput(givenName + " - " + ioe.getMessage());
//    cancelFlag = true;          // don't do anything more
      totalError ++;              // one more file with an error
      return;
    }

    /* Did we find a valid date and time? */

    if (cancelFlag) return;       // stop if user cancel or fatal error

    if (foundBestDate == null)    // won't change from null if nothing found
    {
      putOutComment(givenName + " - date and time not found");
      totalNoData ++;             // one more file with no date, time info
      return;
    }
    else if (debugFlag)           // does user want details?
    {
      putOutput(givenName + " - using date and time " + foundBestDate);
      /* ... and fall through to remaining code, even if both the redate and
      rename flags are turned off. */
    }
    else if ((redateFlag == false) && (renameFlag == false))
    {
      putOutComment(givenName + " - found date and time " + foundBestDate);
      return;
    }
    fileChangeFlag = fileCorrectFlag = fileErrorFlag = false;
                                  // nothing known about this file yet

    /* Convert date and time digits into something Java can use.  Fortunately,
    JPEG dates are very close to ISO standard dates, and we cleverly converted
    the JPEG date into an ISO date while parsing.  (Look at how non-numeric
    fields are initialized in the <found> character array.) */

    try { newMillis = ourDateFormat.parse(foundBestDate).getTime(); }
    catch (ParseException pe)     // unable to parse date, time string
    {
      putOutput(givenName + " - can't parse date and time " + foundBestDate);
      totalError ++;              // one more file with an error
      return;                     // don't continue after error
    }

    /* Don't adjust dates and times by adding to <newMillis> directly, because
    not all days have the same number of seconds.  Clock changes like daylight
    saving time (DST) insert and remove one hour twice a year.  Use a Calendar
    object to avoid introducing bugs in these calculations. */

    adjustCalendar.clear();       // clear any previous date, time fields
    adjustCalendar.setTimeInMillis(newMillis); // put millis into calendar
    adjustCalendar.add(Calendar.YEAR, adjustValueYear); // usually just zero
    adjustCalendar.add(Calendar.MONTH, adjustValueMonth);
    adjustCalendar.add(Calendar.DATE, adjustValueDay);
    adjustCalendar.add(Calendar.HOUR, adjustValueHour);
    adjustCalendar.add(Calendar.MINUTE, adjustValueMinute);
    adjustCalendar.add(Calendar.SECOND, adjustValueSecond);
    newMillis = adjustCalendar.getTimeInMillis(); // bring back as millis
    userNewDate = ourDateFormat.format(new Date(newMillis)); // reformat
    if (foundBestDate.compareTo(userNewDate) != 0) // any difference?
      putOutDebug(givenName + " - adjust date and time " + userNewDate);
    prefix = renameDateFormat.format(new Date(newMillis));
                                  // create rename prefix before adjusting DST

    /* Microsoft Windows adjusts file dates and times using the current rules
    for daylight saving time (DST), no matter which rules should be applied at
    that actual date and time.  Correcting for this assumption is almost
    impossible because both Java and Windows think they are in charge of time
    zones and DST.  The following code only does local times, and is quite
    likely to break if either the JRE or Windows changes. */

    if (mswinFlag)                // do we correct for Windows assumptions?
      newMillis += ourTimeZone.getOffset(newMillis)
        - ourTimeZone.getOffset((new Date()).getTime());

    /* When displaying an old date, we have the same problem as above, but in
    reverse: daylight saving time.  This could devolve into a long discussion
    about the difficulty of following date and time rules.  It would be easier
    if we simply didn't show the old date! */

    oldMillis = canon.lastModified(); // save previous Java time stamp
    if (mswinFlag)                // do we correct for Windows assumptions?
    {
      userOldDate = ourDateFormat.format(new Date(oldMillis
        - ourTimeZone.getOffset(oldMillis)
        + ourTimeZone.getOffset((new Date()).getTime())));
    }
    else                          // don't correct, use time stamp as-is
      userOldDate = ourDateFormat.format(new Date(oldMillis));
    putOutDebug(givenName + " - file date and time is " + userOldDate);

    /* Change the last modification date and time for the file. */

    if (cancelFlag) return;       // stop if user cancel or fatal error

    if (redateFlag == false)      // does the user want date, time changed?
    {
      /* No, do nothing.  Count nothing. */
    }
    else if (Math.abs(newMillis - oldMillis) < MILLI_FUZZ)
    {
      putOutComment(givenName + " - has correct date and time");
      fileCorrectFlag = true;     // at least one correct for this file
    }
    else if (canon.canWrite() == false)
    {
      putOutFailure(givenName + " - can't change read-only file to "
        + userNewDate);
      fileErrorFlag = true;       // at least one error for this file
    }
    else if (debugFlag)           // do we simulate the result?
    {
      putOutSuccess(givenName + " - simulate change " + userNewDate + " from "
        + userOldDate);
    }
    else if (canon.setLastModified(newMillis)) // try to change date, time
    {
      putOutSuccess(givenName + " - changed date to " + userNewDate + " from "
        + userOldDate);
      fileChangeFlag = true;      // at least one change for this file
    }
    else                          // error from setLastModified() method
    {
      putOutFailure(givenName + " - failed change to " + userNewDate + " from "
        + userOldDate);
      fileErrorFlag = true;       // at least one error for this file
    }

    /* Change the file name by inserting a date and time at the beginning of
    the name, if it isn't already there.  Only year-month-day (hour-minute-
    second) order makes sense here, to put files in chronological order when
    sorted by name. */

    if (cancelFlag) return;       // stop if user cancel or fatal error

    putOutDebug(givenName + " - file name prefix is <" + prefix + ">");
    newName = prefix + givenName; // new file name, if prefix is necessary

    if (renameFlag == false)      // does the user want file name changed?
    {
      /* No, do nothing.  Count nothing. */
    }
    else if (givenName.startsWith(prefix))
    {
      putOutComment(givenName + " - has correct file name prefix");
      fileCorrectFlag = true;     // at least one correct for this file
    }
    else if (canon.canWrite() == false)
    {
      putOutFailure(givenName + " - can't rename read-only file as "
        + newName);
      fileErrorFlag = true;       // at least one error for this file
    }
    else if (debugFlag)           // do we simulate the result?
    {
      putOutSuccess(givenName + " - simulate rename " + newName);
    }
    else if (canon.renameTo(new File(canon.getParent(), newName)))
    {                             // try to change file name
      putOutSuccess(givenName + " - changed name to " + newName);
      fileChangeFlag = true;      // at least one change for this file
    }
    else                          // error from renameTo() method
    {
      putOutFailure(givenName + " - failed rename as " + newName);
      fileErrorFlag = true;       // at least one error for this file
    }

    /* Count this file once if we changed the date and time or the file name.
    Otherwise, count as correct if at least one was correct. */

    if (fileChangeFlag)           // were there any successful changes?
      totalChange ++;             // yes, one more file with changes
    if (fileCorrectFlag && (! fileChangeFlag) && (! fileErrorFlag))
      totalCorrect ++;            // only correct if no other changes, errors
    if (fileErrorFlag)            // were there any failures to change?
      totalError ++;              // one more file with an error

  } // end of processFileOrFolder() method


/*
  putOutput() method

  Append a complete line of text to the end of the output text area.  We add a
  newline character at the end of the line, not the caller.  By forcing all
  output to go through this same method, one complete line at a time, the
  generated output is cleaner and can be redirected.

  The output text area is forced to scroll to the end, after the text line is
  written, by selecting character positions that are much too large (and which
  are allowed by the definition of the JTextComponent.select() method).  This
  is easier and faster than manipulating the scroll bars directly.  However, it
  does cancel any selection that the user might have made, for example, to copy
  text from the output area.
*/
  static void putOutput(String text)
  {
    if (mainFrame == null)        // during setup, there is no GUI window
      System.out.println(text);   // console output goes onto standard output
    else
    {
      outputText.append(text + "\n"); // graphical output goes into text area
      outputText.select(999999999, 999999999); // force scroll to end of text
    }
  }

  static void putOutComment(String text) // filter for general information
  {
    if (debugFlag || (showIndex == 0)) // any message not releated to change
      putOutput(text);
  }

  static void putOutDebug(String text) // filter for debug messages
  {
    if (debugFlag)                // if user wants to see details
      putOutput(text);
  }

  static void putOutFailure(String text) // filter for errors on change
  {
    if (debugFlag || (showIndex != 1)) // if failure to change date, name
      putOutput(text);
  }

  static void putOutSuccess(String text) // filter for success on change
  {
    if (debugFlag || (showIndex != 3)) // if successful change date, name
      putOutput(text);
  }


/*
  setStatusMessage() method

  Set the text for the status message if we are running as a GUI application.
  This gives the user some indication of our progress when processing is slow.
  If the update timer is running, then this message will not appear until the
  timer kicks in.  This prevents the status from being updated too often, and
  hence being unreadable.
*/
  static void setStatusMessage(String text)
  {
    if (mainFrame == null)        // are we running as a console application?
      return;                     // yes, console doesn't show running status
    statusPending = text;         // always save caller's status message
    if (statusTimer.isRunning())  // are we updating on a timed basis?
      return;                     // yes, wait for the timer to do an update
    statusDialog.setText(statusPending); // show the status message now
  }


/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("  java  RedatePhotoFile3  [options]  [fileOrFolderNames]");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -a# = number of seconds to add to date and time found (may be negative)");
    System.err.println("  -d = show debug information (may be verbose)");
//  System.err.println("  -f0 = find dates, times but do not change file date or rename");
    System.err.println("  -f1 = change file date in system file directory (default)");
    System.err.println("  -f2 = rename file with date, time prefix (see -p option)");
    System.err.println("  -f3 = change file date and rename file with date, time prefix");
    System.err.println("  -m0 = show all files and general comments (default)");
    System.err.println("  -m1 = show only files with successful changes");
    System.err.println("  -m2 = show files with changes or with errors");
    System.err.println("  -m3 = show only files with failures to change");
    System.err.println("  -p# = SimpleDateFormat for renaming; default is -p\"" + RENAME_FORMAT + "\"");
    System.err.println("  -s0 = do only given files or folders, no subfolders (default)");
    System.err.println("  -s1 = -s = process files, folders, and subfolders");
    System.err.println("  -t0 = find oldest (original) date and time (default)");
    System.err.println("  -t1 = -t = find newest (modified) date and time");
    System.err.println("  -u# = font size for buttons, dialogs, etc; default is local system;");
    System.err.println("      example: -u16");
    System.err.println("  -w(#,#,#,#) = normal window position: left, top, width, height;");
    System.err.println("      example: -w(50,50,700,500)");
    System.err.println("  -x = maximize application window; default is normal window");
    System.err.println();
    System.err.println("Output may be redirected with the \">\" operator.  If no file or folder names");
    System.err.println("are given on the command line, then a graphical interface will open.");
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method


/*
  sortFileList() method

  When we ask for a list of files or subfolders in a directory, the list is not
  likely to be in our preferred order.  Java does not guarantee any particular
  order, and the observed order is whatever is supplied by the underlying file
  system (which can be very jumbled for FAT16/FAT32).  We would like the file
  names to be sorted, and since we recurse on subfolders, we also want the
  subfolders to appear in order.

  The caller's parameter may be <null> and this may happen if the caller asks
  File.listFiles() for the contents of a protected system directory.  All calls
  to listFiles() in this program are wrapped inside a call to us, so we replace
  a null parameter with an empty array as our result.
*/
  static File[] sortFileList(File[] input)
  {
    String fileName;              // file name without the path
    int i;                        // index variable
    TreeMap list;                 // our list of files
    File[] result;                // our result
    StringBuffer sortKey;         // created sorting key for each file

    if (input == null)            // were we given a null pointer?
      result = new File[0];       // yes, replace with an empty array
    else if (input.length < 2)    // don't sort lists with zero or one element
      result = input;             // just copy input array as result array
    else
    {
      /* First, create a sorted list with our choice of index keys and the File
      objects as data.  Names are sorted as files or folders, then in lowercase
      to ignore differences in uppercase versus lowercase, then in the original
      form for systems where case is distinct. */

      list = new TreeMap();       // create empty sorted list with keys
      sortKey = new StringBuffer(); // allocate empty string buffer for keys
      for (i = 0; i < input.length; i ++)
      {
        sortKey.setLength(0);     // empty any previous contents of buffer
        if (input[i].isDirectory()) // is this "file" actually a folder?
          sortKey.append("2 ");   // yes, put subfolders after files
        else                      // must be a file or an unknown object
          sortKey.append("1 ");   // put files before subfolders

        fileName = input[i].getName(); // get the file name without the path
        sortKey.append(fileName.toLowerCase()); // start by ignoring case
        sortKey.append(" ");      // separate lowercase from original case
        sortKey.append(fileName); // then sort file name on original case
        list.put(sortKey.toString(), input[i]); // put file into sorted list
      }

      /* Second, now that the TreeMap object has done all the hard work of
      sorting, pull the File objects from the list in order as determined by
      the sort keys that we created. */

      result = (File[]) list.values().toArray(new File[0]);
    }
    return(result);               // give caller whatever we could find

  } // end of sortFileList() method


/*
  userButton() method

  This method is called by our action listener actionPerformed() to process
  buttons, in the context of the main RedatePhotoFile3 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == cancelButton)   // "Cancel" button
    {
      doCancelButton();           // stop opening files or folders
    }
    else if (source == debugCheckbox) // if we show debug information
    {
      debugFlag = debugCheckbox.isSelected();
    }
    else if (source == exitButton) // "Exit" button
    {
      System.exit(0);             // always exit with zero status from GUI
    }
    else if (source == fontNameDialog) // font name for output text area
    {
      /* We can safely assume that the font name is valid, because we obtained
      the names from getAvailableFontFamilyNames(), and the user can't edit
      this dialog field. */

      fontName = (String) fontNameDialog.getSelectedItem();
      outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    }
    else if (source == fontSizeDialog) // point size for output text area
    {
      /* We can safely parse the point size as an integer, because we supply
      the only choices allowed, and the user can't edit this dialog field. */

      fontSize = Integer.parseInt((String) fontSizeDialog.getSelectedItem());
      outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    }
    else if (source == newDateCheckbox) // if we find newest (modified) dates
    {
      oldDateFlag = ! newDateCheckbox.isSelected();
      oldDateCheckbox.setSelected(oldDateFlag); // fake radio button
    }
    else if (source == oldDateCheckbox) // if we find oldest (original) dates
    {
      oldDateFlag = oldDateCheckbox.isSelected();
      newDateCheckbox.setSelected(! oldDateFlag); // fake radio button
    }
    else if (source == openButton) // "Open" button for files or folders
    {
      doOpenButton();             // open files or folders for processing
    }
    else if (source == recurseCheckbox) // if we search folders and subfolders
    {
      recurseFlag = recurseCheckbox.isSelected();
    }
    else if (source == redateCheckbox) // if we change file modification date
    {
      redateFlag = redateCheckbox.isSelected();
    }
    else if (source == renameCheckbox) // if we rename file with date prefix
    {
      renameFlag = renameCheckbox.isSelected();
    }
    else if (source == renameFormatButton) // "Format" button for renaming
    {
      doFormatButton();           // edit and test new date, time format
    }
    else if (source == renameTestButton) // "Test" button while editing format
    {
      doFormatTest();             // attempt to format with current pattern
    }
    else if (source == saveButton) // "Save Output" button
    {
      doSaveButton();             // write output text area to a file
    }
    else if (source == showDialog) // which files or conditions to report
    {
      showIndex = showDialog.getSelectedIndex(); // no meaning assigned here
    }
    else if (source == statusTimer) // update timer for status message text
    {
      if (statusPending.equals(statusDialog.getText()) == false)
        statusDialog.setText(statusPending); // new status, update the display
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method

} // end of RedatePhotoFile3 class

// ------------------------------------------------------------------------- //

/*
  RedatePhotoFile3User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class RedatePhotoFile3User implements ActionListener, Runnable
{
  /* empty constructor */

  public RedatePhotoFile3User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    RedatePhotoFile3.userButton(event);
  }

  /* separate heavy-duty processing thread */

  public void run()
  {
    RedatePhotoFile3.doOpenRunner();
  }

} // end of RedatePhotoFile3User class

/* Copyright (c) 2017 by Keith Fenske.  Apache License or GNU GPL. */
