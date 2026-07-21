import * as THREE from "three";
import { box, createAreaLight, createLabelSprite, createPoster } from "./primitives.js";

export class ProductArea {
  constructor({ materials, assets }) {
    this.key = "product";
    this.materials = materials;
    this.assets = assets;
    this.group = new THREE.Group();
    this.group.name = "ProductArea";
    this.group.position.set(7.8, 0, -6.6);
    this.group.userData = {
      areaKey: this.key,
      label: "商品展示区",
      cameraPoint: [9.8, 3.0, -4.7],
      material: "productViolet"
    };
  }

  build() {
    this.group.add(box({ name: "product-platform", size: [6.6, 0.12, 5.0], position: [0, 0.02, 0], material: this.materials.productViolet }));

    for (let shelf = 0; shelf < 3; shelf += 1) {
      const z = -1.5 + shelf * 1.2;
      this.group.add(
        box({ name: `product-shelf-${shelf}`, size: [4.8, 0.12, 0.42], position: [0, 0.55 + shelf * 0.38, z], material: this.materials.wood }),
        box({ name: `product-shelf-back-${shelf}`, size: [4.8, 0.82, 0.08], position: [0, 0.76 + shelf * 0.38, z - 0.24], material: this.materials.wall })
      );

      for (let i = 0; i < 6; i += 1) {
        const product = box({
          name: `product-pack-${shelf}-${i}`,
          size: [0.32, 0.42 + (i % 2) * 0.12, 0.24],
          position: [-2 + i * 0.78, 0.83 + shelf * 0.38, z],
          material: i % 3 === 0 ? this.materials.softGreen : i % 3 === 1 ? this.materials.coral : this.materials.washBlue
        });
        this.group.add(product);
      }
    }

    const poster = createPoster(this.assets?.textures?.spa, {
      name: "product-campaign-board",
      size: [2.4, 1.45],
      position: [-2.9, 1.3, 1.75],
      rotationY: Math.PI / 2,
      fallbackMaterial: this.materials.screen
    });

    const label = createLabelSprite("商品区", { accent: "#a78bfa" });
    label.position.set(0, 1.92, -2.25);
    this.group.add(poster, label, createAreaLight({ color: 0xbca4ff, intensity: 1.8, position: [0, 2.2, 0], distance: 7 }));
    return this.group;
  }
}
