import * as THREE from "three";
import { TOUR_STOPS, INTRO_PATH } from "../config/tourStops.js";
import { clamp, easeOutCubic, findTimelineSegment, mixVectorArrays, toVector3 } from "../utils/math.js";

export class CameraController {
  constructor({ camera, controls, profile }) {
    this.camera = camera;
    this.controls = controls;
    this.profile = profile;
    this.stops = TOUR_STOPS;
    this.targetProgress = 0;
    this.currentProgress = 0;
    this.currentLookAt = toVector3(this.stops[0].lookAt);
    this.desiredPosition = toVector3(this.stops[0].camera);
    this.desiredLookAt = toVector3(this.stops[0].lookAt);
    this.activeStop = this.stops[0];
    this.intro = {
      running: false,
      elapsed: 0,
      duration: profile.reducedMotion ? 0.01 : INTRO_PATH.duration
    };

    this.camera.position.copy(toVector3(INTRO_PATH.from));
    this.camera.lookAt(toVector3(INTRO_PATH.lookFrom));
  }

  beginIntro() {
    this.intro.running = true;
    this.intro.elapsed = 0;
  }

  skipIntro() {
    this.intro.running = false;
    this.camera.position.copy(toVector3(this.stops[0].camera));
    this.currentLookAt.copy(toVector3(this.stops[0].lookAt));
    this.camera.lookAt(this.currentLookAt);
  }

  setScrollProgress(progress) {
    this.targetProgress = clamp(progress);
  }

  update(delta) {
    if (this.intro.running) {
      this.updateIntro(delta);
      return;
    }

    const smoothing = this.profile.reducedMotion ? 1 : 1 - Math.exp(-(this.profile.isMobile ? 4.6 : 5.4) * delta);
    this.currentProgress += (this.targetProgress - this.currentProgress) * smoothing;
    this.applyTimeline(this.currentProgress, delta);
  }

  updateIntro(delta) {
    this.intro.elapsed += delta;
    const t = clamp(this.intro.elapsed / this.intro.duration);
    const eased = easeOutCubic(t);
    const firstHalf = Math.min(eased / 0.55, 1);
    const secondHalf = clamp((eased - 0.45) / 0.55);
    const from = toVector3(INTRO_PATH.from);
    const mid = toVector3(INTRO_PATH.mid);
    const to = toVector3(INTRO_PATH.to);
    const position = from.lerp(mid, firstHalf).lerp(to, secondHalf);
    const look = mixVectorArrays(INTRO_PATH.lookFrom, INTRO_PATH.lookTo, eased);

    this.camera.position.lerp(position, 0.22);
    this.currentLookAt.lerp(look, 0.2);
    this.camera.lookAt(this.currentLookAt);

    if (t >= 1) {
      this.intro.running = false;
      this.currentProgress = this.targetProgress;
    }
  }

  applyTimeline(progress, delta = 1 / 60) {
    const sample = this.sample(progress);
    this.desiredPosition.copy(sample.position);
    this.desiredLookAt.copy(sample.lookAt);

    const positionDamping = this.profile.reducedMotion ? 1 : 1 - Math.exp(-9.5 * delta);
    const lookDamping = this.profile.reducedMotion ? 1 : 1 - Math.exp(-8.2 * delta);
    this.camera.position.lerp(this.desiredPosition, positionDamping);
    this.currentLookAt.lerp(this.desiredLookAt, lookDamping);
    this.camera.lookAt(this.currentLookAt);

    this.controls.target.copy(this.currentLookAt);
    this.activeStop = sample.activeStop;
  }

  sample(progress = this.currentProgress) {
    const segment = findTimelineSegment(this.stops, progress);
    const position = toVector3(segment.from.camera).lerp(toVector3(segment.to.camera), segment.t);
    const lookAt = toVector3(segment.from.lookAt).lerp(toVector3(segment.to.lookAt), segment.t);
    const activeStop = segment.t < 0.5 ? segment.from : segment.to;

    return {
      position,
      lookAt,
      activeStop,
      from: segment.from,
      to: segment.to,
      t: segment.t,
      progress
    };
  }

  getState() {
    return {
      progress: this.currentProgress,
      targetProgress: this.targetProgress,
      activeStop: this.activeStop,
      introRunning: this.intro.running
    };
  }
}
