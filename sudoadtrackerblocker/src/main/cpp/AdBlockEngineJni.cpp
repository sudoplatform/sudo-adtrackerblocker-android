/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * JNI Interface wrapper around the Adblock Engine in the adblock-rust-ffi library
 *
 * @since 2020-11-20
 */
#include <jni.h>
#include <vector>
#include <string>

extern "C" {
#include "lib.h" // libadblock C interface header
}

/**
 * Helper class to convert from a Java string (jstring) to a const char* and deallocate
 * the JNI string when it goes out of scope.
 *
 * This is an implementation of "resource acquisition is initialisation"
 * https://en.wikipedia.org/wiki/Resource_acquisition_is_initialization
 *
 * https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/functions.html#GetStringUTFChars
 */
class JniString {
public:
    /**
     * Construct a JniString from a jstring and get the UTF characters from it.
     *
     * @param env JNI environment
     * @param s The jstring from which the UTF characters are extracted
     */
    explicit JniString(JNIEnv* env, jstring s) : jniEnv(env), jstr(s), str(nullptr) {
        if (s) {
            str = env->GetStringUTFChars(s, nullptr);
        }
    }

    /**
     * Release the UTF characters extracted from the jstring back to JNI
     */
    virtual ~JniString() {
        if (jstr && str) {
            jniEnv->ReleaseStringUTFChars(jstr, str);
        }
    }

    /**
     * @return the UTF characters that were extracted from the wrapped jstring.
     */
    const char* get() const {
        return str;
    }

    // Disable copy constructor and assignment operator
    JniString(const JniString& rhs) = delete;
    JniString& operator=(const JniString& rhs) = delete;

private:
    /** JNI environment that we need to call any JNI helper methods */
    JNIEnv* jniEnv;

    /** The jstring that has been wrapped */
    jstring jstr;

    /* The UTF characters extracted from the jstring */
    const char* str;
};

typedef std::vector<C_Engine*> EnginesVector;

/**
 * Get the field "engines" from the Kotlin/Java class which we will use
 * to store a pointer to the vector of adblock-rust engines.
 *
 * @param jniEnv JNI environment that we need to call any JNI helper methods
 * @param thiz The instance of the Kotlin/Java class
 * @return Pointer to the std::vector of adblock-rust engines
 */
static EnginesVector* getEnginesField(JNIEnv* jniEnv, jobject thiz) {
    jclass enginesClass = jniEnv->GetObjectClass(thiz);
    jfieldID enginesFieldId = jniEnv->GetFieldID(enginesClass, "engines", "J");
    jlong enginesField = jniEnv->GetLongField(thiz, enginesFieldId);
    return reinterpret_cast<EnginesVector*>(enginesField);
}

/**
 * Set a long value in the field "engines" of the Kotlin/Java class which we will use
 * to store a pointer to the vector of adblock-rust engines.
 *
 * @param jniEnv JNI environment that we need to call any JNI helper methods
 * @param thiz The instance of the Kotlin/Java class
 * @param engines Pointer to the std::vector of adblock-rust engines (C_Engine*)
 */
static void setEnginesField(JNIEnv* jniEnv, jobject thiz, EnginesVector* engines) {
    jclass enginesClass = jniEnv->GetObjectClass(thiz);
    jfieldID enginesFieldId = jniEnv->GetFieldID(enginesClass, "engines", "J");
    jniEnv->SetLongField(thiz, enginesFieldId, reinterpret_cast<jlong>(engines));
}


extern "C" {

/**
 * A callback that receives a URL and two out-parameters for start and end position.
 * The callback should fill the start and end positions with the start and end indices
 * of the domain part of the URL. This method is required by the adblock-rust
 * library.
 */
static void domainResolver(const char* url, uint32_t* startPosition, uint32_t* endPosition) {
    std::string urlString(url);
    if (urlString.empty()) {
        *startPosition = 0;
        *endPosition = 0;
        return;
    }

    // Find the starting position after the http or https protocol prefix
    const std::string HTTP_PROTOCOL_PREFIX("http://");
    const std::string HTTPS_PROTOCOL_PREFIX("https://");

    if (urlString.find(HTTP_PROTOCOL_PREFIX, 0) != std::string::npos) {
        *startPosition = HTTP_PROTOCOL_PREFIX.length();
    } else if (urlString.find(HTTPS_PROTOCOL_PREFIX, 0) != std::string::npos) {
        *startPosition = HTTPS_PROTOCOL_PREFIX.length();
    } else {
        *startPosition = 0;
    }

    // Find the ending position at the first separator character after the starting position
    int pos = urlString.find_first_of("/?:", *startPosition);
    if (pos != std::string::npos) {
        *endPosition = pos;
    } else {
        *endPosition = urlString.length();
    }
}

/*
 * Class:     com_sudoplatform_sudoadtrackerblocker_blocking_adblock_AdBlockEngine
 * Method:    loadRules
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_com_sudoplatform_sudoadtrackerblocker_blocking_adblock_AdBlockEngine_loadRules(
    JNIEnv* jniEnv,
    jobject thiz,
    jstring jrules
) {
    EnginesVector* engines = getEnginesField(jniEnv, thiz);
    if (engines == nullptr) {
        engines = new EnginesVector();
        set_domain_resolver(domainResolver);
    }

    JniString someRules(jniEnv, jrules);
    C_Engine* engine = engine_create(someRules.get());
    engines->push_back(engine);

    setEnginesField(jniEnv, thiz, engines);
}

/*
 * Class:     com_sudoplatform_sudoadtrackerblocker_blocking_adblock_AdBlockEngine
 * Method:    clearRules
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_sudoplatform_sudoadtrackerblocker_blocking_adblock_AdBlockEngine_clearRules(
    JNIEnv* jniEnv,
    jobject thiz
) {
    EnginesVector* engines = getEnginesField(jniEnv, thiz);
    if (engines != nullptr) {
        for (auto & engine : *engines) {
            engine_destroy(engine);
        }
        engines->clear();
        delete engines;
        setEnginesField(jniEnv, thiz, nullptr);
        set_domain_resolver(nullptr);
    }
}

const char* INIT_ERROR = "Please initialise the engine by loading a set of rules into it first";

/*
 * Class:     com_sudoplatform_sudoadtrackerblocker_blocking_adblock_AdBlockEngine
 * Method:    shouldLoad
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sudoplatform_sudoadtrackerblocker_blocking_adblock_AdBlockEngine_shouldLoad(
    JNIEnv* jniEnv,
    jobject thiz,
    jstring jresourceUrl,
    jstring jcurrentUrl,
    jstring jresourceType,
    jstring jrequestHost,
    jstring jsourceHost
) {
    EnginesVector* engines = getEnginesField(jniEnv, thiz);
    if (engines == nullptr) {
        jclass npe = jniEnv->FindClass("java/lang/NullPointerException");
        jniEnv->ThrowNew(npe, INIT_ERROR);
        return JNI_FALSE;
    }
    if (engines->empty()) {
        jclass ise = jniEnv->FindClass("java/lang/IllegalStateException");
        jniEnv->ThrowNew(ise, INIT_ERROR);
        return JNI_FALSE;
    }

    JniString resourceUrl(jniEnv, jresourceUrl);
    JniString currentUrl(jniEnv, jcurrentUrl);
    JniString resourceType(jniEnv, jresourceType);
    JniString requestHost(jniEnv, jrequestHost);
    JniString sourceHost(jniEnv, jsourceHost);
    bool isThirdPartyRequest = std::strcmp(requestHost.get(), sourceHost.get()) == 0;

    // Outputs from the engine_match method
    bool explicitCancel = false;
    bool savedByException = false;
    char* redirect = nullptr;

    for (auto & engine : *engines) {
        if (engine_match(
                engine,
                resourceUrl.get(),
                requestHost.get(),
                sourceHost.get(),
                isThirdPartyRequest,
                resourceType.get() ? resourceType.get() : "script",
                &explicitCancel,
                &savedByException,
                &redirect
            )
        ) {
            // A match means the rules loaded into the engine have determined it should be blocked
            return JNI_FALSE;
        }
    }

    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_sudoplatform_sudoadtrackerblocker_blocking_adblock_AdBlockEngine_domainResolver(
    JNIEnv *jniEnv,
    jobject thiz,
    jstring jurl
) {
    JniString url(jniEnv, jurl);
    uint32_t startPosition = 0;
    uint32_t endPosition = 0;
    domainResolver(url.get(), &startPosition, &endPosition);

    std::string urlString(url.get());
    std::string domain = urlString.substr(startPosition, endPosition - startPosition);
    return jniEnv->NewStringUTF(domain.c_str());
}

} // extern C
