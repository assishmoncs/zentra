# Zentra

Zentra is a modern Android productivity companion designed to foster mindful digital usage through data-driven insights and focused work sessions. Built with Material 3, it transforms device usage statistics into a meaningful **Focus Score**, helping users reclaim their time.

## 🚀 Key Features

-   **Intelligent Focus Scoring**: A weighted scoring model that distinguishes between Productive and Distracting apps.
-   **Zen Mode (Focus Sessions)**: Built-in Pomodoro-style timer (25-minute default) to facilitate deep work.
-   **Weekly Trends**: Comprehensive 7-day analytics showing focus patterns and screen time metrics.
-   **Daily Goals**: Set and track personalized daily focus targets (Default: 80 points).
-   **Real-time Insights**: View total screen time and top-used apps with high-precision tracking.

## 🧠 Core Philosophy

Zentra operates on a weighted usage model to calculate your **Focus Score (0-100)**:

-   **Productive Apps**: Low impact on score (20% weight).
-   **Neutral Apps**: Standard impact (100% weight).
-   **Distracting Apps**: High impact (200% weight).

The score encourages a balanced digital diet by penalizing time spent on distractions more heavily than time spent on tools for growth and work.

## 🛠 Tech Stack

-   **Language**: Kotlin
-   **Architecture**: MVP (Model-View-Presenter)
-   **UI**: XML with Material 3 Design Components
-   **Data**: Android UsageStatsManager API

## 📂 Project Structure

```text
app/src/main/java/com/hsissa/zentra/
├── core/        # Scoring logic and session management
├── service/     # Usage statistics and system integration
├── ui/          # View logic and Material 3 implementations
└── util/        # Formatting and helper utilities
```

## 📥 Getting Started

1.  **Clone the repository**: `git clone https://github.com/hsissa/zentra.git`
2.  **Open in Android Studio**: Sync Gradle and build the project.
3.  **Deploy**: Run on an Android 8.0+ (API 26) device. (Current Version: 2.0)
4.  **Permissions**: Grant **Usage Access** when prompted to enable data tracking.

## 🧪 Testing

The core scoring logic and utility functions are backed by unit tests:

```bash
./gradlew test
```

## 📜 License

Distributed under the [MIT License](./LICENSE).
