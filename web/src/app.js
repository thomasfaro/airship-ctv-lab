(function () {
  "use strict";

  var config = window.CTVLAB_CONFIG || {};
  var requestedPlatform = window.location.search.match(/[?&]platform=(tizen|webos)(?:&|$)/);
  var platform = requestedPlatform ? requestedPlatform[1] : config.platform || "browser";
  var sdk;
  var lastFocused;
  var diagnostics = [];
  var currentScreen = "home";

  var statusText = document.getElementById("sdk-status");
  var statusDot = document.getElementById("sdk-dot");
  var diagnostic = document.getElementById("diagnostic");
  var channelId = document.getElementById("channel-id");
  var platformLabel = document.getElementById("platform-label");
  var labPlatform = document.getElementById("lab-platform");
  var secureContext = document.getElementById("secure-context");
  var liveSite = document.getElementById("lab-live-site");
  var embeddedSlots = (config.embeddedSlots || [])
    .map(function (slot) {
      return { embeddedId: slot.embeddedId, element: document.querySelector(slot.selector) };
    })
    .filter(function (slot) {
      return slot.element;
    });

  platformLabel.textContent =
    platform === "tizen" ? "Samsung Tizen" : platform === "webos" ? "LG webOS" : "Web preview";
  labPlatform.textContent = platform;
  secureContext.textContent = window.isSecureContext ? "yes" : "no";

  function renderList(elementId, values) {
    var list = document.getElementById(elementId);
    (values || []).forEach(function (value) {
      var item = document.createElement("li");
      item.textContent = value;
      list.appendChild(item);
    });
  }

  liveSite.textContent = config.liveSiteUrl || window.location.origin;
  renderList(
    "lab-embedded-ids",
    embeddedSlots.map(function (slot) {
      return slot.embeddedId + " (" + config.embeddedSlots.filter(function (entry) {
        return entry.embeddedId === slot.embeddedId;
      })[0].selector + ")";
    }),
  );
  renderList("lab-custom-components", config.customComponents);
  renderList("lab-available-urls", config.availableUrls);
  renderList("lab-deep-links", config.deepLinks);

  function log(message) {
    var value = new Date().toISOString().slice(11, 19) + "  " + message;
    diagnostics.push(value);
    if (diagnostics.length > 8) diagnostics.shift();
    diagnostic.textContent = diagnostics.join("\n");
    if (window.console && console.log) console.log("[CTVLAB] " + message);
  }

  function setStatus(message, state) {
    statusText.textContent = message;
    statusDot.className = "status-dot" + (state ? " " + state : "");
    log(message);
  }

  function trackScreen(screen) {
    currentScreen = screen;
    if (!sdk) return Promise.resolve();
    return sdk.analytics.trackScreen(screen).then(function () {
      log("Screen tracked: " + screen + ".");
    });
  }

  function missingWebCredentials() {
    return (
      !config.appKey ||
      config.appKey === "YOUR_APP_KEY" ||
      !config.token ||
      !config.vapidPublicKey
    );
  }

  function loadAirship() {
    if (missingWebCredentials()) {
      setStatus("Web credentials missing", "error");
      log("Add airship.webToken and airship.webVapidPublicKey to config/airship.local.properties.");
      return;
    }

    var options = {
      appKey: config.appKey,
      token: config.token,
      vapidPublicKey: config.vapidPublicKey,
      appVersion: "0.1.0",
      components: {
        embeddedViews: {
          selectors: (config.embeddedSlots || []).reduce(function (selectors, slot) {
            selectors[slot.selector] = slot.embeddedId;
            return selectors;
          }, {}),
        },
        inAppAutomation: {
          displayBreakpoints: {
            medium: 1024,
            large: 1920,
          },
          matchBrowserDarkMode: false,
        },
      },
    };

    var sdkUrl =
      String(config.site).toLowerCase() === "eu"
        ? "https://aswpsdkeu.com/notify/v2/ua-sdk.min.js"
        : "https://aswpsdkus.com/notify/v2/ua-sdk.min.js";

    setStatus("Loading Airship Web SDK…");

    // The SDK resolves window.UA from the downloaded script. A blocked request or a
    // storage the SDK cannot open leaves that promise pending with no error at all.
    var watchdog = setTimeout(function () {
      setStatus("Airship SDK did not initialize", "error");
      log("No answer from " + sdkUrl + " after 10s.");
      log("Suspect a tracker blocker on aswpsdk*/aswpapi* domains, or stale site storage.");
    }, 10000);

    !function (n, r, e, t, c) {
      var resolver;
      var hasPromise = "Promise" in n;
      var unsupported = {
        then: function () { return unsupported; },
        catch: function (handler) {
          handler(new Error("Airship SDK Error: Unsupported browser"));
          return unsupported;
        },
      };
      var promise = hasPromise
        ? new Promise(function (resolve, reject) {
            resolver = function (error, value) {
              error ? reject(error) : resolve(value);
            };
          })
        : unsupported;
      promise._async_setup = function (factory) {
        if (!hasPromise) return;
        try {
          resolver(null, factory(c));
        } catch (error) {
          resolver(error);
        }
      };
      n[t] = promise;
      var script = r.createElement("script");
      script.src = e;
      script.async = true;
      script.id = "_uasdk";
      script.rel = t;
      script.onerror = function () {
        clearTimeout(watchdog);
        setStatus("Airship SDK download failed", "error");
        log("Blocked or unreachable: " + e);
      };
      r.head.appendChild(script);
    }(window, document, sdkUrl, "UA", options);

    window.UA
      .then(function (loadedSdk) {
        clearTimeout(watchdog);
        sdk = loadedSdk;
        log("SDK loaded from " + sdkUrl);
        return sdk.channel.id();
      })
      .then(function (id) {
        if (id) return { channelId: id };
        log("Creating an opted-out Web channel (no push prompt).");
        return sdk.create();
      })
      .then(function (result) {
        var id = result.channelId;
        channelId.textContent = id || "unavailable";
        setStatus(id ? "Airship ready" : "Airship loaded, no channel", id ? "ready" : "error");

        return sdk.channel
          .editTags()
          .add("device", ["ctv_lab", platform])
          .apply()
          .then(function () {
            log("Tags applied: ctv_lab, " + platform);
          });
      })
      .then(function () {
        if (!config.namedUser) return;
        return sdk.contact.identify(config.namedUser).then(function () {
          log("Named user: " + config.namedUser);
        });
      })
      .then(function () {
        return trackScreen(currentScreen);
      })
      .then(function () {
        log(
          "Waiting for " +
            embeddedSlots
              .map(function (slot) {
                return slot.embeddedId;
              })
              .join(", ") +
            ".",
        );
      })
      .catch(function (error) {
        clearTimeout(watchdog);
        var message = error && error.message ? error.message : String(error);
        setStatus("Airship initialization failed", "error");
        log(message);
      });
  }

  function sectionOf(element) {
    var node = element.parentNode;
    while (node && node.classList) {
      if (node.classList.contains("embedded-section")) return node;
      node = node.parentNode;
    }
    return null;
  }

  // An empty wrapper is what the SDK leaves behind when nothing is eligible, so a
  // child only counts when it actually draws: it has height, it carries text, or
  // it is a media element still loading its intrinsic size.
  function draws(element) {
    if (element.offsetHeight > 0) return true;
    if (/^(IMG|PICTURE|VIDEO|IFRAME|CANVAS|OBJECT|EMBED|svg)$/.test(element.tagName || "")) {
      return true;
    }
    var text = element.textContent;
    return !!(text && text.replace(/\s+/g, "").length);
  }

  function hasContent(root) {
    var children = root.children || [];
    var i;

    for (i = 0; i < children.length; i += 1) {
      if (draws(children[i])) return true;
      if (children[i].shadowRoot && hasContent(children[i].shadowRoot)) return true;
      if (hasContent(children[i])) return true;
    }
    return false;
  }

  function slotIsFilled(slot) {
    if (hasContent(slot.element)) return true;
    return !!(slot.element.shadowRoot && hasContent(slot.element.shadowRoot));
  }

  function reflectSlot(slot) {
    var filled = slotIsFilled(slot);
    if (filled === slot.filled) return;
    slot.filled = filled;
    if (slot.section) slot.section.classList.toggle("has-embedded-content", filled);
    log(
      (filled ? "Embedded content attached to " : "Embedded content cleared from ") +
        slot.embeddedId +
        ".",
    );
  }

  // A Scene sizes itself as its media loads, and the first pass after an insertion
  // reports content of zero height, so the state is re-read a few times.
  function observeEmbeddedContent() {
    embeddedSlots.forEach(function (slot) {
      slot.section = sectionOf(slot.element);
      slot.filled = false;

      function check() {
        reflectSlot(slot);
      }

      function recheck() {
        [0, 150, 600, 1500].forEach(function (delay) {
          setTimeout(check, delay);
        });
      }

      if (window.MutationObserver) {
        new MutationObserver(recheck).observe(slot.element, { childList: true, subtree: true });
      }
      // Covers a Scene the SDK renders into a shadow root of the slot itself, where
      // no mutation of the slot's own children is ever observed.
      if (window.ResizeObserver) {
        new ResizeObserver(check).observe(slot.element);
      }
      recheck();
    });
  }

  function removeNetlifyBadge() {
    var badge = document.getElementById("nl-badge-frame");
    if (badge && badge.parentNode) badge.parentNode.removeChild(badge);
  }

  function observeHostingChrome() {
    removeNetlifyBadge();
    if (!window.MutationObserver) return;
    new MutationObserver(removeNetlifyBadge).observe(document.documentElement, {
      childList: true,
      subtree: true,
    });
  }

  function isVisible(element) {
    if (!element || !element.getBoundingClientRect) return false;
    var style = window.getComputedStyle(element);
    var rect = element.getBoundingClientRect();
    return (
      style.display !== "none" &&
      style.visibility !== "hidden" &&
      rect.width > 0 &&
      rect.height > 0
    );
  }

  function collectFocusable(root, result) {
    var selector =
      "[data-focusable],button,a[href],input,select,textarea,[tabindex]:not([tabindex='-1']),iframe";
    var elements = root.querySelectorAll ? root.querySelectorAll(selector) : [];
    var i;

    for (i = 0; i < elements.length; i += 1) {
      if (isVisible(elements[i]) && !elements[i].disabled) result.push(elements[i]);
      if (elements[i].shadowRoot) collectFocusable(elements[i].shadowRoot, result);
    }
  }

  function focusables() {
    var result = [];
    collectFocusable(document, result);
    return result;
  }

  function deepActiveElement() {
    var active = document.activeElement;
    while (active && active.shadowRoot && active.shadowRoot.activeElement) {
      active = active.shadowRoot.activeElement;
    }
    return active;
  }

  function directionForKey(keyCode) {
    if (keyCode === 37) return "left";
    if (keyCode === 38) return "up";
    if (keyCode === 39) return "right";
    if (keyCode === 40) return "down";
    return null;
  }

  function nextInDirection(current, direction) {
    var currentRect = current.getBoundingClientRect();
    var cx = currentRect.left + currentRect.width / 2;
    var cy = currentRect.top + currentRect.height / 2;
    var candidates = focusables();
    var best = null;
    var bestScore = Infinity;
    var i;

    for (i = 0; i < candidates.length; i += 1) {
      var candidate = candidates[i];
      if (candidate === current) continue;
      var rect = candidate.getBoundingClientRect();
      var x = rect.left + rect.width / 2;
      var y = rect.top + rect.height / 2;
      var primary;
      var secondary;

      if (direction === "left" && x >= cx) continue;
      if (direction === "right" && x <= cx) continue;
      if (direction === "up" && y >= cy) continue;
      if (direction === "down" && y <= cy) continue;

      primary = direction === "left" || direction === "right" ? Math.abs(x - cx) : Math.abs(y - cy);
      secondary = direction === "left" || direction === "right" ? Math.abs(y - cy) : Math.abs(x - cx);

      var score = primary * 10 + secondary;
      if (score < bestScore) {
        best = candidate;
        bestScore = score;
      }
    }

    return best;
  }

  function closestAction(element) {
    while (element && element !== document) {
      if (element.getAttribute && element.getAttribute("data-action")) return element;
      element = element.parentNode || (element.host ? element.host : null);
    }
    return null;
  }

  function isEmbeddedSlot(element) {
    return embeddedSlots.some(function (slot) {
      return slot.element === element;
    });
  }

  function isInEmbeddedScene(element) {
    while (element) {
      if (isEmbeddedSlot(element)) return true;
      var root = element.getRootNode ? element.getRootNode() : null;
      element = element.parentNode || (root && root.host ? root.host : null);
    }
    return false;
  }

  // Mirrors the film ids the native labs route on, so ctvlab://play/<id> and
  // /play/<id> name the same title on all four platforms.
  var catalogue = {
    sintel: "Sintel",
    spring: "Spring",
    "big-buck-bunny": "Big Buck Bunny",
    "tears-of-steel": "Tears of Steel",
    "elephants-dream": "Elephants Dream",
    "cosmos-laundromat": "Cosmos Laundromat",
    "caminandes-llamigos": "Caminandes",
    "caminandes-gran-dillama": "Caminandes",
  };

  function requestedFilm() {
    var query = window.location.search.match(/[?&]play=([^&]+)/);
    var path = window.location.pathname.match(/\/play\/([^/?#]+)/);
    var hash = window.location.hash.match(/^#\/?play\/([^/?#]+)/);
    var film = (query && query[1]) || (path && path[1]) || (hash && hash[1]);
    return film ? decodeURIComponent(film).toLowerCase() : null;
  }

  function searchWithPlay(filmId) {
    var parts = window.location.search
      .replace(/^\?/, "")
      .split("&")
      .filter(function (part) {
        return part && part.indexOf("play=") !== 0;
      });
    if (filmId) parts.push("play=" + encodeURIComponent(filmId));
    return parts.length ? "?" + parts.join("&") : "";
  }

  // Keeps the address bar on the screen actually shown, so the player URL can be
  // copied out of the app and pasted into a Scene CTA.
  function reflectPlayer(filmId) {
    if (!window.history || !history.replaceState) return;
    var path = /\/play\//.test(window.location.pathname) ? "/" : window.location.pathname;
    var hash = /^#\/?play\//.test(window.location.hash) ? "" : window.location.hash;
    history.replaceState(null, "", path + searchWithPlay(filmId) + hash);
  }

  function openFilm(filmId) {
    var title = catalogue[filmId];
    if (!title) {
      log("Ignored unknown film id: " + filmId);
      return;
    }
    openPlayer(title, filmId);
  }

  function openPlayer(title, filmId) {
    trackScreen("player").catch(function (error) {
      log("Screen tracking failed: " + error);
    });
    lastFocused = deepActiveElement();
    document.getElementById("player-name").textContent = title || "Sintel";
    var player = document.getElementById("player");
    var video = document.getElementById("video");
    player.classList.add("open");
    player.setAttribute("aria-hidden", "false");
    video.play().catch(function () {});
    reflectPlayer(filmId || null);
    setTimeout(function () {
      player.querySelector("[data-action='close-player']").focus();
    }, 0);
  }

  function closePlayer() {
    var player = document.getElementById("player");
    var video = document.getElementById("video");
    video.pause();
    player.classList.remove("open");
    player.setAttribute("aria-hidden", "true");
    trackScreen("home").catch(function (error) {
      log("Screen tracking failed: " + error);
    });
    reflectPlayer(null);
    if (lastFocused) lastFocused.focus();
  }

  function openLab() {
    trackScreen("lab").catch(function (error) {
      log("Screen tracking failed: " + error);
    });
    lastFocused = deepActiveElement();
    var panel = document.getElementById("lab-panel");
    panel.classList.add("open");
    panel.setAttribute("aria-hidden", "false");
    setTimeout(function () {
      panel.querySelector("[data-action='close-lab']").focus();
    }, 0);
  }

  function closeLab() {
    var panel = document.getElementById("lab-panel");
    panel.classList.remove("open");
    panel.setAttribute("aria-hidden", "true");
    trackScreen("home").catch(function (error) {
      log("Screen tracking failed: " + error);
    });
    if (lastFocused) lastFocused.focus();
  }

  function handleAction(target) {
    var actionElement = closestAction(target);
    var action = actionElement && actionElement.getAttribute("data-action");
    if (action === "play") openFilm("sintel");
    if (action === "show-lab") openLab();
    if (action === "close-lab") closeLab();
    if (action === "close-player") closePlayer();
    if (action === "reset-channel") window.location.replace(urlWithReset());
  }

  document.addEventListener("click", function (event) {
    var card = event.target;
    while (card && card !== document && !card.getAttribute("data-film")) {
      card = card.parentNode;
    }
    if (card && card.getAttribute && card.getAttribute("data-film")) {
      openFilm(card.getAttribute("data-film"));
      return;
    }
    handleAction(event.target);
  });

  document.addEventListener("keydown", function (event) {
    var keyCode = event.keyCode || event.which;
    var direction = directionForKey(keyCode);
    var active = deepActiveElement();

    if (keyCode === 10009 || keyCode === 461 || keyCode === 27 || event.key === "Escape") {
      if (document.getElementById("player").classList.contains("open")) closePlayer();
      else if (document.getElementById("lab-panel").classList.contains("open")) closeLab();
      return;
    }

    if (keyCode === 13) {
      handleAction(active);
      return;
    }

    // Once focus is inside Airship content, let the SDK/browser expose its native
    // remote behavior. This is the behavior this lab exists to evaluate.
    if (!direction || !active || isInEmbeddedScene(active)) return;

    var next = nextInDirection(active, direction);
    if (next) {
      event.preventDefault();
      next.focus();
      if (next.scrollIntoView) {
        next.scrollIntoView({ behavior: "smooth", block: "center", inline: "center" });
      }
    }
  });

  function urlWithReset() {
    var separator = window.location.search ? "&" : "?";
    return window.location.pathname + window.location.search + separator + "reset=1";
  }

  function urlWithoutReset() {
    var search = window.location.search.replace(/([?&])reset=1(&|$)/, "$1").replace(/[?&]$/, "");
    return window.location.pathname + search;
  }

  function emptyDatabase(name) {
    return new Promise(function (resolve) {
      var open = indexedDB.open(name);
      open.onerror = open.onblocked = function () {
        resolve(name + ": unavailable");
      };
      open.onsuccess = function () {
        var database = open.result;
        var stores = Array.prototype.slice.call(database.objectStoreNames);
        if (!stores.length) {
          database.close();
          return resolve(name + ": empty");
        }
        var transaction = database.transaction(stores, "readwrite");
        stores.forEach(function (store) {
          transaction.objectStore(store).clear();
        });
        transaction.oncomplete = transaction.onerror = transaction.onabort = function () {
          database.close();
          resolve(name + ": cleared " + stores.length);
        };
      };
    });
  }

  // Dismissing an embedded Scene finishes its schedule for the current channel, so
  // seeing it again needs a new channel. Object stores are emptied rather than the
  // databases deleted: a deleteDatabase that another client blocks stays pending and
  // every later open waits on it, which leaves window.UA unresolved with no error.
  function resetChannel() {
    setStatus("Resetting the Web channel…");
    var databases =
      window.indexedDB && indexedDB.databases ? indexedDB.databases() : Promise.resolve([]);

    databases
      .then(function (list) {
        return Promise.all(
          list.map(function (database) {
            return emptyDatabase(database.name);
          }),
        );
      })
      .then(function (results) {
        results.forEach(log);
      })
      .catch(function (error) {
        log("Reset error: " + error);
      })
      .then(function () {
        try {
          localStorage.clear();
          sessionStorage.clear();
        } catch (error) {
          log("Storage not clearable: " + error);
        }
        window.location.replace(urlWithoutReset());
      });
  }

  window.addEventListener("load", function () {
    observeHostingChrome();
    if (/[?&]reset=1(?:&|$)/.test(window.location.search)) {
      resetChannel();
      return;
    }
    observeEmbeddedContent();
    loadAirship();
    var film = requestedFilm();
    if (film) {
      openFilm(film);
      return;
    }
    var play = document.querySelector("[data-action='play']");
    if (play) play.focus();
  });
}());
