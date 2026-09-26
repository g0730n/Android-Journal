# Updates

## 2026-09-26
Added option to set a PIN to lock the app. Note that setting a PIN will disable screenshots to maintain privacy while app is seen from android launcher.

## 2026-03-21

### Entry Options (Edit, Copy, Delete)
Tapping the date/title header on any journal entry now shows a popup with three options:
- **Edit** — opens a dialog pre-filled with the entry's text for editing
- **Copy** — copies the raw entry text to the clipboard
- **Delete** — confirms and deletes the entry

### Selectable Entry Text
Entry body text is now selectable using the standard Android text selection handles, allowing users to highlight and copy any portion of an entry.

### Improved Input Method
The journal entry input field now uses auto-capitalization at the start of sentences and standard autocorrect behavior. The edit dialog uses the same input settings.

### Markdown Support
Entries now support standard Markdown syntax. The entry viewer renders the following:
- `**bold**` and `*italic*`
- `# Headings`
- `` `inline code` ``
- `> blockquotes`
- Bullet and numbered lists

Input remains plain text with Markdown syntax typed directly.

### Export as Plain Text (.txt)
Added a new "Export Journal (Text)" option in the overflow menu. Exports all entries as a `.txt` file to the Downloads folder, formatted with a date header and `---` dividers between entries. The existing JSON export/import remains unchanged.

### 30-Day Pagination (Default View)
On launch, only entries from the last 30 days are loaded by default. Two buttons pinned to the bottom of the screen allow paging through older entries:
- **← Older** — shifts the 30-day window back by 30 days
- **Newer →** — shifts forward (disabled when already at the most recent window)

The pagination bar is hidden when a custom date filter or search is active. Posting a new entry always resets to the most recent page so it is immediately visible.

### Search
Added a search bar to the filter dialog (accessed via the toolbar search icon). Entering a word or phrase searches all entries case-insensitively and shows only matching posts. Search can be combined with the date range filter. "Clear All Filters" resets both the search term and date range.
