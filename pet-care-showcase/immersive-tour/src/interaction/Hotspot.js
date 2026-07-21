import * as THREE from "three";
import { createLabelSprite } from "../world/primitives.js";

export class Hotspot {
  constructor(data, { profile }) {
    this.data = data;
    this.profile = profile;
    this.group = new THREE.Group();
    this.group.name = `Hotspot-${data.id}`;
    this.group.position.set(data.position[0], data.position[1], data.position[2]);
    this.clickTargets = [];

    const accent = new THREE.Color(data.color || 0x52d69a);
    const ringGeometry = new THREE.TorusGeometry(0.32, 0.026, 12, 52);
    const ringMaterial = new THREE.MeshBasicMaterial({
      color: 0xf5fff9,
      transparent: true,
      opacity: 0.96,
      depthTest: false,
      depthWrite: false
    });
    this.ring = new THREE.Mesh(ringGeometry, ringMaterial);
    this.ring.name = `${data.id}-ring`;

    const coreGeometry = new THREE.SphereGeometry(0.12, 20, 14);
    const coreMaterial = new THREE.MeshBasicMaterial({
      color: accent,
      transparent: true,
      opacity: 0.92,
      depthTest: false,
      depthWrite: false
    });
    this.core = new THREE.Mesh(coreGeometry, coreMaterial);
    this.core.name = `${data.id}-core`;

    const haloGeometry = new THREE.RingGeometry(0.42, 0.5, 52);
    const haloMaterial = new THREE.MeshBasicMaterial({
      color: accent,
      transparent: true,
      opacity: 0.36,
      side: THREE.DoubleSide,
      depthTest: false,
      depthWrite: false
    });
    this.halo = new THREE.Mesh(haloGeometry, haloMaterial);
    this.halo.name = `${data.id}-halo`;

    this.label = createLabelSprite(data.label, {
      accent: `#${accent.getHexString()}`,
      scale: [1.65, 0.36, 1]
    });
    this.label.position.set(0, 0.48, 0);
    this.label.material.depthTest = false;
    this.label.renderOrder = 10;

    for (const target of [this.ring, this.core, this.halo]) {
      target.userData.hotspot = this;
      target.renderOrder = 9;
      this.clickTargets.push(target);
    }

    this.group.add(this.halo, this.ring, this.core, this.label);
    this.setActive(true);
  }

  update(_delta, elapsed, camera) {
    this.group.lookAt(camera.position);
    if (this.profile.reducedMotion) return;

    const pulse = (Math.sin(elapsed * 2.6 + this.group.position.x) + 1) * 0.5;
    this.ring.scale.setScalar(1 + pulse * 0.11);
    this.halo.scale.setScalar(1.1 + pulse * 0.32);
    this.halo.material.opacity = 0.18 + pulse * 0.16;
  }

  setHovered(isHovered) {
    this.ring.material.opacity = isHovered ? 1 : 0.9;
    this.core.scale.setScalar(isHovered ? 1.22 : 1);
  }

  setActive(isActive) {
    this.group.visible = isActive;
    for (const target of this.clickTargets) {
      target.raycast = isActive ? THREE.Mesh.prototype.raycast : () => {};
    }
  }
}
