package io.leangen.graphql.support;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

public class TestLog implements AutoCloseable {

    private final Logger log;
    private final ListAppender<ILoggingEvent> list;

    public TestLog(Class<?> clazz) {
        this(logCtx -> logCtx.getLogger(clazz));
    }

    private TestLog(Function<LoggerContext, Logger> logProvider) {
        LoggerContext logCtx = (LoggerContext) LoggerFactory.getILoggerFactory();
        log = logProvider.apply(logCtx);
        list = new ListAppender<>();
        list.start();
        log.addAppender(list);
        log.setAdditive(false);
        log.setLevel(Level.WARN);
    }

    public static TestLog unsafe(Class<?> clazz) {
        return new TestLog(logCtx -> logCtx.getLogger("notprivacysafe." + clazz.getName()));
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
