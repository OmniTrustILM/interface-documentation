package com.otilm.openapi.diagnostics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Improves Spring Boot's default "Ambiguous mapping" startup error by explicitly naming both conflicting handler
 * methods and suggesting a concrete fix.
 *
 * <p>
 * Spring's built-in message reads:
 *
 * <pre>
 *   Cannot map 'beanName' method Controller#create(...) to {POST [/v1/foo] ...}:
 *   There is already 'beanName' bean method Controller#list(...) mapped.
 * </pre>
 *
 * This analyzer reformats it as a clear two-entry conflict report.
 */
public class AmbiguousMappingFailureAnalyzer extends AbstractFailureAnalyzer<BeanCreationException> {
    private static final Pattern AMBIGUOUS_PATTERN = Pattern
            .compile("Ambiguous mapping\\. Cannot map '([^']+)' method\\s+(\\S+)\\s+to (\\{[^}]+\\}): "
                    + "There is already '[^']+' bean method\\s+(\\S+)\\s+mapped\\.", Pattern.DOTALL);

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, BeanCreationException cause) {
        String message = cause.getMessage();
        if (message == null || !message.contains("Ambiguous mapping")) {
            return null;
        }

        Matcher matcher = AMBIGUOUS_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }

        String description = formatAmbiguousMappingDetails(matcher);
        String action = "Give one of the conflicting methods a distinct path in its @PostMapping/@GetMapping annotation.";
        return new FailureAnalysis(description, action, cause);
    }

    private static @NonNull String formatAmbiguousMappingDetails(Matcher matcher) {
        String beanName = matcher.group(1);
        String incomingMethod = matcher.group(2);
        String mapping = matcher.group(3);
        String existingMethod = matcher.group(4);

        return String
                .format("Ambiguous request mapping on bean '%s'.%n" + "Both methods resolve to the same mapping %s:%n%n"
                        + "  [1] %s%n" + "  [2] %s", beanName, mapping, existingMethod, incomingMethod);
    }
}
