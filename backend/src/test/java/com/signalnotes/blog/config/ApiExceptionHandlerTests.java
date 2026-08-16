package com.signalnotes.blog.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTests {
    @Test
    void unexpectedExceptionsAreLoggedWithoutExposingDetailsToTheClient() {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            RuntimeException failure = new RuntimeException("database query failed");
            var response = new ApiExceptionHandler().unexpected(failure);

            assertThat(response.getBody()).containsEntry("message", "服务器暂时无法处理请求");
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getFormattedMessage()).contains("Unhandled API exception");
                assertThat(event.getThrowableProxy()).isNotNull();
                assertThat(event.getThrowableProxy().getMessage()).isEqualTo("database query failed");
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
