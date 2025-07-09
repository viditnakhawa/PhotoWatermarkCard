# 📸 AutoFrame for Android

**AutoFrame** is a simple yet powerful Android application that automatically creates beautifully framed copies of your photos, complete with camera metadata, moments after you take them.  
Set it once and let the app handle the rest — giving your photos a professional, consistent look without any manual effort.

---

## ✨ Features

- **Fully Automatic**  
  Runs as a background service. Simply enable it once, and every new photo you take will be processed.

- **Elegant Polaroid Template**  
  Automatically applies a clean, modern Polaroid-style frame to your photos.

- **Dynamic EXIF Data**  
  Intelligently reads your photo's metadata (focal length, aperture, shutter speed, ISO) and embeds it into the frame.

- **Device Branding**  
  Automatically stamps the frame with your device's model name (e.g., _"Google Pixel 9 Pro"_).

- **High-Quality Output**  
  Preserves the original photo's aspect ratio and quality, saving the framed version as a new high-resolution JPEG.

- **Organized Gallery**  
  Saves all framed photos into a dedicated **"AutoFramed"** album in your device's gallery for easy access.

- **Lightweight & Efficient**  
  Designed to be battery-friendly, using modern Android practices like `WorkManager` and `ContentObserver`.

---

## 🚀 Getting Started

1. Clone or download the project repository.
2. Open the project in **Android Studio**.
3. Build and run the app on an Android device or emulator.
4. Launch the app and grant the necessary permissions for photo access and notifications.
5. Toggle the **"Enable Background Service"** switch on the main screen.

That's it! 🎉  
Close the app and take a picture with your camera.  
A new, framed version will appear in your **"AutoFramed"** gallery album shortly.

---

## 🛠️ Built With

- **Kotlin** – The official language for modern Android development.  
- **Jetpack Compose** – For building the simple, declarative UI.  
- **WorkManager** – For reliable and efficient background processing.  
- **ContentObserver** – To detect when new photos are added to the gallery.  
- **Canvas API** – For high-performance, in-memory image manipulation and drawing.  
- **ExifInterface** – For reading photo metadata.

---

## 🗺️ Future Roadmap

This project is just getting started! Here are the exciting features planned for future updates:

### 🖼️ Beautiful In-App Gallery
- A dedicated screen to view all your framed photos.
- A dynamic, Pinterest-style masonry grid layout.
- Grouping photos by date for intuitive browsing.
- Interactive previews with sharing and deleting capabilities.

### 🎨 Multi-Template System
- Allow users to choose from a variety of frame styles (e.g., dark mode, different layouts, film-style borders).
- A settings screen to select and preview the active template.
- A flexible, data-driven system to make adding new templates easy.

### 🧩 Hilt for Dependency Injection
- Integrate **Hilt** to manage dependencies cleanly.
- Make the app more robust, scalable, and easier to maintain.

---

## 🤝 Contributing

Contributions are welcome!  
If you have ideas for new features or improvements, feel free to [open an issue](https://github.com/your-repo/issues) or submit a pull request.

---

> 📂 _AutoFrame makes your photography workflow cleaner, faster, and more aesthetic — all automatically._

