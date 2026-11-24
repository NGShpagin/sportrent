package ru.saschoolsport.gateway.config;

/**
 * Single implementation of the "is this path public?" check, shared by the JWT filter
 * and the permission filter so they can never disagree.
 */
public final class PublicPathMatcher {

    private PublicPathMatcher() {
    }

    /**
     * @param exactPaths    paths that must match exactly
     * @param prefixPaths   path prefixes without trailing slash; matches the prefix itself
     *                      or anything below it ({@code /actuator}, {@code /actuator/health}),
     *                      but not look-alikes such as {@code /actuator-evil}
     */
    public static boolean isPublic(String path, String[] exactPaths, String[] prefixPaths) {
        for (String exact : exactPaths) {
            if (path.equals(exact)) {
                return true;
            }
        }
        for (String prefix : prefixPaths) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }
}
