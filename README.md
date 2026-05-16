# 🚌 Vidya-Vahini — Real-Time Bus Tracker for Rural Students

> *"Waze for Rural Students"* — A crowdsourced real-time bus tracking Android app that empowers rural students to share live bus location updates through a simple one-tap Ping system.

---

## 🎯 Problem Statement

Students in rural India rely entirely on a single state bus or school van to reach college. When the bus is delayed or cancelled:
- Students wait for hours at isolated stops with no information
- Female students face serious safety risks waiting alone
- Parents have no way to track their child's journey
- Existing apps like Google Maps don't cover local rural routes

---

## 💡 Solution

Vidya-Vahini is a **crowdsourced real-time bus tracker** — no GPS required on the bus. Students themselves act as sensors:

1. A student spots the bus crossing a landmark
2. They tap **PING** — one button, one tap
3. Firebase instantly updates all students on that route
4. Everyone sees the bus position and estimated arrival time
5. If no ping for 15+ minutes → **AI Delay Advisor** activates

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔔 **One-tap Ping** | Broadcasts bus position to all route subscribers instantly |
| 🗺️ **Live Route Tracker** | Visual stop-by-stop route line with color indicators |
| ⏱️ **Real-time ETA** | Calculates arrival time based on stop timing map |
| 🤖 **AI Delay Advisor** | Google Gemini API suggests alternatives when bus is late |
| 📱 **Parent SMS Alert** | Automatic SMS to parent when delay is detected |
| ✅ **Safe-Reach Button** | Student confirms safe arrival — parent gets notified |
| 🚨 **Breakdown Alert** | Notifies all students on route to seek alternatives |
| 👤 **Multi-user Support** | Firebase Auth with mobile number + PIN login |

---

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | XML Layouts + Material Design 3 |
| Architecture | MVVM (ViewModel + LiveData + Repository) |
| Backend | Firebase Realtime Database |
| Authentication | Firebase Auth (Email/Password) |
| Notifications | Firebase Cloud Messaging |
| AI Feature | Google Gemini API (Gemini 2.0 Flash) |
| HTTP Client | Retrofit |
| Local Storage | SharedPreferences |
| UI Design | Figma Make |
| Min SDK | API 21 (Android 5.0) |

---

## 📂 Project Structure

```
com.example.vidhyavahini/
├── api/
│   ├── GeminiApi.kt          # Gemini API interface
│   └── RetrofitClient.kt     # Retrofit setup
├── model/
│   └── Models.kt             # Data classes (Ping, Route)
├── ui/
│   ├── splash/               # Splash Screen
│   ├── login/                # Login (Mobile + PIN)
│   ├── onboarding/           # 3-step onboarding
│   ├── home/                 # Main screen with Ping
│   ├── routes/               # All routes list
│   ├── alerts/               # Notifications
│   ├── profile/              # Student profile
│   └── delay/                # AI Delay Advisor
├── repository/               # Data layer
└── utils/                    # Helper functions
```

---

## 🔥 Firebase Database Structure

```
vidya-vahini/
├── routes/
│   └── VV-07/
│       ├── name: "Village Road → College Gate"
│       ├── college: "Govt Degree College"
│       ├── active: true
│       └── pings/
│           └── {pingId}/
│               ├── stop: "Market"
│               ├── studentId: "device_id"
│               ├── studentName: "Sahil"
│               └── timestamp: 1234567890
└── users/
    └── {uid}/
        ├── name: "Sahil"
        ├── mobile: "9876543210"
        ├── parentContact: "9845678901"
        ├── college: "Govt Degree College"
        ├── routeCode: "VV-07"
        └── pingCount: 12
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (Latest Stable)
- Android device or emulator (API 21+)
- Firebase account
- Google AI Studio account (for Gemini API key)

### Setup

**1. Clone the repository**
```bash
git clone https://github.com/yourusername/vidya-vahini.git
cd vidya-vahini
```

**2. Firebase Setup**
- Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
- Add an Android app with package name `com.example.vidhyavahini`
- Download `google-services.json` and place it in the `app/` folder
- Enable **Realtime Database** in test mode
- Enable **Email/Password Authentication**

**3. Gemini API Key**
- Go to [aistudio.google.com](https://aistudio.google.com)
- Click **Get API Key** → Create API Key
- Open `DelayAdvisorActivity.kt` and replace:
```kotlin
private val apiKey = "YOUR_API_KEY_HERE"
```

**4. Build and Run**
```bash
# Open in Android Studio
# Sync Gradle
# Run on device or emulator
```

---

## 🎨 Color Scheme

| Color | Hex | Usage |
|---|---|---|
| Deep Blue | `#1A237E` | Primary, App Bar |
| Orange | `#FF6F00` | Accent, Ping Button |
| Green | `#2E7D32` | ETA Card, Success |
| Red | `#C62828` | Breakdown Alert |
| Light Blue | `#EEF2FF` | Background |

---

## 📊 How Ping Logic Works

```
Student sees bus → Taps PING at "Market Stop"
         ↓
Firebase writes: /routes/VV-07/pings/{id}
{stop: "Market", timestamp: currentTime}
         ↓
All students on VV-07 receive instant update
         ↓
ETA = stopTimes["College"] - stopTimes["Market"]
    = 22 - 12 = 10 minutes
         ↓
App shows "Bus arrives in ~10 mins"
```

---

## 🤖 AI Delay Advisor

When no ping is received for 15+ minutes:

```
Delay detected
     ↓
Calls Google Gemini API with prompt:
"Bus VV-07 not reported for 18 mins.
Last seen at Market Stop.
Alternatives: VV-03 via River Bank,
Shared auto from Market Stand.
Give SHORT 2 sentence suggestion."
     ↓
Gemini returns actionable suggestion
     ↓
Student sees suggestion on screen
     ↓
Automatic SMS sent to parent
```

---

## 🌍 Impact Goals

- 📚 **Educational Access** — Prevent rural student dropouts due to transport uncertainty
- 🛡️ **Safety** — Reduce time students (especially girls) spend waiting alone at isolated stops
- ⚡ **Efficiency** — Help students manage study schedules around unpredictable commutes
- 👨‍👩‍👧 **Parent Peace of Mind** — Real-time SMS alerts on delays and safe arrival

---

## 📋 App Screens

1. **Splash Screen** — App logo and tagline
2. **Login Screen** — Mobile number + 4-digit PIN
3. **Onboarding** — 3-step setup (name, mobile, PIN, college, route, parent number)
4. **Home Screen** — Live route tracker, ETA, Ping button
5. **All Routes** — Browse routes with Active/Delayed/On Time status
6. **Alerts** — Notification history
7. **Profile** — Student stats, ping count, preferences
8. **Delay Advisor** — AI-powered delay suggestions

---

## 👨‍💻 Developer

**Sahil P Amin**
- College: Canara Engineering College
- University: VTU
- Internship: MindMatrix VTU Internship Program Phase 2
- Project: 101 — Android App Development using GenAI

---

## 📄 License

This project is built as part of the MindMatrix VTU Internship Program Phase 2.

---

*Built with ❤️ for rural students across India*
