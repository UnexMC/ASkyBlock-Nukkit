package com.larryTheCoder.utils.json;

import java.util.HashMap;

/**
 * The XMLTokener extends the JSONTokener to provide additional methods for the parsing of XML texts.
 *
 * @author JSON.org
 * @version 2014-05-03
 */
class XMLTokener extends JSONTokener {
    /**
     * The table of entity values. It initially contains Character values for amp, apos, gt, lt, quot.
     */
    private static final HashMap<String, Character> entity;

    static {
        entity = new HashMap<>(8);
        entity.put("amp", XML.AMP);
        entity.put("apos", XML.APOS);
        entity.put("gt", XML.GT);
        entity.put("lt", XML.LT);
        entity.put("quot", XML.QUOT);
    }

    /**
     * Construct an XMLTokener from a string.
     *
     * @param s A source string.
     */
    public XMLTokener(final String s) {
        super(s);
    }

    /**
     * Get the text in the CDATA block.
     *
     * @return The string up to the <code>]]&gt;</code>.
     * @throws JSONException If the <code>]]&gt;</code> is not found.
     */
    public String nextCDATA() throws JSONException {
        final StringBuilder sb = new StringBuilder();
        for (; ; ) {
            char c = next();
            if (end()) {
                throw syntaxError("Unclosed CDATA");
            }
            sb.append(c);
            int i = sb.length() - 3;
            if ((i >= 0) && (sb.charAt(i) == ']') && (sb.charAt(i + 1) == ']') && (sb.charAt(i + 2) == '>')) {
                sb.setLength(i);
                return sb.toString();
            }
        }
    }

    /**
     * Get the next XML outer token, trimming whitespace. There are two kinds of tokens: the '&lt;' character which begins
     * a markup tag, and the content text between markup tags.
     *
     * @return A string, or a '&lt;' Character, or null if there is no more source text.
     * @throws JSONException
     */
    public Object nextContent() throws JSONException {
        char c;
        do {
            c = next();
        } while (Character.isWhitespace(c));
        if (c == 0) {
            return null;
        }
        if (c == '<') {
            return XML.LT;
        }
        StringBuilder sb = new StringBuilder();
        for (; ; ) {
            if ((c == '<') || (c == 0)) {
                back();
                return sb.toString().trim();
            }
            if (c == '&') {
                sb.append(nextEntity('&'));
            } else {
                sb.append(c);
            }
            c = next();
        }
    }

    /**
     * Return the next entity. These entities are translated to Characters: <code>&amp; &quot; &gt;  &lt;
     * &quot;</code>.
     *
     * @param ampersand An ampersand character.
     * @return A Character or an entity String if the entity is not recognized.
     * @throws JSONException If missing ';' in XML entity.
     */
    private Object nextEntity(final char ampersand) throws JSONException {
        final StringBuilder sb = new StringBuilder();
        for (; ; ) {
            final char c = next();
            if (Character.isLetterOrDigit(c) || (c == '#')) {
                sb.append(Character.toLowerCase(c));
            } else if (c == ';') {
                break;
            } else {
                throw syntaxError("Missing ';' in XML entity: &" + sb);
            }
        }
        final String string = sb.toString();
        final Object object = entity.get(string);
        return object != null ? object : ampersand + string + ';';
    }

    /**
     * Returns the next XML meta token. This is used for skipping over <!...> and <?...?> structures.
     *
     * @return Syntax characters (<code>&lt; &gt; / = ! ?</code>) are returned as Character, and strings and names are
     * returned as Boolean. We don't care what the values actually are.
     * @throws JSONException If a string is not properly closed or if the XML is badly structured.
     */
    public Object nextMeta() throws JSONException {
        char c;
        do {
            c = next();
        } while (Character.isWhitespace(c));
        char q;
        switch (c) {
            case 0:
                throw syntaxError("Misshaped meta tag");
            case '<':
                return XML.LT;
            case '>':
                return XML.GT;
            case '/':
                return XML.SLASH;
            case '=':
                return XML.EQ;
            case '!':
                return XML.BANG;
            case '?':
                return XML.QUEST;
            case '"':
            case '\'':
                q = c;
                for (; ; ) {
                    c = next();
                    if (c == 0) {
                        throw syntaxError("Unterminated string");
                    }
                    if (c == q) {
                        return Boolean.TRUE;
                    }
                }
            default:
                for (; ; ) {
                    c = next();
                    if (Character.isWhitespace(c)) {
                        return Boolean.TRUE;
                    }
                    switch (c) {
                        case 0:
                        case '<':
                        case '>':
                        case '/':
                        case '=':
                        case '!':
                        case '?':
                        case '"':
                        case '\'':
                            back();
                            return Boolean.TRUE;
                    }
                }
        }
    }

    /**
     * Get the next XML Token. These tokens are found inside of angle brackets. It may be one of these characters:
     * <code>/ &gt;= ! ?</code> or it may be a string wrapped in single quotes or double quotes, or it may be a name.
     *
     * @return a String or a Character.
     * @throws JSONException If the XML is not well formed.
     */
    public Object nextToken() throws JSONException {
        char c;
        do {
            c = next();
        } while (Character.isWhitespace(c));
        char q;
        StringBuilder sb;
        switch (c) {
            case 0:
                throw syntaxError("Misshaped element");
            case '<':
                throw syntaxError("Misplaced '<'");
            case '>':
                return XML.GT;
            case '/':
                return XML.SLASH;
            case '=':
                return XML.EQ;
            case '!':
                return XML.BANG;
            case '?':
                return XML.QUEST;
            // Quoted string
            case '"':
            case '\'':
                q = c;
                sb = new StringBuilder();
                for (; ; ) {
                    c = next();
                    if (c == 0) {
                        throw syntaxError("Unterminated string");
                    }
                    if (c == q) {
                        return sb.toString();
                    }
                    if (c == '&') {
                        sb.append(nextEntity('&'));
                    } else {
                        sb.append(c);
                    }
                }
            default:
                // Name
                sb = new StringBuilder();
                for (; ; ) {
                    sb.append(c);
                    c = next();
                    if (Character.isWhitespace(c)) {
                        return sb.toString();
                    }
                    switch (c) {
                        case 0:
                            return sb.toString();
                        case '>':
                        case '/':
                        case '=':
                        case '!':
                        case '?':
                        case '[':
                        case ']':
                            back();
                            return sb.toString();
                        case '<':
                        case '"':
                        case '\'':
                            throw syntaxError("Bad character in a name");
                    }
                }
        }
    }

    /**
     * Skip characters until past the requested string. If it is not found, we are left at the end of the source with a
     * result of false.
     *
     * @param to A string to skip past.
     * @throws JSONException
     */
    public boolean skipPast(final String to) throws JSONException {
        char c;
        int i;
        final int length = to.length();
        final char[] circle = new char[length];
        /*
         * First fill the circle buffer with as many characters as are in the
         * to string. If we reach an early end, bail.
         */
        for (i = 0; i < length; i += 1) {
            c = next();
            if (c == 0) {
                return false;
            }
            circle[i] = c;
        }
        /* We will loop, possibly for all of the remaining characters. */
        for (int offset = 0; ; ) {
            int j = offset;
            boolean b = true;
            /* Compare the circle buffer with the to string. */
            for (i = 0; i < length; i += 1) {
                if (circle[j] != to.charAt(i)) {
                    b = false;
                    break;
                }
                j += 1;
                if (j >= length) {
                    j -= length;
                }
            }
            /* If we exit the loop with b intact, then victory is ours. */
            if (b) {
                return true;
            }
            /* Get the next character. If there isn't one, then defeat is ours. */
            c = next();
            if (c == 0) {
                return false;
            }
            /*
             * Shove the character in the circle buffer and advance the
             * circle offset. The offset is mod n.
             */
            circle[offset] = c;
            offset += 1;
            if (offset >= length) {
                offset -= length;
            }
        }
    }
}
