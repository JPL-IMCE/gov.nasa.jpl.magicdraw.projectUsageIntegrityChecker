package gov.nasa.jpl.magicdraw.appenders;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.annotation.Nonnull;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Appender;
import org.apache.log4j.spi.LoggingEvent;

/**
 * @see http://sysgears.com/articles/how-to-redirect-stdout-and-stderr-writing-to-a-log4j-appender/
 * @author Dmitriy
 *
 * Modified to handle a Log4J Appender
 * @author Nicolas F Rouquette (JPL)
 */
public class AppendingOutputStream extends OutputStream {

    /**
     * Default number of bytes in the buffer.
     */
    private static final int DEFAULT_BUFFER_LENGTH = 2048;

    /**
     * Indicates stream state.
     */
    private boolean hasBeenClosed = false;

    /**
     * Internal buffer where data is stored.
     */
    private byte[] buf;

    /**
     * Number of valid bytes in the buffer.
     */
    private int count;

    /**
     * Remembers the size of the buffer.
     */
    private int curBufLength;

    private PrintStream stream;
    
    /**
     * The appender to write to.
     */
    private Appender appender;
    
    private Category category;
    
    /**
     * The log level.
     */
    private Level level;

    /**
     * Creates the Logging instance to flush to the given logger.
     * @param err 
     *
     * @param appender    the Appender to write to
     * @param level       the log level
     * @throws IllegalArgumentException in case if one of arguments
     *                                  is  null.
     */
    public AppendingOutputStream(
    		final @Nonnull PrintStream stream, 
    		final @Nonnull Appender appender,
    		final @Nonnull Category cat,
    		final Level level)
    				throws IllegalArgumentException {
        if (stream == null || appender == null || level == null) {
            throw new IllegalArgumentException(
                    "PrintStream or Appender or log level must be not null");
        }
        this.stream = stream;
        this.appender = appender;
        this.category = cat;
        this.level = level;
        curBufLength = DEFAULT_BUFFER_LENGTH;
        buf = new byte[curBufLength];
        count = 0;
    }
    
    /**
     * Writes the specified byte to this output stream.
     *
     * @param b the byte to write
     * @throws IOException if an I/O error occurs.
     */
    public void write(final int b) throws IOException {
        if (hasBeenClosed) {
            throw new IOException("The stream has been closed.");
        }
        // don't log nulls
        if (b == 0) {
            return;
        }
        
        if (b == '\n')
			flush();
        
        // would this be writing past the buffer?
        if (count == curBufLength) {
            // grow the buffer
            final int newBufLength = curBufLength +
                    DEFAULT_BUFFER_LENGTH;
            final byte[] newBuf = new byte[newBufLength];
            System.arraycopy(buf, 0, newBuf, 0, curBufLength);
            buf = newBuf;
            curBufLength = newBufLength;
        }

        buf[count] = (byte) b;
        count++;
    }

    /**
     * Flushes this output stream and forces any buffered output
     * bytes to be written out.
     */
    public void flush() {
        if (count == 0) {
            return;
        }
        final byte[] bytes = new byte[count];
        System.arraycopy(buf, 0, bytes, 0, count);
        String str = new String(bytes);
        Throwable t = null;
        appender.doAppend(new LoggingEvent(
        		"gov.nasa.jpl.magicdraw.appenders.AppendingOutputStream", 
        		category, level, str, t));
        stream.print(str);
        count = 0;
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with this stream.
     */
    public void close() {
        flush();
        hasBeenClosed = true;
    }
}
