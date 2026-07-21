import * as THREE from "three";
import { AnimationManager } from "./core/AnimationManager.js";
import { AssetLoader } from "./core/AssetLoader.js";
import { CameraController } from "./core/CameraController.js";
import { createPerformanceProfile } from "./core/PerformanceProfile.js";
import { SceneManager } from "./core/SceneManager.js";
import { AREA_MODEL_MANIFEST, TOUR_STOPS } from "./config/tourStops.js";
import { RaycasterManager } from "./interaction/RaycasterManager.js";
import { PetCareWorld } from "./world/PetCareWorld.js";
import { HUD } from "./ui/HUD.js";
import { LoadingScreen } from "./ui/LoadingScreen.js";
import { clamp } from "./utils/math.js";

const canvas = document.getElementById("webgl-canvas");
const loadingScreen = new LoadingScreen();
window.__PET_CARE_BOOT_STATUS__ = { started: true };

let profile;
let sceneManager;
let assetLoader;
let animationManager;
let cameraController;
let world;
let hud;
let raycasterManager;
let activeHotspotArea = null;
const lazyRequests = new Set();
const introSkip = document.getElementById("intro-skip");
introSkip.hidden = true;

bootstrap();

async function bootstrap() {
  try {
    loadingScreen.update({ percent: 1, stage: "启动 3D 引擎" });
    profile = createPerformanceProfile();
    sceneManager = new SceneManager({ canvas, profile });
    assetLoader = new AssetLoader({
      renderer: sceneManager.renderer,
      onProgress: (state) => loadingScreen.update(state)
    });
    animationManager = new AnimationManager();

    const assets = await assetLoader.loadStartupAssets();
    world = new PetCareWorld({ assets, profile }).build();
    sceneManager.scene.add(world.group);

    cameraController = new CameraController({
      camera: sceneManager.camera,
      controls: sceneManager.controls,
      profile
    });

    hud = new HUD({
      stops: TOUR_STOPS,
      onNavigate: scrollToProgress
    });
    hud.setQuality(profile);
    hud.update({ progress: 0, activeStop: TOUR_STOPS[0] });

    raycasterManager = new RaycasterManager({
      camera: sceneManager.camera,
      domElement: sceneManager.renderer.domElement,
      onSelect: (data) => hud.showHotspot(data)
    });
    raycasterManager.setTargets(world.getHotspotTargets());
    exposeDebugState();

    bindScroll();
    bindIntroControls();
    startAnimationLoop();

    await loadingScreen.complete();
    if (profile.reducedMotion) {
      cameraController.skipIntro();
    } else {
      introSkip.hidden = false;
      cameraController.beginIntro();
    }
  } catch (error) {
    console.error(error);
    const message = window.location.protocol === "file:"
      ? "请通过 npm run serve 打开 http://127.0.0.1:4177/"
      : "空间初始化失败，请检查浏览器控制台错误";
    loadingScreen.fail(message);
  }
}

function startAnimationLoop() {
  animationManager.add((delta, elapsed) => {
    cameraController.update(delta);
    world.update(delta, elapsed, sceneManager.camera);

    const state = cameraController.getState();
    hud.update({
      progress: state.currentProgress ?? state.progress,
      activeStop: state.activeStop
    });

    if (!state.introRunning) {
      introSkip.hidden = true;
      maybeLoadAreaModel(state.activeStop?.key);
      updateHotspotArea(state.activeStop?.key);
    }

    sceneManager.render();
  });
  animationManager.start();
}

function bindScroll() {
  const updateFromScroll = () => {
    const maxScroll = Math.max(1, document.documentElement.scrollHeight - window.innerHeight);
    const progress = clamp(window.scrollY / maxScroll);
    cameraController.setScrollProgress(progress);
  };

  updateFromScroll();
  window.addEventListener("scroll", updateFromScroll, { passive: true });
  window.addEventListener("resize", updateFromScroll, { passive: true });
}

function bindIntroControls() {
  introSkip.addEventListener("click", () => {
    cameraController.skipIntro();
    introSkip.hidden = true;
  });
}

function scrollToProgress(progress) {
  const maxScroll = Math.max(1, document.documentElement.scrollHeight - window.innerHeight);
  window.scrollTo({
    top: maxScroll * clamp(progress),
    behavior: profile.reducedMotion ? "auto" : "smooth"
  });
}

async function maybeLoadAreaModel(areaKey) {
  const url = AREA_MODEL_MANIFEST[areaKey];
  if (!url || lazyRequests.has(areaKey)) return;

  lazyRequests.add(areaKey);
  const gltf = await assetLoader.loadAreaModel(areaKey, url);
  if (gltf) {
    world.addAreaModel(areaKey, gltf);
  }
}

function updateHotspotArea(areaKey) {
  if (!areaKey || activeHotspotArea === areaKey) return;
  activeHotspotArea = areaKey;
  world.setActiveHotspotArea(areaKey);
  raycasterManager.setTargets(world.getHotspotTargets(areaKey));
  hud.hideHotspot();
}

function exposeDebugState() {
  window.__PET_CARE_TOUR__ = {
    getState() {
      const state = cameraController.getState();
      return {
        progress: state.progress,
        targetProgress: state.targetProgress,
        activeStop: state.activeStop?.key,
        introRunning: state.introRunning,
        cameraPosition: sceneManager.camera.position.toArray(),
        cameraLookAt: cameraController.currentLookAt.toArray(),
        qualityTier: profile.tier,
        hotspotCount: world.hotspots.length
      };
    },
    getHotspotScreenPositions() {
      const width = window.innerWidth;
      const height = window.innerHeight;
      return world.hotspots.map((hotspot) => {
        const position = hotspot.group.getWorldPosition(new THREE.Vector3());
        position.project(sceneManager.camera);
        return {
          id: hotspot.data.id,
          areaKey: hotspot.data.areaKey,
          visible: hotspot.group.visible,
          x: (position.x * 0.5 + 0.5) * width,
          y: (-position.y * 0.5 + 0.5) * height,
          z: position.z
        };
      });
    }
  };
}
