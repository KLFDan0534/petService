export class HUD {
  constructor({ stops, onNavigate }) {
    this.stops = stops;
    this.onNavigate = onNavigate;
    this.area = document.getElementById("hud-area");
    this.description = document.getElementById("hud-description");
    this.progressText = document.getElementById("hud-progress-text");
    this.progressBar = document.getElementById("hud-progress-bar");
    this.capacity = document.getElementById("hud-capacity");
    this.devices = document.getElementById("hud-devices");
    this.staff = document.getElementById("hud-staff");
    this.nav = document.querySelector(".space-nav");
    this.panel = document.getElementById("hotspot-panel");
    this.hotspotTitle = document.getElementById("hotspot-title");
    this.hotspotFunction = document.getElementById("hotspot-function");
    this.hotspotData = document.getElementById("hotspot-data");
    this.closeButton = this.panel.querySelector(".hotspot-panel__close");
    this.quality = document.getElementById("quality-pill");
    this.activeKey = null;
    this.navButtons = new Map();

    this.buildNavigation();
    this.closeButton.addEventListener("click", () => this.hideHotspot());
    window.addEventListener("keydown", (event) => {
      if (event.key === "Escape") this.hideHotspot();
    });
  }

  buildNavigation() {
    const fragment = document.createDocumentFragment();
    for (const stop of this.stops) {
      const button = document.createElement("button");
      button.type = "button";
      button.setAttribute("aria-label", `前往${stop.label}`);
      button.innerHTML = `<span>${stop.label}</span>`;
      button.addEventListener("click", () => this.onNavigate(stop.progress));
      this.navButtons.set(stop.key, button);
      fragment.append(button);
    }
    this.nav.append(fragment);
  }

  setQuality(profile) {
    const mode = profile.tier === "desktop" ? "桌面电影模式" : profile.tier === "mobile" ? "移动性能模式" : "轻量模式";
    this.quality.textContent = `质量模式：${mode}`;
  }

  update({ progress, activeStop }) {
    const percent = Math.round(progress * 100);
    this.progressText.textContent = `${percent}%`;
    this.progressBar.style.width = `${percent}%`;

    if (!activeStop || activeStop.key === this.activeKey) return;
    this.activeKey = activeStop.key;
    this.area.textContent = activeStop.label;
    this.description.textContent = activeStop.description;
    this.capacity.textContent = activeStop.capacity;
    this.devices.textContent = activeStop.devices;
    this.staff.textContent = activeStop.staff;

    for (const [key, button] of this.navButtons) {
      const isActive = key === activeStop.key;
      button.classList.toggle("is-active", isActive);
      button.setAttribute("aria-current", isActive ? "step" : "false");
    }
  }

  showHotspot(data) {
    this.hotspotTitle.textContent = data.title;
    this.hotspotFunction.textContent = data.functionText;
    this.hotspotData.textContent = data.dataText;
    this.panel.classList.add("is-open");
    this.panel.setAttribute("aria-hidden", "false");
  }

  hideHotspot() {
    this.panel.classList.remove("is-open");
    this.panel.setAttribute("aria-hidden", "true");
  }
}
