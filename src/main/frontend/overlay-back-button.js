(() => {
  if (window.__ftOverlayBackInitialized) return;
  window.__ftOverlayBackInitialized = true;

  const DEBUG = false;
  const log = (...args) => DEBUG && console.log("[overlay-back]", ...args);

  let suppressPush = false;
  let lastOpenCount = 0;

  const hostSelector = [
    "vaadin-select[opened]",
    "vaadin-context-menu[opened]",
    "vaadin-combo-box[opened]",
    "vaadin-multi-select-combo-box[opened]",
    "vaadin-dialog[opened]"
  ].join(",");

  const overlaySelector = [
    "vaadin-select-overlay",
    "vaadin-context-menu-overlay",
    "vaadin-combo-box-overlay",
    "vaadin-multi-select-combo-box-overlay",
    "vaadin-dialog-overlay",
    "vaadin-overlay"
  ].join(",");

  const hasOpened = (el) => el && (el.opened === true || el.hasAttribute("opened"));

  const getOpenHosts = () =>
    Array.from(document.querySelectorAll(hostSelector)).filter(hasOpened);

  const getOpenOverlays = () =>
    Array.from(document.querySelectorAll(overlaySelector)).filter(hasOpened);

  const getOpenTargets = () => [...getOpenHosts(), ...getOpenOverlays()];

  const pushOverlayState = () => {
    history.pushState({ __ftOverlay: true }, "", location.href);
    log("pushState", { lastOpenCount });
  };

  const closeTarget = (el) => {
    if (!el) return false;
    if (typeof el.close === "function") {
      el.close();
      return true;
    }
    if ("opened" in el) {
      el.opened = false;
      el.removeAttribute("opened");
      return true;
    }
    return false;
  };

  const isDialogElement = (el) =>
    !!el && (el.localName === "vaadin-dialog" || el.localName === "vaadin-dialog-overlay");

  const closeTopOverlay = () => {
    const openTargets = getOpenTargets();
    if (!openTargets.length) {
      log("closeTopOverlay: none");
      return false;
    }

    // Prefer non-dialog overlays first (select/context/combo/etc.)
    const nonDialogTargets = openTargets.filter((el) => !isDialogElement(el));
    const top = (nonDialogTargets.length ? nonDialogTargets : openTargets)[
      (nonDialogTargets.length ? nonDialogTargets : openTargets).length - 1
    ];

    const closed = closeTarget(top);

    // IMPORTANT: do not dispatch synthetic Escape; it can also close parent dialog.
    log("closeTopOverlay: closed", top.localName, { closed });
    return closed;
  };

  const syncOverlayHistory = () => {
    const openCount = getOpenTargets().length;
    log("sync", {
      openCount,
      lastOpenCount,
      suppressPush,
      hosts: getOpenHosts().map((e) => e.localName),
      overlays: getOpenOverlays().map((e) => e.localName)
    });

    if (!suppressPush && openCount > lastOpenCount) {
      for (let i = 0; i < openCount - lastOpenCount; i++) {
        pushOverlayState();
      }
    }

    lastOpenCount = openCount;
  };

  const deferredSync = () => setTimeout(syncOverlayHistory, 0);

  const observer = new MutationObserver(() => {
    syncOverlayHistory();
  });

  const start = () => {
    lastOpenCount = getOpenTargets().length;
    log("start", { lastOpenCount, href: location.href });

    observer.observe(document.body, {
      subtree: true,
      childList: true,
      attributes: true,
      attributeFilter: ["opened"]
    });

    document.addEventListener("click", deferredSync, true);
    document.addEventListener("touchend", deferredSync, true);
    document.addEventListener("keydown", deferredSync, true);

    if (DEBUG) {
      window.__ftOverlayBackDebug = {
        getOpenHosts,
        getOpenOverlays,
        getOpenTargets,
        getLastOpenCount: () => lastOpenCount,
        syncOverlayHistory
      };
    }

    window.addEventListener("popstate", () => {
      log("popstate");
      const closed = closeTopOverlay();
      if (!closed) return;

      suppressPush = true;
      queueMicrotask(() => {
        lastOpenCount = getOpenTargets().length;
        suppressPush = false;
        log("post-pop sync", { lastOpenCount });
      });
    });
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start, { once: true });
  } else {
    start();
  }
})();
