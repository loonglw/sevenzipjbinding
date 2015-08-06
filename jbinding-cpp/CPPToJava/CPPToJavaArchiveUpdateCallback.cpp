#include "SevenZipJBinding.h"

#include "JNITools.h"
#include "CPPToJavaArchiveUpdateCallback.h"
#include "CPPToJavaSequentialInStream.h"
#include "CPPToJavaInStream.h"
#include "UnicodeHelper.h"
#include "CodecTools.h"


LONG CPPToJavaArchiveUpdateCallback::freeResourcesForOutItem(JNIEnvInstance & jniEnvInstance) {
    if (_outItem) {
        _iOutCreateCallback->freeResources(jniEnvInstance, _javaImplementation, _outItemLastIndex, _outItem);
        if (jniEnvInstance.exceptionCheck()) {
            return S_FALSE;
        }
        jniEnvInstance->DeleteGlobalRef(_outItem);
        _outItem = NULL;
    }

    return S_OK;
}

LONG CPPToJavaArchiveUpdateCallback::getOrUpdateOutItem(JNIEnvInstance & jniEnvInstance, int index) {
    if (_outItemLastIndex == index && _outItem) {
        return S_OK;
    }

    freeResourcesForOutItem(jniEnvInstance);

    jobject outItemFactory = jni::OutItemFactory::newInstance(jniEnvInstance, _outArchive, index);
    if (jniEnvInstance.exceptionCheck()) {
        return S_FALSE;
    }

    jobject outItem = _iOutCreateCallback->getItemInformation(jniEnvInstance, _javaImplementation, index, outItemFactory);
    if (jniEnvInstance.exceptionCheck()) {
        return S_FALSE;
    }
    if (outItem == NULL) {
        jniEnvInstance.reportError("IOutCreateCallback.getItemInformation() should return "
                "a non-null reference to an item information object. Use outItemFactory to create an instance. "
                "Fill the new object with all necessary information about the archive item being processed.");
        return S_FALSE;
    }

    _outItem = jniEnvInstance->NewGlobalRef(outItem);
    jniEnvInstance->DeleteLocalRef(outItem);
    _outItemLastIndex = index;

    return S_OK;
}


STDMETHODIMP CPPToJavaArchiveUpdateCallback::GetUpdateItemInfo(UInt32 index, Int32 *newData, /*1 - new data, 0 - old data */
Int32 *newProperties, /* 1 - new properties, 0 - old properties */
UInt32 *indexInArchive /* -1 if there is no in archive, or if doesn't matter */
) {
    TRACE_OBJECT_CALL("GetUpdateItemInfo");

    JNIEnvInstance jniEnvInstance(_jbindingSession);

    LONG result = getOrUpdateOutItem(jniEnvInstance, index);
    if (result) {
        return result;
    }

    if (newData) {
        if (_isInArchiveAttached) {
            jobject newDataObject = jni::OutItem::updateIsNewData_Get(jniEnvInstance, _outItem);
            if (newDataObject) {
                *newData = jni::Boolean::booleanValue(jniEnvInstance, newDataObject);
                if (jniEnvInstance.exceptionCheck()) {
                    return S_FALSE;
                }
            } else {
                jniEnvInstance.reportError("The attribute 'updateNewData' of the corresponding IOutItem* class shouldn't be null (index=%i)", index);
                return S_FALSE;
            }
        } else {
            *newData = 1;
        }
    }

    if (newProperties) {
        if (_isInArchiveAttached) {
            jobject newPropertiesObject = jni::OutItem::updateIsNewProperties_Get(jniEnvInstance, _outItem);
            if (newPropertiesObject) {
                *newProperties = jni::Boolean::booleanValue(jniEnvInstance, newPropertiesObject);
                if (jniEnvInstance.exceptionCheck()) {
                    return S_FALSE;
                }
            } else {
                jniEnvInstance.reportError("The attribute 'updateNewProperties' of the corresponding IOutItem* class shouldn't be null (index=%i)", index);
                return S_FALSE;
            }
        } else {
            *newProperties = 1;
        }
    }

    if (indexInArchive) {
        if (_isInArchiveAttached) {
            jobject oldArchiveItemIndexObject = jni::OutItem::updateOldArchiveItemIndex_Get(jniEnvInstance, _outItem);
            if (oldArchiveItemIndexObject) {
                *indexInArchive = (UInt32) jni::Integer::intValue(jniEnvInstance, oldArchiveItemIndexObject);
                if (jniEnvInstance.exceptionCheck()) {
                    return S_FALSE;
                }
            } else {
                *indexInArchive = (UInt32) -1;
            }
        } else {
            *indexInArchive = (UInt32) -1;
        }
    }

    return S_OK;
}

STDMETHODIMP CPPToJavaArchiveUpdateCallback::GetProperty(UInt32 index, PROPID propID,
                                                         PROPVARIANT *value) {

//	#define JNI_TYPE_STRING                              jstring
//	#define JNI_TYPE_INTEGER                             jobject
//	#define JNI_TYPE_UINTEGER                            jobject
//	#define JNI_TYPE_DATE                                jobject
//	#define JNI_TYPE_BOOLEAN                             jboolean
//	#define JNI_TYPE_LONG                                jlong

	#define ASSIGN_VALUE_TO_C_PROP_VARIANT_STRING                                                                   \
        const jchar * jChars = jniEnvInstance->GetStringChars((jstring)value, NULL);                                \
        cPropVariant = UString(UnicodeHelper(jChars));                                                              \
        jniEnvInstance->ReleaseStringChars((jstring)value, jChars);                                                 \

	#define ASSIGN_VALUE_TO_C_PROP_VARIANT_INTEGER                                                                  \
        cPropVariant = jni::Integer::intValue(jniEnvInstance, value);                                               \
        if (jniEnvInstance.exceptionCheck()) {                                                                      \
            return S_FALSE;                                                                                         \
        }                                                                                                           \

    #define ASSIGN_VALUE_TO_C_PROP_VARIANT_BOOLEAN                                                                  \
        cPropVariant = (bool)jni::Boolean::booleanValue(jniEnvInstance, value);                                     \
        if (jniEnvInstance.exceptionCheck()) {                                                                      \
            return S_FALSE;                                                                                         \
        }                                                                                                           \

    #define ASSIGN_VALUE_TO_C_PROP_VARIANT_LONG                                                                     \
        cPropVariant = (UInt64)jni::Long::longValue(jniEnvInstance, value);                                           \
        if (jniEnvInstance.exceptionCheck()) {                                                                      \
            return S_FALSE;                                                                                         \
        }                                                                                                           \

    #define ASSIGN_VALUE_TO_C_PROP_VARIANT_UINTEGER                                                                 \
        cPropVariant = (unsigned int)jni::Integer::intValue(jniEnvInstance, value);                                 \
        if (jniEnvInstance.exceptionCheck()) {                                                                      \
            return S_FALSE;                                                                                         \
        }                                                                                                           \

	#define ASSIGN_VALUE_TO_C_PROP_VARIANT_DATE                                                                     \
        FILETIME filetime;                                                                                          \
        if (!ObjectToFILETIME(jniEnvInstance, value, filetime)) {                                                   \
            return S_FALSE;                                                                                         \
        }                                                                                                           \
        cPropVariant = filetime;                                                                                    \

	#define GET_ATTRIBUTE(TYPE, fieldName)                                                                          \
	{                                                                                                               \
	    jobject value = jni::OutItem::fieldName##_Get(jniEnvInstance, _outItem);                                    \
	    if (value) {                                                                                                \
            ASSIGN_VALUE_TO_C_PROP_VARIANT_##TYPE                                                                   \
        }                                                                                                           \
		break;                                                                                                      \
	}

    TRACE_OBJECT_CALL("GetProperty");
    JNIEnvInstance jniEnvInstance(_jbindingSession);

    if (!value) {
        return S_OK;
    }

    value->vt = VT_NULL;
    NWindows::NCOM::CPropVariant cPropVariant;

    if (propID == kpidIsDir
            && (codecTools.isGZipArchive(_archiveFormatIndex)
                    || codecTools.isBZip2Archive(_archiveFormatIndex))) {
        cPropVariant = false;
        cPropVariant.Detach(value);
        return S_OK;
    }

    if (propID == kpidTimeType) {
        cPropVariant = NFileTimeType::kWindows;
        cPropVariant.Detach(value);
        return S_OK;
    }

    LONG result = getOrUpdateOutItem(jniEnvInstance, index);
    if (result) {
        return result;
    }

    switch (propID) {
    case kpidAttrib:             GET_ATTRIBUTE(UINTEGER, propertyAttributes)
    case kpidPosixAttrib:        GET_ATTRIBUTE(UINTEGER, propertyPosixAttributes)
    case kpidPath:               GET_ATTRIBUTE(STRING,   propertyPath)
    case kpidIsDir:              GET_ATTRIBUTE(BOOLEAN,  propertyIsDir)
    case kpidIsAnti:             GET_ATTRIBUTE(BOOLEAN,  propertyIsAnti)
    case kpidMTime:              GET_ATTRIBUTE(DATE,     propertyLastModificationTime)
    case kpidATime:              GET_ATTRIBUTE(DATE,     propertyLastAccessTime)
    case kpidCTime:              GET_ATTRIBUTE(DATE,     propertyCreationTime)
    case kpidSize:               GET_ATTRIBUTE(LONG,     propertySize)
    case kpidUser:               GET_ATTRIBUTE(STRING,   propertyUser)
    case kpidGroup:              GET_ATTRIBUTE(STRING,   propertyGroup)

    case kpidTimeType: // Should be processed by now
    default:
#ifdef _DEBUG
    	printf("kpidNoProperty: %i\n", (int) kpidNoProperty);
    	printf("kpidMainSubfile: %i\n", (int) kpidMainSubfile);
    	printf("kpidHandlerItemIndex: %i\n", (int) kpidHandlerItemIndex);
    	printf("kpidPath: %i\n", (int) kpidPath);
    	printf("kpidName: %i\n", (int) kpidName);
    	printf("kpidExtension: %i\n", (int) kpidExtension);
    	printf("kpidIsDir: %i\n", (int) kpidIsDir);
    	printf("kpidSize: %i\n", (int) kpidSize);
    	printf("kpidPackSize: %i\n", (int) kpidPackSize);
    	printf("kpidAttrib: %i\n", (int) kpidAttrib);
    	printf("kpidCTime: %i\n", (int) kpidCTime);
    	printf("kpidATime: %i\n", (int) kpidATime);
    	printf("kpidMTime: %i\n", (int) kpidMTime);
    	printf("kpidSolid: %i\n", (int) kpidSolid);
    	printf("kpidCommented: %i\n", (int) kpidCommented);
    	printf("kpidEncrypted: %i\n", (int) kpidEncrypted);
    	printf("kpidSplitBefore: %i\n", (int) kpidSplitBefore);
    	printf("kpidSplitAfter: %i\n", (int) kpidSplitAfter);
    	printf("kpidDictionarySize: %i\n", (int) kpidDictionarySize);
    	printf("kpidCRC: %i\n", (int) kpidCRC);
    	printf("kpidType: %i\n", (int) kpidType);
    	printf("kpidIsAnti: %i\n", (int) kpidIsAnti);
    	printf("kpidMethod: %i\n", (int) kpidMethod);
    	printf("kpidHostOS: %i\n", (int) kpidHostOS);
    	printf("kpidFileSystem: %i\n", (int) kpidFileSystem);
    	printf("kpidUser: %i\n", (int) kpidUser);
    	printf("kpidGroup: %i\n", (int) kpidGroup);
    	printf("kpidBlock: %i\n", (int) kpidBlock);
    	printf("kpidComment: %i\n", (int) kpidComment);
    	printf("kpidPosition: %i\n", (int) kpidPosition);
    	printf("kpidPrefix: %i\n", (int) kpidPrefix);
    	printf("kpidNumSubDirs: %i\n", (int) kpidNumSubDirs);
    	printf("kpidNumSubFiles: %i\n", (int) kpidNumSubFiles);
    	printf("kpidUnpackVer: %i\n", (int) kpidUnpackVer);
    	printf("kpidVolume: %i\n", (int) kpidVolume);
    	printf("kpidIsVolume: %i\n", (int) kpidIsVolume);
    	printf("kpidOffset: %i\n", (int) kpidOffset);
    	printf("kpidLinks: %i\n", (int) kpidLinks);
    	printf("kpidNumBlocks: %i\n", (int) kpidNumBlocks);
    	printf("kpidNumVolumes: %i\n", (int) kpidNumVolumes);
    	printf("kpidTimeType: %i\n", (int) kpidTimeType);
    	printf("kpidBit64: %i\n", (int) kpidBit64);
    	printf("kpidBigEndian: %i\n", (int) kpidBigEndian);
    	printf("kpidCpu: %i\n", (int) kpidCpu);
    	printf("kpidPhySize: %i\n", (int) kpidPhySize);
    	printf("kpidHeadersSize: %i\n", (int) kpidHeadersSize);
    	printf("kpidChecksum: %i\n", (int) kpidChecksum);
    	printf("kpidCharacts: %i\n", (int) kpidCharacts);
    	printf("kpidVa: %i\n", (int) kpidVa);
    	printf("kpidId: %i\n", (int) kpidId);
    	printf("kpidShortName: %i\n", (int) kpidShortName);
    	printf("kpidCreatorApp: %i\n", (int) kpidCreatorApp);
    	printf("kpidSectorSize: %i\n", (int) kpidSectorSize);
    	printf("kpidPosixAttrib: %i\n", (int) kpidPosixAttrib);
    	printf("kpidLink: %i\n", (int) kpidLink);
    	printf("kpidTotalSize: %i\n", (int) kpidTotalSize);
    	printf("kpidFreeSpace: %i\n", (int) kpidFreeSpace);
    	printf("kpidClusterSize: %i\n", (int) kpidClusterSize);
    	printf("kpidVolumeName: %i\n", (int) kpidVolumeName);
    	printf("kpidLocalName: %i\n", (int) kpidLocalName);
    	printf("kpidProvider: %i\n", (int) kpidProvider);
    	printf("kpidUserDefined: %i\n", (int) kpidUserDefined);
#endif // _DEBUG

    	jniEnvInstance.reportError("CPPToJavaArchiveUpdateCallback::GetProperty() : unexpected propID=%u", propID);
    	return S_FALSE;
    }

    cPropVariant.Detach(value);

    return S_OK;
}

STDMETHODIMP CPPToJavaArchiveUpdateCallback::GetStream(UInt32 index, ISequentialInStream **inStream) {
    TRACE_OBJECT_CALL("GetStream");
    JNIEnvInstance jniEnvInstance(_jbindingSession);

    if (!inStream) {
        return S_OK;
    }

    LONG result = getOrUpdateOutItem(jniEnvInstance, index);
    if (result) {
        return result;
    }

    jobject inStreamImpl = jni::OutItem::dataStream_Get(jniEnvInstance, _outItem);
    if (!inStreamImpl) {
        jniEnvInstance.reportError("The attribute 'dataStream' of the corresponding IOutItem* class shouldn't be null (index=%i)", index);
        return S_FALSE;
    }

    if (inStreamImpl) {

        jclass inStreamInterface = jniEnvInstance->FindClass(INSTREAM_CLASS);
        FATALIF(!inStreamInterface, "Class " INSTREAM_CLASS " not found");

        if (jniEnvInstance->IsInstanceOf(inStreamImpl, inStreamInterface)) {
            CPPToJavaInStream * newInStream = new CPPToJavaInStream(_jbindingSession, jniEnvInstance, inStreamImpl);
            CMyComPtr<IInStream> inStreamComPtr = newInStream;
            *inStream = inStreamComPtr.Detach();
        } else {
            CPPToJavaSequentialInStream * newInStream = new CPPToJavaSequentialInStream(
                    _jbindingSession, jniEnvInstance, inStreamImpl);
            CMyComPtr<ISequentialInStream> inStreamComPtr = newInStream;
            *inStream = inStreamComPtr.Detach();
        }
    } else {
        return S_FALSE;
    }

    return S_OK;
}

STDMETHODIMP CPPToJavaArchiveUpdateCallback::SetOperationResult(Int32 operationResult) {
    TRACE_OBJECT_CALL("SetOperationResult");
    JNIEnvInstance jniEnvInstance(_jbindingSession);

    jboolean operationResultBoolean = (operationResult == NArchive::NUpdate::NOperationResult::kOK);

    _iOutCreateCallback->setOperationResult(jniEnvInstance, _javaImplementation,
            operationResultBoolean);
    if (jniEnvInstance.exceptionCheck()) {
        return S_FALSE;
    }

    return S_OK;
}
