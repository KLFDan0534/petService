# PetService Immersive Tour

独立的 Web 3D 数字孪生展示页，入口目录为 `pet-care-showcase/immersive-tour/`。它不会修改主项目页面。

## 当前空间

页面按五个房间构建滚动镜头时间线：

- `H01 日托训练大厅`
- `H02 洗护水疗房`
- `H03 安静寄养房`
- `H04 猫咪跃层乐园`
- `H05 小动物温室`

没有真实 GLB 时，页面会自动使用可维护的程序化 Three.js 房间模型。模型包含独立坐标、灯光、材质、热点和运营 HUD。

## 真实模型替换

把 Blender 导出的 GLB 放到 `assets/models/`，推荐命名：

```text
assets/models/daycare-training-hall.glb
assets/models/spa-grooming-house.glb
assets/models/boarding-kennel-lodge.glb
assets/models/cat-enrichment-loft.glb
assets/models/small-animal-conservatory.glb
```

也可以在 `assets/models/manifest.json` 中声明自定义路径：

```json
{
  "center": null,
  "areas": {
    "daycare": "assets/models/daycare-training-hall.glb",
    "spa": "assets/models/spa-grooming-house.glb",
    "boarding": "assets/models/boarding-kennel-lodge.glb",
    "catloft": "assets/models/cat-enrichment-loft.glb",
    "conservatory": "assets/models/small-animal-conservatory.glb"
  }
}
```

如果某个 GLB 不存在，加载器会跳过该房间并保留程序化占位模型。

## 本地运行

```bash
npm run serve
```

然后打开：

```text
http://127.0.0.1:4177/
```

不要直接双击 `index.html`。这个页面使用 ES Modules、import map、GLTFLoader 和本地 Three.js 模块，浏览器在 `file://` 模式下会拦截模块或资源请求，表现通常是 Loading 停在 0%。

## 验收点

- Loading 页面有项目名、Logo、百分比和阶段文案。
- Loading 完成后相机从空中进入五房间养护中心。
- 滚动控制相机时间线，不切换普通网页段落。
- 五个房间都有独立模型节点、灯光、材质和热点。
- 地面没有旧版发光导航路线。
- H05 温室作为最后一个滚动点，热点位于房间内部可见位置。
- 支持后续替换真实 GLB。
- 桌面启用后期效果，移动端自动降低像素比、阴影和后期质量。
