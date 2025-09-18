package com.smartconfig

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class SmartconfigPackage : BaseReactPackage() {
  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
    return if (name == SmartconfigModule.NAME) {
      SmartconfigModule(reactContext)
    } else {
      null
    }
  }

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
    return ReactModuleInfoProvider {
      mapOf(
        SmartconfigModule.NAME to ReactModuleInfo(
          SmartconfigModule.NAME,
          SmartconfigModule.NAME,
          false, // canOverrideExistingModule
          false, // needsEagerInit
          true,  // hasConstants ✅ (чтобы совпадало с твоей Java-версией)
          false, // isCxxModule
          true   // isTurboModule
        )
      )
    }
  }
}