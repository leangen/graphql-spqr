package io.leangen.graphql.support;

import org.slf4j.LoggerFactory;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class TestLog implements AutoCloseable {

    private final Logger log;
    private final ListAppender<ILoggingEvent> list;

    public TestLog(Class<?> clazz) {
        LoggerContext logCtx = (LoggerContext) LoggerFactory.getILoggerFactory();
        log = logCtx.getLogger(clazz);
        list = new ListAppender<>();
        list.start();
        log.addAppender(list);
        log.setAdditive(false);
        log.setLevel(Level.WARN);
    }

    public List<ILoggingEvent> getEvents() {
        return list.list;
    }

    @Override
    public void close() {
        list.stop();
        log.detachAppender(list);
    }
}
