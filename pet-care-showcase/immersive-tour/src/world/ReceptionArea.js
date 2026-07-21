import * as THREE from "three";
import { box, createAreaLight, createLabelSprite, createPoster, cylinder } from "./primitives.js";

export class ReceptionArea {
  constructor({ materials, assets }) {
    this.key = "entrance";
    this.materials = materials;
    this.assets = assets;
    this.group = new THREE.Group();
    this.group.name = "ReceptionArea";
    this.group.position.set(0, 0, 7.8);
    this.group.userData = {
      areaKey: this.key,
      label: "入口大厅",
      cameraPoint: [0, 3.2, 15.6],
      material: "softGreen"
    };
  }

  build() {
    const desk = box({ name: "reception-desk", size: [4.3, 0.92, 0.9], position: [0, 0.46, 0.1], material: this.materials.wood });
    const counter = box({ name: "checkin-counter-light", size: [4.0, 0.08, 0.72], position: [0, 0.95, 0.1], material: this.materials.softGreen });
    const terminalA = box({ name: "self-service-terminal-a", size: [0.48, 1.15, 0.18], position: [-2.9, 0.72, 0.25], material: this.materials.screen });
    const terminalB = box({ name: "self-service-terminal-b", size: [0.48, 1.15, 0.18], position: [2.9, 0.72, 0.25], material: this.materials.screen });
    const queueRailA = cylinder({ name: "queue-rail-a", radiusTop: 0.04, radiusBottom: 0.04, height: 2.7, position: [-1.7, 0.42, 1.6], material: this.materials.beam, radialSegments: 10 });
    const queueRailB = queueRailA.clone();
    queueRailB.name = "queue-rail-b";
    queueRailB.position.x = 1.7;
    queueRailA.rotation.z = Math.PI / 2;
    queueRailB.rotation.z = Math.PI / 2;

    const poster = createPoster(this.assets?.textures?.interior, {
      name: "reception-digital-poster",
      size: [2.7, 1.55],
      position: [4.6, 1.55, -0.7],
      rotationY: -Math.PI / 2,
      fallbackMaterial: this.materials.screen
    });

    const label = createLabelSprite("入口大厅", { accent: "#4ade80", scale: [2.25, 0.48, 1] });
    label.position.set(0, 1.82, -1.15);

    const light = createAreaLight({ color: 0x7af0c2, intensity: 1.8, position: [0, 2.4, 0.4], distance: 7 });

    this.group.add(desk, counter, terminalA, terminalB, queueRailA, queueRailB, poster, label, light);
    return this.group;
  }
}
