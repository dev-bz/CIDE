#include <jni.h>
#include <malloc.h>
#include <string.h>
#include <stdlib.h>

#ifdef __ANDROID__

#include <android/log.h>

#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "index", __VA_ARGS__))
#endif

#include <clang-c/Index.h>

#ifndef CLANG_C_INDEX_H
#define CLANG_C_INDEX_H
#endif // CLANG_C_INDEX_H
static const char **opt = 0;
static unsigned optcount = 0;
static jsize oldlen = 0;
// static int mode = 0;
// static char linestr[512]={0};
/**#undef JNIEXPORT
#undef JNICALL
#define JNIEXPORT
#define JNICALL*/
#define SF(n) JNICALL Java_org_free_clangide_ClangAPI_##n
#ifdef __cplusplus
#define cENV env
#define pENV
#ifdef JNIEXPORT
#undef JNIEXPORT
#define JNIEXPORT extern "C"
#endif
#else
#define cENV (*env)
#define pENV env,
#endif
#ifdef JNICALL
#undef JNICALL
#define JNICALL
#endif // JNICALL
#ifdef CLANG_C_INDEX_H
static CXIndex ix = 0;
static CXTranslationUnit tu = 0;
static struct CXUnsavedFile *unsavedAll = 0;
static char mainFileName[256] = {0}, currentFile[256] = {0}, rootStr[256] = {0};
static unsigned unsavedcount = 0;
static char *data = 0;
static CXSourceLocation location;
/**struct javaEnv {
  JNIEnv *env;
  jclass cls;
  jmethodID m;
} je;*/
static char *buffer = 0;
static int buffers = 1;
static char *pos;
static int bufferz = 0;
static int reparse = 0;
static char ver[] = "6.1.0-";
#endif // CLANG_C_INDEX_H

JNIEXPORT void SF(putenv)(JNIEnv *env, jclass cls, jstring file) {
    jboolean iscopy;
    int flen = cENV->GetStringUTFLength(pENV file);
    const char *bak = cENV->GetStringUTFChars(pENV file, &iscopy);
    char tmp[strlen(bak) + 1];
    strcpy(tmp, bak);
    putenv(tmp);
    cENV->ReleaseStringUTFChars(pENV file, bak);
}

JNIEXPORT void SF(updateOption)(JNIEnv *env, jclass cls, jobjectArray obj) {
    //  printf("%s %i\n", __FUNCTION__, __LINE__);
    if (buffer == 0)
        buffer = (char *) malloc(1);
    jsize len = 0;
    unsigned changed = 0;
    if (obj) {
        len = cENV->GetArrayLength(pENV obj);
        changed = optcount != len;
        optcount = len;
    }
    jsize optlen = 0;
    for (jsize i = 0; i < len; ++i) {
        jstring ent = (jstring) cENV->GetObjectArrayElement(pENV obj, i);
        unsigned slen = cENV->GetStringUTFLength(pENV ent);
        if (!changed) {
            jboolean iscopy;
            const char *cchars = cENV->GetStringUTFChars(pENV (jstring) ent, &iscopy);
            changed = strcasecmp(opt[i], cchars);
            cENV->ReleaseStringUTFChars(pENV ent, cchars);
        }
        cENV->DeleteLocalRef(pENV ent);
        optlen += slen + 1 + sizeof(const char *);
    }
    if (changed) {
        if (optlen > oldlen) {
            if (opt) {
                opt = (const char **) realloc(opt, optlen);
            } else {
                opt = (const char **) malloc(optlen);
                memset(opt, 0, optlen);
            }
            oldlen = optlen;
        }
        char *write = (char *) (opt + len);
        for (jsize i = 0; i < len; ++i) {
            jboolean iscopy;
            jstring ent = (jstring) cENV->GetObjectArrayElement(pENV obj, i);
            const char *cchars = cENV->GetStringUTFChars(pENV (jstring) ent, &iscopy);
            opt[i] = write;
            strcpy(write, cchars);
            write += (cENV->GetStringUTFLength(pENV ent) + 1);
            cENV->ReleaseStringUTFChars(pENV ent, cchars);
            cENV->DeleteLocalRef(pENV ent);
        }
        if (tu) {
            clang_disposeTranslationUnit(tu);
            tu = 0;
        }
    }
    /*if (unsavedcount) {
      if (ix == 0)
                    ix = clang_createIndex(0, 0);
      tu = clang_parseTranslationUnit(
                      ix, unsave->Filename, opt, optcount, unsavedAll,
    unsavedcount,
                      clang_defaultEditingTranslationUnitOptions());
      reparse = 0;
    }*/
    if (!getenv("CINDEXTEST_PREAMBLE_FILE")) {
#ifdef __ANDROID__
        // putenv("CINDEXTEST_PREAMBLE_FILE=/mnt/sdcard/preamble.pch");
        setenv("CINDEXTEST_PREAMBLE_FILE", "/sdcard/preamble.pch", 24);
#else
        //#ifndef __STRICT_ANSI__
        putenv("CINDEXTEST_PREAMBLE_FILE=./preamble.pch");
    //#error on pc
    //#else
    //    #error
    //    _putenv("CINDEXTEST_PREAMBLE_FILE=./preamble.pch");
    //#endif//__STRICT_ANSI__
#endif //__ANDROID__
    }
#ifdef __ANDROID__
    if (ver[5]) {
        FILE *f = fopen("/data/data/com.n0n3m4.droidc/files/gcc/plugin_version", "r");
        if (f) {
            fread(ver, 1, 5, f);
            fclose(f);
        }
        ver[5] = 0;
    }
    if (!getenv("CPATH")) {
        char d[] =
                //"CPATH="
                "/data/data/com.n0n3m4.droidc/files/gcc/lib/gcc/"
                        "arm-linux-androideabi/4.9.1/include:/data/data/com.n0n3m4.droidc/"
                        "files/gcc/lib/gcc/arm-linux-androideabi/4.9.1/include-fixed:/data/"
                        "data/com.n0n3m4.droidc/files/gcc/arm-linux-androideabi/include";
        if (strcmp(ver, "4.9.1")) {
            char *p = d;
            while ((p = strstr(p, "4.9.1"))) {
                p[0] = ver[0];
                p[2] = ver[2];
                p[4] = ver[4];
                ++p;
            }
        }
        // putenv(d);
        setenv("CPATH", d, strlen(d));
    }
    if (!getenv("CPLUS_INCLUDE_PATH")) {
        char d[] = //"CPLUS_INCLUDE_PATH="
                "/data/data/com.n0n3m4.droidc/files/gcc/"
                        "arm-linux-androideabi/include/c++/4.9.1:/data/data/"
                        "com.n0n3m4.droidc/files/gcc/arm-linux-androideabi/include/c++/"
                        "4.9.1/backward";
        if (strcmp(ver, "4.9.1")) {
            char *p = d;
            while ((p = strstr(p, "4.9.1"))) {
                p[0] = ver[0];
                p[2] = ver[2];
                p[4] = ver[4];
                ++p;
            }
        }
        // putenv(d);
        setenv("CPLUS_INCLUDE_PATH", d, strlen(d));
    }
#else
    if (!getenv("CPATH"))
      putenv("CPATH=D:\\Program Files (x86)\\Java\\jdk1.8.0_45\\include;"
             "D:\\Program Files (x86)\\Java\\jdk1.8.0_45\\include\\win32");
    if (!getenv("CPLUS_INCLUDE_PATH"))
      putenv("CPLUS_INCLUDE_PATH=D:\\Program Files\\LLVM\\include");
#endif
}
// int putenv(const char*set);
// int strcasecmp(const char*a, const char*b);
#ifndef __ANDROID__
//#define strcasecmp stricmp
#endif

JNIEXPORT void SF(updateFileCode)(JNIEnv *env, jclass cls, jstring file, jstring code, jboolean mainFile) {
//  printf("%s %i\n", __FUNCTION__, __LINE__);
#ifdef CLANG_C_INDEX_H
    if (file == 0) {
        if (code == 0) {
            if (tu) {
                clang_disposeTranslationUnit(tu);
                tu = 0;
            }
            if (ix) {
                clang_disposeIndex(ix);
                ix = 0;
            }
            // mode = 0;
            for (int i = 0; i < unsavedcount; ++i) {
                data = (char *) unsavedAll[i].Filename;
                if (data)
                    free(data), data = 0;
            }
            if (unsavedAll)
                free(unsavedAll), unsavedAll = 0;
            unsavedcount = 0;
        }
        return;
    }
    const char *bak;
    char *tmp;
    jboolean iscopy;
    bak = cENV->GetStringUTFChars(pENV file, &iscopy);
    strcpy(currentFile, bak);
    if (mainFile) {
        if (0 != strcasecmp(bak, mainFileName)) {
            if (tu) {
                clang_disposeTranslationUnit(tu);
                tu = 0;
            }
            strcpy(mainFileName, bak);
        }
    }
    if (code == 0) {
        cENV->ReleaseStringUTFChars(pENV file, bak);
        return;
    }
    unsigned i;
    static struct CXUnsavedFile *unsave = 0;
    for (i = 0; i < unsavedcount; ++i) {
        // printf("un: %s\n",unsavedAll[i].Filename);
        fflush(stdout);
        if (0 == strcasecmp(unsavedAll[i].Filename, bak)) {
            unsave = unsavedAll + i;
            break;
        }
    }
    if (unsavedcount == i && tu) {
        for (i = 0; i < unsavedcount; ++i) {
            if (NULL == clang_getFile(tu, unsavedAll[i].Filename)) {
                unsave = unsavedAll + i;
                break;
            }
        }
    }
    if (unsavedcount == i) {
        ++unsavedcount;
        if (unsavedAll)
            unsavedAll = (struct CXUnsavedFile *) realloc(unsavedAll, sizeof(struct CXUnsavedFile) * unsavedcount);
        else
            unsavedAll = (struct CXUnsavedFile *) malloc(sizeof(struct CXUnsavedFile) * unsavedcount);
        unsave = unsavedAll + i;
        unsave->Filename = 0;
    }
    {
        int flen = cENV->GetStringUTFLength(pENV file);
        unsave->Length = cENV->GetStringUTFLength(pENV code);
        data = (char *) unsave->Filename;
        if (data)
            data = (char *) realloc(data, flen + unsave->Length + 2);
        else
            data = (char *) malloc(flen + unsave->Length + 2);
        unsave->Filename = data;
        strcpy(data, bak /* = cENV->GetStringUTFChars(pENV file, &iscopy)*/);
        cENV->ReleaseStringUTFChars(pENV file, bak);
        data[flen] = 0;
        tmp = data + (flen + 1);
        unsave->Contents = tmp;
        strcpy(tmp, bak = cENV->GetStringUTFChars(pENV code, &iscopy));
        cENV->ReleaseStringUTFChars(pENV code, bak);
        tmp[unsave->Length] = 0;
        // unsave = unsavedAll;
    }
    /*if(tu){
      clang_disposeTranslationUnit(tu);tu = 0;
    }*/
    /*  CXCursor cu = clang_getTranslationUnitCursor(tu);
    CXString st = clang_getCursorSpelling(cu);
    printf("main: %s\n", clang_getCString(st));
    fflush(stdout);
    clang_disposeString(st);
  */
    if (mainFileName[0]) {
        if (tu == 0) {
            if (ix == 0) {
                ix = clang_createIndex(0, 0);
            }
            tu = clang_parseTranslationUnit(ix, mainFileName, opt, optcount, unsavedAll, unsavedcount,
                                            clang_defaultEditingTranslationUnitOptions());
            reparse = 0;
        } else {
            clang_reparseTranslationUnit(tu, unsavedcount, unsavedAll, clang_defaultReparseOptions(tu));
            reparse = 1;
        }
    }
#else
    const char *bak;
    jboolean iscopy;
    bak = cENV->GetStringUTFChars(pENV file, &iscopy);
    Tprintf("file len = %d[\n%s\n]\n", cENV->GetStringUTFLength(pENV file), bak);
    cENV->ReleaseStringUTFChars(pENV file, bak);
    bak = cENV->GetStringUTFChars(pENV code, &iscopy);
    Tprintf("code len = %d[\n%s\n]\n", cENV->GetStringUTFLength(pENV code), bak);
    cENV->ReleaseStringUTFChars(pENV code, bak);
#endif // CLANG_C_INDEX_H
}

#define _Version 1
#define _CodeComplete 2
#define _Diagnostic 3
#define _FunctionList 4
#define _CursorInfo 5
#define _IncludeList 6
#define _cmdGotoParent 7
#define _cmdLookReferenced 8
#define _cmdLookDefinition 9
#define _cmdLookCanonical 10
#define _cmdGotoCursor 11
#define _cmdCodeComplete 12
#define _cmdCursorParent 13
#define _cmdSortOn 14
#define _cmdSortOff 15

static CXCursor getParentCursor(CXCursor child);

static void printCursorRange(CXCursor cursor);

static void printCursorLocation(CXCursor cursor);

static void updateNav();

static void takeInfo(CXSourceLocation s);

static void takeIncludes();

static const char *printCompletionText(CXCompletionString cs, enum CXCursorKind kind, int root);

static const char *printCompletionText_(CXCompletionString cs);

static const char *printDiagnostic(CXDiagnostic d);

static const char *get();

static void addString(const char *tmpString);

static char tmpString[512] = {0};
static CXCursor tmpCursor;
static unsigned locationChanged = 0;
static unsigned sort = 0;
static unsigned locationOk = 0;

static void readyTmpCursor() {
    if (locationChanged) {
        locationChanged = 0;
        tmpCursor = clang_getCursor(tu, location);
    } else if (clang_Cursor_isNull(tmpCursor))
        tmpCursor = clang_getCursor(tu, location);
}

static void makeTrue() {
    if (mainFileName[0] && tu == 0) {
        if (ix == 0) {
            ix = clang_createIndex(0, 0);
        }
        tu =
                clang_parseTranslationUnit(ix, mainFileName, opt, optcount, unsavedAll, unsavedcount, clang_defaultEditingTranslationUnitOptions());
        reparse = 0;
    }
}

JNIEXPORT jstring SF(updatePosition)(JNIEnv *env, jclass cls, jint line, jint column) {
    if (line) {
        if (column) {
            CXFile file = clang_getFile(tu, currentFile);
            locationOk = file != NULL;
            if (locationOk)
                location = clang_getLocation(tu, file, line, column);
            else
                location = clang_getNullLocation();
            locationChanged = 1;
        } else {
            if (line > _Version && line < _cmdSortOn)
                makeTrue();
            bufferz = 0;
            if (buffer)
                buffer[0] = 0;
            else
                buffer = malloc(1);
            pos = buffer;
            jstring js = 0;
            unsigned n, i;
            CXString cxs;
            switch (line) {
                case _Version:
                    cxs = clang_getClangVersion();
                    const char *ret = clang_getCString(cxs);
                    if (ret)
                        js = cENV->NewStringUTF(pENV ret);
                    clang_disposeString(cxs);
                    return js;
                case _Diagnostic:
                    n = clang_getNumDiagnostics(tu);
                    for (i = 0; i < n; ++i) {
                        CXDiagnostic d = clang_getDiagnostic(tu, i);
                        /// jstring js = cENV->NewStringUTF(pENV
                        /// printDiagnostic(d));
                        ///(*je.env)->CallStaticVoidMethod(je.env, je.cls, je.m,
                        /// js);
                        addString(printDiagnostic(d));
                    }
                    break;
                case _FunctionList:
                    updateNav();
                    break;
                case _CursorInfo:
                    takeInfo(location);
                    break;
                case _IncludeList:
                    takeIncludes();
                    break;
                case _cmdGotoCursor: {
                    locationChanged = 1;
                    readyTmpCursor();
                    printCursorRange(tmpCursor);
                }
                    break;
                case _cmdCursorParent: {
                    readyTmpCursor();
                    CXCursor cursor = clang_getCursorSemanticParent(tmpCursor);
                    if (!clang_Cursor_isNull(cursor))
                        tmpCursor = cursor;
                    printCursorRange(tmpCursor);
                }
                    break;
                case _cmdGotoParent: {
                    readyTmpCursor();
                    CXCursor cursor = getParentCursor(tmpCursor);
                    if (!clang_Cursor_isNull(cursor))
                        tmpCursor = cursor;
                    printCursorRange(tmpCursor);
                }
                    break;
                case _cmdLookReferenced: {
                    readyTmpCursor();
                    CXCursor cursor = clang_getCursorReferenced(tmpCursor);
                    if (!clang_Cursor_isNull(cursor))
                        printCursorRange(cursor);
                }
                    break;
                case _cmdLookDefinition: {
                    readyTmpCursor();
                    CXCursor cursor = clang_getCursorDefinition(tmpCursor);
                    if (!clang_Cursor_isNull(cursor))
                        printCursorRange(cursor);
                }
                    break;
                case _cmdLookCanonical: {
                    readyTmpCursor();
                    CXCursor cursor = clang_getCursorReferenced(tmpCursor);
                    cursor = clang_getCanonicalCursor(cursor);
                    if (!clang_Cursor_isNull(cursor))
                        printCursorRange(cursor);
                }
                    break;
                case _cmdCodeComplete: {
                    readyTmpCursor();
                    CXCursor cursor = clang_getCursorReferenced(tmpCursor);
                    if (!clang_Cursor_isNull(cursor))
                        addString(printCompletionText(clang_getCursorCompletionString(cursor), clang_getCursorKind(cursor), 1));
                }
                    break;
                case _cmdSortOn: {
                    sort = 1;
                }
                    return 0;
                case _cmdSortOff: {
                    sort = 0;
                }
                    return 0;
            }
            js = cENV->NewStringUTF(pENV buffer);
            return js;
        }
    }
    return 0;
}

typedef struct {
    unsigned size;
    char *data;
    unsigned num;
    unsigned count;
} CodeCompleteResults;
static CodeCompleteResults tmpCc = {0, 0, 0, 0};

JNIEXPORT jint SF(set)(JNIEnv *env, jclass cls, jstring text, jboolean ccc) {
    if (text) {
        const char *bak;
        char *tmp;
        jboolean iscopy;
        bak = cENV->GetStringUTFChars(pENV text, &iscopy);
        strcpy(rootStr, bak);
        cENV->ReleaseStringUTFChars(pENV text, bak);
    } else {
        rootStr[0] = 0;
    }
    if (ccc) {
        CXCodeCompleteResults *cc = 0;
        if (!locationOk)
            return 0;
        unsigned uline, ucolumn;
        clang_getFileLocation(location, 0, &uline, &ucolumn, 0);
        cc = clang_codeCompleteAt(tu, currentFile, uline, ucolumn, unsavedAll, unsavedcount,
                                  clang_defaultCodeCompleteOptions() | CXCodeComplete_IncludeCodePatterns);
        if (cc) {
            if (sort)
                clang_sortCodeCompletionResults(cc->Results, cc->NumResults);
            bufferz = 0;
            unsigned newSize = sizeof(unsigned) * cc->NumResults * 2;
            if (newSize >= buffers) {
                buffers = newSize << 1;
                if (buffer)
                    buffer = realloc(buffer, buffers);
                else
                    buffer = malloc(buffers);
            }
            pos = buffer + newSize;
            bufferz = newSize;
            unsigned long long t = clang_codeCompleteGetContexts(cc);
#ifdef __ANDROID__
            LOGW("%llx - %s", t, "clang_codeCompleteGetContexts");
#endif
            for (unsigned i = 0; i < cc->NumResults; ++i) {
                unsigned *ids = (unsigned *) buffer;
                ids[i] = ids[i + cc->NumResults] = bufferz;
                enum CXCursorKind kind = cc->Results[i].CursorKind;
                const char *ctx = printCompletionText(cc->Results[i].CompletionString, kind, 1);
#ifdef __ANDROID__
// LOGW("%llx - %s", context, ctx);
#endif
                addString(ctx);
                ++pos;
                ++bufferz;
            }
#ifdef __ANDROID__
            if (t == CXCompletionContext_Unexposed)
                LOGW("Unexposed");
            if ((t & CXCompletionContext_AnyType) == CXCompletionContext_AnyType)
                LOGW("AnyType");
            if ((t & CXCompletionContext_AnyValue) == CXCompletionContext_AnyValue)
                LOGW("AnyValue");
            if ((t & CXCompletionContext_ObjCObjectValue) == CXCompletionContext_ObjCObjectValue)
                LOGW("ObjCObjectValue");
            if ((t & CXCompletionContext_ObjCSelectorValue) == CXCompletionContext_ObjCSelectorValue)
                LOGW("ObjCSelectorValue");
            if ((t & CXCompletionContext_CXXClassTypeValue) == CXCompletionContext_CXXClassTypeValue)
                LOGW("CXXClassTypeValue");
            if ((t & CXCompletionContext_DotMemberAccess) == CXCompletionContext_DotMemberAccess)
                LOGW("DotMemberAccess");
            if ((t & CXCompletionContext_ArrowMemberAccess) == CXCompletionContext_ArrowMemberAccess)
                LOGW("ArrowMemberAccess");
            if ((t & CXCompletionContext_ObjCPropertyAccess) == CXCompletionContext_ObjCPropertyAccess)
                LOGW("ObjCPropertyAccess");
            if ((t & CXCompletionContext_EnumTag) == CXCompletionContext_EnumTag)
                LOGW("EnumTag");
            if ((t & CXCompletionContext_UnionTag) == CXCompletionContext_UnionTag)
                LOGW("UnionTag");
            if ((t & CXCompletionContext_StructTag) == CXCompletionContext_StructTag)
                LOGW("StructTag");
            if ((t & CXCompletionContext_ClassTag) == CXCompletionContext_ClassTag)
                LOGW("ClassTag");
            if ((t & CXCompletionContext_Namespace) == CXCompletionContext_Namespace)
                LOGW("Namespace");
            if ((t & CXCompletionContext_NestedNameSpecifier) == CXCompletionContext_NestedNameSpecifier)
                LOGW("NestedNameSpecifier");
            if ((t & CXCompletionContext_ObjCInterface) == CXCompletionContext_ObjCInterface)
                LOGW("ObjCInterface");
            if ((t & CXCompletionContext_ObjCProtocol) == CXCompletionContext_ObjCProtocol)
                LOGW("ObjCProtocol");
            if ((t & CXCompletionContext_ObjCCategory) == CXCompletionContext_ObjCCategory)
                LOGW("ObjCCategory");
            if ((t & CXCompletionContext_ObjCInstanceMessage) == CXCompletionContext_ObjCInstanceMessage)
                LOGW("ObjCInstanceMessage");
            if ((t & CXCompletionContext_ObjCClassMessage) == CXCompletionContext_ObjCClassMessage)
                LOGW("ObjCClassMessage");
            if ((t & CXCompletionContext_ObjCSelectorName) == CXCompletionContext_ObjCSelectorName)
                LOGW("ObjCSelectorName");
            if ((t & CXCompletionContext_MacroName) == CXCompletionContext_MacroName)
                LOGW("MacroName");
            if ((t & CXCompletionContext_NaturalLanguage) == CXCompletionContext_NaturalLanguage)
                LOGW("NaturalLanguage");
            if ((t & CXCompletionContext_Unknown) == CXCompletionContext_Unknown)
                LOGW("Unknown");
#endif
            tmpCc.num = cc->NumResults;
            tmpCc.count = cc->NumResults;
            clang_disposeCodeCompleteResults(cc), cc = 0;
            void *tmp = tmpCc.data;
            if (tmp)
                tmp = realloc(tmp, 256);
            else
                tmp = malloc(256);
            tmpCc.data = buffer;
            tmpCc.size = buffers;
            buffer = tmp;
            pos = buffer;
            bufferz = 0;
            buffers = 256;
        } else {
            tmpCc.num = 0;
            tmpCc.count = 0;
        }
    }
    if (tmpCc.num) {
        unsigned *ids = (unsigned *) tmpCc.data;
        if (rootStr[0]) {
            tmpCc.count = 0;
            for (unsigned i = 0; i < tmpCc.num; ++i) {
                const char *text = strstr(tmpCc.data + ids[i], "↔");
                if (text && (strcasestr(text, rootStr) || !strcmp(text, "↔<提示>"))) {
                    ids[tmpCc.num + tmpCc.count++] = ids[i];
                }
            }
        } else {
            for (unsigned i = 0; i < tmpCc.num; ++i) {
                ids[tmpCc.num + i] = ids[i];
            }
            tmpCc.count = tmpCc.num;
        }
    }
    return tmpCc.count;
}

JNIEXPORT jstring SF(get)(JNIEnv *env, jclass cls, jint id) {
    if (tmpCc.count > id) {
        const char *ret = tmpCc.data + ((unsigned *) tmpCc.data)[tmpCc.num + id];
        return cENV->NewStringUTF(pENV ret);
    }
    return 0;
}

static int DiagCount = 0;
static unsigned *diag = 0;

static void count() {
    unsigned n = clang_getNumDiagnostics(tu);
    DiagCount = 0;
    if (n > 0) {
        if (diag)
            diag = (unsigned *) realloc(diag, n * sizeof(unsigned));
        else
            diag = (unsigned *) malloc(n * sizeof(unsigned));
        for (unsigned i = 0; i < n; ++i) {
            CXDiagnostic d = clang_getDiagnostic(tu, i);
            if (clang_getDiagnosticCategory(d) == 2) {
                CXString s = clang_getDiagnosticSpelling(d);
                const char *t = clang_getCString(s);
                char *st = strchr(t, '\'');
                if (st) {
                    char *ed = strchr(st + 1, '\'');
                    if (ed > st) {
                        diag[DiagCount] = i;
                        ++DiagCount;
                    }
                }
                clang_disposeString(s);
            }
        }
    }
} /*
 static const char *get() {
   count();
   for (int i = 0; i < DiagCount; ++i) {
     CXDiagnostic d = clang_getDiagnostic(tu, diag[i]);
     if (clang_getDiagnosticCategory(d) == 2) {
       CXString s = clang_getDiagnosticSpelling(d);
       const char *t = clang_getCString(s);
       char *st = strchr(t, '\'');
       if (st) {
         char *ed = strchr(st + 1, '\'');
         if (ed > st) {
           char tmp[ed - st];
           strncpy(tmp, st + 1, ed - st - 1);
           tmp[ed - st - 1] = 0;
           {
             sprintf(tmpString, "↔%s\n\n", tmp);
             addString(tmpString);
           }
           /// ret = cENV->NewStringUTF(pENV tmpString);
         }
       }
       clang_disposeString(s);
     }
   }
   for (int i = 0; i < cc->NumResults; ++i) {
     addString(printCompletionText(cc->Results[i].CompletionString,
                                   cc->Results[i].CursorKind, 1));
     addString("\n\n");
   }
   return buffer;
 }*/
static int bVisit;

void visit(CXFile ifile, CXSourceLocation *sl, unsigned include_len, CXClientData client_data) {
    if (clang_File_isEqual(ifile, client_data)) {
        bVisit = 1;
    }
}

static enum CXChildVisitResult visit2(CXCursor cursor, CXCursor p, CXClientData client_data) {
    CXFile file;
    clang_getFileLocation(clang_getCursorLocation(cursor), &file, 0, 0, 0);
    if (clang_File_isEqual(file, client_data)) {
        bVisit = 1;
        return CXChildVisit_Break;
    }
    return CXChildVisit_Continue;
}

JNIEXPORT jboolean SF(bHasFile)(JNIEnv *env, jclass cls, jstring file, jboolean setCurrent) {
    const char *bak;
    jboolean iscopy;
    bak = cENV->GetStringUTFChars(pENV file, &iscopy);
    if (setCurrent)
        strcpy(currentFile, bak);
    if (tu) {
        CXFile cxf = clang_getFile(tu, bak);
        bVisit = 0;
        if (reparse)
            clang_visitChildren(clang_getTranslationUnitCursor(tu), visit2, cxf);
        else
            clang_getInclusions(tu, visit, cxf);
        cENV->ReleaseStringUTFChars(pENV file, bak);
        if (bVisit)
            return JNI_TRUE;
    }
    return JNI_FALSE;
}

#undef SF
//======
typedef struct t {
    CXSourceRange range;
    CXCursor found;
} FindCursor;

static enum CXChildVisitResult cursorVisitParent(CXCursor cursor, CXCursor p, CXClientData client_data) {
    if (clang_equalRanges(((FindCursor *) client_data)->range, clang_getCursorExtent(cursor))) {
        ((FindCursor *) client_data)->found = p;
        return CXChildVisit_Break;
    }
    return CXChildVisit_Recurse;
}

static CXCursor getParentCursor(CXCursor cursor) {
    if (tu) {
        FindCursor out;
        out.range = clang_getCursorExtent(cursor);
        out.found = clang_getCursorLexicalParent(cursor);
        if (clang_Cursor_isNull(out.found))
            out.found = clang_getTranslationUnitCursor(tu);
        clang_visitChildren(out.found, cursorVisitParent, &out);
        return out.found;
    }
    return clang_getNullCursor();
}

//======
static enum CXChildVisitResult cursorVisit(CXCursor cursor, CXCursor p, CXClientData client_data) {
    CXFile file;
    clang_getFileLocation(clang_getCursorLocation(cursor), &file, 0, 0, 0);
    CXString n = clang_getFileName(file);
    const char *s = clang_getCString(n);
    if (!strstr(buffer, s)) {
        addString(s);
        addString("\n");
    }
    clang_disposeString(n);
    return CXChildVisit_Continue;
}

static void visitInclude(CXFile file, CXSourceLocation *l, unsigned d, CXClientData data) {
    CXString n = clang_getFileName(file);
    const char *s = clang_getCString(n);
    if (!strstr(buffer, s)) {
        addString(s);
        addString("\n");
    }
    clang_disposeString(n);
}

static void takeIncludes() {
    if (tu) {
        if (reparse)
            clang_visitChildren(clang_getTranslationUnitCursor(tu), cursorVisit, 0);
        else
            clang_getInclusions(tu, visitInclude, NULL);
    }
}
///---/////////////// no Java Things Here !
///////////////////////////////////////////
#define SEL(a, b) a

static void displayCursorNav(CXCursor Cursor);

static void displayCursorNavFast(CXCursor Cursor);

typedef struct {
    int out;
    CXSourceLocation location;
} Data;

static enum CXChildVisitResult cursorVisitChild(CXCursor Cursor, CXCursor Parent, CXClientData _data) {
    Data *data = _data;
    if (clang_equalLocations(clang_getCursorLocation(Cursor), data->location)) {
        data->out = 0;
        return CXChildVisit_Break;
    }
    return CXChildVisit_Recurse;
}

static unsigned isCursorNoChild(CXCursor cursor) {
    Data data;
    data.out = 1;
    data.location = clang_getCursorLocation(cursor);
    clang_visitChildren(cursor, cursorVisitChild, &data);
    return data.out;
}

unsigned isAfter(CXCursor a, CXCursor b) {
    CXSourceLocation l = clang_getCursorLocation(a);
    CXFile fa, fb;
    unsigned up, down;
    clang_getFileLocation(l, &fa, 0, 0, &up);
    l = clang_getCursorLocation(b);
    clang_getFileLocation(l, &fb, 0, 0, &down);
    return clang_File_isEqual(fa, fb) && (up < down);
}

static enum CXChildVisitResult cListNav(CXCursor Cursor, CXCursor Parent, CXClientData ClientData) {
    CXCursor root = clang_getCursorReferenced(Cursor);
    root = clang_getCanonicalCursor(root);
    CXCursor *cr = (CXCursor *) ClientData;
    if (clang_equalCursors(root, *cr)) {
        if (isCursorNoChild(Cursor))
            displayCursorNav(Cursor);
    } else if (isAfter(Cursor, Parent)) {
        return CXChildVisit_Continue;
    }
    return CXChildVisit_Recurse; // CXChildVisit_Continue;
}

static void takeInfo(CXSourceLocation s) {
    CXCursor cursor, cur;
    cur = clang_getCursor(tu, s);
    if (clang_Cursor_isNull(cur))
        return;
    cursor = clang_getCursorReferenced(cur);
    if (clang_Cursor_isNull(cursor))
        return;
    cursor = clang_getCanonicalCursor(cursor);
    clang_visitChildren(clang_getTranslationUnitCursor(tu), cListNav, &cursor);
}

static char ret[1024];

void displayCursorNav(CXCursor Cursor) {
    CXSourceLocation loc = clang_getCursorLocation(Cursor);
// if (0 == clang_Location_isFromMainFile(loc))return;
#if 1
// Cursor = clang_getCursorReferenced(Cursor);
// CXCompletionString cs = clang_getCursorCompletionString(Cursor);
#if 1
    CXFile file;
    unsigned line, col, off;
    const char *fn;
    clang_getFileLocation(loc, &file, &line, &col, &off);
    CXString strFileName = clang_getFileName(file);
    fn = clang_getCString(strFileName);
    // if (strcasecmp(fn, unsave->Filename) == 0)
    // const char *ret = printCompletionText(cs, 1);
    CXString cns = clang_getCursorDisplayName(Cursor);
    strcpy(ret, clang_getCString(cns));
    if (CXCursor_FunctionDecl == clang_getCursorKind(Cursor)) {
        if (clang_isCursorDefinition(Cursor))
            strcat(ret, "{}");
    }
    CXString shows = clang_getCursorSpelling(Cursor);
    const char *show = clang_getCString(shows);
    if (!*show)
        show = "未命名";
    sprintf(tmpString, "%s:%u:%u: cursor: %s↔%s\n\n", fn, line, col /*, off*/, ret, show);
    clang_disposeString(shows);
    clang_disposeString(cns);
    clang_disposeString(strFileName);
#else
    strcat(fi, "\n");
#endif
#else
    CXString String = clang_getCursorDisplayName(Cursor);
    Tprintf(SEL("显示: [%s]\n", "Display:%s\n"), clang_getCString(String));
    clang_disposeString(String);
#endif
    /**if (je.m)*/
    { addString(tmpString); }
}

static void displayCursorNavFast(CXCursor Cursor) {
    CXSourceLocation loc = clang_getCursorLocation(Cursor);
    CXFile file;
    unsigned line, col, off;
    const char *fn;
    clang_getFileLocation(loc, &file, &line, &col, &off);
    CXString strFileName = clang_getFileName(file);
    fn = clang_getCString(strFileName);
    /// printf("debug %s == %s\n",fn,unsave->Filename);
    if (strcasecmp(fn, currentFile) == 0) {
        enum CXCursorKind kind = clang_getCursorKind(Cursor);
        CXString cursorKind = clang_getCursorKindSpelling(kind);
        *ret = 0;
        CXCompletionString cs = clang_getCursorCompletionString(Cursor);
        const char *completion = printCompletionText_(cs);
        if (CXCursor_FunctionDecl == kind) {
            if (clang_isCursorDefinition(Cursor))
                strcat(ret, "{}");
        }
        CXString display_ = clang_getCursorSpelling(Cursor);
        const char *display = clang_getCString(display_);
        if (!*display)
            display = "未命名";
        sprintf(tmpString, "%s:%u:%u: %s: %s↔%s\n\n", fn, line, col, clang_getCString(cursorKind), completion, display);
        clang_disposeString(display_);
        clang_disposeString(cursorKind);
        addString(tmpString);
    } else
        tmpString[0] = 0;
    clang_disposeString(strFileName);
}

static void addString(const char *tmpString) {
    if (tmpString && tmpString[0]) {
        int tl = strlen(tmpString);
        bufferz += tl;
        while (bufferz >= buffers) {
            buffers <<= 1;
            buffer = realloc(buffer, buffers);
            pos = buffer + bufferz - tl;
        }
        strcpy(pos, tmpString);
        pos += tl;
    }
}

static enum CXChildVisitResult ListNav(CXCursor Cursor, CXCursor Parent, CXClientData ClientData) {
    // displayCursorInfo(Cursor);
    switch (Cursor.kind) {
        case CXCursor_FunctionDecl:
        case CXCursor_CXXMethod:
        case CXCursor_ClassDecl:
        case CXCursor_VarDecl:
        case CXCursor_UnionDecl:
        case CXCursor_StructDecl:
        case CXCursor_Namespace:
        case CXCursor_InclusionDirective:
            // case CXCursor_FieldDecl:
            // case CXCursor_MemberRef:
            // case CXCursor_VarDecl:
            // case CXCursor_NamespaceRef:
            // case CXCursor_DeclStmt:
            // if(!clang_equalCursors(Cursor,Parent))
            if (Parent.kind == CXCursor_TranslationUnit || Parent.kind == CXCursor_StructDecl || Parent.kind == CXCursor_ClassDecl ||
                Parent.kind == CXCursor_Namespace)
                displayCursorNavFast(Cursor);
            break;
        case CXCursor_TypedefDecl:
            displayCursorNavFast(Cursor);
            return CXChildVisit_Continue;
        default:
            break;
    }
    return CXChildVisit_Recurse; // CXChildVisit_Continue;
}

void updateNav() {
    if (tu == 0)
        return;
    CXCursor cursor = clang_getTranslationUnitCursor(tu);
    clang_visitChildren(cursor, ListNav, (CXClientData) NULL);
}

#ifdef CLANG_C_INDEX_H

static void printFileName(CXFile file) {
    CXString string = clang_getFileName(file);
    addString(clang_getCString(string));
    clang_disposeString(string);
}

static void printCursorRange(CXCursor cursor) {
    CXSourceRange range = clang_getCursorExtent(cursor);
    unsigned line, column, line1, column1, line2, column2;
    CXFile file, file1, file2;
    CXSourceLocation location = clang_getCursorLocation(cursor);
    clang_getFileLocation(location, &file, &line, &column, 0);
    location = clang_getRangeStart(range);
    clang_getFileLocation(location, &file1, &line1, &column1, 0);
    location = clang_getRangeEnd(range);
    clang_getFileLocation(location, &file2, &line2, &column2, 0);
    sprintf(tmpString, ":%d:%d:{%d:%d-%d:%d}: cursor", line, column, line1, column1, line2, column2);
    printFileName(file);
    addString(tmpString);
}

static void printCursorLocation(CXCursor cursor) {
    CXSourceLocation location = clang_getCursorLocation(cursor);
    unsigned line, column;
    CXFile file;
    clang_getFileLocation(location, &file, &line, &column, 0);
    sprintf(tmpString, ":%d:%d", line, column);
    printFileName(file);
    addString(tmpString);
}

static const char *ccp(unsigned tt);

const char *printDiagnostic(CXDiagnostic d) {
    CXString cs = clang_formatDiagnostic(d, clang_defaultDiagnosticDisplayOptions());
    strcpy(ret, clang_getCString(cs));
    clang_disposeString(cs);
    // CXSourceLocation dl = clang_getDiagnosticLocation(d);
    unsigned /*n = clang_getDiagnosticNumRanges(d);
  sprintf(ret + strlen(ret), "[%u]", n);
  for (unsigned i = 0; i < n; ++i) {
    CXSourceRange re = clang_getDiagnosticRange(d, i);
    unsigned leftLne, leftCol;
    unsigned rightLne, rightCol;
    clang_getFileLocation(clang_getRangeStart(re), 0, &leftLne, &leftCol, 0);
    clang_getFileLocation(clang_getRangeEnd(re), 0, &rightLne, &rightCol, 0);
    sprintf(ret + strlen(ret), "(%u:%u-%u:%u)", leftLne, leftCol, rightLne,
                                    rightCol);
  }
  if (n == 0) {
    unsigned rightLne, rightCol;
    clang_getFileLocation(clang_getDiagnosticLocation(d), 0, &rightLne, &rightCol, 0);
    sprintf(ret + strlen(ret), "(%u:%u)", rightLne, rightCol);
  }*/
            n = clang_getDiagnosticNumFixIts(d);
    CXSourceRange range;
    for (unsigned i = 0; i < n; ++i) {
        cs = clang_getDiagnosticFixIt(d, i, &range);
        CXSourceLocation start = clang_getRangeStart(range);
        // if (clang_Location_isFromMainFile(start))
        {
            unsigned lrs, lre;
            /*if (clang_equalLocations(dl, rs)) */
            {
                unsigned ln, cl;
                CXFile file;
                clang_getFileLocation(start, 0, &ln, &cl, &lrs);
                CXSourceLocation rs = clang_getRangeEnd(range);
                clang_getFileLocation(rs, &file, 0, 0, &lre);
                const char *m = clang_getCString(cs);
                if (lre == lrs) {
                    sprintf(ret + strlen(ret), "↔增加:%u:%u+'%s'", ln, cl, m);
                    sprintf(ret + strlen(ret), "\n<r offset='%u' length='0'>%s</r>", lrs, m);
                } else {
                    char wt[128];
                    wt[0] = 0;
                    CXToken *tokens;
                    unsigned numTokens;
                    range = clang_getRange(start, clang_getLocationForOffset(tu, file, lre - 1));
                    clang_tokenize(tu, range, &tokens, &numTokens);
                    for (unsigned i = 0; i < numTokens; ++i) {
                        CXString string = clang_getTokenSpelling(tu, tokens[i]);
                        strcat(wt, clang_getCString(string));
                        clang_disposeString(string);
                    }
                    clang_disposeTokens(tu, tokens, numTokens);
                    if (*m) {
                        sprintf(ret + strlen(ret), "↔替换:%u:%u'%s'='%s'", ln, cl, wt, m);
                        sprintf(ret + strlen(ret), "\n<r offset='%u' length='%u'>%s</r>", lrs, lre - lrs, m);
                    } else {
                        sprintf(ret + strlen(ret), "↔删除:%u:%u-'%s'", ln, cl, wt);
                        sprintf(ret + strlen(ret), "\n<r offset='%u' length='%u'></r>", lrs, lre - lrs);
                    }
                }
            }
        }
        clang_disposeString(cs);
    }
    strcat(ret, "~~");
    return ret;
}

const char *printCompletionText(CXCompletionString cs, enum CXCursorKind kind, int root) {
    static char typed[256];
    if (root == 1) {
        *typed = 0;
        *ret = 0;
    }
    for (int ii = 0, n = clang_getNumCompletionChunks(cs); ii < n; ++ii) {
        CXString s = clang_getCompletionChunkText(cs, ii);
        const char *v = clang_getCString(s);
        enum CXCompletionChunkKind k = clang_getCompletionChunkKind(cs, ii);
        switch (k) {
            case CXCompletionChunk_Optional:
                strcat(ret, "[");
                printCompletionText(clang_getCompletionChunkCompletionString(cs, ii), kind, 0);
                strcat(ret, "]");
                break;
            case CXCompletionChunk_Comma:
                strcat(ret, ",");
                break;
            case CXCompletionChunk_Placeholder:
                sprintf(ret + strlen(ret), "'<%s>'", v);
                break;
            case CXCompletionChunk_Informative:
                sprintf(ret + strlen(ret), "'(%s)'", v);
                break;
            case CXCompletionChunk_CurrentParameter:
                sprintf(ret + strlen(ret), "'{%s}'", v);
                break;
            case CXCompletionChunk_Text:
                sprintf(ret + strlen(ret), "%s", v);
                if (kind == CXCursor_Constructor) {
                    strcat(ret, " '<var>'");
                }
                break;
            case CXCompletionChunk_TypedText:
                sprintf(ret + strlen(ret), "%s", v);
                strcat(typed, v);
                break;
            case CXCompletionChunk_ResultType:
                sprintf(ret + strlen(ret), "'[%s]'", v);
                break;
            default:
                sprintf(ret + strlen(ret), "%s", v);
        }
        clang_disposeString(s);
    }
    if (root) {
        strcat(ret, "↔");
        if (*typed) {
            strcat(ret, typed);
        } else
            strcat(ret, "<提示>");
    }
    return ret;
}

const char *printCompletionText_(CXCompletionString cs) {
    for (int ii = 0, n = clang_getNumCompletionChunks(cs); ii < n; ++ii) {
        CXString s = clang_getCompletionChunkText(cs, ii);
        const char *v = clang_getCString(s);
        enum CXCompletionChunkKind k = clang_getCompletionChunkKind(cs, ii);
        switch (k) {
            case CXCompletionChunk_Optional:
                // strcat(ret, "[");
                printCompletionText_(clang_getCompletionChunkCompletionString(cs, ii));
                // strcat(ret, "]");
                break; /*
                                         case CXCompletionChunk_Comma:
                                                       strcat(ret, ",");
                                                       break;
                                         case CXCompletionChunk_Placeholder:
                                                       sprintf(ret +
                strlen(ret), "'<%s>'", v);
                                                       break;
*/
            case CXCompletionChunk_Informative:
                sprintf(ret + strlen(ret), "'(%s)'", v);
                break; /*
                                         case
                CXCompletionChunk_CurrentParameter:
                                                       sprintf(ret +
                strlen(ret), "'{%s}'", v);
                                                       break;
                                         case CXCompletionChunk_Text:
                                                       sprintf(ret +
                strlen(ret), "%s", v);
                                                       break;
                                         case CXCompletionChunk_TypedText:
                                                       sprintf(ret +
                strlen(ret), "%s", v);
                                                       break;
*/
            case CXCompletionChunk_ResultType:
                sprintf(ret + strlen(ret), "%s ", v);
                break;
            default:
                sprintf(ret + strlen(ret), "%s", v);
        }
        clang_disposeString(s);
    }
    return ret;
}

enum {
    CCP_NextInitializer = 7,
    CCP_EnumInCase = 7,
    CCP_SuperCompletion = 20,
    CCP_LocalDeclaration = 34,
    CCP_MemberDeclaration = 35,
    CCP_Keyword = 40,
    CCP_CodePattern = 40,
    CCP_Declaration = 50,
    CCP_Type = CCP_Declaration,
    CCP_Constant = 65,
    CCP_Macro = 70,
    CCP_NestedNameSpecifier = 75,
    CCP_Unlikely = 80,
    CCP_ObjC_cmd = CCP_Unlikely
};

static const char *ccp(unsigned tt) {
    switch (tt) {
        // case CCP_NextInitializer:return "NextInitializer";
        case CCP_EnumInCase:
            return "枚"; //"EnumInCase|NextInitializer";
        case CCP_SuperCompletion:
            return "超"; //"SuperCompletion";
        case CCP_LocalDeclaration:
            return "内"; //"LocalDeclaration";
        case CCP_MemberDeclaration:
            return "员"; //"MemberDeclaration";
            // case CCP_Keyword:return "Keyword";
        case CCP_CodePattern:
            return "快"; //"CodePattern|Keyword";
            // case CCP_Declaration:return "Declaration";
        case CCP_Type:
            return "型"; //"Type|Declaration";
        case CCP_Constant:
            return "常"; //"Constant";
        case CCP_Macro:
            return "宏"; //"Macro";
        case CCP_NestedNameSpecifier:
            return "名"; //"NestedNameSpecifier";
            // case CCP_Unlikely:return "Unlikely";
        case CCP_ObjC_cmd:
            return "欧"; //"ObjC_cmd|CCP_Unlikely";
    }
    return "未";
}
// int main(){return 0;}
#endif // CLANG_C_INDEX_H