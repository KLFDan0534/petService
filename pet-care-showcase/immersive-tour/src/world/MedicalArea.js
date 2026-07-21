import * as THREE from "three";
import { box, createAreaLight, createLabelSprite, cylinder } from "./primitives.js";

export class MedicalArea {
  constructor({ materials }) {
    this.key = "medical";
    this.materials = materials;
    this.group = new THREE.Group();
    this.group.name = "MedicalArea";
    this.group.position.set(-8, 0, -6.4);
    this.group.userData = {
      areaKey: this.key,
      label: "医疗检查区",
      cameraPoint: [-10.3, 3.15, -3.7],
      material: "medicalRose"
    };
  }

  build() {
    this.group.add(
      box({ name: "medical-platform", size: [6.9, 0.12, 5.0], position: [0, 0.02, 0], material: this.materials.medicalRose }),
      box({ name: "exam-table-base", size: [2.2, 0.18, 1.1], position: [0, 0.72, 0.1], material: this.materials.white }),
      box({ name: "exam-table-column", size: [0.28, 0.88, 0.28], position: [0, 0.36, 0.1], material: this.materials.beam }),
      box({ name: "medical-cabinet", size: [1.1, 1.45, 0.55], position: [-2.35, 0.78, -1.55], material: this.materials.white }),
      box({ name: "sterilizer", size: [1.05, 1.0, 0.55], position: [2.35, 0.55, -1.55], material: this.materials.glass }),
      box({ name: "vital-screen", size: [1.25, 0.72, 0.08], position: [0.35, 1.55, -1.85], material: this.materials.screen })
    );

    const scanner = cylinder({ name: "health-scanner-arch", radiusTop: 0.72, radiusBottom: 0.72, height: 0.1, position: [-0.9, 1.05, 0.1], material: this.materials.screen, radialSegments: 32 });
    scanner.rotation.x = Math.PI / 2;
    scanner.scale.y = 1.5;
    this.group.add(scanner);

    const label = createLabelSprite("医疗区", { accent: "#fb7185" });
    label.position.set(0, 1.92, -2.25);
    this.group.add(label, createAreaLight({ color: 0xff9aac, intensity: 2.0, position: [0, 2.35, 0.2], distance: 7 }));
    return this.group;
  }
}
