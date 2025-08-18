package org.yanix.yx_samp_core.util;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class GroupParsing {
    private GroupParsing(){}

    // Шаблон: <type>.<name>.<dept>.rank.<01-10>
    private static final String EMPLOYMENT_REGEX =
            "^(%s)\\.([a-z0-9_]+)\\.([a-z0-9_]+)\\.rank\\.(?:0?([1-9])|10)$";

    public static Pattern compileEmployment(String type) {
        String t = normalizeType(type);
        return Pattern.compile(EMPLOYMENT_REGEX.formatted(Pattern.quote(t)), Pattern.CASE_INSENSITIVE);
    }

    public static Pattern compileEmployment(String type, String dept) {
        String t = normalizeType(type);
        String d = dept.toLowerCase(Locale.ROOT);
        String regex = "^(%s)\\.([a-z0-9_]+)\\.(%s)\\.rank\\.(?:0?([1-9])|10)$"
                .formatted(Pattern.quote(t), Pattern.quote(d));
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    public static Optional<Integer> parseRank(String groupName) {
        var m = Pattern.compile("\\.rank\\.(?:0?([1-9])|10)$", Pattern.CASE_INSENSITIVE)
                .matcher(groupName);
        if (!m.find()) return Optional.empty();
        String g1 = m.group(1);
        if (g1 != null) return Optional.of(Integer.parseInt(g1));
        if (groupName.toLowerCase(Locale.ROOT).endsWith(".rank.10")) return Optional.of(10);
        return Optional.empty();
    }

    /** Возвращает <name> из {type}.{name}.{dept}.rank.N */
    public static Optional<String> extractEmploymentName(String groupName) {
        var m = Pattern.compile("^[a-z]+\\.([a-z0-9_]+)\\.[a-z0-9_]+\\.rank\\.", Pattern.CASE_INSENSITIVE)
                .matcher(groupName);
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    public static String normalizeType(String type) {
        String t = type.toLowerCase(Locale.ROOT);
        return switch (t) {
            case "org", "organization", "organisation" -> "org";
            case "job" -> "job";
            default -> t;
        };
    }
}

