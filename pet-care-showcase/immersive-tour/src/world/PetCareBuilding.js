import * as THREE from "three";
import { box, cylinder, createLabelSprite } from "./primitives.js";

export class PetCareBuilding {
  constructor({ materials, profile }) {
    this.materials = materials;
    this.profile = profile;
    this.group = new THREE.Group();
    this.group.name = "PetCareBuilding";
  }

  build() {
    this.group.add(
      box({ name: "main-foundation", size: [25.8, 0.3, 23.6], position: [0, -0.2, -1.2], material: this.materials.floor }),
      box({ name: "front-threshold", size: [7.4, 0.18, 0.9], position: [0, 0.02, 10.6], material: this.materials.softGreen })
    );

    this.addWalls();
    this.addCeiling();
    this.addSignage();

    return this.group;
  }

  addWalls() {
    const wallHeight = 2.9;
    const y = wallHeight / 2;
    this.group.add(
      box({ name: "north-glass-wall-left", size: [8, wallHeight, 0.18], position: [-8.8, y, 10.3], material: this.materials.glass }),
      box({ name: "north-glass-wall-right", size: [8, wallHeight, 0.18], position: [8.8, y, 10.3], material: this.materials.glass }),
      box({ name: "west-service-wall", size: [0.18, wallHeight, 21.8], position: [-12.6, y, -0.6], material: this.materials.wall }),
      box({ name: "east-service-wall", size: [0.18, wallHeight, 21.8], position: [12.6, y, -0.6], material: this.materials.wall }),
      box({ name: "south-staff-wall", size: [25.4, wallHeight, 0.18], position: [0, y, -12.1], material: this.materials.wall }),
      box({ name: "center-divider-a", size: [0.12, 1.9, 7.2], position: [-3.2, 0.95, -3.3], material: this.materials.glass }),
      box({ name: "center-divider-b", size: [0.12, 1.9, 7.2], position: [3.2, 0.95, -3.3], material: this.materials.glass })
    );
  }

  addCeiling() {
    if (this.profile.detailLevel < 0.55) return;

    for (let x = -10; x <= 10; x += 5) {
      this.group.add(box({ name: `roof-beam-${x}`, size: [0.18, 0.18, 22], position: [x, 3.25, -0.8], material: this.materials.beam }));
    }

    for (let z = -10; z <= 8; z += 4.5) {
      this.group.add(box({ name: `roof-cross-${z}`, size: [25, 0.16, 0.16], position: [0, 3.25, z], material: this.materials.beam }));
    }

    for (let x = -8; x <= 8; x += 4) {
      for (let z = -8; z <= 6; z += 5) {
        const lightDisc = cylinder({
          name: `ceiling-light-${x}-${z}`,
          radiusTop: 0.22,
          radiusBottom: 0.22,
          height: 0.035,
          position: [x, 3.02, z],
          material: this.materials.screen,
          radialSegments: 18
        });
        lightDisc.rotation.x = Math.PI / 2;
        this.group.add(lightDisc);
      }
    }
  }

  addSignage() {
    const label = createLabelSprite("PetService 养护中心", { scale: [4.3, 0.7, 1] });
    label.position.set(0, 2.35, 10.12);
    this.group.add(label);
  }
}
