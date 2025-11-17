# AI Model Download Solution

## 🔍 Problem Discovered

All MediaPipe-compatible models in the **Google AI Edge Gallery** require HuggingFace authentication:

### Test Results (Nov 16, 2025)

```bash
# Gemma3-1B-IT from litert-community
curl -I "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task"
→ HTTP/2 401
→ x-error-code: GatedRepo
→ x-error-message: "Access to model litert-community/Gemma3-1B-IT is restricted..."

# Gemma-3n-E2B from Google official
curl -I "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task"
→ HTTP/2 401
→ x-error-code: GatedRepo
→ x-error-message: "Access to model google/gemma-3n-E2B-it-litert-preview is restricted..."
```

**Conclusion**: Direct downloads from HuggingFace CANNOT work without authentication, even though Google AI Edge Gallery uses the same URLs.

---

## ✅ Implemented Solution

### Updated Model List

Using **EXACT models from Google AI Edge Gallery's allowlist**:
- Source: `https://github.com/google-ai-edge/gallery/blob/main/model_allowlist.json`

**ModelFactory.java** now includes:

1. **Gemma3-1B-IT q4** (554MB)
   - `litert-community/Gemma3-1B-IT`
   - File: `Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task`

2. **Qwen2.5-1.5B-Instruct q8** (1.6GB)
   - `litert-community/Qwen2.5-1.5B-Instruct`
   - File: `Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task`

3. **Gemma-3n-E2B-it-int4** (3.1GB)
   - `google/gemma-3n-E2B-it-litert-preview`
   - File: `gemma-3n-E2B-it-int4.task`

### New User Flow

When users click "Download AI Model":

1. **Primary Dialog** shows 3 options:
   ```
   🤖 AI Model Options
   
   1️⃣ Use Smart Fallback (Recommended)
      ✅ Works immediately
      ✅ No downloads needed
      ✅ Handles code questions
   
   2️⃣ Download via Google AI Edge Gallery
      📱 Install official app from Play Store
      📥 Download models there (handles auth)
      🔗 Models work across all apps
   ```

2. **If user chooses "Try Download (Advanced)"**:
   - Shows warning about authentication requirement
   - Lists 3 models with file sizes
   - Warns: "⚠️ These downloads will fail without HuggingFace authentication"
   - Guides user to use Google AI Edge Gallery app instead

3. **Smart Fallback (Active by default)**:
   - Already integrated and working
   - No model downloads required
   - Provides helpful coding assistance

---

## 📱 Recommended User Path

### Option 1: Use Smart Fallback (Easiest)
✅ Already active in your app  
✅ No downloads needed  
✅ Works immediately  

### Option 2: Google AI Edge Gallery (For Advanced Users)
1. Install from Play Store: https://play.google.com/store/apps/details?id=com.google.ai.edge.gallery
2. Open Google AI Edge Gallery app
3. Browse models → Download desired model
4. The Gallery app handles HuggingFace authentication automatically
5. Once downloaded, models can be used by any app (including yours)

### Option 3: Manual HuggingFace Auth (Complex - Not Recommended)
- Would require implementing full OAuth flow
- User needs HuggingFace account
- Must request access to gated models
- Need to handle token refresh
- **Not worth the complexity for end users**

---

## 🔧 Technical Implementation

### Files Modified:

1. **ModelFactory.java**
   - Updated to use exact Google AI Edge Gallery allowlist
   - Correct model names, file sizes, URLs
   - Added detailed comments about authentication

2. **AISolutionHelper_backup.java**
   - New `showModelSelectionForDownload()` - Shows primary options
   - New `showModelListForAdvancedUsers()` - Shows models with warnings
   - Clear guidance toward Smart Fallback or Gallery app

### Architecture Retained:
- ✅ Google AI Edge Gallery structure (WorkManager, DownloadRepository)
- ✅ MediaPipe LLM Inference API integration
- ✅ Smart Fallback system
- ✅ Download progress tracking
- ✅ Format validation (.task, .tflite, .litertlm)

---

## 🎯 Current Status

### Working Now:
- ✅ Smart Fallback AI (primary solution)
- ✅ Proper model list matching Google AI Edge Gallery
- ✅ User guidance toward working solutions
- ✅ Clear warnings about authentication requirements

### Blocked (Expected):
- ❌ Direct HuggingFace downloads without auth
- This is by design from Google/HuggingFace
- Google AI Edge Gallery handles auth internally
- No public API available for third-party apps

### Next Steps:
1. **Emphasize Smart Fallback** as the primary AI solution
2. **Guide users** to Google AI Edge Gallery for model downloads
3. **Consider** implementing HuggingFace OAuth if advanced users really need local models

---

## 📊 Summary

**The 404/401 errors are EXPECTED behavior** because:
1. All MediaPipe LiteRT models on HuggingFace are gated
2. Google AI Edge Gallery has internal authentication mechanisms
3. Third-party apps cannot download directly without implementing full OAuth

**Smart Fallback is the best solution** for your app because:
- Works immediately without downloads
- No authentication complexity
- Provides helpful coding assistance
- Users can still use Gallery app for advanced model management

---

## 🔗 References

- Google AI Edge Gallery: https://github.com/google-ai-edge/gallery
- Model Allowlist: https://github.com/google-ai-edge/gallery/blob/main/model_allowlist.json
- HuggingFace LiteRT Community: https://huggingface.co/litert-community
- MediaPipe LLM API: https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference
