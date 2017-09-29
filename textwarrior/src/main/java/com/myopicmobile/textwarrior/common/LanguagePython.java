/*
 * Copyright (c) 2013 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.common;

/**
 * Singleton class containing the symbols and operators of the Python language
 */
public class LanguagePython extends Language {
    private final static String[] keywords = {
            "and", "assert", "break", "class", "continue", "def", "del",
            "elif", "else", "except", "exec", "finally", "for", "from",
            "global", "if", "import", "in", "is", "lambda", "not", "or",
            "pass", "print", "raise", "return", "try", "while", "with",
            "yield", "True", "False", "None"
    };
    private final static char[] operators = {
            '(', ')', '{', '}', '.', ',', ';', '=', '+', '-',
            '/', '*', '&', '!', '|', ':', '[', ']', '<', '>',
            '~', '%', '^'
    }; // no ternary operator ? :
    private static Language _theOne = null;


    private LanguagePython() {
        super.setKeywords(keywords);
        super.setOperators(operators);
    }

    public static Language getInstance() {
        if (_theOne == null) {
            _theOne = new LanguagePython();
        }
        return _theOne;
    }

    @Override
    public boolean isWordStart(char c) {
        return (c == '@');
    }

    @Override
    public boolean isLineAStart(char c) {
        return false;
    }

    @Override
    public boolean isLineBStart(char c) {
        return (c == '#');
    }

    @Override
    public boolean isLineStart(char c0, char c1) {
        return false;
    }

    @Override
    public boolean isMultilineStartDelimiter(char c0, char c1) {
        return false;
    }
}
