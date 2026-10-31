// Reclaim download page. This is the only script on the site, and it collects nothing.
// It only checks what kind of device is visiting, so a laptop or an iPhone gets a clear message
// instead of downloading an Android file it cannot open.
(function () {
  "use strict";

  var ua = navigator.userAgent || "";
  var uaData = navigator.userAgentData;

  // iPadOS pretends to be a Mac, so a touch screen on a "Mac" means an iPad.
  var isIOS = /iPhone|iPad|iPod/i.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
  var isAndroid = !isIOS && (/Android/i.test(ua) || (uaData && uaData.platform === "Android"));

  if (isAndroid) return; // Android phones see the normal page, untouched.

  var notice = document.getElementById(isIOS ? "notice-ios" : "notice-desktop");
  if (notice) notice.hidden = false;

  var androidOnly = document.querySelectorAll("[data-android-only]");
  for (var i = 0; i < androidOnly.length; i++) androidOnly[i].hidden = true;

  var copy = document.getElementById("copy-link");
  if (copy && navigator.clipboard && navigator.clipboard.writeText) {
    copy.hidden = false;
    copy.addEventListener("click", function () {
      navigator.clipboard.writeText(location.origin + "/").then(
        function () { copy.textContent = "Copied"; },
        function () { copy.textContent = "Could not copy"; }
      );
    });
  }
})();
