# InkFlow — Copilot Instructions

這份文件定義了 InkFlow 專案的所有開發規範。Copilot 在此 codebase 進行任何程式碼生成或建議時，**必須嚴格遵守**以下所有規則。

---

## 1. 架構原則 (Architecture)

- **採用精簡版 Clean Architecture + MVVM**：UI Layer → ViewModel → Data Layer。目前專案**刻意不建立獨立的 `domain/` 套件**；複雜的業務邏輯（碰撞偵測、套索計算、PDF 匯出）集中於 `util/` 下的純 Kotlin 物件（如 `IntersectionUtils`、`PdfExporter`），ViewModel 直接呼叫。
  - **當單一 `util/` 物件的職責開始超過 3 個不相關功能時**，才提取為獨立的 Use Case 類別並移入新的 `domain/` 套件。在此之前**禁止**預先建立空的 Use Case 骨架。
- **絕不**在 Composable 或 ViewModel 中直接操作 Room DAO 的原始 SQL，須透過 Repository 介面（目前為 `StrokeDao`）。
- 所有 Kotlin 檔案皆位於 `com.vic.inkflow` 套件下，子套件對應層級：`ui/`、`data/`、`util/`。
- ViewModel 的所有公開狀態必須以 `StateFlow`（搭配私有 `MutableStateFlow`）暴露，**禁止**使用 `LiveData`。

## 2. 雙軌制渲染 (Dual-Track Rendering)

- **Static Layer**：`PdfRenderer` 渲染 PDF 頁面為 Bitmap，一律在 `Dispatchers.IO` 執行；結果以 `LruCache` 快取，**禁止**在 Main Thread 呼叫 `PdfRenderer.openPage()`。
- **Active Layer**：Compose `Canvas` 負責即時繪製「當前正在書寫中」的筆劃，只處理尚未落入 DB 的 in-flight path。
- **Cache Layer**：歷史筆劃統一以 `drawWithCache` 渲染成一張透明 Bitmap；**禁止**在每次重組 (recomposition) 時全量重繪 DB 中的所有 `Path`。
- 縮放/平移必須透過共用的 `Matrix`（或 Compose `graphicsLayer` 的 `scaleX/Y` + `translationX/Y`）同步作用於三個圖層，確保像素級對齊。

## 3. 座標系統 (Coordinate System)

- **所有觸控點必須經過反投影才能使用**；從 `pointerInput` 取得的原始點 (`rawX`, `rawY`) 必須立即轉換為 ModelPoint：
  ```kotlin
  val modelX = (rawX - offsetX) / scale
  val modelY = (rawY - offsetY) / scale
  ```
- **繪圖引擎與 DB 只儲存 ModelPoint**，絕不儲存螢幕座標。
- Compose 的 `graphicsLayer` 負責將 ModelPoint 映射回螢幕位置，不需要手動反推。

## 4. 筆劃與貝茲平滑化 (Stroke & Bezier Smoothing)

- 收集觸控點時，**必須先讀取 `event.historical`** 以包含兩幀之間的微小中間點，再加入當前點。
- 繪製筆劃**禁止使用 `lineTo`**；必須使用 `quadraticBezierTo`：
  ```kotlin
  val midX = (prevX + currX) / 2f
  val midY = (prevY + currY) / 2f
  path.quadraticBezierTo(prevX, prevY, midX, midY)
  ```
- 筆劃在 `ACTION_UP` 後才呼叫 `saveStroke()`，由 `viewModelScope.launch(Dispatchers.IO)` 批次寫入 DB，**禁止**在每個 `ACTION_MOVE` 都寫入。

## 5. 資料模型 (Data Model)

- **`StrokeEntity`** 必須包含 `boundsLeft/Top/Right/Bottom` 四個 BoundingBox 欄位，在寫入前計算並填入，用於橡皮擦的 Phase 1 過濾。
- **`PointEntity`** 透過外鍵 (`strokeId`) 關聯至 `StrokeEntity`，外鍵須設定 `onDelete = CASCADE`，並在 `strokeId` 欄位建立 `@Index`。
- 查詢同時需要點的情境（如渲染、匯出）使用 `StrokeWithPoints`（`@Relation` 查詢）。
- 跨實體的新增操作（一筆 `StrokeEntity` + 多筆 `PointEntity`）必須包在 `@Transaction` 中執行。

## 6. 橡皮擦演算法 (Eraser — Two-Phase Detection)

- **Phase 1 (AABB)**：先用 BoundingBox 快速排除，加入誤差半徑 `R`：
  ```kotlin
  if (ex !in (minX - R)..(maxX + R) || ey !in (minY - R)..(maxY + R)) continue
  ```
- **Phase 2 (Point-to-Segment Distance)**：僅對通過 Phase 1 的筆劃計算向量投影精確距離；命中則刪除整條 `StrokeEntity`（CASCADE 會自動刪除 `PointEntity`）。
- **禁止**在 Main Thread 執行 Phase 2 計算；需在 `Dispatchers.Default` 或 `IO` 上執行，結果回到 Main Thread 更新 UI。

## 7. 套索工具 (Lasso — Ray-Casting)

- 判斷筆劃是否在套索多邊形內，使用**奇偶規則 (Even-Odd Rule)** 射線投射法（向 X 正方向發射射線，計算與多邊形邊的交點數量，奇數 = 在內部）。
- 被圈選的筆劃抽離至獨立的 `SelectedLayer`，以額外的 `Offset Matrix` 執行移動操作，**不直接修改原始 `PointEntity` 座標**，直到使用者確認放下 (drop) 後才批次更新 DB。

## 8. 撤銷與重做 (Undo/Redo — Command Pattern) ⚠️ 待實作

> **目前狀態**：`EditorViewModel` 尚未實作 Undo/Redo。在完整基礎建設完成前，**Copilot 禁止在現有 ViewModel 或 UI 中自動插入任何 undo/redo 相關呼叫**，以避免產生破壞性的半成品邏輯。

實作時須遵守以下設計：

- 維護兩個 Stack：`undoStack: ArrayDeque<DrawCommand>` 與 `redoStack: ArrayDeque<DrawCommand>`，宣告於 `EditorViewModel` 內（私有）。
- `DrawCommand` 為密封類別，定義於獨立檔案 `ui/DrawCommand.kt`：
  ```kotlin
  sealed class DrawCommand {
      data class AddStroke(val stroke: StrokeWithPoints) : DrawCommand()
      data class RemoveStroke(val stroke: StrokeWithPoints) : DrawCommand()
      data class MoveStroke(val strokeId: String, val delta: Offset) : DrawCommand()
  }
  ```
- 每次執行新的繪圖操作（新增/刪除/移動筆劃）時，**必須同時清空 `redoStack`**。
- `undo()` / `redo()` 的 DB 反向操作在 `Dispatchers.IO` 執行，**不阻塞 UI**。
- 實作 Undo/Redo 前，必須先將 `saveStroke()` 與 `deleteStrokesIntersecting()` 重構為回傳 `DrawCommand` 的形式，再統一推入 Stack。

## 9. PDF 物理匯出 (PDF Export)

- 匯出前必須讀取 PDF 的 MediaBox 尺寸 (`pdfWidth`, `pdfHeight`)，計算縮放比例：
  ```kotlin
  val ratioX = pdfWidth / canvasWidth
  val ratioY = pdfHeight / canvasHeight
  ```
- PDF 座標系原點在**左下角**，Y 軸需反轉：
  ```kotlin
  val pdfY = pdfHeight - (modelY * ratioY)
  ```
- 匯出路徑使用**二次貝茲曲線指令**寫入 PDF Content Stream（透過 PdfBox-Android），**禁止**將筆劃柵格化為 Bitmap 後嵌入，以確保向量品質。

## 10. 執行緒規範 (Threading)

| 操作 | Dispatcher |
|---|---|
| PDF 渲染 (`PdfRenderer`) | `Dispatchers.IO` |
| DB 讀寫 (Room) | `Dispatchers.IO` |
| 橡皮擦/套索計算 | `Dispatchers.Default` |
| UI 狀態更新 (StateFlow) | `Dispatchers.Main` |
| 觸控事件處理 (`pointerInput`) | Main Thread（但立即 offload 計算） |

- **禁止**在 `@Composable` 函式或 `LaunchedEffect` 以外的地方直接呼叫 `suspend` 函式。
- `viewModelScope.launch` 預設在 Main；CPU 密集或 IO 操作必須明確指定 Dispatcher。

## 11. 顏色型別 (Color Type)

- ViewModel 的狀態層**必須使用 Jetpack Compose 的 `Color` 型別**（不使用 `Int` ARGB）。
- UI 層傳遞顏色給 ViewModel 時，直接傳 `Color` 物件：
  ```kotlin
  fun onColorSelected(color: Color) { _selectedColor.value = color }
  ```
- 需要 ARGB Int（例如存入 DB）時，在 data layer 呼叫 `color.toArgb()`；**禁止**使用 `color.hashCode()` 或 `color.value.toLong().toInt()`。

## 12. 記憶體管理 (Memory)

- `PdfRenderer.Page` 使用後**必須在 `finally` 區塊中 `close()`**，避免 Native Memory 洩漏。
- PDF Bitmap 快取使用 `LruCache`，最大容量建議設定為可用 heap 的 1/8：
  ```kotlin
  val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
  val cacheSize = maxMemory / 8
  ```
- **禁止**以 `val bitmap = Bitmap.createBitmap(...)` 直接持有大 Bitmap 而不釋放；縮放或翻頁後舊 Bitmap 必須從 Cache 淘汰或手動 `recycle()`。

## 13. 狀態保存 (Configuration Change)

- 縮放倍率 (`scale`)、平移座標 (`offset`) 以及「尚未存入 DB 的懸空筆劃」**必須使用 `rememberSaveable`** 保存，以應對旋轉螢幕等 Configuration Change。
- 複雜狀態（無法自動序列化）需透過自訂 `Saver` 或 ViewModel（因 ViewModel 在 Configuration Change 中存活）保存。

## 14. 安全性 (Security)

- 讀取外部 PDF 檔案時，必須透過 `ContentResolver` 搭配 `takePersistableUriPermission` 取得持久化權限，**禁止**直接存取絕對檔案路徑 (`/sdcard/...`)。
- 所有 URI 在傳遞至 Navigation 之前必須經過 `URLEncoder.encode(uri, "UTF-8")` 編碼。

---

## 15. UI 視覺設計規範 (UI Visual Design)

目標裝置為 **Android 平板 (10–14 吋)，橫向模式 (Landscape) 優先**，設計語言基於 **Material Design 3**。

### 15.1 色彩系統 (Color Palette)

| 用途 | 值 |
|---|---|
| 品牌漸變 (FAB / 選中 / 強調) | `LinearGradient(Indigo500 #6366F1 → Purple500 #A855F7)` |
| 彩虹漸變 (調色盤按鈕) | `SweepGradient(紅, 黃, 綠, 青, 藍, 紫, 紅)` |
| 背景 Light | Slate50 `#F8FAFC` |
| 背景 Dark | Slate900 `#0F172A` |
| Surface Light | `#FFFFFF` |
| Surface Dark | Slate800 `#1E293B` |

- **禁止**使用純白 (`#FFFFFF`) 作為 Light mode 頁面背景；必須使用 Slate50。
- 品牌漸變色必須以 `Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))` 實作，**禁止**以單一純色替代。

### 15.2 字體規範 (Typography)

| 層級 | 大小 | 粗細 | 用途 |
|---|---|---|---|
| Display / Headline | 24sp | ExtraBold | App 名稱、首頁大標題 |
| Title | 18–20sp | Bold | 文件名稱、對話框標題 |
| Body | 14sp | Normal / Medium | 一般內文、卡片標題 |
| Label / Caption | 12sp | Normal | 日期、檔案大小、縮圖頁碼 |

### 15.3 形狀規範 (Corner Radius)

| 元件 | 圓角 |
|---|---|
| 按鈕 / 標籤 / 搜尋框 | `CircleShape` (50%) |
| 文件卡片 / 對話框 | `16dp – 24dp` |
| A4 畫布紙張 | `4dp – 8dp` (擬物紙張邊緣) |

### 15.4 首頁檔案櫃 (Home Screen)

- 佈局採**左側導覽軌 (NavigationRail，約 80–100dp) + 右側主內容區**雙欄結構。
- 文件卡片使用 `LazyVerticalGrid`，`minSize = 180dp`（11 吋 ≈ 4 欄，14 吋 ≈ 5–6 欄）。
- 卡片比例 `aspectRatio(0.85f)`：上 60% 為封面（品牌色 10% 透明背景 + 文件 Icon / 縮圖），下 40% 為標題 (`TextOverflow.Ellipsis`) + 日期 + 更多選單。
- FAB 固定於右下角，使用品牌漸變背景，含「＋」圖示與「新增筆記」文字，點擊須有 Ripple + 縮放回饋。
- 無文件時顯示空狀態佔位畫面（插圖 + 主標題 + 引導文字）。

### 15.5 編輯器畫面 (Editor Screen)

#### 頂部工具列 (Top Toolbar)
- 高度 `56–64dp`，背景 Surface 色，帶 `1dp` 陰影。
- 三區塊結構：
  - **左側**：返回鍵 (ArrowBack)、側邊欄切換 (ViewSidebar)、文件標題（截斷）。
  - **中央**：膠囊狀半透明容器，依序放置：筆刷工具組（畫筆/螢光筆/橡皮擦/套索）→ 垂直分隔線 → 顏色圓圈按鈕（選中時顯示外框 Ring）→ 彩虹漸變圓圈（呼叫調色盤）。
  - **右側**：Undo / Redo 按鈕、匯出/分享按鈕（可用漸變色實體按鈕）。
- 工具選中狀態必須有明顯背景高亮，切換時高亮塊需平滑移動（使用 `animateContentSize` 或 `Animatable`）。

#### 縮圖側邊欄 (Thumbnail Sidebar)
- 由工具列按鈕控制，使用 `AnimatedVisibility(expandHorizontally / shrinkHorizontally)`，動畫時長 `tween(300ms)`。
- 固定寬度 `220–260dp`，內容為 A4 比例 (`aspectRatio(1f / 1.414f)`) 的頁面縮圖列表。
- 當前頁面縮圖須顯示 **`2dp` 品牌色邊框**。
- 側邊欄收合時，畫布區必須自動平滑延展填滿剩餘空間（用 `weight(1f)` 或 `AnimatedContent`）。

#### 畫布工作區 (Canvas Workspace)
- 畫布背景色：Light `#E5E7EB`，Dark `#121212`（確保與白色紙張有對比）。
- A4 紙張 `aspectRatio(1f / 1.414f)`，置中顯示，`elevation = 8–12dp` 製造擬物陰影。
- 紙張背景 Light mode 純白，Dark mode `#E0E0E0` 或護眼黃色（可選）。

### 15.6 動畫與互動 (Animations & Interactions)

- 所有可點擊元件（按鈕、卡片、列表項）**必須**觸發原生 Compose Ripple 回饋。
- 側邊欄展開/收合：`tween(300ms)`。
- 深色/亮色切換：顏色漸變過渡（`animateColorAsState`）。
- 首頁卡片初次載入：從下往上浮現的交錯 (Staggered) 動畫（`LaunchedEffect` + `delay(index * 50ms)`）。
- 手勢：雙指縮放 (Pinch-to-Zoom) + 雙指平移作用於畫布；單指在畫布區 = 繪圖，在側邊欄 = 滾動。**禁止**混用。
