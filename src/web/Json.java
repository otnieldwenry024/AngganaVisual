package web;

import java.util.Map;
import java.util.stream.Collectors;

/** Serializer kecil untuk response, tanpa dependensi tambahan. Input memakai form URL-encoded. */
public final class Json {
    private Json() {}
    public static String encode(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?, ?>) {
            return ((Map<?, ?>) value).entrySet().stream()
                    .map(e -> encode(e.getKey().toString()) + ":" + encode(e.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        if (value instanceof Iterable<?>) {
            StringBuilder out = new StringBuilder("[");
            for (Object item : (Iterable<?>) value) {
                if (out.length() > 1) out.append(',');
                out.append(encode(item));
            }
            return out.append(']').toString();
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default: if (c < 32) out.append(String.format("\\u%04x", (int)c)); else out.append(c);
            }
        }
        return out.append('"').toString();
    }
}
