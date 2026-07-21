const STAGE_THRESHOLDS = [0, 35, 60, 90];

export class LoadingScreen {
  constructor() {
    this.root = document.getElementById("loading-screen");
    this.percent = document.getElementById("loading-percent");
    this.bar = document.getElementById("loading-bar");
    this.stage = document.getElementById("loading-stage");
    this.steps = Array.from(document.querySelectorAll(".loading-steps li"));
  }

  update({ percent, stage }) {
    const value = Math.max(0, Math.min(100, Math.round(percent)));
    this.percent.textContent = `${value}%`;
    this.bar.style.width = `${value}%`;
    this.stage.textContent = stage;

    this.steps.forEach((step, index) => {
      step.classList.toggle("is-active", value >= STAGE_THRESHOLDS[index]);
    });
  }

  async complete() {
    this.update({ percent: 100, stage: "准备进入空间" });
    await wait(380);
    this.root.classList.add("is-exiting");
    await wait(780);
  }

  fail(message) {
    this.update({ percent: 100, stage: message });
  }
}

function wait(ms) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}
