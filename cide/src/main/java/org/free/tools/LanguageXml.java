package org.free.tools;
/*
 * Copyright (c) 2013 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */

import com.myopicmobile.textwarrior.common.Language;
//import com.myopicmobile.textwarrior.common.LanguageJava;
//package com.myopicmobile.textwarrior.common;

/**
 * Singleton class containing the symbols and operators of the Java language
 */
class LanguageXml extends Language {
    private final static String[] keywords = {};
    private final static char[] operators = {':', '=', '<', '>', '/', '?'};
    private static Language _theOne = null;
    private boolean keyString;
    private boolean oldState;

    private LanguageXml() {
        super.setKeywords(keywords);
        super.setOperators(operators);
    }

    public static Language getInstance() {
        if (_theOne == null) {
            _theOne = new LanguageXml();
        }
        return _theOne;
    }

    @Override
    public boolean isKeyString() {
        if (oldState) keyString = false;
        return oldState;
    }

    @Override
    public boolean isWhitespace(char c) {
        boolean isWhitespace = super.isWhitespace(c);
        if (!isWhitespace) {
            if (!keyString) {
                keyString = (c == '<');
                oldState = false;
            } else if (c == '?') {
                keyString = false;
            } else oldState = true;
        } else keyString = false;
        return isWhitespace;
    }

    @Override
    public boolean isProgLang() {
        return false;
    }

    /**
     * Java has no preprocessors. Override base class implementation
     */
    public boolean isLineAStart(char c) {
        return false;
    }

    @Override
    public boolean isLineStart(char c0, char c1) {
        // TODO: Implement this method
        return false;
    }

    @Override
    public boolean isMultilineStartDelimiter(char c0, char c1) {
        // TODO: Implement this method
        return (c0 == '<' && c1 == '!');
    }

    @Override
    public boolean isMultilineEndDelimiter(char c0, char c1) {
        // TODO: Implement this method
        return (c0 == '-' && c1 == '>');
    }
}
