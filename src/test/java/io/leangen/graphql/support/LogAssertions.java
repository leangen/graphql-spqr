package io.leangen.graphql.support;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;

public class LogAssertions {

    public static void assertWarningsLogged(List<ILoggingEvent> logEvents, String... messages) {
        assertEquals(messages.length, logEvents.size());
        for (int i = 0; i < messages.length; i++) {
            ILoggingEvent event = logEvents.get(i);
            assertThat(event.getLevel(), is(Level.WARN));
            assertThat(event.getMessage(), containsString(messages[i]));
        }
    }
}
