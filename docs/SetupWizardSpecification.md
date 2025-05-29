# Setup Wizard Specification

## Overview

This specification defines a multi-step setup wizard to replace the current [`WelcomeScreen`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/gui/WelcomeScreen.java:28) with a more comprehensive, user-friendly onboarding experience for Villages Reborn.

## Architecture Goals

- **Modular Design**: Each step is an independent, testable component
- **State Management**: Centralized wizard state with validation
- **Accessibility**: Full screen reader and keyboard navigation support
- **Responsive UI**: Adaptive layout for different screen sizes
- **Validation-First**: TDD approach with comprehensive test coverage

## Wizard Steps

### Step 1: Language Selection
**Purpose**: Set the user's preferred language for the mod interface

**User Inputs**:
- Language dropdown (String): Default to system locale
- Region variant (String): Optional sub-variant (e.g., en_US vs en_GB)

**Validation Rules**:
- Selected language must be supported by the mod
- Language file must exist and be readable
- Fall back to English if validation fails

**UI Components**:
- [`CyclingButtonWidget<Language>`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/gui/WelcomeScreen.java:169) for language selection
- Preview text sample showing translated content
- Language flag icons where available

### Step 2: Accessibility Settings
**Purpose**: Configure accessibility options for users with different needs

**User Inputs**:
- Enable screen reader mode (Boolean): Default false
- High contrast mode (Boolean): Default false
- GUI scale factor (Float): Range 0.5-4.0, default from system
- Animation speed (Enum): FAST, NORMAL, SLOW, DISABLED
- Sound volume level (Float): Range 0.0-1.0, default 0.8
- Enable subtitles (Boolean): Default false

**Validation Rules**:
- GUI scale must be within supported range (0.5-4.0)
- Volume level must be 0.0-1.0
- Screen reader mode requires valid accessibility services

**UI Components**:
- Toggle buttons for boolean options
- Slider widgets for numeric values
- Live preview of settings changes
- Audio test button for volume settings

### Step 3: Hardware Detection & Performance
**Purpose**: Analyze system capabilities and set performance defaults

**User Inputs**:
- Performance preset (Enum): MAXIMUM, BALANCED, BATTERY_SAVER, CUSTOM
- Max village simulation distance (Integer): Range 2-32 chunks
- Enable multithreading (Boolean): Default based on CPU cores
- Memory allocation (Integer): Range 512MB-8GB

**Validation Rules**:
- Simulation distance cannot exceed hardware capabilities
- Memory allocation cannot exceed 80% of available RAM
- Multithreading requires minimum 2 CPU cores
- Performance preset must match [`HardwareTier`](common/src/main/java/com/beeny/villagesreborn/core/hardware/HardwareTier.java)

**Auto-Detection**:
```pseudocode
FUNCTION detectOptimalSettings():
    hardwareInfo = HardwareInfoManager.getHardwareInfo()
    
    IF hardwareInfo.tier == HIGH:
        RETURN PerformanceSettings(MAXIMUM, 16, true, 4096)
    ELSE IF hardwareInfo.tier == MEDIUM:
        RETURN PerformanceSettings(BALANCED, 8, true, 2048)
    ELSE:
        RETURN PerformanceSettings(BATTERY_SAVER, 4, false, 1024)
```

### Step 4: LLM Provider Configuration
**Purpose**: Configure AI provider for villager conversations

**User Inputs**:
- Provider selection ([`LLMProvider`](common/src/main/java/com/beeny/villagesreborn/core/llm/LLMProvider.java)): OPENAI, ANTHROPIC, LOCAL, DISABLED
- Model selection (String): Filtered by provider and hardware
- API key input (String): Secure input field
- Test connection (Action): Validate provider settings

**Validation Rules**:
- API key format must match provider requirements
- Selected model must be compatible with hardware tier
- Connection test must succeed before proceeding
- Local models require sufficient disk space

**Security Requirements**:
- API keys encrypted using [`SecurityUtil`](common/src/main/java/com/beeny/villagesreborn/core/util/SecurityUtil.java)
- No key logging or display in plaintext
- Secure memory handling for sensitive data

### Step 5: Initial Biome Preference
**Purpose**: Set preferred spawn biome and village generation settings

**User Inputs**:
- Preferred biome category (Enum): TEMPERATE, COLD, HOT, MAGICAL
- Village density (Enum): SPARSE, NORMAL, DENSE
- Enable dimensional villages (Boolean): Default false
- Biome-specific settings (Map<String, Object>): Based on selection

**Validation Rules**:
- Biome category must be valid for current world type
- Village density affects performance based on hardware tier
- Dimensional villages require sufficient memory allocation

**UI Components**:
- [`BiomeSelectionWidget`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/gui/BiomeSelectionWidget.java) grid
- Preview panel showing biome characteristics
- Performance impact indicators

### Step 6: Privacy & Data Settings
**Purpose**: Configure data collection and privacy preferences

**User Inputs**:
- Enable usage analytics (Boolean): Default false
- Allow crash reporting (Boolean): Default true
- Share performance metrics (Boolean): Default false
- Participation level (Enum): NONE, MINIMAL, FULL

**Validation Rules**:
- All data sharing options must be explicitly opted-in
- Crash reporting can be enabled independently
- Privacy policy must be acknowledged

### Step 7: Summary & Confirmation
**Purpose**: Review all settings before completion

**Display Components**:
- Configuration summary table
- Estimated performance impact
- Required storage space
- Settings export/import options

**Final Actions**:
- Save configuration to [`FirstTimeSetupConfig`](common/src/main/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfig.java)
- Apply settings to current session
- Generate backup configuration file

## Navigation Controls

### Navigation State Machine
```pseudocode
ENUM WizardStep:
    LANGUAGE_SELECTION = 0
    ACCESSIBILITY_SETTINGS = 1
    HARDWARE_DETECTION = 2
    LLM_CONFIGURATION = 3
    BIOME_PREFERENCE = 4
    PRIVACY_SETTINGS = 5
    SUMMARY_CONFIRMATION = 6

CLASS WizardNavigationController:
    currentStep: WizardStep
    stepData: Map<WizardStep, StepData>
    validationResults: Map<WizardStep, ValidationResult>
    
    FUNCTION canNavigateNext() -> Boolean:
        RETURN validationResults[currentStep].isValid()
    
    FUNCTION canNavigatePrevious() -> Boolean:
        RETURN currentStep.ordinal() > 0
    
    FUNCTION navigateNext() -> Boolean:
        IF NOT canNavigateNext():
            RETURN false
        
        currentStep = WizardStep.values()[currentStep.ordinal() + 1]
        RETURN true
    
    FUNCTION navigatePrevious() -> Boolean:
        IF NOT canNavigatePrevious():
            RETURN false
            
        currentStep = WizardStep.values()[currentStep.ordinal() - 1]
        RETURN true
```

### Button Behavior
- **Next Button**: Enabled only when current step is valid
- **Previous Button**: Always enabled except on first step
- **Cancel Button**: Available on all steps, shows confirmation dialog
- **Finish Button**: Only visible on final step when all steps valid

### Keyboard Navigation
- Tab/Shift+Tab: Navigate between interactive elements
- Enter: Activate focused button or advance to next step
- Escape: Cancel wizard (with confirmation)
- Arrow Keys: Navigate within step-specific controls

## User Input Requirements & Validation

### Input Sanitization
```pseudocode
INTERFACE InputValidator<T>:
    FUNCTION validate(input: T) -> ValidationResult
    FUNCTION sanitize(input: T) -> T
    FUNCTION getErrorMessage(result: ValidationResult) -> String

CLASS StringInputValidator IMPLEMENTS InputValidator<String>:
    maxLength: Integer
    allowedPattern: Regex
    
    FUNCTION validate(input: String) -> ValidationResult:
        IF input.length() > maxLength:
            RETURN ValidationResult.error("Input too long")
        IF NOT allowedPattern.matches(input):
            RETURN ValidationResult.error("Invalid characters")
        RETURN ValidationResult.success()

CLASS NumericInputValidator<T> IMPLEMENTS InputValidator<T>:
    minValue: T
    maxValue: T
    
    FUNCTION validate(input: T) -> ValidationResult:
        IF input < minValue OR input > maxValue:
            RETURN ValidationResult.error("Value out of range")
        RETURN ValidationResult.success()
```

### Validation Framework
```pseudocode
CLASS ValidationResult:
    type: ValidationResultType (SUCCESS, WARNING, ERROR)
    message: String
    code: String
    
    FUNCTION isValid() -> Boolean:
        RETURN type != ERROR

CLASS StepValidator:
    validators: List<InputValidator>
    
    FUNCTION validateStep(stepData: StepData) -> ValidationResult:
        errors = []
        warnings = []
        
        FOR validator IN validators:
            result = validator.validate(stepData.getInput(validator.fieldName))
            IF result.type == ERROR:
                errors.add(result)
            ELSE IF result.type == WARNING:
                warnings.add(result)
        
        IF errors.isEmpty():
            RETURN ValidationResult.success()
        ELSE:
            RETURN ValidationResult.error(errors.join(", "))
```

## TDD Test Cases

### Step Transition Tests
```pseudocode
TEST_CLASS WizardNavigationTests:
    
    TEST testInitialState():
        wizard = createWizard()
        ASSERT wizard.getCurrentStep() == LANGUAGE_SELECTION
        ASSERT wizard.canNavigatePrevious() == false
        ASSERT wizard.canNavigateNext() == false  // No valid input yet
    
    TEST testValidInputEnablesNext():
        wizard = createWizard()
        wizard.setLanguage("en_US")
        wizard.validateCurrentStep()
        ASSERT wizard.canNavigateNext() == true
    
    TEST testNavigationForward():
        wizard = createWizardWithValidInputs()
        initialStep = wizard.getCurrentStep()
        
        success = wizard.navigateNext()
        ASSERT success == true
        ASSERT wizard.getCurrentStep() == expectedNextStep(initialStep)
    
    TEST testNavigationBackward():
        wizard = createWizardAtStep(ACCESSIBILITY_SETTINGS)
        
        success = wizard.navigatePrevious()
        ASSERT success == true
        ASSERT wizard.getCurrentStep() == LANGUAGE_SELECTION
    
    TEST testCannotNavigateNextWithInvalidInput():
        wizard = createWizard()
        wizard.setLanguage("invalid_locale")
        wizard.validateCurrentStep()
        
        ASSERT wizard.canNavigateNext() == false
        ASSERT wizard.navigateNext() == false
        ASSERT wizard.getCurrentStep() == LANGUAGE_SELECTION
    
    TEST testCannotNavigatePreviousFromFirstStep():
        wizard = createWizard()
        
        ASSERT wizard.canNavigatePrevious() == false
        ASSERT wizard.navigatePrevious() == false
        ASSERT wizard.getCurrentStep() == LANGUAGE_SELECTION
```

### Invalid Input Tests
```pseudocode
TEST_CLASS InputValidationTests:
    
    TEST testLanguageValidation():
        validator = createLanguageValidator()
        
        // Valid cases
        ASSERT validator.validate("en_US").isValid() == true
        ASSERT validator.validate("fr_FR").isValid() == true
        
        // Invalid cases
        ASSERT validator.validate("").isValid() == false
        ASSERT validator.validate("invalid").isValid() == false
        ASSERT validator.validate("en_INVALID").isValid() == false
    
    TEST testGUIScaleValidation():
        validator = createGUIScaleValidator()
        
        // Valid range
        ASSERT validator.validate(1.0f).isValid() == true
        ASSERT validator.validate(2.5f).isValid() == true
        
        // Invalid range
        ASSERT validator.validate(0.0f).isValid() == false
        ASSERT validator.validate(5.0f).isValid() == false
    
    TEST testMemoryAllocationValidation():
        hardwareInfo = createMockHardwareInfo(8192) // 8GB RAM
        validator = createMemoryValidator(hardwareInfo)
        
        // Valid allocation (within 80% of total)
        ASSERT validator.validate(4096).isValid() == true
        
        // Invalid allocation (exceeds limit)
        ASSERT validator.validate(8000).isValid() == false
    
    TEST testAPIKeyValidation():
        validator = createAPIKeyValidator(LLMProvider.OPENAI)
        
        // Valid format
        ASSERT validator.validate("sk-1234567890abcdef").isValid() == true
        
        // Invalid format
        ASSERT validator.validate("").isValid() == false
        ASSERT validator.validate("invalid-key").isValid() == false
```

### Cancel Flow Tests
```pseudocode
TEST_CLASS CancelFlowTests:
    
    TEST testCancelFromFirstStep():
        wizard = createWizard()
        
        result = wizard.cancel()
        ASSERT result.requiresConfirmation() == false
        ASSERT result.shouldCloseWizard() == true
    
    TEST testCancelWithUnsavedChanges():
        wizard = createWizardWithUnsavedChanges()
        
        result = wizard.cancel()
        ASSERT result.requiresConfirmation() == true
        ASSERT result.getConfirmationMessage().contains("unsaved changes")
    
    TEST testCancelConfirmationAccepted():
        wizard = createWizardWithUnsavedChanges()
        
        wizard.cancelWithConfirmation(true)
        ASSERT wizard.getState() == WizardState.CANCELLED
    
    TEST testCancelConfirmationRejected():
        wizard = createWizardWithUnsavedChanges()
        originalStep = wizard.getCurrentStep()
        
        wizard.cancelWithConfirmation(false)
        ASSERT wizard.getCurrentStep() == originalStep
        ASSERT wizard.getState() == WizardState.ACTIVE
    
    TEST testDataNotSavedOnCancel():
        wizard = createWizardWithChanges()
        originalConfig = FirstTimeSetupConfig.load()
        
        wizard.cancel()
        
        currentConfig = FirstTimeSetupConfig.load()
        ASSERT currentConfig.equals(originalConfig)
```

### State Persistence Tests
```pseudocode
TEST_CLASS StatePersistenceTests:
    
    TEST testStepDataPreservedOnNavigation():
        wizard = createWizard()
        wizard.setLanguage("fr_FR")
        wizard.navigateNext()
        wizard.setHighContrastMode(true)
        wizard.navigatePrevious()
        
        ASSERT wizard.getLanguage() == "fr_FR"
    
    TEST testValidationStatePreservedOnNavigation():
        wizard = createWizard()
        wizard.setLanguage("invalid")
        validationResult = wizard.validateCurrentStep()
        wizard.navigateNext() // Should fail
        
        ASSERT wizard.getCurrentStep() == LANGUAGE_SELECTION
        ASSERT wizard.getLastValidationResult().isValid() == false
    
    TEST testConfigurationSavedOnFinish():
        wizard = createCompleteWizard()
        
        wizard.finish()
        
        config = FirstTimeSetupConfig.load()
        ASSERT config.isSetupCompleted() == true
        ASSERT config.getSelectedProvider() == wizard.getLLMProvider()
        ASSERT config.getSelectedModel() == wizard.getLLMModel()
```

## Implementation Modules

### Core Components
```pseudocode
// Core wizard management
CLASS SetupWizard:
    - WizardNavigationController navigationController
    - Map<WizardStep, AbstractWizardStep> steps
    - WizardState currentState
    - FirstTimeSetupConfig targetConfig

// Individual step implementations
ABSTRACT CLASS AbstractWizardStep:
    - StepValidator validator
    - StepData data
    - ABSTRACT FUNCTION render(DrawContext, int, int, float)
    - ABSTRACT FUNCTION handleInput(InputEvent)
    - ABSTRACT FUNCTION validate() -> ValidationResult

// Specialized step implementations
CLASS LanguageSelectionStep EXTENDS AbstractWizardStep
CLASS AccessibilitySettingsStep EXTENDS AbstractWizardStep
CLASS HardwareDetectionStep EXTENDS AbstractWizardStep
CLASS LLMConfigurationStep EXTENDS AbstractWizardStep
CLASS BiomePreferenceStep EXTENDS AbstractWizardStep
CLASS PrivacySettingsStep EXTENDS AbstractWizardStep
CLASS SummaryConfirmationStep EXTENDS AbstractWizardStep
```

### UI Framework Integration
```pseudocode
// Main wizard screen
CLASS SetupWizardScreen EXTENDS Screen:
    - SetupWizard wizard
    - WizardProgressBar progressBar
    - WizardNavigationPanel navigationPanel
    - WizardContentPanel contentPanel

// Navigation components
CLASS WizardNavigationPanel:
    - ButtonWidget previousButton
    - ButtonWidget nextButton
    - ButtonWidget cancelButton
    - ButtonWidget finishButton

// Progress indicator
CLASS WizardProgressBar:
    - int totalSteps
    - int currentStep
    - Map<WizardStep, String> stepTitles
```

### Configuration Management
```pseudocode
// Enhanced setup config
CLASS EnhancedFirstTimeSetupConfig EXTENDS FirstTimeSetupConfig:
    - LanguagePreferences languagePrefs
    - AccessibilitySettings accessibilitySettings
    - PerformanceSettings performanceSettings
    - BiomePreferences biomePrefs
    - PrivacySettings privacySettings
    
    FUNCTION saveWizardConfiguration(WizardData) -> Boolean
    FUNCTION loadWizardConfiguration() -> WizardData
    FUNCTION validateConfiguration() -> ValidationResult
```

## Error Handling & Recovery

### Error Categories
```pseudocode
ENUM WizardErrorType:
    VALIDATION_ERROR,    // User input validation failed
    SYSTEM_ERROR,        // Hardware detection or system issues  
    NETWORK_ERROR,       // LLM provider connection issues
    PERSISTENCE_ERROR,   // Configuration save/load failures
    UI_ERROR            // Rendering or interaction problems

CLASS WizardErrorHandler:
    FUNCTION handleError(error: WizardError) -> ErrorRecoveryAction
    FUNCTION showErrorDialog(error: WizardError) -> UserChoice
    FUNCTION attemptRecovery(recovery: ErrorRecoveryAction) -> Boolean
```

### Recovery Strategies
```pseudocode
CLASS ErrorRecoveryStrategies:
    
    FUNCTION recoverFromValidationError(step: WizardStep, error: ValidationError):
        // Highlight invalid fields
        // Show specific error messages
        // Provide suggested corrections
        // Keep user on current step
    
    FUNCTION recoverFromNetworkError(provider: LLMProvider, error: NetworkError):
        // Offer retry with exponential backoff
        // Suggest alternative providers
        // Allow offline mode configuration
        // Skip LLM setup temporarily
    
    FUNCTION recoverFromPersistenceError(config: SetupConfig, error: PersistenceError):
        // Attempt backup location
        // Offer manual configuration export
        // Retry with elevated permissions
        // Fallback to memory-only mode
```

## Performance Considerations

### Memory Management
- Lazy initialization of wizard steps
- Dispose unused UI components when navigating
- Stream large language files instead of loading entirely
- Limit concurrent hardware detection operations

### Rendering Optimization
```pseudocode
CLASS WizardRenderingOptimizer:
    renderCache: Map<WizardStep, CachedRenderData>
    
    FUNCTION shouldRerender(step: WizardStep) -> Boolean:
        cached = renderCache.get(step)
        RETURN cached == null OR cached.isStale() OR step.hasChanged()
    
    FUNCTION optimizeStepRendering(step: WizardStep):
        // Cache static elements
        // Use dirty rectangles for partial updates
        // Batch widget updates
        // Defer expensive calculations
```

### Background Operations
```pseudocode
CLASS BackgroundTaskManager:
    FUNCTION startHardwareDetection() -> Future<HardwareInfo>
    FUNCTION validateLLMConnection(provider, apiKey) -> Future<ConnectionResult>
    FUNCTION loadLanguageResources(locale) -> Future<LanguageBundle>
    
    // All background tasks should be cancellable and report progress
```

This specification provides a comprehensive foundation for implementing a robust, accessible, and well-tested setup wizard that significantly improves upon the current single-screen welcome experience.