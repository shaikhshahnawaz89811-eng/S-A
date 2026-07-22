# SA Companion — Personal AI Voice Assistant

A futuristic Android AI voice assistant that works like a smart operating system assistant.

## Features

- 🎤 **Voice First** — Wake word "SA" + continuous voice pipeline
- 🤖 **Groq AI Brain** — Llama 3.3 70B for natural conversation
- 🗣️ **Hindi/Hinglish Support** — Responds in Hindi or Hinglish naturally
- 📱 **Phone Control** — Battery, volume, torch, camera, brightness, app launcher
- 🎵 **Smart Audio Routing** — Music on speaker, SA voice on earbuds
- 💾 **Local Memory** — SQLite/Room database for conversations, facts, preferences
- 👨‍👩‍👧 **Family Profiles** — Owner/Family/Guest access levels
- 🌊 **3D Futuristic UI** — Holographic design with animated AI orb
- 🪟 **Floating Window** — Always-on-top overlay assistant
- 🔄 **Background Service** — Works with screen off, starts on boot

## Project Structure

```
SA-Companion/
├── .github/workflows/build.yml    # GitHub Actions CI/CD
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/sacompanion/
│       │   ├── SAApplication.kt
│       │   ├── MainActivity.kt
│       │   ├── core/
│       │   │   ├── ai/GroqAIClient.kt        # Groq REST API client
│       │   │   ├── voice/VoiceManager.kt     # Speech recognition
│       │   │   ├── voice/WakeWordDetector.kt # Wake word detection
│       │   │   ├── tts/TTSManager.kt         # TTS (Android/Piper/Coqui)
│       │   │   └── memory/MemoryManager.kt   # Memory & context
│       │   ├── control/
│       │   │   ├── phone/PhoneController.kt  # Battery, volume, torch, etc.
│       │   │   └── media/MediaController.kt  # Music + audio routing
│       │   ├── service/background/
│       │   │   ├── SAAssistantService.kt     # Main foreground service
│       │   │   ├── FloatingWindowService.kt  # Overlay floating window
│       │   │   └── BootReceiver.kt           # Auto-start on boot
│       │   ├── database/
│       │   │   ├── SADatabase.kt             # Room database
│       │   │   ├── entities/Entities.kt      # DB entities
│       │   │   └── dao/*.kt                  # DAOs
│       │   └── ui/
│       │       ├── SAApp.kt                  # Navigation
│       │       ├── theme/                    # Futuristic color theme
│       │       ├── screens/                  # All screens
│       │       └── viewmodel/MainViewModel.kt
│       └── res/
├── gradle/
│   ├── libs.versions.toml         # Version catalog
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew / gradlew.bat
└── README.md
```

## Setup

### 1. Get a Groq API Key

1. Go to [console.groq.com](https://console.groq.com)
2. Create a free account
3. Generate an API key
4. Add it in SA Settings, or set `GROQ_API_KEY` environment variable

### 2. Build Locally

Requirements: Android Studio Ladybug or later, JDK 17

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore)
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/`

### 3. Open in Android Studio

1. Open Android Studio
2. File → Open → select `SA-Companion/` folder
3. Wait for Gradle sync
4. Build → Build APK

### 4. GitHub Actions CI/CD

Push to GitHub — the workflow automatically builds debug + release APKs.

#### Setup GitHub Secrets (for signed release):

| Secret | Description |
|--------|-------------|
| `GROQ_API_KEY` | Your Groq API key |
| `KEYSTORE_BASE64` | Base64-encoded release keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

#### Generate a release keystore:

```bash
keytool -genkeypair \
  -v \
  -storetype PKCS12 \
  -keystore release.jks \
  -alias sa-companion \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Encode for GitHub secrets:
base64 -i release.jks | pbcopy   # macOS
base64 release.jks | xclip       # Linux
```

### 5. First Launch

1. Install APK on Android 8.0+ device
2. Grant permissions: Microphone, Overlay, Phone State
3. Go to Settings → Enter Groq API Key
4. Tap "Start Assistant" on Home screen
5. Say **"SA"** to activate
6. Give commands: "SA battery bata", "SA torch on", "SA music chala"

## Voice Commands

| Command | Action |
|---------|--------|
| `SA battery bata` | Check battery level |
| `SA time kya hai` | Current time |
| `SA torch on/off` | Toggle flashlight |
| `SA volume badha do` | Increase volume |
| `SA music chala` | Open music player |
| `SA camera kholo` | Open camera |
| `SA settings kholo` | Open settings |
| `SA [any question]` | Ask AI anything |

## Requirements

- Android 8.0+ (API 26)
- Microphone permission
- Internet for Groq AI
- System Alert Window for floating overlay

## Architecture

```
Voice Pipeline:
  Microphone → SpeechRecognizer → WakeWordDetector → CommandRouter
      → PhoneController / MediaController / GroqAIClient
      → TTSManager → Speaker

Services:
  SAAssistantService (Foreground) — Main voice loop
  FloatingWindowService — Overlay window
  BootReceiver — Auto-start

Database (Room/SQLite):
  conversations → Conversation history
  memories → Facts SA knows
  user_preferences → Settings
  family_profiles → Family access control
  command_history → Log
```

## Tech Stack

- Kotlin + Jetpack Compose
- Groq AI (Llama 3.3 70B)
- Android SpeechRecognizer + TTS
- Room + SQLite
- ExoPlayer / Media3
- Retrofit + OkHttp
- GitHub Actions

---

*SA Companion — Smart. Fast. Personal.*
