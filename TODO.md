# Zentra Project Roadmap

## 🎯 Current Focus: Mindfulness & Automation
- [x] **Mindfulness Check-ins**
    - Notifications for long sessions on distracting apps.
    - Prompt short reflection/break suggestions via system notifications.
- [ ] **Downtime Automation**
    - Scheduled greyscale mode or DND during late-night hours.
    - User-defined "Quiet Hours" configuration.

## 🎨 UI/UX Modernization
- [x] **Navigation Architecture**
    - Implement a Bottom Navigation Bar for easy access to Dashboard, Insights, and Settings.
- [ ] **Onboarding Flow**
    - Create a multi-step guided tour explaining the Focus Score and app categorization.
- [ ] **Settings Dashboard**
    - Dedicated screen to manage app categories (Productive vs. Distracting).
    - Toggle for Dynamic Color (Material You) support.
- [ ] **Enhanced Visualizations**
    - Replace static text trends with interactive Sparklines or Bar Charts for weekly metrics.
    - Add Lottie animations for "Focus Session Complete" and "Daily Goal Reached" states.
- [ ] **Responsive Layouts**
    - Optimize the card-based UI for tablets and foldable devices using WindowSizeClasses.

## ⚙️ Advanced Logic & Customization
- [ ] **Custom App Categorization**
    - Allow users to manually override app categories.
    - Implementation of an "App Picker" with search and filter capabilities.
- [ ] **Configurable Scoring Model**
    - Slider-based weights for Productive/Distracting penalties.
    - Option to set a custom Daily Focus Goal (current default: 80).
- [ ] **Focus Session History**
    - Persistent storage of past sessions using Room database.
    - "Session Streaks" to encourage daily deep work.

## ✅ Completed Tasks
- [x] Initial MVP with UsageStats integration.
- [x] Weighted Scoring Engine (Productive/Neutral/Distracting).
- [x] Basic Zen Mode (Pomodoro Timer).
- [x] Weekly Trends (7-day average & total usage).
- [x] Material 3 Dark Mode Dashboard.
- [x] Daily Goal Progress Indicator.
- [x] Mindfulness Check-ins (Periodic WorkManager notifications).
- [x] Navigation Architecture (Bottom Navigation Bar).
