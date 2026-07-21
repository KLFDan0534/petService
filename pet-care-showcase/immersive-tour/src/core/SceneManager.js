import * as THREE from "three";
import { OrbitControls } from "three/addons/controls/OrbitControls.js";
import { EffectComposer } from "three/addons/postprocessing/EffectComposer.js";
import { RenderPass } from "three/addons/postprocessing/RenderPass.js";
import { UnrealBloomPass } from "three/addons/postprocessing/UnrealBloomPass.js";
import { BokehPass } from "three/addons/postprocessing/BokehPass.js";

export class SceneManager {
  constructor({ canvas, profile }) {
    this.canvas = canvas;
    this.profile = profile;
    this.scene = new THREE.Scene();
    this.scene.background = new THREE.Color(0x0b1418);
    this.scene.fog = new THREE.FogExp2(0x132326, profile.isMobile ? 0.026 : 0.018);

    this.camera = new THREE.PerspectiveCamera(48, window.innerWidth / window.innerHeight, 0.1, 220);
    this.camera.position.set(0, 4, 16);

    this.renderer = new THREE.WebGLRenderer({
      canvas,
      antialias: !profile.isMobile,
      powerPreference: "high-performance",
      alpha: false
    });
    this.renderer.setPixelRatio(profile.pixelRatio);
    this.renderer.setSize(window.innerWidth, window.innerHeight);
    this.renderer.outputColorSpace = THREE.SRGBColorSpace;
    this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
    this.renderer.toneMappingExposure = 1.12;
    this.renderer.shadowMap.enabled = profile.shadows;
    this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;

    this.controls = new OrbitControls(this.camera, this.renderer.domElement);
    this.controls.enabled = false;
    this.controls.enableDamping = true;
    this.controls.dampingFactor = 0.08;
    this.controls.enablePan = false;
    this.controls.enableZoom = false;
    this.controls.minPolarAngle = Math.PI * 0.26;
    this.controls.maxPolarAngle = Math.PI * 0.47;

    this.composer = null;
    this.bokehPass = null;
    this.createPostProcessing();

    window.addEventListener("resize", this.resize, { passive: true });
  }

  createPostProcessing() {
    if (!this.profile.postProcessing) return;

    this.composer = new EffectComposer(this.renderer);
    this.composer.addPass(new RenderPass(this.scene, this.camera));

    if (this.profile.depthOfField) {
      this.bokehPass = new BokehPass(this.scene, this.camera, {
        focus: 18,
        aperture: 0.00022,
        maxblur: 0.006
      });
      this.composer.addPass(this.bokehPass);
    }

    const bloom = new UnrealBloomPass(
      new THREE.Vector2(window.innerWidth, window.innerHeight),
      this.profile.bloomStrength,
      0.45,
      0.74
    );
    this.composer.addPass(bloom);
  }

  render() {
    this.controls.update();
    if (this.composer) {
      this.composer.render();
    } else {
      this.renderer.render(this.scene, this.camera);
    }
  }

  resize = () => {
    const width = window.innerWidth;
    const height = window.innerHeight;

    this.camera.aspect = width / height;
    this.camera.updateProjectionMatrix();
    this.renderer.setPixelRatio(this.profile.pixelRatio);
    this.renderer.setSize(width, height);
    if (this.composer) {
      this.composer.setSize(width, height);
    }
  };

  dispose() {
    window.removeEventListener("resize", this.resize);
    this.controls.dispose();
    this.renderer.dispose();
  }
}
