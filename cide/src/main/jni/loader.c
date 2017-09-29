#include <stdio.h>
#include <android/native_activity.h>
#include <dlfcn.h>

struct android_app;
//typedef void ANativeActivity_finishFunc(ANativeActivity* activity);
static void *dl;

extern void ANativeActivity_onCreate(ANativeActivity *activity, void *savedState, size_t savedStateSize) {
    char so[256];
    sprintf(so, "%s/temp", activity->internalDataPath);
    if (dl) {
        //ANativeActivity_finishFunc*finish=(ANativeActivity_finishFunc*)dlsym(dl,"ANativeActivity_finish");finish(activity);
        dlclose(dl);
    }
    dl = dlopen(so, RTLD_GLOBAL);
    if (dl == 0) {
        ANativeActivity_finish(activity);
        return;
    }
    ANativeActivity_createFunc *onCreate = (ANativeActivity_createFunc *) dlsym(dl, "ANativeActivity_onCreate");
    if (onCreate == 0)return;
    onCreate(activity, savedState, savedStateSize);
}