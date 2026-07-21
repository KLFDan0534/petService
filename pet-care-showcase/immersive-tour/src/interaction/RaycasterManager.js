import * as THREE from "three";

export class RaycasterManager {
  constructor({ camera, domElement, onSelect = () => {} }) {
    this.camera = camera;
    this.domElement = domElement;
    this.onSelect = onSelect;
    this.raycaster = new THREE.Raycaster();
    this.pointer = new THREE.Vector2();
    this.targets = [];
    this.hovered = null;

    this.domElement.addEventListener("pointermove", this.handlePointerMove, { passive: true });
    this.domElement.addEventListener("click", this.handleClick);
  }

  setTargets(targets) {
    this.targets = targets;
  }

  handlePointerMove = (event) => {
    const hotspot = this.pick(event);
    if (hotspot === this.hovered) return;

    if (this.hovered) this.hovered.setHovered(false);
    this.hovered = hotspot;
    if (this.hovered) this.hovered.setHovered(true);
    this.domElement.style.cursor = hotspot ? "pointer" : "";
  };

  handleClick = (event) => {
    const hotspot = this.pick(event);
    if (hotspot) {
      this.onSelect(hotspot.data);
    }
  };

  pick(event) {
    if (!this.targets.length) return null;

    const rect = this.domElement.getBoundingClientRect();
    this.pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    this.pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;
    this.raycaster.setFromCamera(this.pointer, this.camera);

    const hits = this.raycaster.intersectObjects(this.targets, false);
    if (!hits.length) return null;
    return hits[0].object.userData.hotspot || null;
  }

  dispose() {
    this.domElement.removeEventListener("pointermove", this.handlePointerMove);
    this.domElement.removeEventListener("click", this.handleClick);
  }
}
