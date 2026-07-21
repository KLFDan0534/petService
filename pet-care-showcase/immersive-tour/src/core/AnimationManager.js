export class AnimationManager {
  constructor() {
    this.callbacks = new Set();
    this.clock = null;
    this.frame = 0;
    this.running = false;
    this.lastTime = 0;
    this.elapsed = 0;
  }

  add(callback) {
    this.callbacks.add(callback);
    return () => this.callbacks.delete(callback);
  }

  start() {
    if (this.running) return;
    this.running = true;
    this.lastTime = performance.now();
    this.frame = requestAnimationFrame(this.tick);
  }

  stop() {
    this.running = false;
    cancelAnimationFrame(this.frame);
  }

  tick = (time) => {
    if (!this.running) return;

    const delta = Math.min((time - this.lastTime) / 1000, 0.5);
    this.elapsed += delta;
    this.lastTime = time;

    for (const callback of this.callbacks) {
      callback(delta, this.elapsed);
    }

    this.frame = requestAnimationFrame(this.tick);
  };
}
