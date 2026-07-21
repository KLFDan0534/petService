import * as THREE from "three";
import { box, createAreaLight, createLabelSprite } from "./primitives.js";

export class StaffArea {
  constructor({ materials }) {
    this.key = "staff";
    this.materials = materials;
    this.group = new THREE.Group();
    this.group.name = "StaffArea";
    this.group.position.set(0, 0, -10.2);
    this.group.userData = {
      areaKey: this.key,
      label: "工作人员区域",
      cameraPoint: [2.2, 3.1, -14.0],
      material: "staffTeal"
    };
  }

  build() {
    this.group.add(
      box({ name: "staff-platform", size: [7.4, 0.12, 3.5], position: [0, 0.02, 0], material: this.materials.staffTeal }),
      box({ name: "ops-desk-left", size: [2.2, 0.72, 0.78], position: [-1.8, 0.38, 0.15], material: this.materials.wood }),
      box({ name: "ops-desk-right", size: [2.2, 0.72, 0.78], position: [1.8, 0.38, 0.15], material: this.materials.wood }),
      box({ name: "schedule-screen", size: [2.6, 1.0, 0.08], position: [0, 1.5, -1.53], material: this.materials.screen }),
      box({ name: "locker-a", size: [0.8, 1.65, 0.55], position: [-3.05, 0.85, -1.05], material: this.materials.wall }),
      box({ name: "locker-b", size: [0.8, 1.65, 0.55], position: [3.05, 0.85, -1.05], material: this.materials.wall })
    );

    for (let i = 0; i < 4; i += 1) {
      this.group.add(
        box({
          name: `task-tile-${i}`,
          size: [0.48, 0.34, 0.04],
          position: [-0.9 + i * 0.6, 1.52, -1.47],
          material: i === 2 ? this.materials.coral : this.materials.softGreen
        })
      );
    }

    const label = createLabelSprite("员工区", { accent: "#2dd4bf" });
    label.position.set(0, 1.88, -1.58);
    this.group.add(label, createAreaLight({ color: 0x55f0df, intensity: 1.9, position: [0, 2.1, 0], distance: 7 }));
    return this.group;
  }
}
