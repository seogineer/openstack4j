package org.openstack4j.model.heat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** The query options of {@code POST /validate}. */
public final class TemplateValidateOptions {

    private boolean showNested;
    private final List<String> ignoreErrors = new ArrayList<>();

    private TemplateValidateOptions() {
    }

    public static TemplateValidateOptions create() {
        return new TemplateValidateOptions();
    }

    /** show_nested=true: also validate nested stacks and return their parameters. */
    public TemplateValidateOptions showNested(boolean showNested) {
        this.showNested = showNested;
        return this;
    }

    /** ignore_errors: error codes to ignore, e.g. "99001" (sent comma-separated). */
    public TemplateValidateOptions ignoreErrors(String... errorCodes) {
        for (String code : Objects.requireNonNull(errorCodes, "errorCodes"))
            ignoreErrors.add(Objects.requireNonNull(code, "errorCode"));
        return this;
    }

    public boolean isShowNested() {
        return showNested;
    }

    /** @return the error codes to ignore, unmodifiable and empty by default */
    public List<String> getIgnoreErrors() {
        return Collections.unmodifiableList(ignoreErrors);
    }
}
