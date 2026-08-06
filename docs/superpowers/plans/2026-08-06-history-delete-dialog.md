# 历史记录删除确认弹层 Implementation Plan

> **For agentic workers:** Inline execution in this session is selected; no worktree or sub-agent is used.

**Goal:** Replace the native delete confirmation with an accessible in-page confirmation dialog without changing deletion behavior.

**Architecture:** Keep dialog state local to `RecordsView.vue` because only the history view owns the delete action. Reuse the existing store deletion method and `deletingRecordKey` for request serialization, and add focused global styles beside the existing archive styles.

**Tech Stack:** Vue 3 `<script setup>`, TypeScript, Pinia, lucide-vue-next, shared CSS, Vite.

---

### Task 1: Replace the native confirmation flow

**Files:**
- Modify: `pregnancy-assistant/src/views/RecordsView.vue:1-131`

- [x] Add local state for the pending delete payload and a computed label for the selected record type.
- [x] Change the delete icon handler to open the dialog and clear only the selected record's prior error.
- [x] Add a confirm handler that checks the selected payload, sets `deletingRecordKey`, calls `store.deleteRecord`, clears note editing after success, and resets dialog state in `finally`.
- [x] Render an accessible dialog with `role="dialog"`, `aria-modal="true"`, close/cancel actions, and a disabled processing state.

### Task 2: Add dialog styling

**Files:**
- Modify: `pregnancy-assistant/src/styles.css:500-516,782-866`

- [x] Add a fixed viewport overlay above the workspace with a readable surface and existing color tokens.
- [x] Add responsive dialog content and action layout that remains usable on narrow screens.
- [x] Preserve the existing delete icon and error styles.

### Task 3: Verify and document

**Files:**
- Modify: `memory.md`
- Modify: `docs/superpowers/plans/2026-08-06-history-delete-dialog.md`

- [x] Run `npm run type-check` in `pregnancy-assistant` and expect exit code 0.
- [x] Run `npm run build` in `pregnancy-assistant` and expect exit code 0.
- [x] Run `git diff --check` and inspect the final diff for native confirmation remnants.
- [x] Update memory with the confirmed UI-only behavior; business rules remain unchanged.
- [x] Commit only this change's files with a Chinese commit message; leave unrelated staged log changes untouched.
