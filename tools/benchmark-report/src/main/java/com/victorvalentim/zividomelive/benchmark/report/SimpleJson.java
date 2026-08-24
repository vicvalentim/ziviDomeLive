package com.victorvalentim.zividomelive.benchmark.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Minimal JSON reader/writer used to keep benchmark reporting offline and dependency-free. */
final class SimpleJson {
    private SimpleJson() {
    }

    static Object parse(String source) {
        Parser parser = new Parser(source);
        Object value = parser.value();
        parser.whitespace();
        if (!parser.end()) {
            throw parser.error("Unexpected trailing content");
        }
        return value;
    }

    static String write(Object value) {
        StringBuilder output = new StringBuilder();
        append(output, value, 0);
        output.append('\n');
        return output.toString();
    }

    private static void append(StringBuilder output, Object value, int depth) {
        if (value == null) {
            output.append("null");
        } else if (value instanceof String) {
            string(output, (String) value);
        } else if (value instanceof Boolean) {
            output.append(value);
        } else if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            if (!Double.isFinite(number)) {
                throw new IllegalArgumentException("JSON numbers must be finite");
            }
            output.append(value);
        } else if (value instanceof Map) {
            output.append("{\n");
            int index = 0;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                indent(output, depth + 1);
                string(output, String.valueOf(entry.getKey()));
                output.append(": ");
                append(output, entry.getValue(), depth + 1);
                output.append(++index < ((Map<?, ?>) value).size() ? ",\n" : "\n");
            }
            indent(output, depth).append('}');
        } else if (value instanceof Iterable) {
            output.append('[');
            int index = 0;
            for (Object item : (Iterable<?>) value) {
                if (index++ > 0) output.append(", ");
                append(output, item, depth + 1);
            }
            output.append(']');
        } else {
            throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
        }
    }

    private static void string(StringBuilder output, String value) {
        output.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"': output.append("\\\""); break;
                case '\\': output.append("\\\\"); break;
                case '\b': output.append("\\b"); break;
                case '\f': output.append("\\f"); break;
                case '\n': output.append("\\n"); break;
                case '\r': output.append("\\r"); break;
                case '\t': output.append("\\t"); break;
                default:
                    if (character < 0x20) {
                        output.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        output.append(character);
                    }
            }
        }
        output.append('"');
    }

    private static StringBuilder indent(StringBuilder output, int depth) {
        return output.append("  ".repeat(Math.max(0, depth)));
    }

    private static final class Parser {
        private final String source;
        private int position;

        private Parser(String source) {
            this.source = source == null ? "" : source;
        }

        private Object value() {
            whitespace();
            if (end()) throw error("Expected a JSON value");
            char current = source.charAt(position);
            if (current == '{') return object();
            if (current == '[') return array();
            if (current == '"') return string();
            if (current == 't') return literal("true", Boolean.TRUE);
            if (current == 'f') return literal("false", Boolean.FALSE);
            if (current == 'n') return literal("null", null);
            if (current == '-' || Character.isDigit(current)) return number();
            throw error("Unexpected character '" + current + "'");
        }

        private Map<String, Object> object() {
            position++;
            Map<String, Object> object = new LinkedHashMap<>();
            whitespace();
            if (take('}')) return object;
            while (true) {
                whitespace();
                if (end() || source.charAt(position) != '"') throw error("Expected object key");
                String key = string();
                whitespace();
                expect(':');
                if (object.containsKey(key)) throw error("Duplicate key '" + key + "'");
                object.put(key, value());
                whitespace();
                if (take('}')) return object;
                expect(',');
            }
        }

        private List<Object> array() {
            position++;
            List<Object> array = new ArrayList<>();
            whitespace();
            if (take(']')) return array;
            while (true) {
                array.add(value());
                whitespace();
                if (take(']')) return array;
                expect(',');
            }
        }

        private String string() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (!end()) {
                char character = source.charAt(position++);
                if (character == '"') return value.toString();
                if (character == '\\') {
                    if (end()) throw error("Unterminated escape sequence");
                    char escaped = source.charAt(position++);
                    switch (escaped) {
                        case '"': value.append('"'); break;
                        case '\\': value.append('\\'); break;
                        case '/': value.append('/'); break;
                        case 'b': value.append('\b'); break;
                        case 'f': value.append('\f'); break;
                        case 'n': value.append('\n'); break;
                        case 'r': value.append('\r'); break;
                        case 't': value.append('\t'); break;
                        case 'u': value.append(unicode()); break;
                        default: throw error("Invalid escape sequence");
                    }
                } else {
                    if (character < 0x20) throw error("Control character in string");
                    value.append(character);
                }
            }
            throw error("Unterminated string");
        }

        private char unicode() {
            if (position + 4 > source.length()) throw error("Incomplete Unicode escape");
            try {
                char value = (char) Integer.parseInt(source.substring(position, position + 4), 16);
                position += 4;
                return value;
            } catch (NumberFormatException exception) {
                throw error("Invalid Unicode escape");
            }
        }

        private Number number() {
            int start = position;
            if (take('-') && end()) throw error("Incomplete number");
            if (take('0')) {
                if (!end() && Character.isDigit(source.charAt(position))) throw error("Leading zero");
            } else {
                digits();
            }
            if (take('.')) digits();
            if (!end() && (source.charAt(position) == 'e' || source.charAt(position) == 'E')) {
                position++;
                if (!end() && (source.charAt(position) == '+' || source.charAt(position) == '-')) position++;
                digits();
            }
            String text = source.substring(start, position);
            try {
                if (!text.contains(".") && !text.contains("e") && !text.contains("E")) {
                    return Long.parseLong(text);
                }
                double value = Double.parseDouble(text);
                if (!Double.isFinite(value)) throw error("Non-finite number");
                return value;
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        private void digits() {
            int start = position;
            while (!end() && Character.isDigit(source.charAt(position))) position++;
            if (start == position) throw error("Expected digit");
        }

        private Object literal(String literal, Object value) {
            if (!source.startsWith(literal, position)) throw error("Invalid literal");
            position += literal.length();
            return value;
        }

        private void whitespace() {
            while (!end() && Character.isWhitespace(source.charAt(position))) position++;
        }

        private boolean take(char expected) {
            if (!end() && source.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) throw error("Expected '" + expected + "'");
        }

        private boolean end() {
            return position >= source.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + position);
        }
    }
}
