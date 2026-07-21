export function createPerformanceProfile() {
  const connection = navigator.connection || navigator.webkitConnection || navigator.mozConnection;
  const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const narrow = window.matchMedia("(max-width: 780px)").matches;
  const coarsePointer = window.matchMedia("(pointer: coarse)").matches;
  const memory = navigator.deviceMemory || 4;
  const slowConnection = connection
    ? ["slow-2g", "2g", "3g"].includes(connection.effectiveType)
    : false;

  const isMobile = narrow || coarsePointer || memory <= 3;
  const tier = reducedMotion || slowConnection || memory <= 2 ? "lite" : isMobile ? "mobile" : "desktop";

  return {
    tier,
    isMobile,
    reducedMotion,
    slowConnection,
    pixelRatio: tier === "desktop" ? Math.min(window.devicePixelRatio || 1, 2) : Math.min(window.devicePixelRatio || 1, 1.35),
    shadows: tier === "desktop",
    shadowMapSize: tier === "desktop" ? 2048 : 768,
    postProcessing: tier === "desktop" && !reducedMotion,
    depthOfField: tier === "desktop" && !reducedMotion,
    bloomStrength: tier === "desktop" ? 0.38 : 0.18,
    detailLevel: tier === "desktop" ? 1 : tier === "mobile" ? 0.72 : 0.48,
    maxDecorations: tier === "desktop" ? 1 : 0.45,
    targetFps: tier === "desktop" ? 60 : 30
  };
}
