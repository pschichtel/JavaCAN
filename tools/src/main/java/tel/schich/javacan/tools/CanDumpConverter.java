/*
 * The MIT License
 * Copyright © 2018 Phillip Schichtel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package tel.schich.javacan.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a human-readable CAN dump into a {@code canplayer} replayable format.
 * Such a dump may have be created from the output of {@code candump} with the parameter {@code -t A}
 * <p>
 * The input lines are transformed from<br>
 * {@code " (2019-12-11 17:26:46.545849)  can0  301   [8]  08 4C 01 4E 02 C2 0C 5C"}<br>
 * to<br>
 * {@code "(1576495052.545849) can0 301#084C014E02C20C5C"}
 * </p>
 * The resulting dump can be replayed with the {@code canplayer} from the {@code can-utils} package.
 *
 * @author Maik Scheibler
 */
public class CanDumpConverter {

    private static final Pattern LINEPATTERN = Pattern.compile(
            " \\((\\d+-\\d+-\\d+ \\d+:\\d+:\\d+)(\\.\\d+)\\)\\s+(\\w+)\\s+(\\w+)\\s+\\[\\d]\\s+(.+)");
    private static final DateFormat DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws IOException, ParseException {
        if (args.length < 2) {
            System.out.println("usage: CANDumpConverter <srcFile> <dstFile>");
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(args[0]))) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processLine(line, writer);
                }
            }
        }
    }

    private static void processLine(String line, BufferedWriter writer) throws ParseException, IOException {
        Matcher matcher = LINEPATTERN.matcher(line);
        if (!matcher.matches()) {
            System.out.println("ignoring line: " + line);
            return;
        }
        String builder = "(" + DATEFORMAT.parse(matcher.group(1)).getTime() / 1000 + // UNIX timestamp seconds
                matcher.group(2) + ") " + // microseconds
                matcher.group(3) + ' ' + // CAN interface
                matcher.group(4) + '#' + // CAN ID
                matcher.group(5).replaceAll("\\s", ""); // data bytes
        writer.write(builder);
        writer.newLine();
    }
}
