# Android-Journal
Journal App is a private, offline Android journal application. All entries are stored locally on your device using Android's SharedPreferences system. No data is sent to any server or cloud service.

## Writing an Entry

Type your journal entry into the text field at the top of the screen. The field supports multiple lines, auto-capitalization at the start of sentences, and standard autocorrect. When you are done, tap the **Post Entry** button. Your entry will appear at the top of the list immediately.

Entries support standard Markdown syntax. You can use **\*\*bold\*\***, *\*italic\**, `#` headings, `` `inline code` ``, `>` blockquotes, and both bullet and numbered lists. Markdown is typed directly into the entry field as plain text and rendered when displayed in the list.

## Viewing Entries

The main screen shows a scrollable list of your journal entries. Each entry displays its date and time as a header, followed by the entry body. By default, only entries from the most recent 30-day window are shown. Use the **← Older** and **Newer →** buttons pinned to the bottom of the screen to page through earlier or later 30-day windows. The Newer button is disabled when you are already viewing the most recent window. When you post a new entry, the view automatically resets to the most recent page so your new entry is visible right away.

## Entry Options (Edit, Copy, Delete)

Tap the date and time header on any journal entry to open a popup menu with three options.

**Edit** opens a dialog pre-filled with the entry's existing text. Make your changes and tap Save. The entry is updated in place and its original timestamp is preserved.

**Copy** copies the full text of the entry to your device clipboard so you can paste it anywhere.

**Delete** shows a confirmation dialog before permanently removing the entry. This cannot be undone.

The entry body text is also selectable using standard Android text selection handles, so you can highlight and copy any portion of an entry without opening the options menu.

## PIN Lock

You can optionally protect the app with a PIN so that anyone picking up your phone can't casually open your journal. This is an access lock only — it does not encrypt the journal data stored on disk, and it is not intended to protect against someone extracting app data directly from the device (e.g. via ADB or a backup). If you forget your PIN, that same lack of encryption means your entries are still recoverable through those channels rather than lost.

Open the overflow menu and use the following options, which appear depending on whether a PIN is currently set:

**Set PIN** — enter a PIN (minimum 4 digits) and confirm it. Once set, the app will prompt for this PIN whenever it is opened or resumed from the background.

**Change PIN** — enter your current PIN, then set and confirm a new one.

**Remove PIN** — enter your current PIN to disable the lock entirely.

While a PIN is set, screenshots of the app and the app's preview in the Recent Apps switcher are automatically disabled to keep journal content from being visible outside the app itself. This is turned off automatically if you remove your PIN.

## Search and Filter

Tap the search icon in the toolbar to open the Search and Filter dialog. You can use any combination of the following:

**Search** — type a word or phrase to search all entries. The search is case-insensitive and will show any entry whose text contains the search term.

**Start Date / End Date** — tap either button to open a date picker and set a date range. Only entries within the selected range will be shown. You can set just a start date, just an end date, or both.

Tap **Apply** to activate your filters. When a search or date filter is active, the 30-day pagination buttons are hidden and all matching entries are shown at once. Tap the search icon again and use the **Clear All Filters** button to reset everything and return to the default paginated view.

## Exporting Your Journal

Open the overflow menu (three dots) in the toolbar to access export options. Exported files are saved to your device's Downloads folder.

**Export Journal (JSON)** saves all entries as a JSON file named with the current timestamp (e.g. `journal_export_1234567890.json`). This format is used for backup and can be re-imported into the app.

**Export Journal (Text)** saves all entries as a plain text `.txt` file. Each entry is formatted with a date header followed by the entry text, with entries separated by `---` dividers. This format is intended for reading or sharing outside the app and cannot be re-imported.

## Importing a Journal

Open the overflow menu and tap **Import Journal**. A warning dialog will appear explaining that importing will erase all existing entries and replace them with the data from the file. This cannot be undone. Tap Import to proceed, then select a previously exported JSON file using the Android file picker. The app accepts only `.json` files in the format produced by the JSON export feature. After a successful import, all entries from the file will be loaded and sorted by date.

## About / Help

Open the overflow menu and tap **About / Help** to view the in-app update history, which describes recent changes and new features.

## Data Storage

All journal entries are stored locally on your device in Android's SharedPreferences as a Gson-serialized JSON string. No account, login, or internet connection is required or used. Uninstalling the app will permanently delete all entries that have not been exported.

## Building from Source

**Requirements**

- [Android Studio](https://developer.android.com/studio) (Koala or newer recommended)
- JDK 8 or later (bundled with recent Android Studio installs)
- Android SDK Platform 34, installed via the Android Studio SDK Manager

**Steps**

1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/<your-repo>.git
   ```
2. Open the project folder in Android Studio.
3. Let Gradle sync automatically. If it doesn't start on its own, choose **File → Sync Project with Gradle Files**.
4. Connect an Android device (with USB debugging enabled) or start an emulator running API 24 or higher.
5. Click **Run ▶** in Android Studio, or build and install from the command line:
   ```bash
   ./gradlew installDebug
   ```

**Building a release APK**

```bash
./gradlew assembleRelease
```

The unsigned release APK will be output to `app/build/outputs/apk/release/`. To install a signed release build on a device, configure a signing config in `app/build.gradle.kts` first.

---

## Update History

### 2026-09-26

Added an optional PIN lock, accessible from the overflow menu (Set PIN / Change PIN / Remove PIN). When enabled, the app requires the PIN on launch and whenever it's resumed from the background. Screenshots and the Recent Apps preview are automatically disabled while a PIN is set. This is an app-lock feature only and does not encrypt journal data on disk.

### 2026-03-21

Added entry options (Edit, Copy, Delete) accessible by tapping the date header on any entry. Added selectable entry body text. Added Markdown rendering support for bold, italic, headings, inline code, blockquotes, and lists. Added Export Journal (Text) option that saves entries as a formatted `.txt` file to Downloads. Added 30-day pagination to the default view with Older and Newer navigation buttons. Added a Search bar to the filter dialog supporting case-insensitive full-text search combinable with date range filtering.
