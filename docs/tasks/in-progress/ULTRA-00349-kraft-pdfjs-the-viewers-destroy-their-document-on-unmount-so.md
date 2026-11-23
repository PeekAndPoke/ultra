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

- `PdfDocumentOwner` (internal, one place for both viewers): the loader offers it every document it produces
  (`dataLoader { props.src.load().onEach { documents.offer(it) } }`), so also one that arrives after unmount, when the
  viewer's own stream subscriptions are already gone (`Component.onUnmount` removes them before the unmount hooks).
  The first document is the viewer's; any other one, and one offered after `release()`, is destroyed at once;
  `claim()` lets the viewer start showing its document once; `release()` destroys it. A failed `destroy()` is caught.
- Both viewers: `onUnmount { jobQueue.clear(); documents.release() }`. KDoc of both: the viewer owns the document, a
  `State.doc` handed to `onChange` cannot be used after unmount.
- A load that fails or is given up destroys its loading task (`PdfJs.load`, only around the wait): pdf.js keeps the
  worker of a rejected task otherwise (a bad URL, a 404, a password PDF).
- Work cut off by an unmount stops quietly: the render functions return once released, `visitGuarded` logs nothing
  then, and `ScrollingPdfViewer` no longer waits forever for canvases that will not come.
- Declared: `PDFDocumentProxy.destroy()` and `loadingTask`, `PDFDocumentLoadingTask.destroyed`.
- Defensive: a second document, should the loader ever produce one, is destroyed. Today it loads once: neither
  viewer reloads, and neither follows a new `src` prop (older behaviour, not changed here).
- `kraft/addons/pdfjs/build.gradle.kts`: the tests get `:kraft:testing`.
- Tests (`PdfViewerLifecycleSpec`): for each viewer, a real one-page PDF through the real worker, mounted and
  unmounted: `doc.loadingTask.destroyed` false, then true; for each viewer, unmounted before its document arrives:
  one pdf.js worker destroyed (counted on `PDFWorker.prototype.destroy`); a load that fails: its worker destroyed; the
  owner with fake documents (offer, claim once, release, a late one destroyed; offered after release before a claim).
- Mutation: `release()` removed from both viewers: both viewer tests RED (`expected:<true> but was:<false>`); the owner
  not destroying a second document: RED (`expected:<["second"]> but was:<[]>`); the documents offered from the
  viewer's subscription instead of the loader (the round 1 MAJOR): RED (`expected:<1> but was:<0>`); a failed load not
  destroying its task: RED (`expected:<1> but was:<0>`). Each restored byte-identical (`cmp`).
- Counts (XML, `--rerun`): `:kraft:addons:pdfjs:jsBrowserTest` 10, 0 failures.
- Not changed: the stray `console.log(it)` in `ScrollingPdfViewer`'s loader callback.

## Review record

### Round 1 (2026-11-23, level 1 of a standard gate, `reviewer-opus-high`, all three charters, with ULTRA-00350 and
### the ULTRA-00348 delta)

**NOT CLEAN**: MAJOR M1, a document arriving after unmount never reached the owner (the viewer's subscription is
removed before its unmount hook; the loader runs on in a global scope): verified against `Component.kt` and
`DataLoader.kt`, FIXED as above. MINORs: m1 console errors from a render cut off by unmount: FIXED; m2 the dropped
`destroy()` promise: FIXED (caught); m3 the "second document ... a new source" claim could not happen: FIXED (text);
m4 a fully qualified `jsObject` in the Breakout example: FIXED (ULTRA-00350); m5 a failed load kept its worker (older):
FIXED here, same leak; m6 `ScrollingPdfViewer` waiting forever after unmount (older): FIXED here. ULTRA-00350 and the
ULTRA-00348 delta: no CRITICAL or MAJOR. A delta review follows, one level up.

## Acceptance

- After a viewer unmounts its document is destroyed and its worker terminated; no document a viewer loads is left
  undestroyed.

## History

- 2026-11-23: filed on the maintainer's word and built on master.
- 2026-11-23: IN PROGRESS.
