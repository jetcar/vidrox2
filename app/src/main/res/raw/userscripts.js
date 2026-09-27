/*
 * VidroX userscripts.
 *
 * Injected at document start (before YouTube's own scripts run) when the
 * WebView supports DOCUMENT_START_SCRIPT, with a fallback evaluation after
 * page load otherwise. Nothing outside `whenDomReady` callbacks may touch
 * document.head/document.body — they do not exist yet at document start.
 */
(function () {
  if (window.__vidroxUserscriptLoaded) return;
  window.__vidroxUserscriptLoaded = true;

  function whenDomReady(fn) {
    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", fn, { once: true });
    } else {
      fn();
    }
  }

  /* Start spoofViewport.js */
  // Enables 4K resolution tricking youtube into thinking that we are on a 4K TV
  whenDomReady(function () {
    //if (window.screen.width >= 3840 || window.screen.height >= 2160) return;

    var existing = document.querySelector('meta[name="viewport"]');
    if (existing) {
      existing.setAttribute(
        "content",
        "width=3840, height=2160, initial-scale=1.0"
      );
    } else {
      var meta = document.createElement("meta");
      meta.name = "viewport";
      meta.content = "width=3840, height=2160, initial-scale=1.0";
      document.head.appendChild(meta);
    }
  });
  /* End spoofViewport.js */

  /* Start menuTrigger.js */
  // Add a "button" to fool you...
  whenDomReady(function () {
    function getSearchBar() {
      const searchBars = document.querySelectorAll(
        '[idomkey="ytLrSearchBarSearchTextBox"]'
      );
      return searchBars[searchBars.length - 1] ?? null;
    }

    function addMenuButton() {
      const searchBar = getSearchBar();
      if (!searchBar) return;

      const parent = searchBar.parentNode;
      if (parent.querySelector('button[data-notubetv="menu"]')) return; // already exists

      // Align horizontally to the search box
      parent.style.display = "flex";
      parent.style.flexDirection = "row";
      parent.style.alignItems = "center";

      // Create the NoTUbeTV Menu button
      const menuButton = document.createElement("button");
      menuButton.setAttribute("data-notubetv", "menu");
      menuButton.innerHTML = `
      <svg xmlns="http://www.w3.org/2000/svg" height="56px" viewBox="0 -960 960 960" width="56px" fill="#FFFFFF" fill-opacity="0.8">
        <path d="M480-480q0-91 64.5-155.5T700-700q91 0 155.5 64.5T920-480H480ZM260-260q-91 0-155.5-64.5T40-480h440q0 91-64.5 155.5T260-260Zm220-220q-91 0-155.5-64.5T260-700q0-91 64.5-155.5T480-920v440Zm0 440v-440q91 0 155.5 64.5T700-260q0 91-64.5 155.5T480-40Z"/>
      </svg>`;
      menuButton.style.marginLeft = "54px";
      menuButton.style.padding = "35px";
      menuButton.style.background = "rgba(255, 255, 255, 0.1)";
      menuButton.style.border = "none";
      menuButton.style.borderRadius = "88px";

      // Insert right next the search box
      parent.insertBefore(menuButton, searchBar.nextSibling);
    }

    addMenuButton();

    // Here the fooling part begins.
    // If the search tab is focused and the 'right arrow" is pressed, open up the menu.

    document.addEventListener("keydown", function (event) {
      if (event.key === "ArrowRight") {
        const searchBar = getSearchBar();
        const isFocused = searchBar?.classList?.contains(
          "ytLrSearchTextBoxFocused"
        );
        if (searchBar && isFocused) {
          modernUI(); // from 'userscript.js'
          const menuButton = document.querySelector(
            'button[data-notubetv="menu"]'
          );
          menuButton.style.background = "white";
        }
      }
    });

    const observer = new MutationObserver((mutations) => {
      const searchBar = getSearchBar();
      if (
        searchBar &&
        !searchBar.parentNode.querySelector('[data-notubetv="menu"]')
      ) {
        addMenuButton(); // Re-add if missing
      }
    });

    observer.observe(document.body, {
      childList: true,
      subtree: true,
    });
  });
  /* End menuTrigger.js */

  /* Start exitBridge.js */
  // Exit Bridge to react to exit button call.
  whenDomReady(function () {
    const observer = new MutationObserver((mutations, obs) => {
      const exitButton = document.querySelector(
        ".ytVirtualListItemLast ytlr-button.ytLrButtonLargeShape"
      );

      if (exitButton) {
        exitButton.addEventListener(
          "keydown",
          (e) => {
            if (
              (e.key === "Enter" || e.keyCode === 13) &&
              typeof ExitBridge !== "undefined" &&
              ExitBridge.onExitCalled
            ) {
              e.preventDefault();
              e.stopPropagation();
              ExitBridge.onExitCalled();
            }
          },
          true
        );
      }
    });
    observer.observe(document.body, { childList: true, subtree: true });
  });
  /* End exitBridge.js */

  /* Start TizenTubeScripts.js */
  (function () {
    "use strict";

    const CONFIG_KEY = "ytaf-configuration";
    const defaultConfig = {
      enableAdBlock: true,
      enableSponsorBlock: true,
      sponsorBlockManualSkips: [],
      enableSponsorBlockSponsor: true,
      enableSponsorBlockIntro: true,
      enableSponsorBlockOutro: true,
      enableSponsorBlockInteraction: true,
      enableSponsorBlockSelfPromo: true,
      enableSponsorBlockMusicOfftopic: true,
      enableShorts: false,
    };

    let localConfig;

    try {
      localConfig = JSON.parse(window.localStorage[CONFIG_KEY]);
    } catch (err) {
      //console.warn('Config read failed:', err);
      localConfig = { ...defaultConfig };
      try {
        window.localStorage[CONFIG_KEY] = JSON.stringify(localConfig);
      } catch (storageErr) {
        // Storage unavailable; keep the in-memory defaults.
      }
    }

    window.localConfig = localConfig;

    window.configRead = function (key) {
      if (window.localConfig[key] === undefined) {
        window.localConfig[key] = defaultConfig[key];
      }
      return window.localConfig[key];
    };

    window.configWrite = function (key, value) {
      window.localConfig[key] = value;
      window.localStorage[CONFIG_KEY] = JSON.stringify(window.localConfig);
    };

    function showToast(title, subtitle, thumbnails) {
      const toastCmd = {
        openPopupAction: {
          popupType: "TOAST",
          popup: {
            overlayToastRenderer: {
              title: {
                simpleText: title,
              },
              subtitle: {
                simpleText: subtitle,
              },
            },
          },
        },
      };
      resolveCommand(toastCmd);
    }

    function showModal(title, content, selectIndex, id, update) {
      if (!update) {
        const closeCmd = {
          signalAction: {
            signal: "POPUP_BACK",
          },
        };
        resolveCommand(closeCmd);
      }

      const modalCmd = {
        openPopupAction: {
          popupType: "MODAL",
          popup: {
            overlaySectionRenderer: {
              overlay: {
                overlayTwoPanelRenderer: {
                  actionPanel: {
                    overlayPanelRenderer: {
                      header: {
                        overlayPanelHeaderRenderer: {
                          title: {
                            simpleText: title,
                          },
                        },
                      },
                      content: {
                        overlayPanelItemListRenderer: {
                          items: content,
                          selectedIndex: selectIndex,
                        },
                      },
                    },
                  },
                  backButton: {
                    buttonRenderer: {
                      accessibilityData: {
                        accessibilityData: {
                          label: "Back",
                        },
                      },
                      command: {
                        signalAction: {
                          signal: "POPUP_BACK",
                        },
                      },
                    },
                  },
                },
              },
              dismissalCommand: {
                signalAction: {
                  signal: "POPUP_BACK",
                },
              },
            },
          },
          uniqueId: id,
        },
      };

      if (update) {
        modalCmd.openPopupAction.shouldMatchUniqueId = true;
        modalCmd.openPopupAction.updateAction = true;
      }

      resolveCommand(modalCmd);
    }

    function buttonItem(title, icon, commands) {
      const button = {
        compactLinkRenderer: {
          serviceEndpoint: {
            commandExecutorCommand: {
              commands,
            },
          },
        },
      };

      if (title) {
        button.compactLinkRenderer.title = {
          simpleText: title.title,
        };
      }

      if (title.subtitle) {
        button.compactLinkRenderer.subtitle = {
          simpleText: title.subtitle,
        };
      }

      if (icon) {
        button.compactLinkRenderer.icon = {
          iconType: icon.icon,
        };
      }

      if (icon && icon.secondaryIcon) {
        button.compactLinkRenderer.secondaryIcon = {
          iconType: icon.secondaryIcon,
        };
      }

      return button;
    }

    window.modernUI = function modernUI(update, parameters) {
      const settings = [
        {
          name: "Ad block",
          icon: "DOLLAR_SIGN",
          value: "enableAdBlock",
        },
        {
          name: "SponsorBlock",
          icon: "MONEY_HAND",
          value: "enableSponsorBlock",
        },
        {
          name: "Skip Sponsor Segments",
          icon: "MONEY_HEART",
          value: "enableSponsorBlockSponsor",
        },
        {
          name: "Skip Intro Segments",
          icon: "PLAY_CIRCLE",
          value: "enableSponsorBlockIntro",
        },
        {
          name: "Skip Outro Segments",
          value: "enableSponsorBlockOutro",
        },
        {
          name: "Skip Interaction Reminder Segments",
          value: "enableSponsorBlockInteraction",
        },
        {
          name: "Skip Self-Promotion Segments",
          value: "enableSponsorBlockSelfPromo",
        },
        {
          name: "Skip Off-Topic Music Segments",
          value: "enableSponsorBlockMusicOfftopic",
        },
        {
          name: "Shorts",
          icon: "YOUTUBE_SHORTS_FILL_24",
          value: "enableShorts",
        },
      ];

      const buttons = [];

      let index = 0;
      for (const setting of settings) {
        const currentVal = setting.value ? configRead(setting.value) : null;
        buttons.push(
          buttonItem(
            { title: setting.name, subtitle: setting.subtitle },
            {
              icon: setting.icon ? setting.icon : "CHEVRON_DOWN",
              secondaryIcon:
                currentVal === null
                  ? "CHEVRON_RIGHT"
                  : currentVal
                    ? "CHECK_BOX"
                    : "CHECK_BOX_OUTLINE_BLANK",
            },
            currentVal !== null
              ? [
                {
                  setClientSettingEndpoint: {
                    settingDatas: [
                      {
                        clientSettingEnum: {
                          item: setting.value,
                        },
                        boolValue: !configRead(setting.value),
                      },
                    ],
                  },
                },
                {
                  customAction: {
                    action: "SETTINGS_UPDATE",
                    parameters: [index],
                  },
                },
              ]
              : [
                {
                  customAction: {
                    action: "OPTIONS_SHOW",
                    parameters: {
                      options: setting.options,
                      selectedIndex: 0,
                      update: false,
                    },
                  },
                },
              ]
          )
        );
        index++;
      }

      showModal(
        "NotubeTv Settings",
        buttons,
        parameters && parameters.length > 0 ? parameters[0] : 0,
        "tt-settings",
        update
      );
    };

    function resolveCommand(cmd, _) {
      for (const key in window._yttv) {
        if (
          window._yttv[key] &&
          window._yttv[key].instance &&
          window._yttv[key].instance.resolveCommand
        ) {
          return window._yttv[key].instance.resolveCommand(cmd, _);
        }
      }
    }

    function patchResolveCommand() {
      for (const key in window._yttv) {
        if (
          window._yttv[key] &&
          window._yttv[key].instance &&
          window._yttv[key].instance.resolveCommand
        ) {
          const ogResolve = window._yttv[key].instance.resolveCommand;
          window._yttv[key].instance.resolveCommand = function (cmd, _) {
            if (cmd.setClientSettingEndpoint) {
              for (const settings of cmd.setClientSettingEndpoint.settingDatas) {
                if (!settings.clientSettingEnum.item.includes("_")) {
                  for (const setting of cmd.setClientSettingEndpoint.settingDatas) {
                    const valName = Object.keys(setting).find((key) =>
                      key.includes("Value")
                    );
                    const value =
                      valName === "intValue"
                        ? Number(setting[valName])
                        : setting[valName];
                    if (valName === "arrayValue") {
                      const arr = configRead(setting.clientSettingEnum.item);
                      if (arr.includes(value)) {
                        arr.splice(arr.indexOf(value), 1);
                      } else {
                        arr.push(value);
                      }
                      configWrite(setting.clientSettingEnum.item, arr);
                    } else configWrite(setting.clientSettingEnum.item, value);
                  }
                }
              }
            } else if (cmd.customAction) {
              customAction(cmd.customAction.action, cmd.customAction.parameters);
              return true;
            } else if (cmd?.showEngagementPanelEndpoint?.customAction) {
              customAction(
                cmd.showEngagementPanelEndpoint.customAction.action,
                cmd.showEngagementPanelEndpoint.customAction.parameters
              );
              return true;
            }
            return ogResolve.call(this, cmd, _);
          };
        }
      }
    }

    function customAction(action, parameters) {
      switch (action) {
        case "SETTINGS_UPDATE":
          modernUI(true, parameters);
          break;
        case "SKIP":
          const video = document.querySelector("video");
          if (video) {
            video.currentTime = parameters.time;
          }
          resolveCommand({
            signalAction: {
              signal: "POPUP_BACK",
            },
          });
          break;
      }
    }

    /*
     * Surgical ad removal, matching the proven behaviour of upstream
     * TizenTube / youtube-webos.
     *
     * We intentionally do NOT recursively delete every ad-related key. In
     * particular the ad telemetry/heartbeat payloads (adBreakHeartbeatParams,
     * adLayoutLoggingData, adDurationRemaining, …) are left untouched: they
     * carry the "ad was shown" handshake, and stripping them makes YouTube's
     * TV backend escalate to server-stitched ads that no client-side filter
     * can remove. We only neutralise the ad *content* payloads and remove ad
     * *tiles* from feeds.
     */

    // Neutralise the player-response ad payloads (top level of /player and
    // watch-next responses). Emptying rather than deleting matches upstream
    // and keeps the shapes the player expects.
    function scrubPlayerAds(r) {
      if (Array.isArray(r?.adPlacements)) r.adPlacements = [];
      if (r?.playerAds) r.playerAds = false;
      if (Array.isArray(r?.adSlots)) r.adSlots = [];
    }

    // Remove ad tiles from a single section list: full-width ad cards, the
    // home masthead ad, and ad tiles inside horizontal shelves.
    function filterAdsFromSectionList(sectionList) {
      if (!sectionList || !Array.isArray(sectionList.contents)) return;

      sectionList.contents = sectionList.contents.filter(
        (elm) =>
          !elm?.adSlotRenderer &&
          !elm?.tvMastheadRenderer &&
          !elm?.mastheadAd &&
          !elm?.promotedSparklesTextSearchRenderer
      );

      for (const shelf of sectionList.contents) {
        const items = shelf?.shelfRenderer?.content?.horizontalListRenderer?.items;
        if (Array.isArray(items)) {
          shelf.shelfRenderer.content.horizontalListRenderer.items = items.filter(
            (item) => !item?.adSlotRenderer
          );
        }
      }
    }

    // Strip ads from every payload surface, mirroring the shorts traversal.
    function stripAdsFromResponse(r) {
      scrubPlayerAds(r);
      forEachSectionList(r, filterAdsFromSectionList);

      // Sponsored reels in the shorts player queue.
      if (Array.isArray(r?.entries)) {
        r.entries = r.entries.filter(
          (entry) => !entry?.command?.reelWatchEndpoint?.adClientParams?.isAd
        );
      }
    }

    /*
     * Server-stitched ("SSAP") ads are part of the video stream itself, so
     * removing ad payloads cannot prevent them from playing. Before scrubbing
     * we record any ad time ranges the player response declares, and a
     * watchdog seeks past them during playback.
     */
    const adSkipState = { videoId: null, ranges: [] };
    window.__vidroxAdSkipState = adSkipState; // exposed for debugging

    function collectDeclaredAdRanges(value, ranges, seen) {
      if (!value || typeof value !== "object" || seen.has(value)) return;
      seen.add(value);

      if (Array.isArray(value)) {
        for (const entry of value) collectDeclaredAdRanges(entry, ranges, seen);
        return;
      }

      const offset = value.adTimeOffset;
      if (
        offset &&
        offset.offsetStartMilliseconds !== undefined &&
        offset.offsetEndMilliseconds !== undefined
      ) {
        const start = Number(offset.offsetStartMilliseconds) / 1000;
        const end = Number(offset.offsetEndMilliseconds) / 1000;
        // Zero-length offsets are client-side trigger points, not stitched
        // segments; only real ranges are skippable.
        if (isFinite(start) && isFinite(end) && end - start > 0.5) {
          ranges.push({ start, end });
        }
      }

      for (const key of Object.keys(value)) {
        collectDeclaredAdRanges(value[key], ranges, seen);
      }
    }

    function captureAdRangesFromPlayerResponse(r) {
      const videoId = r?.videoDetails?.videoId;
      if (!videoId) return;

      const ranges = [];
      collectDeclaredAdRanges(r, ranges, new WeakSet());
      adSkipState.videoId = videoId;
      adSkipState.ranges = ranges;
      if (ranges.length) {
        console.info(
          `[vidrox] captured ${ranges.length} declared ad range(s) for ${videoId}`
        );
      }
    }

    /*
     * Central response filter used by every parse hook below. Must never
     * throw: an exception escaping into YouTube's own parsing would break
     * the whole app.
     */
    const processedPayloads = typeof WeakSet !== "undefined" ? new WeakSet() : null;

    function processApiResponse(r) {
      if (!r || typeof r !== "object") return r;
      if (processedPayloads) {
        if (processedPayloads.has(r)) return r;
        processedPayloads.add(r);
      }

      try {
        if (configRead("enableAdBlock")) {
          captureAdRangesFromPlayerResponse(r);
          stripAdsFromResponse(r);
        }

        // Filter out shorts/reels everywhere when shorts are disabled
        if (!configRead("enableShorts")) {
          stripShortsFromResponse(r);
        }
      } catch (err) {
        console.error("[vidrox] response filtering failed:", err);
      }

      return r;
    }

    /*
     * Parse/serialize hooks. This script runs at document start, so these
     * cover YouTube's bootstrap traffic too. Every path YouTube can use to
     * turn bytes into objects is hooked: JSON.parse (XHR responseText),
     * Response.json (fetch), and the XHR `response` getter (responseType
     * "json").
     */
    const origParse = JSON.parse;
    JSON.parse = function () {
      return processApiResponse(origParse.apply(this, arguments));
    };

    // Mark inline (preview) playback requests as ad-free, mirroring TizenTube.
    // The flag is set on a deep clone, never on YouTube's live request object:
    // mutating the original corrupts player state across subsequent requests.
    const origStringify = JSON.stringify;
    JSON.stringify = function (value, replacer, space) {
      try {
        const playbackCtx = value?.playbackContext?.contentPlaybackContext;
        if (
          playbackCtx &&
          !playbackCtx.isInlinePlaybackNoAd &&
          configRead("enableAdBlock")
        ) {
          const clone = origParse(origStringify.call(this, value));
          clone.playbackContext.contentPlaybackContext.isInlinePlaybackNoAd = true;
          return origStringify.call(this, clone, replacer, space);
        }
      } catch (err) {
        // Leave the request untouched.
      }
      return origStringify.call(this, value, replacer, space);
    };

    if (
      typeof Response !== "undefined" &&
      Response.prototype &&
      typeof Response.prototype.json === "function"
    ) {
      const origResponseJson = Response.prototype.json;
      Response.prototype.json = function () {
        return origResponseJson.apply(this, arguments).then(processApiResponse);
      };
    }

    if (typeof XMLHttpRequest !== "undefined") {
      const xhrResponse = Object.getOwnPropertyDescriptor(
        XMLHttpRequest.prototype,
        "response"
      );
      if (xhrResponse && xhrResponse.get) {
        Object.defineProperty(XMLHttpRequest.prototype, "response", {
          configurable: true,
          enumerable: xhrResponse.enumerable,
          get: function () {
            const value = xhrResponse.get.call(this);
            if (this.responseType === "json") processApiResponse(value);
            return value;
          },
        });
      }
    }

    /*
     * YouTube's TV bundle keeps its own captured JSON object
     * (window._yttv[key].JSON), which bypasses the global hooks above.
     * Re-point those references at our patched functions, mirroring
     * upstream TizenTube.
     */
    function patchBundledJsonReferences() {
      if (!window._yttv) return;
      for (const key in window._yttv) {
        const mod = window._yttv[key];
        if (
          mod &&
          mod.JSON &&
          typeof mod.JSON.parse === "function" &&
          mod.JSON.parse !== JSON.parse
        ) {
          mod.JSON.parse = JSON.parse;
          mod.JSON.stringify = JSON.stringify;
          console.info("[vidrox] patched bundled JSON reference:", key);
        }
      }
    }

    /*
     * Last line of defense for ads that survive payload scrubbing (e.g.
     * server-stitched ones): auto-click any visible skip-ad button, and seek
     * past ad ranges the player response declared. Seeking additionally
     * requires ad UI to be visible in the player, so a declared range that
     * merely marks an overlay-ad display window can never skip real content.
     */
    const AD_SKIP_TICK_MS = 500;
    // Unambiguously ad-related; safe to click whenever visible.
    // 'skip-ad' and aria-label variants are generic fallbacks that survive
    // future renames.
    const AD_SKIP_BUTTON_SELECTORS = [
      '[class*="skip-ad" i]',
      '[class*="SkipAd"]',
      '[idomkey*="skip-ad" i]',
      '[idomkey*="skipAd"]',
      '[aria-label*="skip ad" i]',
    ].join(", ");
    // The leanback skip control. Base name `ytlr-skip-button` is confirmed
    // from captured watch-page DOM (its animation class is
    // `ytlr-skip-button-animate-in`). Only clicked while ad UI is on screen,
    // so a non-ad skip control can never be spammed.
    const AD_CONTEXT_SKIP_BUTTON_SELECTORS = [
      AD_SKIP_BUTTON_SELECTORS,
      "ytlr-skip-button",
      '[class*="ytlr-skip-button"]',
      '[class*="skip-button" i]',
    ].join(", ");

    // Ad UI elements, verified against captured YouTube TV watch-page DOM:
    // ads render <ytlr-ad-attribution> / <ytlr-ad-notify> elements. These
    // exist in the DOM but stay zero-size until an ad shows, so callers gate
    // on getBoundingClientRect (see isAdUiVisible). The kebab-case fallbacks
    // cover related ad elements the static capture didn't surface.
    const AD_OVERLAY_SELECTORS = [
      "ytlr-ad-attribution",
      "ytlr-ad-notify",
      '[class*="ytlr-ad-" i]',
      "[class*='ad-showing']",
      "[class*='ad-interrupting']",
    ].join(", ");

    function isAdUiVisible() {
      const overlay = document.querySelector(AD_OVERLAY_SELECTORS);
      if (!overlay) return false;
      const rect = overlay.getBoundingClientRect();
      return rect.width > 0 || rect.height > 0;
    }

    function clickSkipButton(adUiVisible) {
      const candidates = document.querySelectorAll(
        adUiVisible ? AD_CONTEXT_SKIP_BUTTON_SELECTORS : AD_SKIP_BUTTON_SELECTORS
      );
      for (const el of candidates) {
        const rect = el.getBoundingClientRect();
        if (!rect.width && !rect.height) continue; // not visible

        for (const type of ["mousedown", "mouseup", "click"]) {
          el.dispatchEvent(
            new MouseEvent(type, { bubbles: true, cancelable: true })
          );
        }
        const enterKey = {
          key: "Enter",
          keyCode: 13,
          bubbles: true,
          cancelable: true,
        };
        el.dispatchEvent(new KeyboardEvent("keydown", enterKey));
        el.dispatchEvent(new KeyboardEvent("keyup", enterKey));
        console.info(
          "[vidrox] auto-clicked ad skip button:",
          el.className || el.tagName
        );
        return true;
      }
      return false;
    }

    function skipDeclaredAdRange(video) {
      if (!adSkipState.ranges.length) return false;

      // Only act on the video the ranges were captured for.
      const match = location.hash.match(/[?&]v=([^&]+)/);
      if (match && adSkipState.videoId && match[1] !== adSkipState.videoId) {
        return false;
      }

      const current = video.currentTime;
      for (const range of adSkipState.ranges) {
        if (current >= range.start - 0.3 && current < range.end - 0.25) {
          let target = range.end + 0.05;
          if (isFinite(video.duration) && video.duration > 1) {
            target = Math.min(target, video.duration - 0.1);
          }
          video.currentTime = target;
          console.info(
            `[vidrox] skipped stitched ad segment ${range.start}s-${range.end}s`
          );
          try {
            showToast("Ad block", "Skipping ad");
          } catch (toastErr) {
            // Toast is cosmetic; never let it break the skip.
          }
          return true;
        }
      }
      return false;
    }

    let adSkipTickCount = 0;
    let adUiWasVisible = false;
    setInterval(() => {
      try {
        adSkipTickCount++;
        // The bundle can (re)create its JSON reference late; re-check every 2s.
        if (adSkipTickCount % 4 === 0) patchBundledJsonReferences();

        if (!configRead("enableAdBlock")) return;
        const video = document.querySelector("video");
        if (!video || video.paused || !isFinite(video.currentTime)) return;

        const adUiVisible = isAdUiVisible();
        if (adUiVisible !== adUiWasVisible) {
          adUiWasVisible = adUiVisible;
          console.info(`[vidrox] ad ui ${adUiVisible ? "appeared" : "gone"}`);
        }

        if (clickSkipButton(adUiVisible)) return;
        if (adUiVisible) skipDeclaredAdRange(video);
      } catch (err) {
        // Keep the watchdog alive no matter what.
      }
    }, AD_SKIP_TICK_MS);

    function getShelfTitleText(shelf) {
      return (
        shelf?.shelfRenderer?.headerRenderer?.shelfHeaderRenderer?.avatarLockup?.avatarLockupRenderer?.title?.runs?.map((run) => run.text).join("") ||
        shelf?.shelfRenderer?.headerRenderer?.shelfHeaderRenderer?.title?.runs?.map((run) => run.text).join("") ||
        shelf?.shelfRenderer?.headerRenderer?.shelfHeaderRenderer?.title?.simpleText ||
        ""
      );
    }

    function isShortsEndpoint(endpoint) {
      return Boolean(
        endpoint?.reelWatchEndpoint ||
        endpoint?.watchEndpoint?.playerParams?.includes?.("shorts") ||
        endpoint?.commandMetadata?.webCommandMetadata?.url?.includes?.("/shorts/") ||
        endpoint?.browseEndpoint?.browseId?.includes?.("FEshorts")
      );
    }

    function isShortsItem(item) {
      const gridVideo = item?.gridVideoRenderer;
      const tile = item?.tileRenderer;
      const reel = item?.reelItemRenderer;
      const shortsLockup = item?.shortsLockupViewModel;
      const navigationEndpoint =
        gridVideo?.navigationEndpoint ||
        tile?.onSelectCommand ||
        tile?.onLongPressCommand ||
        reel?.navigationEndpoint;
      const title =
        gridVideo?.title?.simpleText ||
        gridVideo?.title?.runs?.map((run) => run.text).join("") ||
        tile?.metadata?.tileMetadataRenderer?.title?.simpleText ||
        reel?.headline?.simpleText ||
        "";

      return Boolean(
        reel ||
        shortsLockup ||
        tile?.tvhtml5ShelfRendererType === "TVHTML5_TILE_RENDERER_TYPE_SHORTS" ||
        isShortsEndpoint(navigationEndpoint) ||
        /shorts/i.test(title)
      );
    }

    function isShortsShelf(shelf) {
      const title = getShelfTitleText(shelf);
      const items = shelf?.shelfRenderer?.content?.horizontalListRenderer?.items || [];

      // A reel shelf is shorts by definition.
      if (shelf?.reelShelfRenderer) {
        return true;
      }

      if (
        shelf?.shelfRenderer?.tvhtml5ShelfRendererType ===
        "TVHTML5_SHELF_RENDERER_TYPE_SHORTS"
      ) {
        return true;
      }

      if (/shorts/i.test(title)) {
        return true;
      }

      return items.length > 0 && items.every(isShortsItem);
    }

    function removeShortsFromShelf(shelf) {
      const items = shelf?.shelfRenderer?.content?.horizontalListRenderer?.items;
      if (!items) {
        return shelf;
      }

      shelf.shelfRenderer.content.horizontalListRenderer.items = items.filter(
        (item) => !isShortsItem(item)
      );

      if (!shelf.shelfRenderer.content.horizontalListRenderer.items.length) {
        return null;
      }

      return shelf;
    }

    function filterShortsFromSectionList(sectionList) {
      if (!sectionList || !Array.isArray(sectionList.contents)) return;
      sectionList.contents = sectionList.contents
        .filter((shelf) => !isShortsShelf(shelf))
        .map((shelf) => removeShortsFromShelf(shelf))
        .filter((shelf) => shelf != null);
    }

    /*
     * Feeds appear on more surfaces than the home page: search results,
     * subscription/channel tabs, the related-videos pivot next to the
     * player, and rows paged in via continuations. Both the ad filter and
     * the shorts filter run over every one of them via this single walker,
     * so a surface can never be handled for one but missed for the other.
     */
    function forEachSectionList(r, cb) {
      // Home screen
      cb(
        r?.contents?.tvBrowseRenderer?.content?.tvSurfaceContentRenderer
          ?.content?.sectionListRenderer
      );

      // Search results
      cb(r?.contents?.sectionListRenderer);

      // Rows paged in while scrolling home/search
      cb(r?.continuationContents?.sectionListContinuation);

      // Related videos next to/below the player
      cb(r?.contents?.singleColumnWatchNextResults?.pivot?.sectionListRenderer);

      // Subscriptions/channel tabs
      const navSections =
        r?.contents?.tvBrowseRenderer?.content?.tvSecondaryNavRenderer?.sections;
      if (Array.isArray(navSections)) {
        for (const section of navSections) {
          const tabs = section?.tvSecondaryNavSectionRenderer?.tabs;
          if (!Array.isArray(tabs)) continue;
          for (const tab of tabs) {
            cb(
              tab?.tabRenderer?.content?.tvSurfaceContentRenderer?.content
                ?.sectionListRenderer
            );
          }
        }
      }
    }

    // Strip shorts/reels from every payload surface when shorts are disabled.
    function stripShortsFromResponse(r) {
      forEachSectionList(r, filterShortsFromSectionList);

      // Items paged into a single row
      const horizontalItems = r?.continuationContents?.horizontalListContinuation?.items;
      if (Array.isArray(horizontalItems)) {
        r.continuationContents.horizontalListContinuation.items =
          horizontalItems.filter((item) => !isShortsItem(item));
      }

      // The reel (shorts) player's playback queue
      if (Array.isArray(r?.entries)) {
        r.entries = r.entries.filter(
          (entry) => !entry?.command?.reelWatchEndpoint
        );
      }

      // Left-nav "Shorts" entry (/youtubei/v1/guide). The rendered DOM uses
      // obfuscated classes, so the CSS fallback can't target it reliably.
      if (Array.isArray(r?.items)) {
        for (const section of r.items) {
          const sectionItems = section?.guideSectionRenderer?.items;
          if (Array.isArray(sectionItems)) {
            section.guideSectionRenderer.items = sectionItems.filter(
              (item) => !isShortsGuideEntry(item)
            );
          }
        }
      }

      // Guide startup behaviour that relaunches the app into the Shorts player.
      if (Array.isArray(r?.startupBehaviours)) {
        r.startupBehaviours = r.startupBehaviours.filter(
          (behaviour) => !behaviour?.launchToShorts
        );
      }
    }

    function isShortsGuideEntry(item) {
      const entry = item?.guideEntryRenderer;
      return Boolean(
        entry &&
        (isShortsEndpoint(entry.navigationEndpoint) ||
          /^YOUTUBE_SHORTS/.test(entry.icon?.iconType || ""))
      );
    }

    // The tiny-sha256 module, edited to export itself.
    var sha256 = function sha256(ascii) {
      function rightRotate(value, amount) {
        return (value >>> amount) | (value << (32 - amount));
      }
      var mathPow = Math.pow;
      var maxWord = mathPow(2, 32);
      var lengthProperty = "length";
      var i, j;
      var result = "";

      var words = [];
      var asciiBitLength = ascii[lengthProperty] * 8;

      var hash = (sha256.h = sha256.h || []);
      var k = (sha256.k = sha256.k || []);
      var primeCounter = k[lengthProperty];

      var isComposite = {};
      for (var candidate = 2; primeCounter < 64; candidate++) {
        if (!isComposite[candidate]) {
          for (i = 0; i < 313; i += candidate) {
            isComposite[i] = candidate;
          }
          hash[primeCounter] = (mathPow(candidate, 0.5) * maxWord) | 0;
          k[primeCounter++] = (mathPow(candidate, 1 / 3) * maxWord) | 0;
        }
      }

      ascii += "\x80";
      while ((ascii[lengthProperty] % 64) - 56) ascii += "\x00";
      for (i = 0; i < ascii[lengthProperty]; i++) {
        j = ascii.charCodeAt(i);
        if (j >> 8) return;
        words[i >> 2] |= j << (((3 - i) % 4) * 8);
      }
      words[words[lengthProperty]] = (asciiBitLength / maxWord) | 0;
      words[words[lengthProperty]] = asciiBitLength;

      for (j = 0; j < words[lengthProperty];) {
        var w = words.slice(j, (j += 16));
        var oldHash = hash;
        hash = hash.slice(0, 8);

        for (i = 0; i < 64; i++) {
          var w15 = w[i - 15],
            w2 = w[i - 2];

          var a = hash[0],
            e = hash[4];
          var temp1 =
            hash[7] +
            (rightRotate(e, 6) ^ rightRotate(e, 11) ^ rightRotate(e, 25)) +
            ((e & hash[5]) ^ (~e & hash[6])) +
            k[i] +
            (w[i] =
              i < 16
                ? w[i]
                : (w[i - 16] +
                  (rightRotate(w15, 7) ^ rightRotate(w15, 18) ^ (w15 >>> 3)) +
                  w[i - 7] +
                  (rightRotate(w2, 17) ^ rightRotate(w2, 19) ^ (w2 >>> 10))) |
                0);
          var temp2 =
            (rightRotate(a, 2) ^ rightRotate(a, 13) ^ rightRotate(a, 22)) +
            ((a & hash[1]) ^ (a & hash[2]) ^ (hash[1] & hash[2]));

          hash = [(temp1 + temp2) | 0].concat(hash);
          hash[4] = (hash[4] + temp1) | 0;
        }

        for (i = 0; i < 8; i++) {
          hash[i] = (hash[i] + oldHash[i]) | 0;
        }
      }

      for (i = 0; i < 8; i++) {
        for (j = 3; j + 1; j--) {
          var b = (hash[i] >> (j * 8)) & 255;
          result += (b < 16 ? 0 : "") + b.toString(16);
        }
      }
      return result;
    };

    // Copied from https://github.com/ajayyy/SponsorBlock/blob/9392d16617d2d48abb6125c00e2ff6042cb7bebe/src/config.ts#L179-L233
    const barTypes = {
      sponsor: { color: "#00d400", opacity: "0.7", name: "sponsored segment" },
      intro: { color: "#00ffff", opacity: "0.7", name: "intro" },
      outro: { color: "#0202ed", opacity: "0.7", name: "outro" },
      interaction: { color: "#cc00ff", opacity: "0.7", name: "interaction reminder" },
      selfpromo: { color: "#ffff00", opacity: "0.7", name: "self-promotion" },
      music_offtopic: { color: "#ff9900", opacity: "0.7", name: "non-music part" },
    };

    const sponsorblockAPI = "https://api.sponsor.ajay.app/api";

    class SponsorBlockHandler {
      video = null;
      active = true;
      attachVideoTimeout = null;
      nextSkipTimeout = null;
      sliderInterval = null;
      observer = null;
      scheduleSkipHandler = null;
      durationChangeHandler = null;
      segments = null;
      skippableCategories = [];
      manualSkippableCategories = [];

      constructor(videoID) {
        this.videoID = videoID;
      }

      async init() {
        if (!configRead("enableSponsorBlock")) return;

        const videoHash = sha256(this.videoID).substring(0, 4);
        const categories = [
          "sponsor", "intro", "outro", "interaction", "selfpromo", "music_offtopic",
        ];

        const resp = await new Promise((resolve) => {
          window.onNetworkBridgeResponse = (jsonString) => resolve(jsonString);
          NetworkBridge.fetch(
            `${sponsorblockAPI}/skipSegments/${videoHash}?categories=${encodeURIComponent(
              JSON.stringify(categories)
            )}`,
            this.videoID
          );
        });

        if (!resp) {
          return;
        }

        let result;
        try {
          result = JSON.parse(resp);
        } catch (err) {
          return;
        }

        if (!result || !result.segments || !result.segments.length) {
          return;
        }

        this.segments = result.segments;
        this.manualSkippableCategories = configRead("sponsorBlockManualSkips");
        this.skippableCategories = this.getSkippableCategories();

        this.scheduleSkipHandler = () => this.scheduleSkip();
        this.durationChangeHandler = () => this.buildOverlay();

        this.attachVideo();
        this.buildOverlay();
      }

      getSkippableCategories() {
        const skippableCategories = [];
        if (configRead("enableSponsorBlockSponsor")) skippableCategories.push("sponsor");
        if (configRead("enableSponsorBlockIntro")) skippableCategories.push("intro");
        if (configRead("enableSponsorBlockOutro")) skippableCategories.push("outro");
        if (configRead("enableSponsorBlockInteraction")) skippableCategories.push("interaction");
        if (configRead("enableSponsorBlockSelfPromo")) skippableCategories.push("selfpromo");
        if (configRead("enableSponsorBlockMusicOfftopic")) skippableCategories.push("music_offtopic");
        return skippableCategories;
      }

      attachVideo() {
        clearTimeout(this.attachVideoTimeout);
        this.attachVideoTimeout = null;

        this.video = document.querySelector("video");
        if (!this.video) {
          this.attachVideoTimeout = setTimeout(() => this.attachVideo(), 100);
          return;
        }

        this.video.addEventListener("play", this.scheduleSkipHandler);
        this.video.addEventListener("durationchange", this.durationChangeHandler);
      }

      buildOverlay() {
        if (this.segmentsoverlay) return;
        if (!this.video || !this.video.duration) return;

        const videoDuration = this.video.duration;

        this.segmentsoverlay = document.createElement("div");
        this.segments.forEach((segment) => {
          const [start, end] = segment.segment;
          const barType = barTypes[segment.category] || { color: "blue", opacity: 0.7 };
          const transform = `translateX(${(start / videoDuration) * 100.0}%) scaleX(${(end - start) / videoDuration})`;
          const elm = document.createElement("div");
          elm.classList.add("ytLrProgressBarPlayed");
          elm.style["background"] = barType.color;
          elm.style["opacity"] = barType.opacity;
          elm.style["-webkit-transform"] = transform;
          this.segmentsoverlay.appendChild(elm);
        });

        this.observer = new MutationObserver((mutations) => {
          mutations.forEach((m) => {
            if (m.removedNodes) {
              for (const node of m.removedNodes) {
                if (node === this.segmentsoverlay) {
                  this.slider.appendChild(this.segmentsoverlay);
                }
              }
            }
          });
        });

        this.sliderInterval = setInterval(() => {
          this.slider = document.querySelector('[idomkey="slider"]');
          if (this.slider) {
            clearInterval(this.sliderInterval);
            this.sliderInterval = null;
            this.observer.observe(this.slider, { childList: true });
            this.slider.appendChild(this.segmentsoverlay);
          }
        }, 500);
      }

      scheduleSkip() {
        clearTimeout(this.nextSkipTimeout);
        this.nextSkipTimeout = null;

        if (!this.active || this.video.paused) return;

        const current = this.video.currentTime;

        const nextSegments = this.segments
          .filter((seg) => seg.segment[0] >= current - 0.2)
          .sort((a, b) => a.segment[0] - b.segment[0]);

        if (!nextSegments.length) return;

        const [segment] = nextSegments;
        const [start, end] = segment.segment;

        if (current >= end) return;

        const delay = Math.max(0, (start - current) * 1000);

        this.nextSkipTimeout = setTimeout(() => {
          if (this.video.paused) return;
          if (!this.skippableCategories.includes(segment.category)) return;

          const skipName = barTypes[segment.category]?.name || segment.category;
          if (!this.manualSkippableCategories.includes(segment.category)) {
            showToast("SponsorBlock", `Skipping ${skipName}`);
            this.video.currentTime = end;
            this.scheduleSkip();
          }
        }, delay);
      }

      destroy() {
        this.active = false;
        this.segments = null;

        if (this.nextSkipTimeout) { clearTimeout(this.nextSkipTimeout); this.nextSkipTimeout = null; }
        if (this.attachVideoTimeout) { clearTimeout(this.attachVideoTimeout); this.attachVideoTimeout = null; }
        if (this.sliderInterval) { clearInterval(this.sliderInterval); this.sliderInterval = null; }
        if (this.observer) { this.observer.disconnect(); this.observer = null; }
        if (this.segmentsoverlay) { this.segmentsoverlay.remove(); this.segmentsoverlay = null; }

        if (this.video) {
          this.video.removeEventListener("play", this.scheduleSkipHandler);
          this.video.removeEventListener("durationchange", this.durationChangeHandler);
        }
      }
    }

    window.sponsorblock = null;

    window.addEventListener(
      "hashchange",
      () => {
        if (!configRead("enableSponsorBlock")) {
          if (window.sponsorblock) window.sponsorblock.destroy();
          return;
        }
        const match = location.hash.match(/[?&]v=([^&]+)/);
        const videoID = match ? match[1] : null;
        if (!videoID) return;
        const needsReload =
          !window.sponsorblock || window.sponsorblock.videoID != videoID;

        if (needsReload) {
          if (window.sponsorblock) {
            window.sponsorblock.destroy();
            window.sponsorblock = null;
          }
          window.sponsorblock = new SponsorBlockHandler(videoID);
          window.sponsorblock.init();
        }
      },
      false
    );

    /*global navigate*/

    // It just works, okay?
    const interval$1 = setInterval(() => {
      patchBundledJsonReferences();
      const videoElement = document.querySelector("video");
      if (videoElement) {
        execute_once_dom_loaded();
        patchResolveCommand();
        clearInterval(interval$1);
      }
    }, 250);

    function execute_once_dom_loaded() {
      var css_248z =
        ".ytaf-ui-container {\n  position: absolute;\n  top: 10%;\n  left: 10%;\n  right: 10%;\n  bottom: 10%;\n\n  background: rgba(0, 0, 0, 0.8);\n  color: white;\n  border-radius: 20px;\n  padding: 20px;\n  font-size: 1.5rem;\n  z-index: 1000;\n}\n\n.ytaf-ui-container :focus {\n  outline: 4px red solid;\n}\n\n.ytaf-ui-container h1 {\n  margin: 0;\n  margin-bottom: 0.5em;\n  text-align: center;\n}\n\n.ytaf-ui-container input[type='checkbox'] {\n  width: 1.4rem;\n  height: 1.4rem;\n}\n\n.ytaf-ui-container input[type='radio'] {\n  width: 1.4rem;\n  height: 1.4rem;\n}\n\n.ytaf-ui-container label {\n  display: block;\n  font-size: 1.4rem;\n}\n\n.ytaf-notification-container {\n  position: absolute;\n  right: 10px;\n  bottom: 10px;\n  font-size: 16pt;\n  z-index: 1200;\n}\n\n.ytaf-notification-container .message {\n  background: rgba(0, 0, 0, 0.7);\n  color: white;\n  padding: 1em;\n  margin: 0.5em;\n  transition: all 0.3s ease-in-out;\n  opacity: 1;\n  line-height: 1;\n  border-right: 10px solid rgba(50, 255, 50, 0.3);\n  display: inline-block;\n  float: right;\n}\n\n.ytaf-notification-container .message-hidden {\n  opacity: 0;\n  margin: 0 0.5em;\n  padding: 0 1em;\n  line-height: 0;\n}\n\n/* Fixes transparency effect for the video player */\n\n.ytLrWatchDefaultShadow {\n  background-image: linear-gradient(to bottom, rgba(0, 0, 0, 0) 0, rgba(0, 0, 0, 0.8) 90%) !important;\n  background-color: rgba(0, 0, 0, 0.3) !important;\n  display: block !important;\n  height: 100% !important;\n  pointer-events: none !important;\n  position: absolute !important;\n  width: 100% !important;\n}\n\n/* Hide shorts shelf by exact type - safe, won't match regular video rows */\n\n[tvhtml5-shelf-renderer-type='TVHTML5_SHELF_RENDERER_TYPE_SHORTS'] {\n  display: none !important;\n}\n\n.ytLrTileHeaderRendererShorts {\n  display: none !important;\n}";

      const existingStyle = document.querySelector("style[nonce]");
      if (existingStyle) {
        existingStyle.textContent += css_248z;
      } else {
        const style = document.createElement("style");
        style.textContent = css_248z;
        document.head.appendChild(style);
      }

      var uiContainer = document.createElement("div");
      uiContainer.classList.add("ytaf-ui-container");
      uiContainer.style["display"] = "none";
      uiContainer.setAttribute("tabindex", 0);

      uiContainer.addEventListener(
        "keydown",
        (evt) => {
          if (evt.keyCode === 13 || evt.keyCode === 32) {
            const focusedElement = document.querySelector(":focus");
            if (focusedElement.type === "checkbox") {
              focusedElement.checked = !focusedElement.checked;
              focusedElement.dispatchEvent(new Event("change"));
            }
            evt.preventDefault();
            evt.stopPropagation();
            return;
          }
        },
        true
      );

      applyShortsHidingCSS();
      installShortsHidingObserver();
    }

    // Function to hide shorts via CSS — uses only precise selectors, safe for regular content
    function applyShortsHidingCSS() {
      const shortsStyleId = 'notubetv-hide-shorts';
      let existingStyle = document.getElementById(shortsStyleId);

      if (!configRead("enableShorts")) {
        if (!existingStyle) {
          const style = document.createElement("style");
          style.id = shortsStyleId;
          style.textContent = `
          [tvhtml5-shelf-renderer-type='TVHTML5_SHELF_RENDERER_TYPE_SHORTS'] {
            display: none !important;
          }
          .ytLrTileHeaderRendererShorts {
            display: none !important;
          }
          ytlr-guide-item-renderer[title='Shorts'],
          ytlr-guide-item-renderer[href*='shorts'],
          a[href*='/shorts/'],
          a[href*='reel_watch_sequence'],
          a[href*='reelWatchSequence'],
          [href*='/shorts/'],
          [href*='reel_watch_sequence'],
          [href*='reelWatchSequence'],
          [aria-label='Shorts'],
          [title='Shorts'] {
            display: none !important;
          }
        `;
          document.head.appendChild(style);
        }

        removeShortsElements();
      } else {
        if (existingStyle) existingStyle.remove();
      }
    }

    function removeShortsElements() {
      const selectors = [
        "[tvhtml5-shelf-renderer-type='TVHTML5_SHELF_RENDERER_TYPE_SHORTS']",
        ".ytLrTileHeaderRendererShorts",
        "ytlr-guide-item-renderer[title='Shorts']",
        "ytlr-guide-item-renderer[href*='shorts']",
        "a[href*='/shorts/']",
        "a[href*='reel_watch_sequence']",
        "a[href*='reelWatchSequence']",
        "[href*='/shorts/']",
        "[href*='reel_watch_sequence']",
        "[href*='reelWatchSequence']",
        "[aria-label='Shorts']",
        "[title='Shorts']"
      ];

      selectors.forEach((selector) => {
        document.querySelectorAll(selector).forEach((element) => {
          const shelf = element.closest("[tvhtml5-shelf-renderer-type='TVHTML5_SHELF_RENDERER_TYPE_SHORTS']");
          if (shelf) {
            shelf.remove();
            return;
          }

          const reelContainer = element.closest("ytlr-shelf-renderer, ytlr-grid-renderer, ytlr-guide-item-renderer, a");
          if (reelContainer) {
            reelContainer.remove();
          } else {
            element.remove();
          }
        });
      });
    }

    function installShortsHidingObserver() {
      if (window.__notubetvShortsObserverInstalled) return;
      window.__notubetvShortsObserverInstalled = true;

      const observer = new MutationObserver(() => {
        if (!configRead("enableShorts")) {
          applyShortsHidingCSS();
        }
      });

      observer.observe(document.documentElement || document.body, {
        childList: true,
        subtree: true,
        attributes: true,
        attributeFilter: ["href", "title", "aria-label", "tvhtml5-shelf-renderer-type"]
      });
    }

    // Re-apply shorts hiding when config changes
    window.addEventListener('storage', function (e) {
      if (e.key === 'ytaf-configuration') {
        applyShortsHidingCSS();
      }
    });
  })();
  /* End TizenTubeScripts.js */
})();
