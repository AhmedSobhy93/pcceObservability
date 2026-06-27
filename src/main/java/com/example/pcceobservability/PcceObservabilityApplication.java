package com.example.pcceobservability;

/**
 * Temporary compatibility launcher for existing IntelliJ run configurations.
 *
 * <p>The application was renamed to
 * {@code com.cisco.cx.observability.CxObservabilityApplication}. Update the
 * IntelliJ main class when convenient, then this shim can be removed.</p>
 */
@Deprecated(forRemoval = true)
public final class PcceObservabilityApplication {

    private PcceObservabilityApplication() {
    }

    public static void main(String[] args) {
        com.cisco.cx.observability.CxObservabilityApplication.main(args);
    }
}
