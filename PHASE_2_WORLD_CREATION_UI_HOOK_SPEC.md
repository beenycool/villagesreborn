# Phase 2: World Creation UI Hook Specification

## 1. CreateWorldScreenMixin Injection
```java
// fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/mixin/client/CreateWorldScreenMixin.java
@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void injectVillagesRebornTab(CallbackInfo ci) {
        // Implementation details...
    }
    
    @Redirect(method = "keyPressed", at = @At("INVOKE"))
    private void handleTabNavigation(int keyCode) {
        // Tab navigation logic...
    }
}
```

## 2. VillagesRebornTab UI Components
```java
// fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/gui/world/VillagesRebornTab.java
public class VillagesRebornTab extends AbstractWidget {
    private final FeatureToggleSection featureToggles;
    private final PerformanceSection performanceSettings;
    
    public VillagesRebornTab(CreateWorldScreen parent) {
        // UI initialization...
    }
    
    private void validateInputs() {
        // Validation logic...
    }
}
```

## 3. Extended Settings Model
```java
// common/src/main/java/com/beeny/villagesreborn/core/world/VillagesRebornWorldSettings.java
public class VillagesRebornWorldSettings {
    // New Phase 2 fields
    private boolean biomeSpecificExpansion;
    private int maxCaravanDistance;
    
    // Updated serialization methods...
}
```

## 4. Settings Transfer Flow
```java
// fabric/src/main/java/com/beeny/villagesreborn/platform/fabric/event/WorldCreationEventHandler.java
public class WorldCreationEventHandler {
    @Subscribe
    public void onScreenClose(CreateWorldScreen.CloseEvent event) {
        // Settings capture logic...
    }
    
    @Subscribe 
    public void onWorldLoad(ServerWorld.LoadEvent event) {
        // Settings application logic...
    }
}
```

## 5. TDD Implementation Plan

### Test Cases
1. `CreateWorldScreenMixinTest`
   - Verify tab injection order
   - Test keyboard navigation
   - Validate tab persistence

2. `VillagesRebornTabTest`  
   - Test UI control bindings
   - Validate input ranges
   - Verify dependency validation

3. `WorldSettingsSerializationTest`
   - Round-trip serialization
   - Version migration
   - Custom data persistence

4. `IntegrationTestSuite`
   - Full UI-to-gameplay flow
   - Multiplayer sync
   - Mod compatibility