# EWasteTriage

**EWasteTriage** is an offline, on-device Android application designed to spot and identify e-waste items using a TensorFlow Lite object detector, evaluate hazard levels, and present safety handling protocols and recycling guidance.

Built with **Kotlin**, **Jetpack Compose**, **CameraX**, and **TensorFlow Lite (LiteRT)**, running 100% offline without requiring internet permissions.

---

## Features

- **Live Camera Scanning:** Real-time object detection using CameraX and on-device TFLite inference.
- **Photo Gallery Analysis:** Pick photos from your gallery to inspect e-waste items without using the live camera.
- **Triage & Hazard Severity:** Evaluates items into confidence tiers and highlights hazards (Fire, Toxic, High Voltage, Broken Glass, Corrosive) paired with clear severity badges (High, Medium, Low).
- **Curated Safety Guidance:** Actionable safety protocols, required PPE (gloves, eye protection, respirator), and recovery potential for valuable components.
- **Scan History:** Locally persist past scans with swipe-to-dismiss and undo capability.
- **100% Offline Operation:** No internet permission, zero tracking, bundled model and curated guidance.
- **Adaptive UI:** Full Material 3 design supporting both dynamic Light and Dark themes.

---

## Supported Categories

| Category | Typical Hazard | Recovery Potential |
|---|---|---|
| **Smartphone** | Li-ion battery puncture/fire | Gold, silver, copper, rare earths |
| **Laptop** | Swollen Li-ion battery, mercury backlights | Motherboard metals, RAM, aluminum |
| **CRT Monitor / TV** | High-voltage capacitors, leaded glass, vacuum implosion | Copper yoke, scrap glass |
| **Flat-panel Screen** | Mercury CCFL tubes, sharp glass | Indium tin oxide, aluminum housing |
| **Lithium-ion Battery** | Thermal runaway, chemical fire | Cobalt, lithium, nickel |
| **Cylindrical / Alkaline Battery** | Potassium hydroxide electrolyte leakage | Zinc, manganese, steel |

---

## Tech Stack & Architecture

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Camera:** CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`)
- **Machine Learning:** TensorFlow Lite / LiteRT (`tensorflow-lite`, `tensorflow-lite-support`)
- **Min SDK:** 24 (Android 7.0 Nougat)
- **Target SDK:** 34 (Android 14)

---

## Getting Started

1. Clone or download the repository.
2. Open the project in **Android Studio** (Hedgehog or newer recommended).
3. Let Gradle sync project dependencies.
4. Connect an Android device (API 24+) with **USB debugging** enabled (or start an emulator).
5. Click **Run** (`Shift + F10`) or install via terminal:
   ```bash
   ./gradlew installDebug
   ```

---

## License

This project is open-source. See the repository for further license information.
