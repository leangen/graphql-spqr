package io.leangen.graphql.support;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class LogAssertions {

    public static void assertWarningsLogged(List<ILoggingEvent> logEvents, String... messages) {
        assertEquals(messages.length, logEvents.size());
        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getLevel(), is(Level.WARN));
        for (String message : messages) {
            assertThat(event.getMessage(), containsString(message));
        }
    }
}
