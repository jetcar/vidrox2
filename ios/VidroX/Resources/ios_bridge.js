(function () {
  "use strict";

  window.ExitBridge = window.ExitBridge || {
    onExitCalled: function () {
      window.webkit.messageHandlers.exitBridge.postMessage(null);
    }
  };

  window.NetworkBridge = window.NetworkBridge || {
    fetch: function (url, videoId) {
      window.webkit.messageHandlers.networkBridge.postMessage({
        url: String(url),
        videoId: String(videoId)
      });
    }
  };
})();

