import * as THREE from "three";
import { GLTFLoader } from "three/addons/loaders/GLTFLoader.js";
import { DRACOLoader } from "three/addons/loaders/DRACOLoader.js";
import { KTX2Loader } from "three/addons/loaders/KTX2Loader.js";

const STARTUP_TEXTURES = {
  exterior: "assets/pet_care_house_preview.png",
  interior: "assets/pet_care_house_interior_preview.png",
  boarding: "assets/pet_cultivation_five_houses_preview.png",
  spa: "assets/pet_spa_pavilion_preview.png"
};

const STAGES = [
  { percent: 0, text: "加载环境模型" },
  { percent: 12, text: "检查模型清单" },
  { percent: 22, text: "加载空间预览纹理" },
  { percent: 35, text: "加载建筑结构" },
  { percent: 60, text: "加载宠物设施" },
  { percent: 90, text: "初始化空间" },
  { percent: 100, text: "准备进入空间" }
];

const TEXTURE_TIMEOUT_MS = 5000;

export class AssetLoader {
  constructor({ renderer, onProgress = () => {} } = {}) {
    this.onProgress = onProgress;
    this.modelCache = new Map();
    this.modelManifest = {
      center: null,
      areas: {}
    };
    this.progressRange = { min: 0, max: 88 };

    this.loadingManager = new THREE.LoadingManager();
    this.loadingManager.onProgress = (_url, loaded, total) => {
      if (!total) return;
      const ratio = loaded / total;
      const progress = this.progressRange.min + (this.progressRange.max - this.progressRange.min) * ratio;
      this.report(progress, this.stageForPercent(progress));
    };

    this.textureLoader = new THREE.TextureLoader(this.loadingManager);
    this.gltfLoader = new GLTFLoader(this.loadingManager);

    this.dracoLoader = new DRACOLoader();
    this.dracoLoader.setDecoderPath("./node_modules/three/examples/jsm/libs/draco/");
    this.gltfLoader.setDRACOLoader(this.dracoLoader);

    this.ktx2Loader = new KTX2Loader(this.loadingManager);
    this.ktx2Loader.setTranscoderPath("./node_modules/three/examples/jsm/libs/basis/");
    if (renderer) {
      this.ktx2Loader.detectSupport(renderer);
    }
  }

  async loadStartupAssets() {
    this.report(0, STAGES[0].text);
    await this.wait(120);
    this.report(12, "检查模型清单");

    await this.loadModelManifest();
    this.report(22, "加载空间预览纹理");
    this.progressRange = { min: 22, max: 34 };
    const textures = await this.loadPreviewTextures();
    this.report(35, "加载建筑结构");

    this.progressRange = { min: 35, max: 58 };
    const centerModel = this.modelManifest.center
      ? await this.loadModel(this.modelManifest.center, {
          cacheKey: "pet-care-center",
          configure: true
        })
      : null;
    this.report(60, "加载宠物设施");

    await this.wait(280);
    this.report(90, "初始化空间");
    await this.wait(320);
    this.report(100, "准备进入空间");

    return {
      textures,
      centerModel,
      usedFallback: !centerModel
    };
  }

  async loadPreviewTextures() {
    const entries = await Promise.all(
      Object.entries(STARTUP_TEXTURES).map(async ([key, url]) => {
        try {
          const texture = await this.loadTexture(url);
          texture.colorSpace = THREE.SRGBColorSpace;
          texture.anisotropy = 4;
          return [key, texture];
        } catch {
          return [key, null];
        }
      })
    );

    return Object.fromEntries(entries);
  }

  async loadAreaModel(areaKey, url) {
    const modelUrl = this.modelManifest.areas?.[areaKey] || url || null;
    if (!modelUrl) return null;

    try {
      return await this.loadModel(modelUrl, {
        cacheKey: `area:${areaKey}`,
        configure: true
      });
    } catch (error) {
      console.warn(`Area model skipped: ${areaKey}`, error);
      return null;
    }
  }

  async loadModel(url, { cacheKey = url, configure = true } = {}) {
    if (this.modelCache.has(cacheKey)) {
      return this.modelCache.get(cacheKey);
    }

    const gltf = await new Promise((resolve, reject) => {
      this.gltfLoader.load(url, resolve, undefined, reject);
    });

    if (configure) {
      gltf.scene.traverse((child) => {
        if (child.isMesh) {
          child.castShadow = true;
          child.receiveShadow = true;
          if (child.material) {
            child.material.needsUpdate = true;
          }
        }
      });
    }

    this.modelCache.set(cacheKey, gltf);
    return gltf;
  }

  async loadModelManifest() {
    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), 2000);
    try {
      const response = await fetch("assets/models/manifest.json", {
        cache: "no-store",
        signal: controller.signal
      });
      if (!response.ok) return;
      const manifest = await response.json();
      this.modelManifest = {
        center: manifest.center || null,
        areas: manifest.areas || {}
      };
    } catch {
      this.modelManifest = {
        center: null,
        areas: {}
      };
    } finally {
      window.clearTimeout(timer);
    }
  }

  loadTexture(url) {
    return new Promise((resolve, reject) => {
      const timer = window.setTimeout(() => {
        reject(new Error(`Texture load timed out: ${url}`));
      }, TEXTURE_TIMEOUT_MS);

      this.textureLoader.load(
        url,
        (texture) => {
          window.clearTimeout(timer);
          resolve(texture);
        },
        undefined,
        (error) => {
          window.clearTimeout(timer);
          reject(error);
        }
      );
    });
  }

  loadCompressedTexture(url) {
    return new Promise((resolve, reject) => {
      this.ktx2Loader.load(url, resolve, undefined, reject);
    });
  }

  stageForPercent(percent) {
    const stage = [...STAGES].reverse().find((item) => percent >= item.percent);
    return stage ? stage.text : STAGES[0].text;
  }

  report(percent, stage) {
    this.onProgress({
      percent: Math.max(0, Math.min(100, Math.round(percent))),
      stage
    });
  }

  wait(ms) {
    return new Promise((resolve) => window.setTimeout(resolve, ms));
  }

  dispose() {
    this.dracoLoader.dispose();
    this.ktx2Loader.dispose();
  }
}
