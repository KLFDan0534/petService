import * as THREE from "three";
import { box, createAreaLight, createLabelSprite, createPetSilhouette, cylinder, sphere } from "./primitives.js";

export class WashArea {
  constructor({ materials, profile }) {
    this.key = "grooming";
    this.materials = materials;
    this.profile = profile;
    this.group = new THREE.Group();
    this.group.name = "WashArea";
    this.group.position.set(-8, 0, 1.4);
    this.group.userData = {
      areaKey: this.key,
      label: "宠物洗护区",
      cameraPoint: [-10.7, 3.0, 6.2],
      material: "washBlue"
    };
  }

  build() {
    this.group.add(
      box({ name: "wash-platform", size: [6.7, 0.12, 5.2], position: [0, 0.02, 0], material: this.materials.washBlue }),
      box({ name: "wash-back-wall", size: [6.6, 1.7, 0.16], position: [0, 0.85, -2.6], material: this.materials.glass })
    );

    for (let i = 0; i < 3; i += 1) {
      const x = -2.1 + i * 2.1;
      const tub = box({ name: `wash-tub-${i + 1}`, size: [1.35, 0.62, 1.0], position: [x, 0.34, -0.6], material: this.materials.white });
      const basin = box({ name: `wash-basin-${i + 1}`, size: [1.05, 0.12, 0.72], position: [x, 0.7, -0.6], material: this.materials.washBlue });
      const dryer = cylinder({ name: `dryer-ring-${i + 1}`, radiusTop: 0.45, radiusBottom: 0.45, height: 0.11, position: [x, 1.18, 0.75], material: this.materials.screen, radialSegments: 24 });
      dryer.rotation.x = Math.PI / 2;
      this.group.add(tub, basin, dryer);
    }

    const pet = createPetSilhouette(this.materials, { position: [-2.05, 0.55, -0.55], scale: 0.82, colorMaterial: "coral" });
    this.group.add(pet);

    if (this.profile.detailLevel > 0.6) {
      for (let i = 0; i < 14; i += 1) {
        const bubble = sphere({
          name: `wash-bubble-${i}`,
          radius: 0.055 + (i % 3) * 0.018,
          position: [-2.7 + (i % 7) * 0.42, 1.0 + (i % 4) * 0.2, -0.15 + Math.sin(i) * 0.38],
          material: this.materials.glass,
          widthSegments: 12,
          heightSegments: 8
        });
        this.group.add(bubble);
      }
    }

    const label = createLabelSprite("洗护区", { accent: "#38bdf8" });
    label.position.set(0, 1.92, -2.35);
    this.group.add(label, createAreaLight({ color: 0x62d7ff, intensity: 2.1, position: [0, 2.3, 0], distance: 7 }));
    return this.group;
  }
}
