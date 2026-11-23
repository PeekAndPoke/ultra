# ULTRA-00349: Kraft pdfjs: the viewers destroy their document on unmount, so no PDF worker is left behind

> **Status (2026-11-23): IN PROGRESS.**
>
> **Part of:** ULTRA-00258 (Epic: Kraft addons)

**Found by:** the review of ULTRA-00348 (three.js context loss), 2026-11-23, while checking the other addons.

## Why this exists

Neither PDF viewer ever destroyed the document it loaded: `PagedPdfViewer` and `ScrollingPdfViewer` had no unmount
cleanup, and `PDFDocumentProxy` declared no `destroy`. pdf.js 4.10.38 makes a new `PDFWorker` for every
`getDocument` while `GlobalWorkerOptions.workerPort` is unset (`pdf.mjs:11412-11414`), and the addon never sets it.
So every visit to a page with a viewer left a worker thread running, holding its parsed PDF, for the life of the tab.

## Decided (maintainer, 2026-11-23)

"Yes file both as tickets and fix them now please."

## Built (2026-11-23, on master)

- `PdfDocumentOwner` (internal, one place for both viewers): the first document loaded is the viewer's; any other one,
  and one that arrives after the viewer unmounted, is destroyed at once; `release()` destroys the viewer's.
- Both viewers: `onUnmount { jobQueue.clear(); documents.release() }`; a document is adopted through the owner. KDoc
  of both: the viewer owns the document, a `State.doc` handed to `onChange` cannot be used after unmount.
- Declared: `PDFDocumentProxy.destroy()` and `loadingTask`, `PDFDocumentLoadingTask.destroyed`.
- Behaviour beside the leak: a second document loaded for the same viewer (the loader again, a new source) was
  ignored and leaked; it is now destroyed. The viewer still shows its first document, as before.
- `kraft/addons/pdfjs/build.gradle.kts`: the tests get `:kraft:testing`.
- Tests (`PdfViewerLifecycleSpec`): for each viewer, a real one-page PDF through the real worker, mounted and
  unmounted: `doc.loadingTask.destroyed` false, then true; the owner with fake documents: adopt, adopt again, a
  second destroyed, release, a late one destroyed.
- Mutation: `release()` removed from both viewers: both viewer tests RED (`expected:<true> but was:<false>`); the owner
  not destroying a second document: RED (`expected:<["second"]> but was:<[]>`). Restored byte-identical (`cmp`).
- Counts (XML, `--rerun`): `:kraft:addons:pdfjs:jsBrowserTest` 7, 0 failures.
- Not changed: the stray `console.log(it)` in `ScrollingPdfViewer`'s loader callback.

## Acceptance

- After a viewer unmounts its document is destroyed and its worker terminated; no document a viewer loads is left
  undestroyed.

## History

- 2026-11-23: filed on the maintainer's word and built on master.
- 2026-11-23: IN PROGRESS.
