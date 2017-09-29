package com.myopicmobile.textwarrior;

import com.myopicmobile.textwarrior.common.Language;

public class LanguageSmali extends Language {
    private final static String[] keywords = {/*"void", "strictfp",
    "new", "class", "interface", "extends", "implements", "enum",
	"public", "private", "protected", "static", "abstract", "final", "native", "volatile",
	"assert", "try", "throw", "throws", "catch", "finally", "instanceof", "super", "this",
	"continue", "break", "return", "synchronized", "transient",
	"true", "false", "null",
	"source", "synthetic","field","method"
	*/};
    private final static char[] operators = {'(', ')', '{', '}', ',', ';', ':',/*'-','<','>',*/'['};
    private static Language _theOne = null;
    private boolean wstart;

    private boolean keyStaus;

    private boolean pointer;

    private boolean lineStart;

    private LanguageSmali() {
        super.setKeywords(keywords);
        super.setOperators(operators);
    }

    public static Language getInstance() {
        if (_theOne == null) {
            _theOne = new LanguageSmali();
        }
        return _theOne;
    }

    @Override
    public boolean isLineStart(char c0, char c1) {
        // TODO: Implement this method
        return (c0 == '#' && c1 == ' ') || (c0 == ' ' && c1 == '.');
    }

    @Override
    public boolean isWordStart(char c) {
        return wstart;
    }

    @Override
    public boolean isKeyString() {
        if (lineStart) {
            lineStart = keyStaus = false;
            return true;
        }
        return keyStaus;
    }

    @Override
    public boolean isWhitespace(char c) {
        if (c == '\n') lineStart = true;
        else if ((!lineStart && c == '/') || pointer) return true;
        wstart = (c == ':' || c == '(');
        keyStaus = c == ';';
        return super.isWhitespace(c);
    }

    @Override
    public boolean isMultilineStartDelimiter(char c0, char c1) {
        pointer = '-' == c0 && '>' == c1;
        return false;
    }
}
