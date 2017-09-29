package org.free.tools;

import android.content.Context;
import android.preference.PreferenceManager;
import android.webkit.MimeTypeMap;

import com.myopicmobile.textwarrior.common.Language;
import com.myopicmobile.textwarrior.common.LanguageC;
import com.myopicmobile.textwarrior.common.LanguageCpp;
import com.myopicmobile.textwarrior.common.LanguageCsharp;
import com.myopicmobile.textwarrior.common.LanguageJava;
import com.myopicmobile.textwarrior.common.LanguageJavascript;
import com.myopicmobile.textwarrior.common.LanguageNonProg;
import com.myopicmobile.textwarrior.common.LanguageObjectiveC;

import java.io.File;

public class DocumentState {
    public static Language getLanguage(File result, Context context) {
        //if(result.isFile()&&(result.length()<Integer.MAX_VALUE)){
        if (result.getPath().equals("New")) {
            if (context != null) {
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("NewLanguageCpp", false)) {
                    return LanguageCpp.getInstance();
                } else
                    return LanguageC.getInstance();
            }
        }
        String name = result.toURI().toASCIIString();
        name = MimeTypeMap.getFileExtensionFromUrl(name);
        if (name != null)
            switch (name.toLowerCase()) {
                case "h":
                case "inc":
                case "c":
                    return LanguageC.getInstance();
                case "cc":
                case "hpp":
                case "hxx":
                case "cpp":
                case "cxx":
                    return LanguageCpp.getInstance();
                case "css":
                    return LanguageCsharp.getInstance();
                case "java":
                case "class":
                    return LanguageJava.getInstance();
                case "smali":
                    return LanguageSmali.getInstance();
                case "js":
                    return LanguageJavascript.getInstance();
                case "xml":
                case "html":
                    return LanguageXml.getInstance();
                case ".m":
                case "mm":
                    return LanguageObjectiveC.getInstance();
                //case "txt":
            }
        //}
        return LanguageNonProg.getInstance();
    }

    public static boolean getLanguageCpp(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("NewLanguageCpp", false);
    }
}
