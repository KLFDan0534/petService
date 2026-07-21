import * as THREE from "three";
import { box, createAreaLight, createLabelSprite, createPetSilhouette, cylinder } from "./primitives.js";

export class RestArea {
  constructor({ materials, profile }) {
    this.key = "boarding";
    this.materials = materials;
    this.profile = profile;
    this.group = new THREE.Group();
    this.group.name = "RestArea";
    this.group.position.set(8, 0, 1.2);
    this.group.userData = {
      areaKey: this.key,
      label: "宠物寄养区",
      cameraPoint: [11.4, 3.3, 5.4],
      material: "restAmber"
    };
  }

  build() {
    this.group.add(
      box({ name: "boarding-platform", size: [6.9, 0.12, 5.4], position: [0, 0.02, 0], material: this.materials.restAmber }),
      box({ name: "boarding-soft-mat", size: [2.4, 0.08, 1.5], position: [1.9, 0.11, 1.2], material: this.materials.softGreen })
    );

    for (let row = 0; row < 2; row += 1) {
      for (let col = 0; col < 3; col += 1) {
        const cabin = box({
          name: `boarding-cabin-${row}-${col}`,
          size: [1.25, 0.9, 1.0],
          position: [-2.2 + col * 1.55, 0.5, -1.25 + row * 1.25],
          material: row === 0 ? this.materials.white : this.materials.wall
        });
        const door = box({
          name: `boarding-door-${row}-${col}`,
          size: [0.08, 0.62, 0.7],
          position: [-1.6 + col * 1.55, 0.48, -1.25 + row * 1.25],
          material: this.materials.glass
        });
        this.group.add(cabin, door);
      }
    }

    const tunnel = cylinder({ name: "play-tunnel", radiusTop: 0.42, radiusBottom: 0.42, height: 1.4, position: [2.2, 0.52, -1.35], material: this.materials.coral, radialSegments: 20 });
    tunnel.rotation.z = Math.PI / 2;
    this.group.add(tunnel);

    if (this.profile.detailLevel > 0.5) {
      this.group.add(
        createPetSilhouette(this.materials, { position: [1.25, 0.18, 1.25], scale: 0.72, colorMaterial: "white" }),
        createPetSilhouette(this.materials, { position: [2.35, 0.18, 0.95], scale: 0.58, colorMaterial: "coral" })
      );
    }

    const label = createLabelSprite("寄养区", { accent: "#f59e0b" });
    label.position.set(0, 1.92, -2.45);
    this.group.add(label, createAreaLight({ color: 0xffbe62, intensity: 2.0, position: [0, 2.35, 0.4], distance: 7 }));
    return this.group;
  }
}
